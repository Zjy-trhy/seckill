package com.zjy.seckill.service;

import com.zjy.seckill.service.model.PromoModel;

public interface PromoService {

    //生成秒杀令牌
    String generateSecondKillToken(Integer promoId, Integer itemId, Integer userId);

    //发布活动
    void publishPromo(Integer promoId);

    PromoModel getPromoByItemId(Integer itemId);
}
