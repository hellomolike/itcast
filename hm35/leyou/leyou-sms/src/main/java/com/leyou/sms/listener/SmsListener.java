package com.leyou.sms.listener;

import com.aliyuncs.exceptions.ClientException;
import com.leyou.sms.config.SmsProperties;
import com.leyou.sms.utils.SmsUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Map;

@Component
@EnableConfigurationProperties(SmsProperties.class)
public class SmsListener {

    @Autowired
    private SmsUtils smsUtils;

    @Autowired
    private SmsProperties properties;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "LEYOU_SMS_MESSAGE", durable = "true"),
            exchange = @Exchange(value = "LEYOU_SMS_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"sms.verifycode"}
    ))
    public void listener(Map<String, String> msg) throws ClientException {
        if (CollectionUtils.isEmpty(msg)){
            return;
        }
        String phone = msg.get("phone");
        String code = msg.get("code");
        if (StringUtils.isEmpty(phone) || StringUtils.isEmpty(code)) {
            return;
        }
        this.smsUtils.sendSms(phone, code, this.properties.getSignName(), this.properties.getVerifyCodeTemplate());
    }
}
