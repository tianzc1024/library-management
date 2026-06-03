package com.library.controller;

import com.library.dto.BorrowDTO;
import com.library.service.BorrowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/borrower")
public class BorrowerController {

    private static final Logger log = LoggerFactory.getLogger(BorrowerController.class);

    @Autowired
    private BorrowService borrowService;

    @GetMapping("/my-records")
    public Map<String, Object> myRecords() {
        Map<String, Object> result = new HashMap<>();
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Long userId = null;
            if (auth != null && auth.getDetails() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> details = (Map<String, Object>) auth.getDetails();
                Object userIdObj = details.get("userId");
                if (userIdObj instanceof Integer) {
                    userId = ((Integer) userIdObj).longValue();
                } else if (userIdObj instanceof Long) {
                    userId = (Long) userIdObj;
                }
            }

            if (userId == null) {
                result.put("code", 401);
                result.put("message", "无法识别用户身份");
                return result;
            }

            List<BorrowDTO> records = borrowService.getRecordsByUserId(userId);
            result.put("code", 200);
            result.put("data", records);
        } catch (Exception e) {
            log.error("查询借阅记录失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
