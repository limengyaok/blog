package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Chat;
import com.hmdp.mapper.ChatMapper;
import com.hmdp.service.IChatService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author author
 * @since 2026-08-04
 */
@Service
public class ChatServiceImpl extends ServiceImpl<ChatMapper, Chat> implements IChatService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private IUserService userService;


    @Override
    public Result create(Integer userId) {
        UserDTO user = UserHolder.getUser();
        stringRedisTemplate.opsForSet().add("user:chat:" + user.getId().toString(), userId.toString());
        Chat chat = new Chat();
        chat.setUser1Id(userId);
        chat.setUser2Id(user.getId().intValue());
        chat.setCreateTime(LocalDateTime.now());
        save(chat);
        return Result.ok();
    }

    @Override
    public Result getList() {
        Set<String> members = stringRedisTemplate.opsForSet().members("user:chat:" + UserHolder.getUser().getId().toString());
        List<String> userIds = new ArrayList<>(members);
        List<UserDTO> userDTOList = new ArrayList<>();
        for(String userId : userIds){
            Result userInfo = userService.getUserInfo(Long.valueOf(userId));
            UserDTO user = (UserDTO)userInfo.getData();
            userDTOList.add(user);
        }
        return Result.ok(userDTOList);
    }
}
