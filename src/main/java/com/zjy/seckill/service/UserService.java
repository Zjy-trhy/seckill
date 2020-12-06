package com.zjy.seckill.service;

import com.zjy.seckill.service.model.UserModel;

public interface UserService {

    /**
     * 通过用户ID获取用户
     * @param id
     */
    UserModel getUserById(Integer id);
}
