package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_book")
public class Book {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String author;
    private String isbn;
    private String publisher;
    private String category;
    private Integer totalCount;
    private Integer availableCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
