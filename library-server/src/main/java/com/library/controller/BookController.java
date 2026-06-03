package com.library.controller;

import com.library.entity.Book;
import com.library.service.BookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/book")
public class BookController {

    private static final Logger log = LoggerFactory.getLogger(BookController.class);

    @Autowired
    private BookService bookService;

    @GetMapping("/list")
    public Map<String, Object> list() {
        List<Book> list = bookService.list();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", list);
        return result;
    }

    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody Book book) {
        Map<String, Object> result = new HashMap<>();
        try {
            bookService.addBook(book);
            result.put("code", 200);
            result.put("message", "添加成功");
        } catch (Exception e) {
            log.error("添加图书失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PutMapping("/update")
    public Map<String, Object> update(@RequestBody Book book) {
        Map<String, Object> result = new HashMap<>();
        try {
            bookService.updateBook(book);
            result.put("code", 200);
            result.put("message", "更新成功");
        } catch (Exception e) {
            log.error("更新图书失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean deleted = bookService.deleteBook(id);
            if (deleted) {
                result.put("code", 200);
                result.put("message", "删除成功");
            } else {
                result.put("code", 404);
                result.put("message", "图书不存在");
            }
        } catch (Exception e) {
            log.error("删除图书失败, id={}", id, e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", bookService.getById(id));
        return result;
    }
}
