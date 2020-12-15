package com.zjy.seckill.service;

import com.zjy.seckill.service.model.PromoModel;

public interface PromoService {

    //发布活动
    void publishPromo(Integer promoId);

    PromoModel getPromoByItemId(Integer itemId);
}
