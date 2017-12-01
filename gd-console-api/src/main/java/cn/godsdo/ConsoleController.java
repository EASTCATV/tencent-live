package cn.godsdo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.Charset;
import org.springframework.boot.logging.LoggingSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
@EnableDiscoveryClient
//@EnableAspectJAutoProxy
public class ConsoleController {

    //@Configuration
    //@EnableWebMvc
    //public class WebMvcConfg implements WebMvcConfigurer {
    //    @Bean
    //    public HttpMessageConverter<String> responseBodyConverter() {
    //        StringHttpMessageConverter converter = new StringHttpMessageConverter(Charset.forName("UTF-8"));
    //        return converter;
    //    }
    //
    //    @Override
    //    public void addCorsMappings(CorsRegistry registry) {
    //        WebMvcConfigurer.super.addCorsMappings(registry);
    //        registry.addMapping("/**")//需要跨域访问的Map路径
    //                .allowedOriginPatterns("*")//允许跨域访问的ip及端口
    //                //.allowedOrigins("*")//允许跨域访问的ip及端口
    //                .allowedHeaders("*")//允许跨域访问的Headers内容
    //                .allowedMethods("POST", "GET", "PUT", "OPTIONS", "DELETE")//允许跨域访问的方法，OPTIONS必须设置Shiro中用到
    //                .allowCredentials(true)
    //                .maxAge(3600);
    //    }
    //}
    @Configuration
    @EnableWebMvc
    public class WebMvcConfg implements WebMvcConfigurer {
        @Bean
        public HttpMessageConverter<String> responseBodyConverter() {
            StringHttpMessageConverter converter = new StringHttpMessageConverter(Charset.forName("UTF-8"));
            return converter;
        }

        @Override
        public void addCorsMappings(CorsRegistry registry) {
            WebMvcConfigurer.super.addCorsMappings(registry);
            registry.addMapping("/**")//需要跨域访问的Map路径
                    .allowedOriginPatterns("*")//允许跨域访问的ip及端口
                    //.allowedOrigins("*")//允许跨域访问的ip及端口
                    .allowedHeaders("*")//允许跨域访问的Headers内容
                    .allowedMethods("POST", "GET", "PUT", "OPTIONS", "DELETE")//允许跨域访问的方法，OPTIONS必须设置Shiro中用到
                    .allowCredentials(true)
                    .maxAge(3600);
        }
    }


    public static void main(String[] args) {
        //// 获取当前的LoggingSystem实例
        //LoggingSystem loggingSystem = LoggingSystem.get(ConsoleController.class.getClassLoader());
        //Logger logger = LoggerFactory.getLogger(ConsoleController.class);
        ////// 添加自定义日志信息
        //logger.info("This is a custom log message at INFO level.");
        ////


        SpringApplication.run(ConsoleController.class, args);
    }



}
