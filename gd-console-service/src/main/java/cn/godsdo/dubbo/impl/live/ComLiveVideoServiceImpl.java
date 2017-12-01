package cn.godsdo.dubbo.impl.live;

import cn.godsdo.dto.live.AddLiveVideoDto;
import cn.godsdo.dubbo.live.ComLiveVideoService;
import cn.godsdo.entity.com.ComLiveVideo;
import cn.godsdo.entity.com.ComVideoDat;
import cn.godsdo.entity.com.LiveBackVideo;
import cn.godsdo.mapper.com.ComVideoDatMapper;
import cn.godsdo.mapper.com.LiveBackVideoMapper;
import cn.godsdo.mapper.live.ComLiveVideoMapper;
import cn.godsdo.util.R;
import cn.godsdo.vo.live.GetRoomVideoVo;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.interfaces.IdService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 用户直播间视频配置表 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2018-04-12
 */
@DubboService
public class ComLiveVideoServiceImpl extends ServiceImpl<ComLiveVideoMapper, ComLiveVideo> implements ComLiveVideoService {

    @Resource
    private ComVideoDatMapper comVideoDatMapper;
    @Resource
    private LiveBackVideoMapper liveBackVideoMapper;
    @DubboReference
    IdService idService;


    @Override
    public R deleteRoomVideo(Long videoId, Long comId) {
        // 删除指定comId和videoId的房间视频记录
        this.baseMapper.delete(Wrappers.<ComLiveVideo>update().lambda().eq(ComLiveVideo::getVideoId, videoId).eq(ComLiveVideo::getComId, comId));
        return R.ok();
    }

    @Override
    public R addLiveVideo(Long comId, Long accountId, AddLiveVideoDto addLiveVideoDto) {
        // 获取需要绑定的视频信息和房间ID
        List<Long> checkIds = addLiveVideoDto.getCheckIds();
        Long liveId = addLiveVideoDto.getLiveId();
        List<String> ids = addLiveVideoDto.getIds();
        List<String> backIds = addLiveVideoDto.getBackIds();
        List<ComLiveVideo> needBindVideos = new ArrayList<ComLiveVideo>();

        // 删除不在checkIds列表中的房间视频
        this.baseMapper.delete(Wrappers.<ComLiveVideo>update().lambda().eq(ComLiveVideo::getLiveId, liveId).notIn(ObjectUtils.isNotEmpty(checkIds), ComLiveVideo::getVideoId, checkIds));

        // 绑定媒体库视频
        if (ids.size() != 0) {
            for (String idStr : ids) {
                Long id = Long.valueOf(idStr);
                ComVideoDat comVideoDat = comVideoDatMapper.selectOne(Wrappers.<ComVideoDat>lambdaQuery().eq(ComVideoDat::getId, id));
                ComLiveVideo comLiveVideo = new ComLiveVideo(idService.nextId(), comId, liveId, id, comVideoDat.getOssM3u8(), comVideoDat.getVideoName(), 2, comVideoDat.getImage(), accountId);
                comLiveVideo.setFileId(comVideoDat.getFileId());
                needBindVideos.add(comLiveVideo);
            }
        }

        // 绑定直播回放视频
        if (backIds.size() != 0) {
            for (String idStr : backIds) {
                Long id = Long.valueOf(idStr);
                LiveBackVideo liveBackVideo = liveBackVideoMapper.selectOne(Wrappers.<LiveBackVideo>lambdaQuery().eq(LiveBackVideo::getId, id));
                if (null != liveBackVideo) {
                    ComLiveVideo comLiveVideo = new ComLiveVideo(idService.nextId(), comId, liveId, id, liveBackVideo.getM3u8Url(), liveBackVideo.getVideoName(), 2, liveBackVideo.getImage(), accountId);
                    comLiveVideo.setFileId(liveBackVideo.getFileId());
                    needBindVideos.add(comLiveVideo);
                }
            }
        }

        // 添加需要绑定的视频信息到数据库
        if (ObjectUtils.isNotEmpty(needBindVideos)) {
            this.baseMapper.insertBatch(needBindVideos);
        }
        // 返回操作成功结果
        return R.ok();
    }

    @Override
    public R getCheckVideoList(Long comId, Long roomId) {
        List<GetRoomVideoVo> list = this.baseMapper.getCheckVideoList(comId, roomId);
        return R.ok(list);
    }

    @Override
    public R delRoomVideoByFitment(Long comId, Long liveId) {
        this.baseMapper.delete(Wrappers.<ComLiveVideo>update().lambda().eq(ComLiveVideo::getComId, comId).eq(ComLiveVideo::getLiveId, liveId));
        return R.ok();
    }

    @Override
    public R updateVideoName(Long accountId, Long comId, Long id, String name) {
//        this.baseMapper.delete(Wrappers.<ComLiveVideo>update().lambda().eq(ComLiveVideo::getComId, comId).eq(ComLiveVideo::getRoomId, roomId));
        this.baseMapper.update(null,new LambdaUpdateWrapper<ComLiveVideo>().set(ComLiveVideo::getName, name)
                .set(ComLiveVideo::getUpdateBy, accountId).eq(ComLiveVideo::getVideoId,id).eq(ComLiveVideo::getComId,comId));
        return R.ok();
    }


}
