package com.library.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.library.dto.BorrowDTO;
import com.library.entity.Book;
import com.library.entity.BorrowRecord;
import com.library.mapper.BookMapper;
import com.library.mapper.BorrowRecordMapper;
import com.library.service.BorrowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BorrowServiceImpl extends ServiceImpl<BorrowRecordMapper, BorrowRecord> implements BorrowService {

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Override
    @Transactional
    public boolean borrowBook(BorrowDTO dto) {
        // 检查图书是否存在且可借
        Book book = bookMapper.selectById(dto.getBookId());
        if (book == null) {
            throw new RuntimeException("图书不存在");
        }
        if (book.getAvailableCount() <= 0) {
            throw new RuntimeException("该书已全部借出，无可借数量");
        }

        // 创建借阅记录
        BorrowRecord record = new BorrowRecord();
        record.setUserId(dto.getUserId());
        record.setBookId(dto.getBookId());
        record.setBorrowDate(LocalDateTime.now());
        record.setDueDate(LocalDateTime.now().plusDays(dto.getBorrowDays()));
        record.setStatus(0); // 借阅中
        save(record);

        // 扣减可借数量
        book.setAvailableCount(book.getAvailableCount() - 1);
        bookMapper.updateById(book);

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
        record.setStatus(1); // 已归还
        record.setReturnDate(LocalDateTime.now());
        updateById(record);

        // 恢复可借数量
        Book book = bookMapper.selectById(record.getBookId());
        if (book != null) {
            book.setAvailableCount(book.getAvailableCount() + 1);
            bookMapper.updateById(book);
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
}
