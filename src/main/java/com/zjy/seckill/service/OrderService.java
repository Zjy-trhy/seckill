package com.zjy.seckill.service;

import com.zjy.seckill.error.BusinessException;
import com.zjy.seckill.service.model.OrderModel;

public interface OrderService {

    //推荐使用这种方法1.通过前端url传过来的秒杀活动给id，然后在下单接口内校验对应id是否属于对应商品，且活动已经开始
    //2.直接在下单接口内判断对应的商品是否存在秒杀活动，若存在且正在进行，以秒杀价格下单
    OrderModel createOrder(Integer userId, Integer itemId, Integer promoId, Integer amount) throws BusinessException;
}
