package cn.godsdo.util.tencent;

import io.github.doocs.im.ClientConfiguration;
import io.github.doocs.im.ImClient;
import io.github.doocs.im.core.Account;
import io.github.doocs.im.core.Group;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author : yang
 * @Date :
 * @ApiNote :
 */
@Slf4j
public class ImHelperUtil {
    private final ImClient client;


    public ImHelperUtil(long appId, String userId, String key) {


        ClientConfiguration config = new ClientConfiguration();
        config.setExpireTime(7 * 24 * 60 * 60L);
        config.setAutoRenewSig(false);
         client = ImClient.getInstance(appId, userId, key, config);
    }
    public Group group(){
        return client.group;
    }
    public Account account() {
        return client.account;
    }


}
