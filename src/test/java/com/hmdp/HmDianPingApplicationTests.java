package com.hmdp;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.UserHolder;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@SpringBootTest
class HmDianPingApplicationTests {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void testRocketMq(){
        try {
            rocketMQTemplate.syncSend("test-topic", "test message");
            System.out.println("RocketMQ连接成功！");
        } catch (Exception e) {
            System.out.println("RocketMQ连接失败: " + e.getMessage());
        }
    }

    @Test
    void testZsetScore(){
        Set<ZSetOperations.TypedTuple<String>> follows = stringRedisTemplate.opsForZSet().reverseRangeByScoreWithScores("feed:1010", 0,
                1763345151114L, 1, 2);
        if(follows == null || follows.isEmpty()){
            System.out.println("空的");
        }else{
            for(ZSetOperations.TypedTuple<String> follow : follows){
                System.out.println(follow.getValue());
            }
        }
    }

    @Autowired
    private ISeckillVoucherService  seckillVoucherService;

    @Test
    void seckillVoucherInRedis(){
        String key = "seckill:time:12";
        Map<String,String> map = new HashMap<>();
        map.put("begin","1974-02-06 00:22:19");
        map.put("end","2026-07-03 05:05:50");
        stringRedisTemplate.opsForHash().putAll(key,map);

    }

}
