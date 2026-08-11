package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.USER_SIGN_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    private CacheClient  cacheClient;

    @Autowired
    private IUserInfoService userInfoService;



    @Override
    public Result sendCode(String phone, HttpSession session) {
        if(RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        String code = RandomUtil.randomNumbers(6);
        redisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY + phone,code,RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);
        log.debug("发送验证码成功: {}", code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginFormDTO, HttpSession session) {
        if(RegexUtils.isPhoneInvalid(loginFormDTO.getPhone())) {
            return Result.fail("手机号格式错误");
        }
        User user = query().eq("phone", loginFormDTO.getPhone()).one();
        if(StrUtil.isBlank(loginFormDTO.getPassword())) {
            String trueCode = redisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + loginFormDTO.getPhone());
            String code = loginFormDTO.getCode();
            if(!code.equals(trueCode)) {
                return Result.fail("验证码错误");
            }
            if(user == null) {
                user = createUserWithPhone(loginFormDTO.getPhone());
            }
        } else {
            if(user == null) {
                return Result.fail("该用户不存在");
            }
            if(!user.getPassword().equals(loginFormDTO.getPassword())) {
                return Result.fail("密码错误");
            }
        }
        String token = UUID.randomUUID().toString(true);
        UserDTO userDTO = BeanUtil.copyProperties(user,UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        String tokenKey = RedisConstants.LOGIN_USER_KEY + token;
        redisTemplate.opsForHash().putAll(tokenKey,userMap);
        redisTemplate.expire(tokenKey, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        return user;
    }


    @Override
    public Result logout(HttpServletRequest request) {
        String token = request.getHeader("authorization");
        String tokenKey = RedisConstants.LOGIN_USER_KEY + token;
        redisTemplate.delete(tokenKey);
        return Result.ok();
    }

    @Override
    public Result getUserInfo(Long id) {
        User user = cacheClient.queryWithMutex(RedisConstants.CACHE_USER_KEY, id, User.class, this::getById, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        UserDTO userDTO = BeanUtil.copyProperties(user,UserDTO.class);
        if(userDTO == null) {
            return Result.fail("找不到该用户");
        }
        return Result.ok(userDTO);
    }

//    @Override
//    public Result sign() {
//        // 1.获取当前登录用户
//        Long userId = UserHolder.getUser().getId();
//        // 2.获取日期
//        LocalDateTime now = LocalDateTime.now();
//        // 3.拼接key
//        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
//        String key = USER_SIGN_KEY + userId + keySuffix;
//        // 4.获取今天是本月的第几天
//        int dayOfMonth = now.getDayOfMonth();
//        // 5.写入Redis SETBIT key offset 1
//        redisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
//        return Result.ok();
//    }
//
//    @Override
//    public Result signCount() {
//        // 1.获取当前登录用户
//        Long userId = UserHolder.getUser().getId();
//        // 2.获取日期
//        LocalDateTime now = LocalDateTime.now();
//        // 3.拼接key
//        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
//        String key = USER_SIGN_KEY + userId + keySuffix;
//        // 4.获取今天是本月的第几天
//        int dayOfMonth = now.getDayOfMonth();
//        // 5.获取本月截止今天为止的所有的签到记录，返回的是一个十进制的数字 BITFIELD sign:5:202203 GET u14 0
//        List<Long> result = redisTemplate.opsForValue().bitField(
//                key,
//                BitFieldSubCommands.create()
//                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
//        );
//        if (result == null || result.isEmpty()) {
//            // 没有任何签到结果
//            return Result.ok(0);
//        }
//        Long num = result.get(0);
//        if (num == null || num == 0) {
//            return Result.ok(0);
//        }
//        // 6.循环遍历
//        int count = 0;
//        while (true) {
//            // 6.1.让这个数字与1做与运算，得到数字的最后一个bit位  // 判断这个bit位是否为0
//            if ((num & 1) == 0) {
//                // 如果为0，说明未签到，结束
//                break;
//            }else {
//                // 如果不为0，说明已签到，计数器+1
//                count++;
//            }
//            // 把数字右移一位，抛弃最后一个bit位，继续下一个bit位
//            num >>>= 1;
//        }
//        return Result.ok(count);
//    }

    @Override
    public Result setPassword(String phone, String password, String code) {
        User user = query().eq("phone", phone).one();
        String trueCode = redisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + phone);
        if(!code.equals(trueCode)) {
            return Result.fail("验证码错误");
        }
        user.setId(null);
        user.setPassword(password);
        updateById(user);
        return Result.ok();
    }

    @Override
    public Result show() {
        return Result.ok(userInfoService.getById(UserHolder.getUser().getId()));
    }
}
