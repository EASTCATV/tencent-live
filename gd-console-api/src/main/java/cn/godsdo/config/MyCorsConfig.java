package cn.godsdo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 */
@Configuration
public class MyCorsConfig {

    private CorsConfiguration corsConfig() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        //允许所有域名访问
        corsConfiguration.addAllowedOriginPattern("*");
        //允许所有请求头
        corsConfiguration.addAllowedHeader("*");
        //允许所有的请求类型
        corsConfiguration.addAllowedMethod("*");
        corsConfiguration.setMaxAge(3600L);
        //允许请求携带验证信息（cookie）
        corsConfiguration.setAllowCredentials(true);
        return corsConfiguration;
    }

    @Bean
    public CorsFilter corsFilter() {
        //存储request与跨域配置信息的容器，基于url的映射
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig());
        return new CorsFilter(source);
    }
}
