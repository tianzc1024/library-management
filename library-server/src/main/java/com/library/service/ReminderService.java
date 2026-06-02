package com.library.service;

import com.library.dto.BorrowDTO;

import java.util.List;

public interface ReminderService {
    /** 更新逾期状态 */
    int updateOverdueStatus();
    /** 获取到期提醒列表 */
    List<BorrowDTO> getReminderList();
}
