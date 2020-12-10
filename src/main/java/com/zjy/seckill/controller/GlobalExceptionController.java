package com.zjy.seckill.controller;

import com.zjy.seckill.error.BusinessException;
import com.zjy.seckill.error.EmBusinessError;
import com.zjy.seckill.response.CommonReturnType;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

//controller切面的编程
@RestControllerAdvice
public class GlobalExceptionController {

    @ExceptionHandler(Exception.class)
    public CommonReturnType doError(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Exception exception) {

        exception.printStackTrace();
        Map<String, Object> responseData = new HashMap<>();

        if (exception instanceof BusinessException) {
            BusinessException businessException = (BusinessException) exception;
            responseData.put("errorCode", businessException.getErrCode());
            responseData.put("errorMsg", businessException.getErrMsg());
        } else if (exception instanceof ServletRequestBindingException) {
            responseData.put("errorCode", EmBusinessError.UNKNOWN_ERROR.getErrCode());
            responseData.put("errorMsg", "url绑定路由问题");
        } else if (exception instanceof NoHandlerFoundException) {
            responseData.put("errorCode", EmBusinessError.UNKNOWN_ERROR.getErrCode());
            responseData.put("errorMsg", "没有找到对应的访问路径");
        } else {
            responseData.put("errorCode", EmBusinessError.UNKNOWN_ERROR.getErrCode());
            responseData.put("errorMsg", exception.getMessage());
        }

        return CommonReturnType.create(responseData, "fail");
    }
}
