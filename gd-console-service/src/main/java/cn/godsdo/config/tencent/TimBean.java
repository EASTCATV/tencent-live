package cn.godsdo.config.tencent;

import cn.godsdo.util.tencent.ImHelperUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author : yang
 * @Date :
 * @ApiNote :
 */
@Configuration
@RefreshScope
public class TimBean {

    /**
     * sdk appId
     */
    @Value("${Tencent.im.appId}")
    long appId = 111111;

    /**
     * admin userId
     */
    @Value("${Tencent.im.userId}")
    String userId = "1111";

    /**
     * application key
     */
    @Value("${Tencent.im.key}")
    String key = "1111122";


    @Bean("TimBean")
    public ImHelperUtil initBean() {
        ImHelperUtil imHelperUtil = new ImHelperUtil(appId, userId, key);
        return imHelperUtil;
    }



}
