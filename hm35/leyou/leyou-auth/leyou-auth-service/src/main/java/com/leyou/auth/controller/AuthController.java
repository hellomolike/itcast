package com.leyou.auth.controller;

import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.pojo.UserInfo;
import com.leyou.auth.service.AuthService;
import com.leyou.auth.ustils.JwtUtils;
import com.leyou.common.utils.CookieUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@EnableConfigurationProperties(JwtProperties.class)
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtProperties properties;

    @PostMapping("accredit")
    public ResponseEntity<Void> accredit(@RequestParam("username")String userName, @RequestParam("password")String password,
                                         HttpServletRequest request, HttpServletResponse response){
        // 调用service，生成token
        String token = this.authService.accredit(userName, password);
        if (StringUtils.isBlank(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // 把token放入cookie中
        CookieUtils.setCookie(request, response, this.properties.getCookieName(), token, this.properties.getExpire() * 60);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("verify")
    public ResponseEntity<UserInfo> verify(@CookieValue("LY_TOKEN")String token, HttpServletRequest request, HttpServletResponse response){
        try {
            // 解析用户信息
            UserInfo userInfo = JwtUtils.getInfoFromToken(token, this.properties.getPublicKey());
            if (userInfo == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            // 刷新有效时间
            token = JwtUtils.generateToken(userInfo, this.properties.getPrivateKey(), this.properties.getExpire());
            // 用新的token覆盖
            CookieUtils.setCookie(request, response, this.properties.getCookieName(), token, this.properties.getExpire() * 60);
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

}
