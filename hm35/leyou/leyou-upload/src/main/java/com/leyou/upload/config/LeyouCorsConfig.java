package com.leyou.upload.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class LeyouCorsConfig {

    @Bean
    public CorsFilter corsFilter(){
        // 构造跨域配置类
        CorsConfiguration config = new CorsConfiguration();
        // 添加允许跨域的域名
        config.addAllowedOrigin("http://manage.leyou.com");
        // 允许携带cookie
        config.setAllowCredentials(true);
        // 允许所有方法
        config.addAllowedMethod("*");
        // 允许所有的头信息
        config.addAllowedHeader("*");

        // url跨域配置器
        UrlBasedCorsConfigurationSource configurationSource = new UrlBasedCorsConfigurationSource();
        // 拦截所有路径
        configurationSource.registerCorsConfiguration("/**", config);

        return new CorsFilter(configurationSource);
    }
}
