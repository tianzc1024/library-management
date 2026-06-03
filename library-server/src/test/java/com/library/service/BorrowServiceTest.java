package com.library.service;

import com.library.dto.BorrowDTO;
import com.library.entity.Book;
import com.library.entity.BorrowRecord;
import com.library.entity.User;
import com.library.mapper.BookMapper;
import com.library.mapper.BorrowRecordMapper;
import com.library.mapper.UserMapper;
import com.library.service.impl.BorrowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * BorrowService 单元测试 —— 使用 Mockito 隔离测试借阅/归还核心逻辑
 *
 * 注意：BorrowServiceImpl extends ServiceImpl<BorrowRecordMapper, BorrowRecord>，
 * 其父类 ServiceImpl 中的 baseMapper 字段无法通过 @InjectMocks 自动注入，
 * 因此手动创建 service 实例并通过 ReflectionTestUtils 注入所有依赖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BorrowServiceTest {

    @Mock private BookMapper bookMapper;
    @Mock private BorrowRecordMapper borrowRecordMapper;
    @Mock private UserMapper userMapper;

    private BorrowServiceImpl borrowService;

    private Book testBook;
    private User testUser;
    private BorrowDTO borrowDTO;
    private BorrowRecord testRecord;

    @BeforeEach
    void setUp() {
        borrowService = new BorrowServiceImpl();

        ReflectionTestUtils.setField(borrowService, "bookMapper", bookMapper);
        ReflectionTestUtils.setField(borrowService, "borrowRecordMapper", borrowRecordMapper);
        ReflectionTestUtils.setField(borrowService, "userMapper", userMapper);
        ReflectionTestUtils.setField(borrowService, "baseMapper", borrowRecordMapper);

        when(borrowRecordMapper.insert(any(BorrowRecord.class))).thenReturn(1);
        when(borrowRecordMapper.updateById(any(BorrowRecord.class))).thenReturn(1);

        testBook = new Book();
        testBook.setId(1L);
        testBook.setName("测试图书");
        testBook.setAuthor("测试作者");
        testBook.setTotalCount(5);
        testBook.setAvailableCount(5);

        testUser = new User();
        testUser.setId(1L);
        testUser.setName("测试用户");
        testUser.setPhone("13800001111");

        borrowDTO = new BorrowDTO();
        borrowDTO.setUserId(1L);
        borrowDTO.setBookId(1L);
        borrowDTO.setBorrowDays(30);

        testRecord = new BorrowRecord();
        testRecord.setId(1L);
        testRecord.setUserId(1L);
        testRecord.setBookId(1L);
        testRecord.setBorrowDate(LocalDateTime.now().minusDays(5));
        testRecord.setDueDate(LocalDateTime.now().plusDays(25));
        testRecord.setStatus(0);
    }

    // ==================== 借书测试 ====================

    @Nested
    @DisplayName("借书流程")
    class BorrowBook {

        @Test
        @DisplayName("正常借书: 可借数量扣减，借阅记录创建")
        void shouldBorrowSuccessfully() {
            when(bookMapper.selectById(1L)).thenReturn(testBook);
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(bookMapper.decrementAvailableCount(1L)).thenReturn(1);

            boolean result = borrowService.borrowBook(borrowDTO);

            assertTrue(result, "借阅应成功");
            verify(bookMapper).decrementAvailableCount(1L);
        }

        @Test
        @DisplayName("借书: 图书不存在 → 抛异常")
        void shouldThrowWhenBookNotFound() {
            when(userMapper.selectById(99L)).thenReturn(testUser);
            when(bookMapper.selectById(99L)).thenReturn(null);

            borrowDTO.setBookId(99L);
            borrowDTO.setUserId(99L);
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> borrowService.borrowBook(borrowDTO));
            assertEquals("图书不存在", ex.getMessage());

            verify(bookMapper, never()).decrementAvailableCount(any());
        }

        @Test
        @DisplayName("借书: 可借数量为0 → 抛异常（decrementAvailableCount返回0）")
        void shouldThrowWhenNoAvailable() {
            when(bookMapper.selectById(1L)).thenReturn(testBook);
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(bookMapper.decrementAvailableCount(1L)).thenReturn(0);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> borrowService.borrowBook(borrowDTO));
            assertTrue(ex.getMessage().contains("已全部借出") || ex.getMessage().contains("无可借"),
                    "异常消息应包含库存不足提示");
        }

        @Test
        @DisplayName("借书: 用户不存在 → 抛异常")
        void shouldThrowWhenUserNotFound() {
            when(userMapper.selectById(1L)).thenReturn(null);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> borrowService.borrowBook(borrowDTO));
            assertEquals("借阅用户不存在", ex.getMessage());
        }

        @Test
        @DisplayName("借书: 验证借阅天数 → dueDate = borrowDate + days")
        void shouldSetCorrectDueDate() {
            when(bookMapper.selectById(1L)).thenReturn(testBook);
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(bookMapper.decrementAvailableCount(1L)).thenReturn(1);
            borrowDTO.setBorrowDays(15);

            borrowService.borrowBook(borrowDTO);

            verify(borrowRecordMapper).insert(any(BorrowRecord.class));
            verify(bookMapper).decrementAvailableCount(1L);
        }
    }

    // ==================== 还书测试 ====================

    @Nested
    @DisplayName("还书流程")
    class ReturnBook {

        @Test
        @DisplayName("正常还书: 状态变为已归还，可借数量恢复")
        void shouldReturnSuccessfully() {
            BorrowRecord record = new BorrowRecord();
            record.setId(100L);
            record.setUserId(1L);
            record.setBookId(1L);
            record.setBorrowDate(LocalDateTime.now().minusDays(5));
            record.setDueDate(LocalDateTime.now().plusDays(25));
            record.setStatus(0);

            when(borrowRecordMapper.selectById(100L)).thenReturn(record);
            when(bookMapper.incrementAvailableCount(1L)).thenReturn(1);

            boolean result = borrowService.returnBook(100L);
            assertTrue(result, "归还应成功");

            ArgumentCaptor<BorrowRecord> recordCaptor = ArgumentCaptor.forClass(BorrowRecord.class);
            verify(borrowRecordMapper).updateById(recordCaptor.capture());
            BorrowRecord updated = recordCaptor.getValue();
            assertEquals(1, updated.getStatus(), "状态应为已归还(1)");
            assertNotNull(updated.getReturnDate(), "归还日期应被设置");

            verify(bookMapper).incrementAvailableCount(1L);
        }

        @Test
        @DisplayName("还书: 记录不存在 → 抛异常")
        void shouldThrowWhenRecordNotFound() {
            when(borrowRecordMapper.selectById(999L)).thenReturn(null);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> borrowService.returnBook(999L));
            assertEquals("借阅记录不存在", ex.getMessage());
        }

        @Test
        @DisplayName("还书: 已归还 → 抛异常")
        void shouldThrowWhenAlreadyReturned() {
            BorrowRecord returnedRecord = new BorrowRecord();
            returnedRecord.setId(100L);
            returnedRecord.setStatus(1);
            when(borrowRecordMapper.selectById(100L)).thenReturn(returnedRecord);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> borrowService.returnBook(100L));
            assertEquals("该记录已归还", ex.getMessage());
        }

        @Test
        @DisplayName("还书: 逾期后归还也能成功 (status=2)")
        void shouldReturnOverdueBook() {
            BorrowRecord overdueRecord = new BorrowRecord();
            overdueRecord.setId(100L);
            overdueRecord.setUserId(1L);
            overdueRecord.setBookId(1L);
            overdueRecord.setStatus(2);
            when(borrowRecordMapper.selectById(100L)).thenReturn(overdueRecord);
            when(bookMapper.incrementAvailableCount(1L)).thenReturn(1);

            boolean result = borrowService.returnBook(100L);
            assertTrue(result, "逾期后归还应成功");
        }
    }

    // ==================== 查询测试 ====================

    @Nested
    @DisplayName("查询功能")
    class Query {

        @Test
        @DisplayName("查询所有借阅记录应返回关联的用户名和书名")
        void shouldReturnRecordsWithDetails() {
            BorrowDTO dto = new BorrowDTO();
            dto.setRecordId(1L);
            dto.setUserName("张三");
            dto.setBookName("Spring实战");
            dto.setStatus(0);
            dto.setRemainingDays(25L);

            when(borrowRecordMapper.selectAllWithDetails()).thenReturn(Arrays.asList(dto));

            List<BorrowDTO> result = borrowService.listWithDetails();

            assertEquals(1, result.size());
            BorrowDTO r = result.get(0);
            assertEquals("张三", r.getUserName());
            assertEquals("Spring实战", r.getBookName());
            assertEquals(25L, r.getRemainingDays());
        }

        @Test
        @DisplayName("查询即将到期记录")
        void shouldReturnUpcomingDueRecords() {
            BorrowDTO dto = new BorrowDTO();
            dto.setRecordId(1L);
            dto.setRemainingDays(2L);

            when(borrowRecordMapper.selectUpcomingDueRecords()).thenReturn(Arrays.asList(dto));

            List<BorrowDTO> result = borrowService.getUpcomingDueRecords();

            assertEquals(1, result.size());
            assertEquals(2L, result.get(0).getRemainingDays());
        }
    }
}
