package com.hmdp;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.User;
import com.hmdp.entity.UserInfo;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.UserHolder;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@SpringBootTest
class HmDianPingApplicationTests {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private IUserInfoService userInfoService;

    @Autowired
    private IUserService userService;

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


    @Test
    void setUserInfo(){
        List<User> list = userService.list();
        // ✅ 1. 在循环外创建 Random 对象
        Random random = new Random();
        List<UserInfo> userInfoList = new ArrayList<>();

        for (User user : list) {
            UserInfo userInfo = new UserInfo();

            // 固定值
            userInfo.setLevel(false);
            userInfo.setFans(0);
            userInfo.setFollowee(0);
            userInfo.setCity("深圳");
            userInfo.setCredits(0);
            userInfo.setUserId(user.getId());
            userInfo.setUpdateTime(user.getUpdateTime());
            userInfo.setCreateTime(user.getCreateTime());

            // 随机值
            // 生日：1980-2025年，1-12月，1-27日
            LocalDate birthday = LocalDate.of(
                    1980 + random.nextInt(46),    // 1980-2025
                    random.nextInt(11) + 1,        // 1-12月
                    random.nextInt(27) + 1         // 1-27日
            );
            userInfo.setGender(random.nextBoolean());
            userInfo.setBirthday(birthday);

            userInfoList.add(userInfo);
        }

        // ✅ 2. 批量插入（性能提升10倍以上）
        long start = System.currentTimeMillis();
        boolean success = userInfoService.saveBatch(userInfoList);
        long end = System.currentTimeMillis();

        System.out.println("成功插入 " + userInfoList.size() + " 条用户信息，耗时：" + (end - start) + "ms");
    }

}
