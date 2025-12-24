package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisIdWorker redisIdWorker;
    @Autowired
    private UserServiceImpl userServiceImpl;

    @Override
    public Result followOrNot(Long id) {
        Long userId = UserHolder.getUser().getId();
        Integer count = query().eq("user_id", id).eq("follow_user_id", userId).count();
        return Result.ok(count > 0);
    }

    @Override
    public Result followChange(Long id, Boolean status) {
        Long userId = UserHolder.getUser().getId();
        String key = "follows:" + userId;
        if(status){
            Follow follow = new Follow();
            follow.setUserId(id);
            follow.setFollowUserId(userId);
            boolean save = save(follow);
            if(save){
                stringRedisTemplate.opsForSet().add(key,id.toString());
            }
        }else{
            boolean remove = remove(new QueryWrapper<Follow>().eq("user_id", id).eq("follow_user_id", userId));
            if(remove){
                stringRedisTemplate.opsForSet().remove(key,id.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result commonFollows(Long id) {
        Long userId = UserHolder.getUser().getId();
        String key1= "follows:" + id;
        String key2= "follows:" + userId;
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key1, key2);
        if(intersect == null || intersect.isEmpty()){
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        List<User> users = userServiceImpl.listByIds(ids);
        List<UserDTO> userDTOS = new ArrayList<>();
        for(User user : users){
            UserDTO userDTO = new UserDTO();
            BeanUtils.copyProperties(user,userDTO);
            userDTOS.add(userDTO);
        }
        return Result.ok(userDTOS);
    }
}
