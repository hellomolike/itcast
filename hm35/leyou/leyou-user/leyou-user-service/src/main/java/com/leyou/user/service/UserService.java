package com.leyou.user.service;

import com.leyou.common.utils.NumberUtils;
import com.leyou.user.mapper.UserMapper;
import com.leyou.user.pojo.User;
import com.leyou.user.utils.CodecUtils;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String USER_VERIFY_PREFIX = "user:verify:";

    public Boolean checkUser(String data, Integer type) {

        // 查询对象
        User user = new User();
        switch (type) {
            case 1: // 如果type=1，说明校验用户名
                user.setUsername(data);
                break;
            case 2: // 如果type=2，校验电话
                user.setPhone(data);
                break;
            default: // 如果不是1或者2，说明参数不合法
                return null;
        }
        // 如果查询结果为0，返回true；否则返回false
        return this.userMapper.selectCount(user) == 0;
    }

    public Boolean sendVerifyCode(String phone) {
        try {
            // 随机生成6位校验码
            String code = NumberUtils.generateCode(6);

            // 发送消息到队列（phone code）
            Map<String, String> msg = new HashMap<>();
            msg.put("phone", phone);
            msg.put("code", code);
            this.amqpTemplate.convertAndSend("LEYOU_SMS_EXCHANGE", "sms.verifycode", msg);

            // 把code保存到redis中
            this.redisTemplate.opsForValue().set(USER_VERIFY_PREFIX + phone, code, 5, TimeUnit.MINUTES);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Boolean register(User user, String code) {

        try {
            // 验证校验码
            String cacheCode = this.redisTemplate.opsForValue().get(USER_VERIFY_PREFIX + user.getPhone());
            if (!StringUtils.equals(code, cacheCode)) {
                return false;
            }

            // 生成盐
            String salt = CodecUtils.generateSalt();
            user.setSalt(salt);

            // 给密码加密，加盐
            user.setPassword(CodecUtils.md5Hex(user.getPassword(), salt));

            // 保存用户信息
            user.setCreated(new Date());
            user.setId(null);
            boolean flag = this.userMapper.insertSelective(user) == 1;

            // 删除redis
            if (flag) {
                this.redisTemplate.delete(USER_VERIFY_PREFIX + user.getPhone());
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public User queryUser(String username, String password) {
        User record = new User();
        record.setUsername(username);
        User user = this.userMapper.selectOne(record);

        // 判断用户是否存在
        if (user == null) {
            return null;
        }

        // 比较密码
        if (StringUtils.equals(user.getPassword(), CodecUtils.md5Hex(password, user.getSalt()))){
            return user;
        }
        return null;
    }
}
