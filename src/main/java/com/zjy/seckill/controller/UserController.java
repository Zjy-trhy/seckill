package com.zjy.seckill.controller;


import com.zjy.seckill.controller.viewobject.UserVO;
import com.zjy.seckill.error.BusinessException;
import com.zjy.seckill.error.EmBusinessError;
import com.zjy.seckill.response.CommonReturnType;
import com.zjy.seckill.service.UserService;
import com.zjy.seckill.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Random;

@RestController("user")
@RequestMapping("/user")
@CrossOrigin
public class UserController extends BaseController{

    @Resource
    private UserService userService;

    //重点！理论上来说这样注入的，是单例模式，但是这个通过了ThreadLoacl的多线程处理，还拥有ThreadLocal清除机制，本质是proxy
    @Resource
    private HttpServletRequest httpServletRequest;

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

    private UserVO convertFromModel(UserModel userModel) {
        if (userModel == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userModel, userVO);
        return userVO;
    }

}
