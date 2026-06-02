package com.library.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.library.entity.SystemUser;

public interface SystemUserService extends IService<SystemUser> {
    SystemUser findByUsername(String username);
}
