package com.library.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.library.entity.Book;
import com.library.mapper.BookMapper;
import com.library.service.BookService;
import org.springframework.stereotype.Service;

@Service
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {

    @Override
    public boolean addBook(Book book) {
        // 始终将可借数量设为总数量，防止客户端传入不一致的值
        if (book.getTotalCount() == null) {
            book.setTotalCount(0);
        }
        book.setAvailableCount(book.getTotalCount());
        return save(book);
    }

    @Override
    public boolean updateBook(Book book) {
        return updateById(book);
    }

    @Override
    public boolean deleteBook(Long id) {
        return removeById(id);
    }
}
