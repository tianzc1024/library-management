package com.library.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BorrowDTO {
    private Long userId;
    private Long bookId;
    /** 借阅天数，默认30天 */
    private Integer borrowDays = 30;
    private Long recordId;

    // 查询返回时携带关联信息
    private String userName;
    private String bookName;
    private String bookAuthor;
    private String bookIsbn;
    private LocalDateTime borrowDate;
    private LocalDateTime dueDate;
    private LocalDateTime returnDate;
    private Integer status;
    /** 剩余天数 */
    private Long remainingDays;
}
