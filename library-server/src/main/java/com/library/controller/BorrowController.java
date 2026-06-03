package com.library.controller;

import com.library.dto.BorrowDTO;
import com.library.service.BorrowService;
import com.library.service.ReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/borrow")
public class BorrowController {

    @Autowired
    private BorrowService borrowService;

    @Autowired
    private ReminderService reminderService;

    /**
     * 借书
     */
    @PostMapping("/borrow")
    public Map<String, Object> borrow(@RequestBody BorrowDTO dto) {
        Map<String, Object> result = new HashMap<>();
        try {
            borrowService.borrowBook(dto);
            result.put("code", 200);
            result.put("message", "借阅成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 还书
     */
    @PostMapping("/return/{recordId}")
    public Map<String, Object> returnBook(@PathVariable Long recordId) {
        Map<String, Object> result = new HashMap<>();
        try {
            borrowService.returnBook(recordId);
            result.put("code", 200);
            result.put("message", "归还成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 借阅记录列表
     */
    @GetMapping("/list")
    public Map<String, Object> list() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<BorrowDTO> list = borrowService.listWithDetails();
            result.put("code", 200);
            result.put("data", list);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 统计信息（当前借阅中数量）
     */
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> result = new HashMap<>();
        try {
            long borrowingCount = borrowService.countBorrowing();
            result.put("code", 200);
            result.put("data", new HashMap<String, Object>() {{
                put("borrowingCount", borrowingCount);
            }});
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 归还提醒列表（即将到期 + 已逾期）。
     * 注意：只做查询不产生副作用；逾期状态的实际更新仅由定时任务执行。
     */
    @GetMapping("/reminders")
    public Map<String, Object> reminders() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<BorrowDTO> list = reminderService.getReminderList();
            result.put("code", 200);
            result.put("data", list);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
