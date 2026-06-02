package com.library.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.library.entity.User;

public interface UserService extends IService<User> {
    boolean addUser(User user);
    boolean updateUser(User user);
    boolean deleteUser(Long id);
}
