package com.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.dto.BorrowDTO;
import com.library.entity.BorrowRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface BorrowRecordMapper extends BaseMapper<BorrowRecord> {

    @Select("SELECT r.id as recordId, r.user_id as userId, r.book_id as bookId, " +
            "r.borrow_date as borrowDate, r.due_date as dueDate, r.return_date as returnDate, r.status, " +
            "u.name as userName, b.name as bookName, b.author as bookAuthor, b.isbn as bookIsbn, " +
            "DATEDIFF(r.due_date, NOW()) as remainingDays " +
            "FROM tb_borrow_record r " +
            "LEFT JOIN tb_user u ON r.user_id = u.id " +
            "LEFT JOIN tb_book b ON r.book_id = b.id " +
            "ORDER BY r.create_time DESC")
    List<BorrowDTO> selectAllWithDetails();

    @Select("SELECT r.id as recordId, r.user_id as userId, r.book_id as bookId, " +
            "r.borrow_date as borrowDate, r.due_date as dueDate, r.return_date as returnDate, r.status, " +
            "u.name as userName, b.name as bookName, b.author as bookAuthor, b.isbn as bookIsbn, " +
            "DATEDIFF(r.due_date, NOW()) as remainingDays " +
            "FROM tb_borrow_record r " +
            "LEFT JOIN tb_user u ON r.user_id = u.id " +
            "LEFT JOIN tb_book b ON r.book_id = b.id " +
            "WHERE r.status = 0 AND DATEDIFF(r.due_date, NOW()) <= 3 " +
            "ORDER BY remainingDays ASC")
    List<BorrowDTO> selectUpcomingDueRecords();

    @Update("UPDATE tb_borrow_record SET status = 2 " +
            "WHERE status = 0 AND due_date < NOW()")
    int updateOverdueRecords();

    @Select("SELECT r.id as recordId, r.user_id as userId, r.book_id as bookId, " +
            "r.borrow_date as borrowDate, r.due_date as dueDate, r.return_date as returnDate, r.status, " +
            "u.name as userName, b.name as bookName, b.author as bookAuthor, b.isbn as bookIsbn, " +
            "DATEDIFF(r.due_date, NOW()) as remainingDays " +
            "FROM tb_borrow_record r " +
            "LEFT JOIN tb_user u ON r.user_id = u.id " +
            "LEFT JOIN tb_book b ON r.book_id = b.id " +
            "WHERE r.user_id = #{userId} " +
            "ORDER BY r.create_time DESC")
    List<BorrowDTO> selectByUserId(@Param("userId") Long userId);

    /** 统计当前借阅中的记录数（status=0） */
    @Select("SELECT COUNT(*) FROM tb_borrow_record WHERE status = 0")
    long countBorrowing();
}
