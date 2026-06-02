package com.library.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.library.dto.BorrowDTO;
import com.library.entity.BorrowRecord;

import java.util.List;

public interface BorrowService extends IService<BorrowRecord> {
    /** 借书 */
    boolean borrowBook(BorrowDTO dto);
    /** 还书 */
    boolean returnBook(Long recordId);
    /** 查询所有借阅记录（含关联信息） */
    List<BorrowDTO> listWithDetails();
    /** 查询即将到期的记录 */
    List<BorrowDTO> getUpcomingDueRecords();
}
