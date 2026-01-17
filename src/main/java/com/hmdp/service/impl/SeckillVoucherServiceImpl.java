package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.config.RedisClient;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.SneakyThrows;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 秒杀优惠券表，与优惠券是一对一关系 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2022-01-04
 */
@Service
public class SeckillVoucherServiceImpl extends ServiceImpl<SeckillVoucherMapper, SeckillVoucher> implements ISeckillVoucherService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisIdWorker redisIdWorker;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private RedissonClient redissonClient;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
//        SeckillVoucher voucher = getById(voucherId);
        Long userId = UserHolder.getUser().getId();
//        if(voucher.getBeginTime().isAfter(LocalDateTime.now())) {
//            return Result.fail("不在活动时间范围内");
//        }
//        if(voucher.getEndTime().isBefore(LocalDateTime.now())) {
//            return Result.fail("不在活动时间范围内");
//        }
        String key = "seckill:time:" + voucherId;
        String begin = (String)stringRedisTemplate.opsForHash().get(key, "begin");
        String end = (String)stringRedisTemplate.opsForHash().get(key, "end");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime beginTime = LocalDateTime.parse(begin,formatter);
        LocalDateTime endTime = LocalDateTime.parse(end,formatter);
        if(beginTime.isAfter(LocalDateTime.now()) || endTime.isBefore(LocalDateTime.now())){
            return Result.fail("不在活动时间范围内");
        }
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString()
        );
        if (result == null) {
            return Result.fail("系统异常，请稍后重试");
        }

        if (result == 1) {
            return Result.fail("当前无库存");
        } else if (result == 2) {
            return Result.fail("您已购买该优惠券");
        }
        Long orderId = redisIdWorker.nextId("order");
        Map<String,Long> voucherDetail = new HashMap<>();
        voucherDetail.put("voucherId", voucherId);
        voucherDetail.put("orderId", orderId);
        voucherDetail.put("userId", userId);
        try{
            rocketMQTemplate.syncSend("orderTopic",MessageBuilder.withPayload(voucherDetail).build());
            return Result.ok(orderId);
        }catch (Exception e){
            stringRedisTemplate.opsForValue().increment("seckill:stock:" + voucherId);
            stringRedisTemplate.opsForSet().remove("seckill:order:" + voucherId, userId.toString());
            return Result.fail("系统繁忙，请重试");
        }
    }
}
