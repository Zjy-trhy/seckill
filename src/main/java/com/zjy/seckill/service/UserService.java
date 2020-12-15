package com.zjy.seckill.service;

import com.zjy.seckill.error.BusinessException;
import com.zjy.seckill.service.model.UserModel;
import org.apache.catalina.User;

public interface UserService {

    //通过缓存获取用户对象
    UserModel getUserByIdInCache(Integer id);

    /**
     * 用户登录接口
     * @param telPhone 用户账号
     * @param encryptPassword 用户输入的明文密码经过加密之后传入的加密密码
     * @return
     * @throws BusinessException
     */
    UserModel validateLogin(String telPhone, String encryptPassword) throws BusinessException;

    /**
     * 用户注册接口
     * @param userModel 用户提交的注册信息
     * @throws BusinessException
     */
    void register(UserModel userModel) throws BusinessException;

    /**
     * 通过用户ID获取用户
     * @param id
     */
    UserModel getUserById(Integer id);
}
