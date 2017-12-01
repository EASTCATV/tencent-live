package cn.godsdo.dubbo.impl.com;

import cn.godsdo.dto.com.GetComVideoShareUrlDto;
import cn.godsdo.dubbo.live.LiveDatService;
import cn.godsdo.entity.com.ComVideoDat;
import cn.godsdo.entity.com.ComVideoGroup;
import cn.godsdo.entity.com.LiveBackVideo;
import cn.godsdo.entity.com.VideoShareRecord;
import cn.godsdo.entity.live.LiveConfig;
import cn.godsdo.enums.live.VideoTypeEnum;
import cn.godsdo.mapper.com.ComVideoDatMapper;
import cn.godsdo.mapper.com.LiveBackVideoMapper;
import cn.godsdo.mapper.com.VideoShareRecordMapper;
import cn.godsdo.dubbo.com.VideoShareRecordService;
import cn.godsdo.mapper.live.LiveConfigMapper;
import cn.godsdo.util.FileTokenUtil;
import cn.godsdo.util.R;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.constant.Constants;
import com.y20y.interfaces.IdService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 视频分享记录表 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2024-06-29
 */
@DubboService
public class VideoShareRecordServiceImpl extends ServiceImpl<VideoShareRecordMapper, VideoShareRecord> implements VideoShareRecordService {
    @Resource
    ComVideoDatMapper comVideoDatMapper;
    @Resource
    LiveBackVideoMapper liveBackVideoMapper;
    @Resource
    LiveConfigMapper liveConfigMapper;
    @Resource
    private FileTokenUtil fileTokenUtil;
    @DubboReference
    private IdService idService;
    @DubboReference
    private LiveDatService liveDatService;

    @Override
    public R getComVideoShareUrl(Long accountId, Long comId, GetComVideoShareUrlDto dto) {
        Long videoId = dto.getVideoId();
        Integer day = dto.getDay();
        Integer type = dto.getType();
        if (type == 1) {
            day = 365 * 2;
        }
        Integer videoType = dto.getVideoType();
        VideoShareRecord videoShareRecord = new VideoShareRecord();
        Long id = idService.nextId();
        videoShareRecord.setId(id);
        videoShareRecord.setComId(comId);
        videoShareRecord.setVideoId(videoId);
        videoShareRecord.setVideoType(videoType);
        videoShareRecord.setCreateBy(accountId);
        videoShareRecord.setType(type);
        videoShareRecord.setShareDay(day);

        if (VideoTypeEnum.MEDIA.getValue() == videoType) {
            ComVideoDat comVideoDat = comVideoDatMapper.selectById(videoId);
            videoShareRecord.setFileId(comVideoDat.getFileId());
            videoShareRecord.setVideoName(comVideoDat.getVideoName());
            videoShareRecord.setMobileThemeType(1);
        } else {
            LiveBackVideo liveBackVideo = liveBackVideoMapper.selectById(videoId);
            videoShareRecord.setFileId(liveBackVideo.getFileId());
            videoShareRecord.setVideoName(liveBackVideo.getVideoName());
            LiveConfig liveConfig = liveConfigMapper.selectOne(Wrappers.<LiveConfig>lambdaQuery()
                    .eq(LiveConfig::getLiveId, liveBackVideo.getLiveId()));
            if (ObjectUtils.isNotEmpty(liveConfig)) {
                videoShareRecord.setMobileThemeType(liveConfig.getMobileThemeType());
            }else{
                videoShareRecord.setMobileThemeType(1);
            }
        }
        String token = fileTokenUtil.getToken(videoShareRecord.getFileId(), day);
        videoShareRecord.setToken(token);
        this.baseMapper.insert(videoShareRecord);
        String domain = liveDatService.getShareComDomainUrl(comId, accountId);
        String url = domain + Constants.SLASH + "video" + Constants.SLASH +id;
        return R.ok(url);
    }
}
