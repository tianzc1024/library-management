package com.library.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.library.entity.Book;

public interface BookService extends IService<Book> {
    boolean addBook(Book book);
    boolean updateBook(Book book);
    boolean deleteBook(Long id);
}
