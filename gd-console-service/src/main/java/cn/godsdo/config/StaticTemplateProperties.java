package cn.godsdo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author: CR7
 * @Date: 2019/5/5 16:30
 * @Description:
 */
@Data
@Component
@ConfigurationProperties(prefix = "static")
public class StaticTemplateProperties {

    private String databaseUrl;

}
