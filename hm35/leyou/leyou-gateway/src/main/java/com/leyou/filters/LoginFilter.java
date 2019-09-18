package com.leyou.filters;

import com.leyou.auth.pojo.UserInfo;
import com.leyou.auth.ustils.JwtUtils;
import com.leyou.common.utils.CookieUtils;
import com.leyou.config.FilterProperties;
import com.leyou.config.JwtProperties;
import com.netflix.discovery.converters.Auto;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
@EnableConfigurationProperties({JwtProperties.class, FilterProperties.class})
public class LoginFilter extends ZuulFilter {

    @Autowired
    private JwtProperties properties;

    @Autowired
    private FilterProperties filterProperties;

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 10;
    }

    @Override
    public boolean shouldFilter() {
        // zuul的上下文
        RequestContext context = RequestContext.getCurrentContext();
        // 从上下文中获取request对象
        HttpServletRequest request = context.getRequest();
        // 获取请求路径
        String url = request.getRequestURL().toString();
        // 遍历白名单，判断当前路径是否在白名单中
        for(String path: this.filterProperties.getAllowPaths()){
            if (StringUtils.contains(url, path)){
                return false;
            }
        }
        // 如果不在白名单中，就拦截
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        // zuul的上下文
        RequestContext context = RequestContext.getCurrentContext();
        // 从上下文中获取request对象
        HttpServletRequest request = context.getRequest();

        // 获取cookie中的token信息
        String token = CookieUtils.getCookieValue(request, this.properties.getCookieName());
        // 判断token是否为null
        if (StringUtils.isBlank(token)) {
            context.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value());
            context.setSendZuulResponse(false);
            return null;
        }
        try {
            // 解析token
            UserInfo userInfo = JwtUtils.getInfoFromToken(token, this.properties.getPublicKey());
            if (userInfo == null) {
                context.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value());
                context.setSendZuulResponse(false);
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            context.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value());
            context.setSendZuulResponse(false);
        }
        return null;
    }
}
