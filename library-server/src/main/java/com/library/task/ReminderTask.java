package com.library.task;

import com.library.service.ReminderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 归还提醒定时任务
 * 每天凌晨 2 点执行，自动将已逾期的借阅记录标记为"已逾期"状态
 */
@Slf4j
@Component
public class ReminderTask {

    @Autowired
    private ReminderService reminderService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void checkOverdueRecords() {
        log.info("开始执行借阅逾期检查...");
        try {
            int count = reminderService.updateOverdueStatus();
            log.info("逾期检查完成，更新了 {} 条逾期记录", count);
        } catch (Exception e) {
            log.error("逾期检查出错: {}", e.getMessage(), e);
        }
    }
}
