package com.zjy.seckill.service.impl;

import com.zjy.seckill.dataobject.OrderDO;
import com.zjy.seckill.dataobject.SequenceDO;
import com.zjy.seckill.error.BusinessException;
import com.zjy.seckill.error.EmBusinessError;
import com.zjy.seckill.mapper.OrderDOMapper;
import com.zjy.seckill.mapper.SequenceDOMapper;
import com.zjy.seckill.service.ItemService;
import com.zjy.seckill.service.OrderService;
import com.zjy.seckill.service.UserService;
import com.zjy.seckill.service.model.ItemModel;
import com.zjy.seckill.service.model.OrderModel;
import com.zjy.seckill.service.model.UserModel;
import org.junit.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Service
public class OrderServiceImpl implements OrderService {

    @Resource
    private ItemService itemService;

    @Resource
    private UserService userService;

    @Resource
    private OrderDOMapper orderDOMapper;

    @Resource
    private SequenceDOMapper sequenceDOMapper;

    @Override
    @Transactional
    public OrderModel createOrder(Integer userId, Integer itemId, Integer promoId,Integer amount) throws BusinessException {

        //校验下单状态?商品是否存在，用户是否合法，购买数量是否正确
//        ItemModel itemModel = itemService.getItemById(itemId);
        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if (itemModel == null) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "商品信息不存在");
        }
//        UserModel userModel = userService.getUserById(userId);
        UserModel userModel = userService.getUserByIdInCache(userId);
        if (userModel == null) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "用户信息不存在");
        }

        if (amount <= 0 || amount > 99) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "数量不正确");
        }

        //校验秒杀活动
        if (promoId != null) {
            //1.校验对应活动是否适用当前商品
            if (promoId != itemModel.getPromoModel().getId()) {
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "活动信息不正确");
            } else if (itemModel.getPromoModel().getStatus() != 2) {//校验活动是否正在进行
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "活动还未开始");
            }
        }

        //落单减库存
        boolean result = itemService.decreaseStock(itemId, amount);
        if (!result) {
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
        }
        //订单入库
        OrderModel orderModel = new OrderModel();
        orderModel.setUserId(userId);
        orderModel.setItemId(itemId);
        orderModel.setAmount(amount);
        if (promoId != null) {
            orderModel.setItemPrice(itemModel.getPromoModel().getPromoItemPrice());
        } else {
            orderModel.setItemPrice(itemModel.getPrice());
        }
        orderModel.setPromoId(promoId);
        orderModel.setOrderPrice(orderModel.getItemPrice().multiply(new BigDecimal(amount)));

        //生成订单号
        orderModel.setId(generateOrderNo());
        OrderDO orderDO = convertFromModel(orderModel);
        orderDOMapper.insertSelective(orderDO);

        //更新销量
        itemService.increaseSales(itemId, amount);
        //返回前端
        return orderModel;
    }

    private OrderDO convertFromModel(OrderModel orderModel) {
        if (orderModel == null) {
            return null;
        }
        OrderDO orderDO = new OrderDO();
        BeanUtils.copyProperties(orderModel, orderDO);
        return orderDO;
    }

    //开启一个新事物
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    String generateOrderNo() {
        //订单号有16位，前8位时间信息，年月日，中间6位是自增序列，最后两位：分库分表位
        StringBuilder stringBuilder = new StringBuilder();
        LocalDateTime date = LocalDateTime.now();
        String nowDate = date.format(DateTimeFormatter.ISO_DATE).replace("-", "");
        stringBuilder.append(nowDate);

        //获取当前sequence
        int sequence = 0;
        SequenceDO sequenceDO = sequenceDOMapper.getSequenceByName("order_info");
        sequence = sequenceDO.getCurrentValue();
        sequenceDO.setCurrentValue(sequenceDO.getCurrentValue() + sequenceDO.getStep());
        sequenceDOMapper.updateByPrimaryKeySelective(sequenceDO);
        String sequenceStr = String.valueOf(sequence);
        for (int i = 0; i < 6 - sequenceStr.length(); i++) {
            stringBuilder.append("0");
        }
        stringBuilder.append(sequenceStr);
        //分库分表位，暂时写死
        stringBuilder.append("00");
        return stringBuilder.toString();
    }
}
