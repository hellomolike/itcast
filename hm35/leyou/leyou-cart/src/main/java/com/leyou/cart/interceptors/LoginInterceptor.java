package com.leyou.cart.interceptors;

import com.leyou.auth.pojo.UserInfo;
import com.leyou.auth.ustils.JwtUtils;
import com.leyou.cart.config.JwtProperties;
import com.leyou.common.utils.CookieUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@EnableConfigurationProperties(JwtProperties.class)
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties properties;

    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取cookie中的token
        String token = CookieUtils.getCookieValue(request, this.properties.getCookieName());
        // 判断token是否为null
        if (StringUtils.isBlank(token)) {
            response.sendRedirect("http://www.leyou.com/login.html?returnUrl=" + request.getRequestURL());
            return false;
        }
        // 解析jwt，获取用户信息
        UserInfo userInfo = JwtUtils.getInfoFromToken(token, this.properties.getPublicKey());

        // 判断用户信息是否为null
        if (userInfo == null){
            response.sendRedirect("http://www.leyou.com/login.html?returnUrl=" + request.getRequestURL());
            return false;
        }

        // 如果不为null，把用户保存下来
        THREAD_LOCAL.set(userInfo);

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 必须清除线程变量，因为我们使用的是线程池
        THREAD_LOCAL.remove();
    }

    /**
     * 封装静态方法，方便从ThreadLocal中获取用户信息
     * @return
     */
    public static UserInfo getUserInfo(){
        return THREAD_LOCAL.get();
    }
}
