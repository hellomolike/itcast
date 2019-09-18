package com.leyou.auth.service;

import com.leyou.auth.client.UserClient;
import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.pojo.UserInfo;
import com.leyou.auth.ustils.JwtUtils;
import com.leyou.user.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserClient userClient;

    @Autowired
    private JwtProperties properties;

    public String accredit(String userName, String password) {
        // 调用用户中心的接口，校验用户信息
        User user = this.userClient.queryUser(userName, password);

        // 不存在，直接返回null
        if (user == null) {
            return null;
        }

        try {
            // 生成token
            return JwtUtils.generateToken(new UserInfo(user.getId(), user.getUsername()),
                    this.properties.getPrivateKey(), this.properties.getExpire());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
