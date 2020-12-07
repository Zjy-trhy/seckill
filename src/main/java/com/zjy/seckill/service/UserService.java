package com.zjy.seckill.service;

import com.zjy.seckill.error.BusinessException;
import com.zjy.seckill.service.model.UserModel;
import org.apache.catalina.User;

public interface UserService {

    void register(UserModel userModel) throws BusinessException;

    /**
     * 通过用户ID获取用户
     * @param id
     */
    UserModel getUserById(Integer id);
}
