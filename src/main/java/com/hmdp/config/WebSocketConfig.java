package com.hmdp.config;

import com.hmdp.utils.WebSocketAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker  // 开启 STOMP 消息代理
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private WebSocketAuthInterceptor authInterceptor;

    /**
     * 注册 STOMP 端点，客户端通过这个端点连接
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")              // WebSocket 连接地址
                .setAllowedOrigins("*")         // 允许跨域（生产环境改为具体域名）
                .addInterceptors(authInterceptor) // 添加认证拦截器
                .withSockJS();                  // 启用 SockJS 降级方案
    }

    /**
     * 配置消息代理
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 1. 启用简单消息代理
        registry.enableSimpleBroker("/queue", "/topic");

        // 2. 应用前缀：前端发消息到 /xxx
        registry.setApplicationDestinationPrefixes("");

        // 3. 用户前缀：固定为 /user，不要改！
        registry.setUserDestinationPrefix("/user");
    }
}