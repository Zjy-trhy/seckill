package com.zjy.seckill.service;

import com.zjy.seckill.error.BusinessException;
import com.zjy.seckill.service.model.ItemModel;

import java.util.List;

public interface ItemService {

    //异步扣减数据库失败时，需要回滚库存
    boolean increaseStock(Integer itemId, Integer amount);

    //异步扣减库存
    boolean asyncDecreaseStock(Integer itemId, Integer amount);

    //item及promo model缓存模型
    ItemModel getItemByIdInCache(Integer id);

    //商品销量增加
    void increaseSales(Integer itemId, Integer amount);

    //库存扣减
    boolean decreaseStock(Integer itemId, Integer amount);

    //创建商品
    ItemModel createItem(ItemModel itemModel) throws BusinessException;

    //商品列表浏览
    List<ItemModel> listItem();

    //商品详情浏览
    ItemModel getItemById(Integer id);
}
