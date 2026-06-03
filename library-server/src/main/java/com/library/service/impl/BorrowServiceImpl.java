package com.library.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.library.dto.BorrowDTO;
import com.library.entity.Book;
import com.library.entity.BorrowRecord;
import com.library.entity.User;
import com.library.mapper.BookMapper;
import com.library.mapper.BorrowRecordMapper;
import com.library.mapper.UserMapper;
import com.library.service.BorrowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BorrowServiceImpl extends ServiceImpl<BorrowRecordMapper, BorrowRecord> implements BorrowService {

    private static final Logger log = LoggerFactory.getLogger(BorrowServiceImpl.class);

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional
    public boolean borrowBook(BorrowDTO dto) {
        // 校验borrowDays
        if (dto.getBorrowDays() == null || dto.getBorrowDays() <= 0) {
            throw new RuntimeException("借阅天数必须为正整数");
        }

        // 校验用户是否存在
        User user = userMapper.selectById(dto.getUserId());
        if (user == null) {
            throw new RuntimeException("借阅用户不存在");
        }

        // 检查图书是否存在
        Book book = bookMapper.selectById(dto.getBookId());
        if (book == null) {
            throw new RuntimeException("图书不存在");
        }

        // 原子扣减库存
        int affected = bookMapper.decrementAvailableCount(dto.getBookId());
        if (affected == 0) {
            throw new RuntimeException("该书已全部借出，无可借数量");
        }

        // 创建借阅记录
        BorrowRecord record = new BorrowRecord();
        record.setUserId(dto.getUserId());
        record.setBookId(dto.getBookId());
        record.setBorrowDate(LocalDateTime.now());
        record.setDueDate(LocalDateTime.now().plusDays(dto.getBorrowDays()));
        record.setStatus(0);
        save(record);

        return true;
    }

    @Override
    @Transactional
    public boolean returnBook(Long recordId) {
        BorrowRecord record = getById(recordId);
        if (record == null) {
            throw new RuntimeException("借阅记录不存在");
        }
        if (record.getStatus() != 0 && record.getStatus() != 2) {
            throw new RuntimeException("该记录已归还");
        }

        // 更新归还状态
        record.setStatus(1);
        record.setReturnDate(LocalDateTime.now());
        updateById(record);

        // 原子恢复可借数量；图书不存在时记录告警日志
        int affected = bookMapper.incrementAvailableCount(record.getBookId());
        if (affected == 0) {
            log.warn("归还操作：图书(id={})不存在，可借数量未恢复。借阅记录id={}",
                    record.getBookId(), recordId);
        }

        return true;
    }

    @Override
    public List<BorrowDTO> listWithDetails() {
        return borrowRecordMapper.selectAllWithDetails();
    }

    @Override
    public List<BorrowDTO> getUpcomingDueRecords() {
        return borrowRecordMapper.selectUpcomingDueRecords();
    }

    @Override
    public List<BorrowDTO> getRecordsByUserId(Long userId) {
        return borrowRecordMapper.selectByUserId(userId);
    }

    @Override
    public long countBorrowing() {
        return borrowRecordMapper.countBorrowing();
    }
}
