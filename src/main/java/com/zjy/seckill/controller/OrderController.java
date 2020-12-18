package com.zjy.seckill.controller;

import com.google.common.util.concurrent.RateLimiter;
import com.zjy.seckill.error.BusinessException;
import com.zjy.seckill.error.EmBusinessError;
import com.zjy.seckill.mq.MqProducer;
import com.zjy.seckill.response.CommonReturnType;
import com.zjy.seckill.service.ItemService;
import com.zjy.seckill.service.OrderService;
import com.zjy.seckill.service.PromoService;
import com.zjy.seckill.service.model.OrderModel;
import com.zjy.seckill.service.model.UserModel;
import com.zjy.seckill.util.CodeUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

@RestController("order")
@RequestMapping("/order")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")
public class OrderController extends BaseController {

    @Resource
    private PromoService promoService;

    @Resource
    private HttpServletRequest httpServletRequest;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private MqProducer mqProducer;

    @Resource
    private ItemService itemService;

    private ExecutorService executorService;

    private RateLimiter orderCreateRateLimiter;

    @PostConstruct
    public void init() {
        executorService = Executors.newFixedThreadPool(20);
        orderCreateRateLimiter = RateLimiter.create(300);
    }

    @RequestMapping(value = "/generateVerifyCode", method = {RequestMethod.POST, RequestMethod.GET})
    public void generateVerifyCode(HttpServletResponse response) throws BusinessException, IOException {
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isEmpty(token)) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户还没登录，不能生成验证码");
        }
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if (userModel == null) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户未登录，不能生成验证码");
        }
        Map<String,Object> map = CodeUtil.generateCodeAndPic();
        redisTemplate.opsForValue().set("verify_code_" + userModel.getId(), map.get("code"));
        redisTemplate.expire("verify_code_" + userModel.getId(), 5, TimeUnit.MINUTES);
        ImageIO.write((RenderedImage) map.get("codePic"), "jpeg", response.getOutputStream());
        System.out.println("验证码的值为："+map.get("code"));
    }

    /**
     * 生成秒杀令牌
     *
     * @param itemId
     * @param promoId
     * @return
     */
    @PostMapping(value = "/generateToken", consumes = {CONTENT_TYPE_FORMED})
    public CommonReturnType generateToken(@RequestParam("itemId") Integer itemId,
                                          @RequestParam(value = "promoId") Integer promoId,
                                          @RequestParam("verifyCode") String verifyCode) throws BusinessException {

        //获取用户信息
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isEmpty(token)) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if (userModel == null) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户未登录，不能下单");
        }

        String redisVerifyCode = (String) redisTemplate.opsForValue().get("verify_code_" + userModel.getId());
        if (StringUtils.isEmpty(redisVerifyCode)) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "请求非法");
        }
        if (!redisVerifyCode.equalsIgnoreCase(verifyCode)) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "验证码不正确");
        }


        //获取秒杀令牌
        String secondKillToken = promoService.generateSecondKillToken(promoId, itemId, userModel.getId());
        if (secondKillToken == null) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "生成令牌失败");
        }
        return CommonReturnType.create(secondKillToken);
    }

    @PostMapping(value = "/createOrder", consumes = {CONTENT_TYPE_FORMED})
    public CommonReturnType createOrder(@RequestParam("itemId") Integer itemId,
                                        @RequestParam("amount") Integer amount,
                                        @RequestParam(value = "promoId", required = false) Integer promoId,
                                        @RequestParam(value = "secondKillToken", required = false) String secondKillToken) throws BusinessException {

//        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        if (!orderCreateRateLimiter.tryAcquire()) {
            throw new BusinessException(EmBusinessError.RATE_LIMITE);
        }
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isEmpty(token)) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if (userModel == null) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户未登录，不能下单");
        }
        //校验秒杀令牌是否正确
        if (promoId != null) {
            String inRedisSecondKillToken = (String) redisTemplate.opsForValue().get("promo_token_" + promoId + "_userId_" + userModel.getId() + "_itemId_" + itemId);
            if (inRedisSecondKillToken == null) {
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "令牌检验失败");
            }
            if (!StringUtils.equals(inRedisSecondKillToken, secondKillToken)) {
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "令牌检验失败");
            }
        }

//        if (isLogin == null || !isLogin) {
//            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
////        }
//        UserModel userModel = (UserModel) httpServletRequest.getSession().getAttribute("LOGIN_USER");
        //封装下单请求
//        OrderModel orderModel = orderService.createOrder(userModel.getId(), itemId, promoId, amount);

        //检查是否售罄
//        if (redisTemplate.hasKey("promo_item_stock_invalid_" + itemId)) {
//            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
//        }

        //拥塞窗口为20的等待队列，用来队列化泄洪
        Future<Object> future = executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                //加入事务的流水状态
                String stockLogId = itemService.initStockLog(itemId, amount);

                if (!mqProducer.transactionAsyncReduceStock(userModel.getId(), itemId, promoId, amount, stockLogId)) {
                    throw new BusinessException(EmBusinessError.UNKNOWN_ERROR, "下单失败");
                }
                return null;
            }
        });

        try {
            future.get();
        } catch (InterruptedException e) {
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
        } catch (ExecutionException e) {
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
        }

//        //加入事务的流水状态
//        String stockLogId = itemService.initStockLog(itemId, amount);
//
//        if (!mqProducer.transactionAsyncReduceStock(userModel.getId(), itemId, promoId, amount, stockLogId)) {
//            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR, "下单失败");
//        }
        return CommonReturnType.create(null);
    }
}
