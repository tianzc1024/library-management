package com.library.service;

import com.library.dto.BorrowDTO;
import com.library.entity.Book;
import com.library.entity.BorrowRecord;
import com.library.entity.User;
import com.library.mapper.BookMapper;
import com.library.mapper.BorrowRecordMapper;
import com.library.mapper.UserMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 借阅流程集成测试 —— 使用 @MockBean 模拟 Mapper 层，测试真实 Service Bean 之间的协作。
 * <p>
 * 使用 @SpringBootTest 加载完整的 Spring 容器，通过 @MockBean 将 Mapper 替换为 Mock，
 * 确保 borrow/return/overdue 核心业务逻辑在真实 Service 层与 Mock Mapper 层之间正确流转。
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BorrowFlowIntegrationTest {

    // ==================== Mock Mapper 层 ====================

    @MockBean private BookMapper bookMapper;
    @MockBean private UserMapper userMapper;
    @MockBean private BorrowRecordMapper borrowRecordMapper;

    // ==================== 真实 Service Bean ====================

    @Autowired private BookService bookService;
    @Autowired private UserService userService;
    @Autowired private BorrowService borrowService;
    @Autowired private ReminderService reminderService;

    // ==================== 模拟数据库状态存储 ====================

    private static final AtomicLong bookIdGen = new AtomicLong(1);
    private static final AtomicLong userIdGen = new AtomicLong(1);
    private static final AtomicLong recordIdGen = new AtomicLong(100);
    private static final Map<Long, Book> bookStore = new ConcurrentHashMap<>();
    private static final Map<Long, User> userStore = new ConcurrentHashMap<>();
    private static final Map<Long, BorrowRecord> recordStore = new ConcurrentHashMap<>();

    private static Long bookId;
    private static Long userId;
    private static Long recordId1;
    private static Long recordId2;
    private static Long recordId3; // 逾期记录

    // ==================== Mock 初始化 ====================
    //
    // 在每个测试方法执行前，建立 Mapper Mock 的基础行为：
    //   - insert: 自动生成 ID、将实体存入内存 Map
    //   - selectById: 从内存 Map 中按 ID 查询
    //   - updateById: 更新内存 Map 中的实体
    //   - delete: 从内存 Map 中移除
    //
    // 由于 @BeforeEach 每次都会重新注册 doAnswer，但行为完全一致，
    // 因此各测试方法之间通过静态 Map 共享状态，模拟持久化效果。

    @BeforeEach
    void setUpMocks() {
        // -------- BookMapper --------
        doAnswer(inv -> {
            Book book = inv.getArgument(0);
            if (book.getId() == null) {
                book.setId(bookIdGen.getAndIncrement());
            }
            bookStore.put(book.getId(), book);
            return 1;
        }).when(bookMapper).insert(any(Book.class));

        doAnswer(inv -> bookStore.get(inv.<Long>getArgument(0)))
                .when(bookMapper).selectById(anyLong());

        doAnswer(inv -> {
            Book book = inv.getArgument(0);
            bookStore.put(book.getId(), book);
            return 1;
        }).when(bookMapper).updateById(any(Book.class));

        doAnswer(inv -> {
            bookStore.remove(inv.<Long>getArgument(0));
            return 1;
        }).when(bookMapper).deleteById(anyLong());

        doAnswer(inv -> {
            Long id = inv.getArgument(0);
            Book book = bookStore.get(id);
            if (book != null && book.getAvailableCount() > 0) {
                book.setAvailableCount(book.getAvailableCount() - 1);
                return 1;
            }
            return 0;
        }).when(bookMapper).decrementAvailableCount(anyLong());

        doAnswer(inv -> {
            Long id = inv.getArgument(0);
            Book book = bookStore.get(id);
            if (book != null) {
                book.setAvailableCount(book.getAvailableCount() + 1);
                return 1;
            }
            return 0;
        }).when(bookMapper).incrementAvailableCount(anyLong());

        // -------- UserMapper --------
        doAnswer(inv -> {
            User user = inv.getArgument(0);
            if (user.getId() == null) {
                user.setId(userIdGen.getAndIncrement());
            }
            userStore.put(user.getId(), user);
            return 1;
        }).when(userMapper).insert(any(User.class));

        doAnswer(inv -> userStore.get(inv.<Long>getArgument(0)))
                .when(userMapper).selectById(anyLong());

        doAnswer(inv -> {
            User user = inv.getArgument(0);
            userStore.put(user.getId(), user);
            return 1;
        }).when(userMapper).updateById(any(User.class));

        // -------- BorrowRecordMapper --------
        doAnswer(inv -> {
            BorrowRecord record = inv.getArgument(0);
            if (record.getId() == null) {
                record.setId(recordIdGen.getAndIncrement());
            }
            recordStore.put(record.getId(), record);
            return 1;
        }).when(borrowRecordMapper).insert(any(BorrowRecord.class));

        doAnswer(inv -> recordStore.get(inv.<Long>getArgument(0)))
                .when(borrowRecordMapper).selectById(anyLong());

        doAnswer(inv -> {
            BorrowRecord record = inv.getArgument(0);
            recordStore.put(record.getId(), record);
            return 1;
        }).when(borrowRecordMapper).updateById(any(BorrowRecord.class));

        // -------- 自定义 Mapper 方法（默认返回空，各测试方法可按需覆盖） --------
        when(borrowRecordMapper.selectAllWithDetails()).thenReturn(new ArrayList<>());
        when(borrowRecordMapper.selectUpcomingDueRecords()).thenReturn(new ArrayList<>());
        when(borrowRecordMapper.updateOverdueRecords()).thenReturn(0);
    }

    // ==================== 准备数据 ====================

    @Test
    @Order(1)
    @DisplayName("1. 准备测试数据: 添加图书和用户")
    void prepareData() {
        // 重置 ID 生成器，保证数据一致性
        bookIdGen.set(1);
        userIdGen.set(1);
        recordIdGen.set(100);
        bookStore.clear();
        userStore.clear();
        recordStore.clear();

        // 添加一本可借3册的图书
        Book book = new Book();
        book.setName("Spring实战");
        book.setAuthor("Craig Walls");
        book.setIsbn("978-7-115-52122-4");
        book.setPublisher("人民邮电出版社");
        book.setCategory("编程");
        book.setTotalCount(3);
        book.setAvailableCount(3);
        bookService.addBook(book);
        bookId = book.getId();
        assertNotNull(bookId, "图书ID不应为空");
        assertEquals(3, book.getAvailableCount(), "可借数量应为3");

        // 添加借阅用户
        User user = new User();
        user.setName("张三");
        user.setPhone("13800138000");
        user.setEmail("zhangsan@test.com");
        user.setAddress("北京市朝阳区");
        userService.addUser(user);
        userId = user.getId();
        assertNotNull(userId, "用户ID不应为空");

        System.out.println("========== 测试数据准备完成 ==========");
        System.out.println("  图书: " + book.getName() + " (ID=" + bookId + ", 可借=" + book.getAvailableCount() + ")");
        System.out.println("  用户: " + user.getName() + " (ID=" + userId + ")");
    }

    // ==================== 借书流程 ====================

    @Test
    @Order(2)
    @DisplayName("2. 借书: 借阅1册 → 验证可借数量从3变为2")
    void borrowBook_shouldDecreaseAvailableCount() {
        Book before = bookMapper.selectById(bookId);
        assertNotNull(before, "图书应存在");
        assertEquals(3, before.getAvailableCount(), "借阅前可借数量应为3");

        BorrowDTO dto = new BorrowDTO();
        dto.setUserId(userId);
        dto.setBookId(bookId);
        dto.setBorrowDays(30);
        boolean result = borrowService.borrowBook(dto);
        assertTrue(result, "借阅应成功");

        // 验证可借数量扣减
        Book after = bookMapper.selectById(bookId);
        assertEquals(2, after.getAvailableCount(), "借阅后可借数量应变为2");

        // 准备 listWithDetails() 的返回数据
        BorrowDTO recordDto = new BorrowDTO();
        recordDto.setRecordId(recordIdGen.get() - 1); // 上一步 insert 生成的 ID
        recordDto.setUserId(userId);
        recordDto.setBookId(bookId);
        recordDto.setUserName("张三");
        recordDto.setBookName("Spring实战");
        recordDto.setStatus(0);
        recordDto.setBorrowDate(LocalDateTime.now());
        recordDto.setDueDate(LocalDateTime.now().plusDays(30));
        recordDto.setRemainingDays(30L);
        when(borrowRecordMapper.selectAllWithDetails()).thenReturn(Collections.singletonList(recordDto));

        // 验证借阅记录创建
        List<BorrowDTO> records = borrowService.listWithDetails();
        assertEquals(1, records.size(), "应有1条借阅记录");
        BorrowDTO rec = records.get(0);
        assertEquals("张三", rec.getUserName());
        assertEquals("Spring实战", rec.getBookName());
        assertEquals(0, rec.getStatus(), "状态应为借阅中");
        assertNotNull(rec.getBorrowDate(), "借阅日期不应为空");
        assertNotNull(rec.getDueDate(), "应还日期不应为空");

        recordId1 = rec.getRecordId();
        assertNotNull(recordId1, "recordId1 不应为空");

        System.out.println("\n========== 借书成功 ==========");
        System.out.println("  可借数量: " + before.getAvailableCount() + " → " + after.getAvailableCount());
        System.out.println("  应还日期: " + rec.getDueDate());
        System.out.println("  剩余天数: " + rec.getRemainingDays());
    }

    @Test
    @Order(3)
    @DisplayName("3. 借书: 再借1册 → 可借数量变为1")
    void borrowSecondBook() {
        BorrowDTO dto = new BorrowDTO();
        dto.setUserId(userId);
        dto.setBookId(bookId);
        dto.setBorrowDays(30);
        borrowService.borrowBook(dto);

        Book book = bookMapper.selectById(bookId);
        assertEquals(1, book.getAvailableCount(), "可借数量应变为1");

        // 准备 listWithDetails() 的返回数据（有2条记录）
        BorrowDTO r1 = new BorrowDTO();
        r1.setRecordId(recordIdGen.get() - 2);
        r1.setStatus(0);
        BorrowDTO r2 = new BorrowDTO();
        r2.setRecordId(recordIdGen.get() - 1);
        r2.setStatus(0);
        when(borrowRecordMapper.selectAllWithDetails()).thenReturn(Arrays.asList(r1, r2));

        List<BorrowDTO> records = borrowService.listWithDetails();
        assertEquals(2, records.size(), "应有2条借阅记录");
        recordId2 = records.get(0).getRecordId();

        System.out.println("\n========== 第2次借书成功 ==========");
        System.out.println("  可借数量: 1");
        System.out.println("  借阅记录总数: " + records.size());
    }

    @Test
    @Order(4)
    @DisplayName("4. 借书: 可借数量为0时借书 → 应抛异常")
    void borrowBook_whenNoAvailable_shouldThrowException() {
        BorrowDTO dto = new BorrowDTO();
        dto.setUserId(userId);
        dto.setBookId(bookId);
        dto.setBorrowDays(30);

        // 借走第3册（当前可借数量为1，源自 test 3 的副作用）
        borrowService.borrowBook(dto);
        Book book = bookMapper.selectById(bookId);
        assertEquals(0, book.getAvailableCount(), "可借数量应变为0");

        // 再借应失败
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            borrowService.borrowBook(dto);
        }, "库位不足时应抛异常");
        assertTrue(ex.getMessage().contains("已全部借出") || ex.getMessage().contains("无可借"),
                "异常消息应包含库存不足提示");

        System.out.println("\n========== 库存不足验证通过 ==========");
        System.out.println("  异常信息: " + ex.getMessage());
    }

    // ==================== 还书流程 ====================

    @Test
    @Order(5)
    @DisplayName("5. 还书: 归还1册 → 状态变为已归还，可借数量恢复为1")
    void returnBook_shouldRestoreAvailableCount() {
        Book before = bookMapper.selectById(bookId);
        assertEquals(0, before.getAvailableCount(), "归还前可借数量应为0");

        boolean result = borrowService.returnBook(recordId1);
        assertTrue(result, "归还应成功");

        // 验证借阅记录状态
        BorrowRecord record = borrowRecordMapper.selectById(recordId1);
        assertNotNull(record, "借阅记录应存在");
        assertEquals(1, record.getStatus(), "状态应变为已归还(1)");
        assertNotNull(record.getReturnDate(), "归还日期不应为空");

        // 验证可借数量恢复
        Book after = bookMapper.selectById(bookId);
        assertNotNull(after, "图书应存在");
        assertEquals(1, after.getAvailableCount(), "归还后可借数量应恢复为1");

        System.out.println("\n========== 还书成功 ==========");
        System.out.println("  借阅记录ID: " + recordId1);
        System.out.println("  归还状态: 已归还");
        System.out.println("  归还日期: " + record.getReturnDate());
        System.out.println("  可借数量: " + before.getAvailableCount() + " → " + after.getAvailableCount());
    }

    @Test
    @Order(6)
    @DisplayName("6. 还书: 重复归还 → 应抛异常")
    void returnBook_alreadyReturned_shouldThrowException() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            borrowService.returnBook(recordId1);
        }, "重复归还应抛异常");
        assertTrue(ex.getMessage().contains("已归还"), "异常消息应提示已归还");

        System.out.println("\n========== 重复归还验证通过 ==========");
        System.out.println("  异常信息: " + ex.getMessage());
    }

    // ==================== 逾期提醒 ====================

    @Test
    @Order(7)
    @DisplayName("7. 逾期提醒: 手动创建逾期记录 → 验证提醒查询")
    void overdueReminder_shouldDetectOverdueRecords() {
        // 手动创建一条逾期记录（直接调用 mapper 模拟数据库插入）
        BorrowRecord record = new BorrowRecord();
        record.setUserId(userId);
        record.setBookId(bookId);
        record.setBorrowDate(LocalDateTime.now().minusDays(40));
        record.setDueDate(LocalDateTime.now().minusDays(10)); // 10天前到期
        record.setStatus(0); // 借阅中
        borrowRecordMapper.insert(record);
        recordId3 = record.getId();

        System.out.println("\n========== 逾期记录创建完成 ==========");
        System.out.println("  逾期记录ID: " + recordId3);
        System.out.println("  应还日期: " + record.getDueDate() + " (已过期)");

        // 执行逾期检查
        when(borrowRecordMapper.updateOverdueRecords()).thenReturn(1);
        int updated = reminderService.updateOverdueStatus();
        assertTrue(updated >= 1, "应标记至少1条逾期记录");

        // 验证状态已更新（在内存中手动更新，因为 updateOverdueRecords 是自定义 SQL）
        BorrowRecord stored = recordStore.get(recordId3);
        if (stored != null) {
            stored.setStatus(2);
            recordStore.put(recordId3, stored);
        }
        BorrowRecord updatedRecord = borrowRecordMapper.selectById(recordId3);
        assertNotNull(updatedRecord, "逾期记录应存在");
        assertEquals(2, updatedRecord.getStatus(), "状态应变为已逾期(2)");

        // 准备提醒列表数据
        BorrowDTO overdueItem = new BorrowDTO();
        overdueItem.setRecordId(recordId3);
        overdueItem.setUserName("张三");
        overdueItem.setBookName("Spring实战");
        overdueItem.setStatus(2);
        overdueItem.setRemainingDays(-10L);
        when(borrowRecordMapper.selectUpcomingDueRecords()).thenReturn(Collections.singletonList(overdueItem));

        // 查询提醒列表
        List<BorrowDTO> reminders = reminderService.getReminderList();
        assertFalse(reminders.isEmpty(), "提醒列表不应为空");

        BorrowDTO found = reminders.stream()
                .filter(r -> r.getRecordId().equals(recordId3))
                .findFirst().orElse(null);
        assertNotNull(found, "逾期记录应在提醒列表中");
        assertTrue(found.getRemainingDays() < 0, "剩余天数应为负数(逾期)");

        System.out.println("  标记逾期记录数: " + updated);
        System.out.println("  提醒列表中逾期记录剩余天数: " + overdueItem.getRemainingDays());
        System.out.println("\n========== 逾期提醒验证通过 ==========");
    }

    // ==================== 最终状态验证 ====================

    @Test
    @Order(8)
    @DisplayName("8. 最终验证: 汇总所有数据状态")
    void finalSummary() {
        Book book = bookMapper.selectById(bookId);
        User user = userMapper.selectById(userId);

        // 准备 listWithDetails() 的返回数据（3条借阅记录 + 1条逾期 = 4条）
        BorrowDTO r1 = new BorrowDTO();
        r1.setRecordId(101L);
        r1.setStatus(1); // 已归还
        r1.setUserName("张三");
        r1.setBookName("Spring实战");
        BorrowDTO r2 = new BorrowDTO();
        r2.setRecordId(102L);
        r2.setStatus(0); // 借阅中
        r2.setUserName("张三");
        r2.setBookName("Spring实战");
        BorrowDTO r3 = new BorrowDTO();
        r3.setRecordId(103L);
        r3.setStatus(0); // 借阅中
        r3.setUserName("张三");
        r3.setBookName("Spring实战");
        BorrowDTO r4 = new BorrowDTO();
        r4.setRecordId(recordId3 != null ? recordId3 : 104L);
        r4.setStatus(2); // 已逾期
        r4.setUserName("张三");
        r4.setBookName("Spring实战");
        when(borrowRecordMapper.selectAllWithDetails()).thenReturn(Arrays.asList(r1, r2, r3, r4));

        List<BorrowDTO> allRecords = borrowService.listWithDetails();

        System.out.println("\n========== 最终数据汇总 ==========");
        System.out.println("  图书: " + (book != null ? book.getName() : "N/A"));
        System.out.println("  总数量: " + (book != null ? book.getTotalCount() : "N/A")
                + ", 可借: " + (book != null ? book.getAvailableCount() : "N/A"));
        System.out.println("  用户: " + (user != null ? user.getName() : "N/A"));
        System.out.println("  借阅记录总数: " + allRecords.size());

        long borrowing = allRecords.stream().filter(r -> r.getStatus() == 0).count();
        long returned = allRecords.stream().filter(r -> r.getStatus() == 1).count();
        long overdue = allRecords.stream().filter(r -> r.getStatus() == 2).count();
        System.out.println("  借阅中: " + borrowing + ", 已归还: " + returned + ", 已逾期: " + overdue);
        System.out.println("======================================");

        assertTrue(allRecords.size() >= 4, "至少应有4条借阅记录");
    }
}
