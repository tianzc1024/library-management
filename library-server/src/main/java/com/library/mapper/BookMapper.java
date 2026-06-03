package com.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.entity.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BookMapper extends BaseMapper<Book> {

    /** 原子扣减可借数量，返回受影响行数（0=库存不足） */
    @Update("UPDATE tb_book SET available_count = available_count - 1 " +
            "WHERE id = #{bookId} AND available_count > 0")
    int decrementAvailableCount(@Param("bookId") Long bookId);

    /** 原子增加可借数量 */
    @Update("UPDATE tb_book SET available_count = available_count + 1 " +
            "WHERE id = #{bookId}")
    int incrementAvailableCount(@Param("bookId") Long bookId);
}
