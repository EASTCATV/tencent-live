package cn.godsdo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @description: 拦截器配置类
 **/

//@Configuration
//public class WebConfig implements WebMvcConfigurer {
//
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        //registry.addMapping("/**")
//        //        .allowedOriginPatterns("*")
//        //        .allowedMethods("GET","POST","HEAD","PUT","DELETE","OPTIONS")
//        //        .allowCredentials(true)
//        //        .allowedHeaders("*")
//        //        .allowedOrigins("*")
//        //        .maxAge(3600);
//        registry.addMapping("/**")
//                .allowedOrigins("*") // 允许所有来源
//                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的HTTP方法
//                .allowedHeaders("*") // 允许的HTTP头部
//                .allowCredentials(true); // 是否允许携带cookie
//    }
//}
