package com.zjy.seckill.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;

@Configuration
public class MyMvcConfig implements WebMvcConfigurer {

    /**
     * 用于添加登录页面和首页的路径映射，避免了新写一个Controller
     * 这也是SpringMVC中传统的设置路径映射的方式，即一个类实现Handler接口
     * @param registry
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("index");
        registry.addViewController("/index.html").setViewName("index");
        registry.addViewController("/main.html").setViewName("dashboard");
    }

    /**
     * 添加拦截器
     * addPathPatterns("/**") 拦截了所有请求
     * 但是同时放行了ArrayList中的请求
     * @param registry
     */
//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//        registry.addInterceptor(new LoginHandlerInterceptor()).addPathPatterns("/**").excludePathPatterns(new ArrayList<String>() {{
//            add("/index.html");
//            add("/");
//            add("/user/login");
//            add("/asserts/**");
//        }});
//    }
}
