package com.zjy.seckill.controller;


import com.alibaba.druid.util.StringUtils;
import com.zjy.seckill.controller.viewobject.UserVO;
import com.zjy.seckill.error.BusinessException;
import com.zjy.seckill.error.EmBusinessError;
import com.zjy.seckill.response.CommonReturnType;
import com.zjy.seckill.service.UserService;
import com.zjy.seckill.service.model.UserModel;
import io.netty.handler.codec.base64.Base64Encoder;
import org.apache.tomcat.util.security.MD5Encoder;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import sun.misc.BASE64Encoder;
import sun.security.provider.MD5;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

@RestController("user")
@RequestMapping("/user")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")
public class UserController extends BaseController {

    @Resource
    private UserService userService;

    //重点！理论上来说这样注入的，是单例模式，但是这个通过了ThreadLoacl的多线程处理，还拥有ThreadLocal清除机制，本质是proxy
    @Resource
    private HttpServletRequest httpServletRequest;

    //用户注册接口
    @PostMapping(value = "/register", consumes = {CONTENT_TYPE_FORMED})
    public CommonReturnType register(@RequestParam("telPhone") String telPhone,
                                     @RequestParam("otpCode")  String otpCode,
                                     @RequestParam("name")     String name,
                                     @RequestParam("gender")   Integer gender,
                                     @RequestParam("age")      Integer age,
                                     @RequestParam("password") String password) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {
        //验证手机号和对应的otpCode
        String inSessionOtpCode = (String) httpServletRequest.getSession().getAttribute(telPhone);
        //这里是阿里巴巴的StringUtils
        if (!StringUtils.equals(otpCode, inSessionOtpCode)) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "短信验证码不符合");
        }
        //进入用户的注册流程
        UserModel userModel = new UserModel();
        userModel.setName(name);
        userModel.setGender(new Byte(String.valueOf(gender.intValue())));
        userModel.setAge(age);
        userModel.setTelphone(telPhone);
        userModel.setRegisterMode("byphone");
        //对明文密码进行加密
        userModel.setEncrptPassword(encodeByMd5(password));
        userService.register(userModel);
        return CommonReturnType.create(null);
    }

    @RequestMapping(value = "/getOtp", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    public CommonReturnType getOtp(@RequestParam("telPhone") String telPhone) {
        //按照一定的规则生成验证码
        Random random = new Random();
        int randomInt = random.nextInt(99999);
        randomInt += 10000;
        String otpCode = String.valueOf(randomInt);

        //将opt验证码同对应的手机号关联,使用httpSession的方式去绑定
        httpServletRequest.getSession().setAttribute(telPhone, otpCode);

        //将opt验证码通过短信发送给用户，省略(可以买第三方短信服务通道)
        System.out.println("telPhone = " + telPhone + " —— otpCOde：" + otpCode);
        return CommonReturnType.create(null);
    }

    @RequestMapping("/get")
    public CommonReturnType getUser(@RequestParam("id") Integer id) throws BusinessException {
        //调用service服务获取对应id的用户，并返回给前端
        UserModel userModel = userService.getUserById(id);

        //若获取的用户信息不存在
        if (userModel == null) {
            throw new BusinessException(EmBusinessError.USER_NOT_EXIST);
        }
        //将核心领域模型用户对象转化为可供UI使用的viewobject
        UserVO userVO = convertFromModel(userModel);

        //返回通用对象
        return CommonReturnType.create(userVO);
    }

    private String encodeByMd5(String password) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        //确定计算方法
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        BASE64Encoder base64Encoder = new BASE64Encoder();
        //加密字符串
        String newPassword = base64Encoder.encode(md5.digest(password.getBytes("utf-8")));
        return newPassword;
    }

    private UserVO convertFromModel(UserModel userModel) {
        if (userModel == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userModel, userVO);
        return userVO;
    }

}
