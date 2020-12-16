package com.zjy.seckill.service.impl;

import com.zjy.seckill.dataobject.ItemDO;
import com.zjy.seckill.dataobject.ItemStockDO;
import com.zjy.seckill.error.BusinessException;
import com.zjy.seckill.error.EmBusinessError;
import com.zjy.seckill.mapper.ItemDOMapper;
import com.zjy.seckill.mapper.ItemStockDOMapper;
import com.zjy.seckill.mq.MqProducer;
import com.zjy.seckill.service.ItemService;
import com.zjy.seckill.service.PromoService;
import com.zjy.seckill.service.model.ItemModel;
import com.zjy.seckill.service.model.PromoModel;
import com.zjy.seckill.validator.ValidationResult;
import com.zjy.seckill.validator.ValidatorImpl;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {

    @Resource
    private ItemDOMapper itemDOMapper;

    @Resource
    private ItemStockDOMapper itemStockDOMapper;

    @Resource
    private ValidatorImpl validator;

    @Resource
    private PromoService promoService;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private MqProducer mqProducer;

    @Override
    public boolean increaseStock(Integer itemId, Integer amount) {
        redisTemplate.opsForValue().increment("promo_item_stock_" + itemId, amount.intValue());
        return true;
    }

    @Override
    public boolean asyncDecreaseStock(Integer itemId, Integer amount) {
        boolean sendResult = mqProducer.asyncReduceStock(itemId, amount);
        return sendResult;
    }

    @Override
    public ItemModel getItemByIdInCache(Integer id) {
        String key = "item_validate_" + id;
        ItemModel itemModel = (ItemModel) redisTemplate.opsForValue().get(key);
        if (itemModel == null) {
            itemModel = getItemById(id);
            redisTemplate.opsForValue().set(key, itemModel);
            redisTemplate.expire(key, 10, TimeUnit.MINUTES);
        }
        return itemModel;
    }

    @Override
    @Transactional
    public void increaseSales(Integer itemId, Integer amount) {
        itemDOMapper.increaseSales(itemId, amount);
    }

    @Override
    @Transactional
    public boolean decreaseStock(Integer itemId, Integer amount) {
//        int affectedRow = itemStockDOMapper.decreaseStock(itemId, amount);
        Long result = redisTemplate.opsForValue().decrement("promo_item_stock_" + itemId, amount.intValue());
        if (result >= 0) {
            //更新库存成功
//            boolean sendResult = mqProducer.asyncReduceStock(itemId, amount);
//            if (!sendResult) {
//                redisTemplate.opsForValue().increment("promo_item_stock_" + itemId, amount.intValue());
//                return false;
//            }
            return true;
        } else {
            //更新库存失败
            increaseStock(itemId, amount);
            return false;
        }
    }

    @Override
    @Transactional
    public ItemModel createItem(ItemModel itemModel) throws BusinessException {
        //校验入参
        ValidationResult validationResult = validator.validate(itemModel);
        if (validationResult.isHasErrors()) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, validationResult.getErrMsg());
        }
        //转化Model ->dataObject
        ItemDO itemDO = convertFromModel(itemModel);
        //写入数据库
        itemDOMapper.insertSelective(itemDO);
        itemModel.setId(itemDO.getId());
        ItemStockDO itemStockDO = convertStockFromModel(itemModel);
        itemStockDOMapper.insertSelective(itemStockDO);

        //返回创建完成的对象
        return getItemById(itemModel.getId());
    }

    @Override
    public List<ItemModel> listItem() {
        List<ItemDO> itemDOList = itemDOMapper.listItem();
        List<ItemModel> itemModelList = itemDOList.stream().map(itemDO -> {
            ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());
            ItemModel itemModel = this.convertFromDataObject(itemDO, itemStockDO);
            return itemModel;
        }).collect(Collectors.toList());
        return itemModelList;
    }

    @Override
    public ItemModel getItemById(Integer id) {
        ItemDO itemDO = itemDOMapper.selectByPrimaryKey(id);
        if (itemDO == null) {
            return null;
        }
        //得到库存
        ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());
        //将dataObject ——> Model
        ItemModel itemModel = convertFromDataObject(itemDO, itemStockDO);
        //查询活动信息
        PromoModel promoModel = promoService.getPromoByItemId(itemModel.getId());

        //活动已经结束了，不应该展示
        if (promoModel != null && promoModel.getStatus().intValue() != 3) {
            itemModel.setPromoModel(promoModel);
        }
        return itemModel;
    }

    private ItemModel convertFromDataObject(ItemDO itemDO, ItemStockDO itemStockDO) {
        ItemModel itemModel = new ItemModel();
        BeanUtils.copyProperties(itemDO, itemModel);
        itemModel.setStock(itemStockDO.getStock());
        return itemModel;
    }

    private ItemDO convertFromModel(ItemModel itemModel) {
        if (itemModel == null) {
            return null;
        }
        ItemDO itemDO = new ItemDO();
        BeanUtils.copyProperties(itemModel, itemDO);
        return itemDO;
    }

    private ItemStockDO convertStockFromModel(ItemModel itemModel) {
        if (itemModel == null) {
            return null;
        }
        ItemStockDO itemStockDO = new ItemStockDO();
        itemStockDO.setItemId(itemModel.getId());
        itemStockDO.setStock(itemModel.getStock());
        return itemStockDO;
    }
}
