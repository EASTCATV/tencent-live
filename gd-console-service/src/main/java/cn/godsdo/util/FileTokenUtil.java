package cn.godsdo.util;

import com.y20y.entity.FilePsignInfo;
import com.y20y.utils.file.FilePsign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 获取fileId所需要的token
 * @author W~Y~H
 * @Date : 2018/05/18
 */
@Component
@Slf4j
public class FileTokenUtil {
    @Value("${Tencent.vod.subAppId}")
    private Integer appid;
    @Value("${Tencent.vod.playKey}")
    private String playKey;

    public String getToken(String fileId,Integer time){
        FilePsignInfo filePsignInfo = FilePsignInfo.builder()
                .expireTime(time)  //过期时间 --天
                .fileId(fileId)
                .appId(appid) //放到nacos 固定参数
                .playKey(playKey) //放到nacos 固定参数
                .build();
        return FilePsign.gettToken(filePsignInfo);
    }
}
