package com.zjy.seckill.service.model;


import java.math.BigDecimal;

//用户下单的交易模型
public class OrderModel {

    //订单id
    //20201209xxxx
    private String id;

    //用户id
    private Integer userId;

    //商品id
    private Integer itemId;

    //若非空，则表示为秒杀价格下单
    private Integer promoId;

    //商品单价，若promoId不为空，则表示秒杀价格
    private BigDecimal itemPrice;

    //购买数量
    private Integer amount;

    //订单金额，若promoId不为空，则表示秒杀价格
    private BigDecimal orderPrice;

    public Integer getPromoId() {
        return promoId;
    }

    public void setPromoId(Integer promoId) {
        this.promoId = promoId;
    }

    public BigDecimal getOrderPrice() {
        return orderPrice;
    }

    public void setOrderPrice(BigDecimal orderPrice) {
        this.orderPrice = orderPrice;
    }

    public BigDecimal getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(BigDecimal itemPrice) {
        this.itemPrice = itemPrice;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

}
