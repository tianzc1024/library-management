package com.library.service.impl;

import com.library.dto.BorrowDTO;
import com.library.mapper.BorrowRecordMapper;
import com.library.service.ReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReminderServiceImpl implements ReminderService {

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Override
    public int updateOverdueStatus() {
        return borrowRecordMapper.updateOverdueRecords();
    }

    @Override
    public List<BorrowDTO> getReminderList() {
        return borrowRecordMapper.selectUpcomingDueRecords();
    }
}
