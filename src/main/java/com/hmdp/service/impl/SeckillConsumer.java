package com.hmdp.service.impl;

import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@RocketMQMessageListener(consumerGroup = "seckill-consumer-group",topic = "orderTopic")
public class SeckillConsumer implements RocketMQListener<Map<String,Long>> {

    @Autowired
    private IVoucherOrderService voucherOrderService;

    @Autowired
    private ISeckillVoucherService seckillVoucherService;

    @Override
    @Transactional
    public void onMessage(Map<String, Long> map) {
        VoucherOrder voucherOrder = new VoucherOrder();
        Long userId = map.get("userId");
        Long orderId = map.get("orderId");
        Long voucherId = map.get("voucherId");
        voucherOrder.setUserId(userId);
        voucherOrder.setId(orderId);
        voucherOrder.setVoucherId(voucherId);
        voucherOrderService.createVoucherOrder(voucherOrder);
        seckillVoucherService.update().setSql("stock = stock - 1").eq("voucher_id", voucherId).gt("stock",0).update();
    }
}
