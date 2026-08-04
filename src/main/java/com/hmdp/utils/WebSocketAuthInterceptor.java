package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

@Slf4j
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    public WebSocketAuthInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

            // 从 URL 参数获取 token（WebSocket 握手只能从这里传）
            String token = servletRequest.getParameter("token");

            if (StrUtil.isBlank(token)) {
                log.warn("WebSocket 握手失败：缺少 token");
                return false;
            }

            // 和你的 RefreshTokenInterceptor 做一模一样的事情！
            String key = LOGIN_USER_KEY + token;
            Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);

            if (userMap.isEmpty()) {
                log.warn("WebSocket 握手失败：token 无效");
                return false;
            }

            UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
            stringRedisTemplate.expire(key, LOGIN_USER_TTL, TimeUnit.MINUTES);

            // 存入 attributes，供 WebSocket 后续使用
            attributes.put("userId", userDTO.getId().toString());

            log.info("WebSocket 握手成功，用户: {}", userDTO.getId());
            return true;
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {

    }
}