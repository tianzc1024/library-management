package com.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.dto.BorrowDTO;
import com.library.service.BorrowService;
import com.library.service.ReminderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BorrowController API 接口测试
 * <p>
 * 使用 @SpringBootTest + @AutoConfigureMockMvc 替代 @WebMvcTest，
 * 避免 MyBatis-Plus 自动配置与 @WebMvcTest 的兼容性问题。
 * 通过 @MockBean 模拟所有 Service 层依赖，专注于 Controller 层行为验证。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class BorrowControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private BorrowService borrowService;
    @MockBean private ReminderService reminderService;

    @Nested
    @DisplayName("POST /api/borrow/borrow - 借书")
    class BorrowBook {

        @Test
        @DisplayName("借书成功 → 返回 code=200")
        void shouldBorrowSuccessfully() throws Exception {
            when(borrowService.borrowBook(any(BorrowDTO.class))).thenReturn(true);

            BorrowDTO dto = new BorrowDTO();
            dto.setUserId(1L);
            dto.setBookId(1L);
            dto.setBorrowDays(30);

            mockMvc.perform(post("/api/borrow/borrow")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("借阅成功"));
        }

        @Test
        @DisplayName("借书失败(库存不足) → 返回 code=500")
        void shouldFailWhenNoStock() throws Exception {
            when(borrowService.borrowBook(any(BorrowDTO.class)))
                    .thenThrow(new RuntimeException("该书已全部借出，无可借数量"));

            BorrowDTO dto = new BorrowDTO();
            dto.setUserId(1L);
            dto.setBookId(1L);
            dto.setBorrowDays(30);

            mockMvc.perform(post("/api/borrow/borrow")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500))
                    .andExpect(jsonPath("$.message").value("该书已全部借出，无可借数量"));
        }
    }

    @Nested
    @DisplayName("POST /api/borrow/return/{recordId} - 还书")
    class ReturnBook {

        @Test
        @DisplayName("还书成功 → 返回 code=200")
        void shouldReturnSuccessfully() throws Exception {
            when(borrowService.returnBook(anyLong())).thenReturn(true);

            mockMvc.perform(post("/api/borrow/return/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("归还成功"));
        }

        @Test
        @DisplayName("重复还书 → 返回 code=500")
        void shouldFailWhenAlreadyReturned() throws Exception {
            when(borrowService.returnBook(anyLong()))
                    .thenThrow(new RuntimeException("该记录已归还"));

            mockMvc.perform(post("/api/borrow/return/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500))
                    .andExpect(jsonPath("$.message").value("该记录已归还"));
        }
    }

    @Nested
    @DisplayName("GET /api/borrow/list - 借阅记录列表")
    class ListBorrows {

        @Test
        @DisplayName("查询成功 → 返回记录列表")
        void shouldReturnBorrowList() throws Exception {
            BorrowDTO dto = new BorrowDTO();
            dto.setRecordId(1L);
            dto.setUserName("张三");
            dto.setBookName("Spring实战");
            dto.setStatus(0);
            dto.setRemainingDays(25L);

            when(borrowService.listWithDetails()).thenReturn(Arrays.asList(dto));

            mockMvc.perform(get("/api/borrow/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data[0].userName").value("张三"))
                    .andExpect(jsonPath("$.data[0].bookName").value("Spring实战"))
                    .andExpect(jsonPath("$.data[0].remainingDays").value(25));
        }

        @Test
        @DisplayName("空列表 → 返回空数组")
        void shouldReturnEmptyList() throws Exception {
            when(borrowService.listWithDetails()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/borrow/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/borrow/reminders - 归还提醒")
    class Reminders {

        @Test
        @DisplayName("有逾期记录 → 返回提醒列表")
        void shouldReturnReminders() throws Exception {
            BorrowDTO overdue = new BorrowDTO();
            overdue.setRecordId(10L);
            overdue.setUserName("张三");
            overdue.setBookName("Spring实战");
            overdue.setRemainingDays(-5L);

            when(reminderService.updateOverdueStatus()).thenReturn(2);
            when(reminderService.getReminderList()).thenReturn(Arrays.asList(overdue));

            mockMvc.perform(get("/api/borrow/reminders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data[0].remainingDays").value(-5));
        }

        @Test
        @DisplayName("无提醒 → 返回空列表")
        void shouldReturnEmptyReminders() throws Exception {
            when(reminderService.updateOverdueStatus()).thenReturn(0);
            when(reminderService.getReminderList()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/borrow/reminders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }
}
