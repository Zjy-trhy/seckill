package com.zjy.seckill.controller;

import com.zjy.seckill.controller.viewobject.ItemVO;
import com.zjy.seckill.error.BusinessException;
import com.zjy.seckill.mapper.ItemDOMapper;
import com.zjy.seckill.response.CommonReturnType;
import com.zjy.seckill.service.CacheService;
import com.zjy.seckill.service.ItemService;
import com.zjy.seckill.service.model.ItemModel;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController("item")
@RequestMapping("/item")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")
public class ItemController extends BaseController {

    @Resource
    ItemService itemService;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private CacheService cacheService;

    @GetMapping(value = "/listItem")
    public CommonReturnType listItem() {
        List<ItemModel> itemModelList = itemService.listItem();
        //转换成VO
        List<ItemVO> itemVOList = itemModelList.stream().map(itemModel -> {
            ItemVO itemVO = convertFromModel(itemModel);
            return itemVO;
        }).collect(Collectors.toList());
        return CommonReturnType.create(itemVOList);
    }


    @GetMapping(value = "/get")
    public CommonReturnType getItem(@RequestParam("id") Integer id) {
        String itemKey = "item_" + id;
        ItemModel itemModel = null;
        //先取本地缓存
        itemModel = (ItemModel) cacheService.getFromCommonCache(itemKey);
        //根据商品Id到redis内查询
        if (itemModel == null) {
            itemModel = (ItemModel) redisTemplate.opsForValue().get(itemKey);
            if (itemModel == null) {
                //若redis内不存在对应的itemModel，去数据库查询
                itemModel = itemService.getItemById(id);
                //将查询结果放入redis
                redisTemplate.opsForValue().set(itemKey, itemModel);
                redisTemplate.expire(itemKey, 10, TimeUnit.MINUTES);
            }
            //填充本地缓存
            cacheService.setCommonCache(itemKey, itemModel);
        }
        ItemVO itemVO = convertFromModel(itemModel);
        return CommonReturnType.create(itemVO);
    }

    @PostMapping(value = "/createItem", consumes = {CONTENT_TYPE_FORMED})
    public CommonReturnType createItem(@RequestParam("title")       String title,
                                       @RequestParam("price")       BigDecimal price,
                                       @RequestParam("stock")       Integer stock,
                                       @RequestParam("description") String description,
                                       @RequestParam("imgUrl")      String imgUrl) throws BusinessException {
        //封装service请求来创建商品
        ItemModel itemModel = new ItemModel();
        itemModel.setTitle(title);
        itemModel.setPrice(price);
        itemModel.setStock(stock);
        itemModel.setDescription(description);
        itemModel.setImgUrl(imgUrl);

        //经过此方法，可以得到设置了自增id熟悉的Model对象
        ItemModel itemModelForReturn = itemService.createItem(itemModel);
        ItemVO itemVO = convertFromModel(itemModelForReturn);
        return CommonReturnType.create(itemVO);
    }

    private ItemVO convertFromModel(ItemModel itemModel) {
        if (itemModel == null) {
            return null;
        }
        ItemVO itemVO = new ItemVO();
        BeanUtils.copyProperties(itemModel, itemVO);
        if (itemModel.getPromoModel() != null) {
            //有秒杀活动
            //未开始还是正在进行中，VO的status和ProModel的status数值一致
            itemVO.setPromoStatus(itemModel.getPromoModel().getStatus());
            itemVO.setPromoId(itemModel.getPromoModel().getId());
            itemVO.setStartDate(itemModel.getPromoModel().getStartDate().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
            itemVO.setPromoPrice(itemModel.getPromoModel().getPromoItemPrice());
        } else {
            //没有秒杀活动
            itemVO.setPromoStatus(0);
        }
        return itemVO;
    }
}
