package com.sauvignon.seckill.mq;

import com.sauvignon.seckill.pojo.entities.Order;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MessageProvider
{
    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    public void asyncSendOrderly(String topic,String tag,Object payload,String hashKey)
    {
        rocketMQTemplate.asyncSendOrderly(topic+":"+tag, payload, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult)
            { }
            @Override
            public void onException(Throwable e)
            {
                e.printStackTrace();
                log.info("error!"+payload);
            }
        },1000);
    }

    /**
     * 重发延迟 30 s
     * 底层使用异步发送
     * @param payload redis中订单延时标记的key
     */
    public String sendDelay(String topic,String tag,Object payload)
    {
        String msgReply = null;
        try {
            msgReply = rocketMQTemplate.sendAndReceive(topic + ":" + tag,
                    payload, String.class, 1000, 4);
        } catch (Exception e) {
            log.info("ERROR:"+e.getMessage()+":"+payload);
        }
        return msgReply;
    }
}
