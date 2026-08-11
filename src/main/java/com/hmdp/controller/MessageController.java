package com.hmdp.controller;

import com.hmdp.entity.Message;
import com.hmdp.service.IMessageService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.collect.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@Slf4j
public class MessageController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private IMessageService messageService;

    /**
     * 处理私聊消息
     * 前端发送地址：/chat
     */
    @MessageMapping("/chat")
    public void handlePrivateMessage(
            @Payload Message messageDTO,
            Principal principal) {

        try {
            // 1. 从 Principal 获取当前用户ID（由拦截器注入）
            String userIdStr = principal.getName();
            Integer senderId = Integer.parseInt(userIdStr);

            log.info("收到私聊消息: 发送者={}, 接收者={}, 内容={}",
                    senderId, messageDTO.getReceiveId(), messageDTO.getContent());

            // 2. 构建消息实体
            Message message = new Message();
            message.setChatId(messageDTO.getChatId());
            message.setSendId(senderId);
            message.setReceiveId(messageDTO.getReceiveId());
            message.setContent(messageDTO.getContent());

            // 3. 保存到数据库
            messageService.save(message);

            // 4. 设置完整信息到 DTO（用于推送给接收者）
            messageDTO.setSendId(senderId);
            messageDTO.setId(message.getId()); // 可选

            // 5. 推送给接收者（点对点）
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(messageDTO.getReceiveId()),  // 接收者ID
                    "/queue/private",                            // 队列地址
                    messageDTO                                   // 消息内容
            );

            // 6. 可选：发送成功回执给发送者
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(senderId),
                    "/queue/receipt",
                    Map.of("messageId", message.getId(), "status", "sent", "timestamp", System.currentTimeMillis())
            );

        } catch (Exception e) {
            log.error("处理私聊消息失败", e);
            // 发送错误回执给发送者
            try {
                messagingTemplate.convertAndSendToUser(
                        principal.getName(),
                        "/queue/error",
                        Map.of("error", "消息发送失败: " + e.getMessage())
                );
            } catch (Exception ex) {
                log.error("发送错误回执失败", ex);
            }
        }
    }

}
