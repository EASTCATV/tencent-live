package cn.godsdo.util;

import com.y20y.entity.FilePsignInfo;
import com.y20y.utils.file.FilePsign;

import java.util.*;

/**
 * @Author : just do it
 * @ApiNote : 获取fileid 的token信息
 */
public class jwt {
    public static void main(String[] args) {


        FilePsignInfo filePsignInfo = FilePsignInfo.builder()
                .expireTime(1)  //过期时间 --天
                .fileId("1253642698417827889")
                .appId(1256670631) //放到nacos 固定参数
                .playKey("9W8evyFvkrTIpmCeaWOR") //放到nacos 固定参数
                .build();
        String token = FilePsign.gettToken(filePsignInfo);
        System.out.println("====token:" + token + "====");

    }

}
