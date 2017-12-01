//package cn.godsdo.config;
//
//import com.maxmind.geoip2.DatabaseReader;
//import jakarta.annotation.Resource;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//
///**
// * @Author: CR7
// * @Date: 2019/5/5 16:33
// * @Description:
// */
//@Slf4j
//@Configuration
//public class IpDataBases {
//
//    @Resource
//    private StaticTemplateProperties templateProperties;
//
//    @Bean
//    public DatabaseReader databaseReader() {
//        try {
//            // 创建数据库
//            FileInputStream database = new FileInputStream(new File(templateProperties.getDatabaseUrl()));
//            return new DatabaseReader.Builder(database).build();
//        } catch (IOException e) {
//            log.info("templateProperties is not find");
////            e.printStackTrace();
//        }
//        return null;
//    }
//}
//
