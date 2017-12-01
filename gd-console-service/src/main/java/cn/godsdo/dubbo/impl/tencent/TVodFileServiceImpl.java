package cn.godsdo.dubbo.impl.tencent;

import cn.godsdo.dto.live.tencent.VodMetaData;
import cn.godsdo.dubbo.tencent.TVodFileService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tencentcloudapi.vod.v20180717.models.DescribeMediaInfosResponse;
import com.y20y.base.DescribeMediaInfosInfo;
import com.y20y.vod.DescribeMediaInfos;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Value;

/**
 * @Author : just do it
 * @ApiNote :
 */
@DubboService
@Slf4j
public class TVodFileServiceImpl implements TVodFileService {

    @Value("${Tencent.secretId}")
    private String secretId;
    @Value("${Tencent.secretKey}")
    private String secretKey;
    @Value("${Tencent.vod.subAppId}")
    private Long vodSubAppId;
    @Value("${Tencent.vod.regionSh}")
    private String regionSh;


    @Override
    public VodMetaData describemediainfos(String fileId) {
        DescribeMediaInfosInfo describeMediaInfosInfo = new DescribeMediaInfosInfo();
        describeMediaInfosInfo.setSecretId(secretId);
        describeMediaInfosInfo.setSecretKey(secretKey);
        describeMediaInfosInfo.setRegion(regionSh);
        describeMediaInfosInfo.setFileId(fileId);
        describeMediaInfosInfo.setSubAppId(vodSubAppId);
        DescribeMediaInfosResponse describemediainfos = DescribeMediaInfos.describemediainfos(describeMediaInfosInfo);
        if (describemediainfos != null) {
            String resultJsonStr = DescribeMediaInfosResponse.toJsonString(describemediainfos);
            JSONObject jsonObject = JSONObject.parseObject(resultJsonStr);
            //JSONObject mediaInfoSet = jsonObject.getJSONObject("MediaInfoSet");
            JSONArray jsonArray = jsonObject.getJSONArray("MediaInfoSet");
            if (ObjectUtils.isNotEmpty(jsonArray)){
                JSONObject o = jsonArray.getJSONObject(0);
                JSONObject basicInfo = o.getJSONObject("BasicInfo");
                JSONObject metaData = o.getJSONObject("MetaData");
//          BasicInfo  获取视频封面CoverUrl
//            MetaData     Duration
                String coverUrl = basicInfo.getString("CoverUrl");
                log.info("coverUrl==={}",coverUrl);
                // 时长   秒
                Integer duration = metaData.getInteger("Duration");
                // 文件大小  字节
                Long size = metaData.getLong("Size");
                log.info("duration==={}",duration);
                log.info("size==={}",size);
                VodMetaData vodMetaData = new VodMetaData();
                vodMetaData.setDuration(duration);
                vodMetaData.setSize(size);
                vodMetaData.setCoverUrl(coverUrl);
                return vodMetaData;
            }
        }
        return null;
    }
}
