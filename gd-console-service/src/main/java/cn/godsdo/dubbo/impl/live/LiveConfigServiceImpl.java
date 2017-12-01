package cn.godsdo.dubbo.impl.live;

import cn.godsdo.dto.live.UpdateGiftRewardDto;
import cn.godsdo.dto.live.UpdateHeatDto;
import cn.godsdo.dto.live.UpdateReplayDto;
import cn.godsdo.dubbo.cache.ClearCache;
import cn.godsdo.dubbo.im.ImService;
import cn.godsdo.dubbo.live.ComGiftConfigService;
import cn.godsdo.dubbo.live.LiveConfigService;
import cn.godsdo.entity.com.ComVideoDat;
import cn.godsdo.entity.com.LiveBackVideo;
import cn.godsdo.entity.live.LiveBindBack;
import cn.godsdo.entity.live.LiveConfig;
import cn.godsdo.enums.im.TIMSendTypeEnum;
import cn.godsdo.enums.live.LiveBackTypeEnum;
import cn.godsdo.enums.live.VideoTypeEnum;
import cn.godsdo.mapper.com.ComVideoDatMapper;
import cn.godsdo.mapper.com.LiveBackVideoMapper;
import cn.godsdo.mapper.live.LiveBindBackMapper;
import cn.godsdo.mapper.live.LiveConfigMapper;
import cn.godsdo.query.im.ImMsgQuery;
import cn.godsdo.util.R;
import cn.godsdo.vo.live.GetBackSetVo;
import cn.hutool.json.JSONObject;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.interfaces.IdService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * <p>
 * 房间配置信息表 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2018/11/18
 */
@DubboService
public class LiveConfigServiceImpl extends ServiceImpl<LiveConfigMapper, LiveConfig> implements LiveConfigService {

    @DubboReference
    ComGiftConfigService comGiftConfigService;
    @DubboReference
    IdService idService;
    @Resource
    LiveBindBackMapper liveBindBackMapper;
    @Resource
    ComVideoDatMapper comVideoDatMapper;
    @Resource
    LiveBackVideoMapper liveBackVideoMapper;
    @DubboReference(check = false)
    ClearCache clearcache;
    @DubboReference(check = false, retries = 0)
    private ImService imService;

    @Override
//    @CacheEvict(key = "#dto.liveId", value = CacheConstants.ROOM_WATCH_BASE_INFO)
    public R updateReplay(Long comId, Long accountId, UpdateReplayDto dto) {
        Long liveId = dto.getLiveId();
        //删除缓存
        clearcache.delLiveCache(comId, liveId);
        LiveConfig liveConfig = getLiveConfig(comId, liveId);
        if (ObjectUtils.isEmpty(liveConfig)) {
            return R.failed("操作的直播间不存在");
        }
        Boolean replayEnable = dto.getReplayEnable();
        // 回放开关
        liveConfig.setReplayEnable(replayEnable);
        if (replayEnable) {
            Boolean videoExpiredEnable = dto.getVideoExpiredEnable();
            liveConfig.setVideoExpiredEnable(videoExpiredEnable);
            // 回放过期开启后，设置过期回看时间
            if (videoExpiredEnable) {
                liveConfig.setVideoExpiredTime(dto.getVideoExpiredTime());
            }
            // 回看设置绑定
            insertBindBack(comId, accountId, dto);
        } else {
            // 回放开关关闭后，过期开启恢复默认
            liveConfig.setVideoExpiredEnable(false);
            // 回看过期时间恢复默认
            liveConfig.setVideoExpiredTime(null);
        }
        liveConfig.setUpdateBy(accountId);
        this.baseMapper.updateById(liveConfig);
//        if (replayEnable) {
//            LiveBindBack liveBindBack = liveBindBackMapper.selectOne(new LambdaQueryWrapper<LiveBindBack>().eq(LiveBindBack::getComId, comId).eq(LiveBindBack::getLiveId, liveId));
//            if (ObjectUtils.isEmpty(liveBindBack)) {
//                liveBindBack = new LiveBindBack();
//                liveBindBack.setComId(comId);
//                liveBindBack.setLiveId(liveId);
//                liveBindBack.setType(dto.getType());
//                liveBindBack.setVideoId(dto.getVideoId());
//                liveBindBack.setVideoType(dto.getVideoType());
//                liveBindBack.setCreateBy(accountId);
//                liveBindBackMapper.insert(liveBindBack);
//            } else {
//                liveBindBack.setType(dto.getType());
//                liveBindBack.setVideoId(dto.getVideoId());
//                liveBindBack.setVideoType(dto.getVideoType());
//                liveBindBack.setUpdateBy(accountId);
//                liveBindBackMapper.updateById(liveBindBack);
//            }
//        }else{
//            liveBindBackMapper.delete(new LambdaQueryWrapper<LiveBindBack>()
//                    .eq(LiveBindBack::getLiveId, dto.getLiveId()));
//        }

        return R.ok();
    }

    @Override
    public R getBackSet(Long comId, Long liveId) {
//        GetBackSetVo lc = this.baseMapper.getBackSet(comId, liveId);
        LiveConfig lc = this.baseMapper.selectOne(new LambdaQueryWrapper<LiveConfig>().select(LiveConfig::getId, LiveConfig::getComId,
                        LiveConfig::getReplayEnable, LiveConfig::getVideoExpiredEnable, LiveConfig::getVideoExpiredTime)
                .eq(LiveConfig::getComId, comId).eq(LiveConfig::getLiveId, liveId));
        GetBackSetVo vo = new GetBackSetVo();
        vo.setComId(comId);
        vo.setLiveId(liveId);
        vo.setReplayEnable(lc.getReplayEnable());
        vo.setVideoExpiredEnable(lc.getVideoExpiredEnable());
        vo.setVideoExpiredTime(lc.getVideoExpiredTime());
        if (lc.getReplayEnable()) {
            LiveBindBack liveBindBack = liveBindBackMapper.selectOne(new LambdaQueryWrapper<LiveBindBack>().eq(LiveBindBack::getComId, comId).eq(LiveBindBack::getLiveId, liveId));
            if (ObjectUtils.isNotEmpty(liveBindBack)) {
                Integer type = liveBindBack.getType();
                vo.setType(type);
                Long videoId = liveBindBack.getVideoId();
                if (ObjectUtils.isNotEmpty(videoId) && videoId != 0) {
                    Integer videoType = liveBindBack.getVideoType();
                    vo.setVideoId(videoId);
                    vo.setVideoType(videoType);
                    // 获取视频名称
                    // 媒体库
                    if (VideoTypeEnum.MEDIA.getValue() == videoType) {
                        ComVideoDat comVideoDat = comVideoDatMapper.selectOne(new LambdaQueryWrapper<ComVideoDat>().eq(ComVideoDat::getComId, comId).eq(ComVideoDat::getId, videoId));
                        if (ObjectUtils.isNotEmpty(comVideoDat)) {
                            vo.setVideoName(comVideoDat.getVideoName());
                        }
                    } else {
                        LiveBackVideo liveBackVideo = liveBackVideoMapper.selectOne(new LambdaQueryWrapper<LiveBackVideo>().eq(LiveBackVideo::getComId, comId).eq(LiveBackVideo::getId, videoId));
                        if (ObjectUtils.isNotEmpty(liveBackVideo)) {
                            vo.setVideoName(liveBackVideo.getVideoName());
                        }
                    }
                }
            }
        }
        return R.ok(vo);
    }

    @Override
    public R getHeat(Long comId, Long liveId) {
        LiveConfig lc = this.baseMapper.selectOne(new LambdaQueryWrapper<LiveConfig>().select(LiveConfig::getId, LiveConfig::getComId,
                        LiveConfig::getHeatEnable, LiveConfig::getOnlineMultiple, LiveConfig::getLookBase)
                .eq(LiveConfig::getComId, comId).eq(LiveConfig::getLiveId, liveId));
        return R.ok(lc);
    }

    /**
     * 回看设置绑定
     *
     * @param comId
     * @param accountId
     * @param dto
     */
    private void insertBindBack(Long comId, Long accountId, UpdateReplayDto dto) {
        Long liveId = dto.getLiveId();
        //删除缓存
        clearcache.delLiveCache(comId, liveId);
//        liveBindBackMapper.delete(new LambdaQueryWrapper<LiveBindBack>()
//                .eq(LiveBindBack::getLiveId, dto.getLiveId()));
//        Boolean replayEnable = dto.getReplayEnable();
//        if(replayEnable){
        LiveBindBack liveBindBack = liveBindBackMapper.selectOne(new LambdaQueryWrapper<LiveBindBack>().eq(LiveBindBack::getComId, comId).eq(LiveBindBack::getLiveId, liveId));
            if (ObjectUtils.isEmpty(liveBindBack)) {
                liveBindBack = new LiveBindBack();
                liveBindBack.setId(idService.nextId());
                liveBindBack.setComId(comId);
                liveBindBack.setCreateBy(accountId);
                liveBindBack.setLiveId(dto.getLiveId());
                liveBindBack.setType(dto.getType());
                liveBindBack.setVideoType(dto.getVideoType());
                liveBindBack.setVideoExpiredTime(dto.getVideoExpiredTime());
                if (LiveBackTypeEnum.TWO.getValue() == dto.getType()) {
                    liveBindBack.setVideoId(dto.getVideoId());
                }
                liveBindBackMapper.insert(liveBindBack);
            }else{
                liveBindBack.setType(dto.getType());
                liveBindBack.setVideoType(dto.getVideoType());
                liveBindBack.setVideoExpiredTime(dto.getVideoExpiredTime());
                if (LiveBackTypeEnum.TWO.getValue() == dto.getType()) {
                    liveBindBack.setVideoId(dto.getVideoId());
                }
                liveBindBackMapper.updateById(liveBindBack);
            }

//        }
    }

    @Override
//    @CacheEvict(key = "#dto.liveId", value = CacheConstants.ROOM_WATCH_BASE_INFO)
    public R updateHeat(Long comId, Long accountId, UpdateHeatDto dto) {
        Long liveId = dto.getLiveId();
        //删除缓存
        clearcache.delLiveCache(comId, liveId);
        // 获取直播间配置信息
        LiveConfig liveConfig = getLiveConfig(comId, dto.getLiveId());
        // 如果直播间配置信息为空，则返回操作的直播间不存在
        if (ObjectUtils.isEmpty(liveConfig)) {
            return R.failed("操作的直播间不存在");
        }
        liveConfig.setHeatEnable(dto.getHeatEnable());
        if (!dto.getHeatEnable()) {
            // 更新在线人数倍数和固定观看人数基数
            liveConfig.setOnlineMultiple(0);
            liveConfig.setLookBase(0);
        } else {
            // 更新在线人数倍数和固定观看人数基数
            liveConfig.setOnlineMultiple(dto.getOnlineMultiple());
            liveConfig.setLookBase(dto.getLookBase());
        }

        // 设置更新人并更新数据库中的直播间配置信息
        liveConfig.setUpdateBy(accountId);
        this.baseMapper.updateById(liveConfig);
        return R.ok();
    }

    @Override
    public R getGiftInfo(Long comId, Long liveId) {
        LiveConfig lc = this.baseMapper.selectOne(new LambdaQueryWrapper<LiveConfig>().select(LiveConfig::getId, LiveConfig::getComId,
                        LiveConfig::getRewardEnable, LiveConfig::getRewardQuickEnable, LiveConfig::getRewardMoney,
                        LiveConfig::getGiftGivingEnable, LiveConfig::getRedpackEnable)
                .eq(LiveConfig::getComId, comId).eq(LiveConfig::getLiveId, liveId));
        return R.ok(lc);
    }

    /**
     * 获取直播间配置信息
     *
     * @param comId
     * @param liveId
     * @return
     */
    private LiveConfig getLiveConfig(Long comId, Long liveId) {
        LiveConfig liveConfig = this.baseMapper.selectOne(new LambdaQueryWrapper<LiveConfig>()
                .eq(LiveConfig::getComId, comId)
                .eq(LiveConfig::getLiveId, liveId)
                .eq(LiveConfig::getDeleteFlg, false));
        return liveConfig;
    }


    @Override
    public R updateGift(Long comId, Long accountId, LiveConfig liveConfig) {
        Long liveId = liveConfig.getLiveId();
        //删除缓存
        clearcache.delLiveCache(comId, liveId);

        LiveConfig config = getLiveConfig(comId, liveConfig.getLiveId());
        if (ObjectUtils.isEmpty(config)) {
            return R.failed("操作的直播间不存在");
        }

//        打赏：0，未开启1开启
        config.setRewardEnable(liveConfig.getRewardEnable());
//        0-开启快捷打赏 1-关闭快捷打赏
        config.setRewardQuickEnable(liveConfig.getRewardQuickEnable());
        // 快捷打赏金额，以逗号分隔开，如（1,5,10）
        config.setRewardMoney(liveConfig.getRewardMoney());
        // 送礼物是否可用 1-是 0-否
        config.setGiftGivingEnable(liveConfig.getGiftGivingEnable());
        // 需要判断是否存在礼物
        if (liveConfig.getGiftGivingEnable()) {
            comGiftConfigService.addDefaultGifts(comId, accountId);
        }
        // 红包是否可用 0- 不可用， 1- 可用
        config.setRedpackEnable(liveConfig.getRedpackEnable());
        this.baseMapper.updateById(config);
        return R.ok();
    }

    @Override
    //@CacheEvict(key = "#liveId", value = CacheConstants.ROOM_WATCH_BASE_INFO)
    public R updateCommodityEnable(Long comId, Long accountId, Boolean enabled, Long liveId,String imGroupId) {
        //删除缓存
        clearcache.delLiveCache(comId, liveId);

        LiveConfig config = getLiveConfig(comId, liveId);
        if (ObjectUtils.isEmpty(config)) {
            return R.failed("操作的直播间不存在");
        }
        // 商品是否可用 1-是 0-否
        config.setCommodityEnable(enabled);
        config.setUpdateBy(accountId);
        this.baseMapper.updateById(config);
        if (StringUtils.isNotBlank(imGroupId)) {
            ImMsgQuery msgQuery = new ImMsgQuery();
            JSONObject json = new JSONObject();
            json.set("userInfo", null);
            json.set("commodityEnable", enabled ? 1 : 0);
            msgQuery.setCloudCustomData(JSON.toJSONString(json));
            msgQuery.setNotifyTypeEnum(TIMSendTypeEnum.COMMODITY_ENABLE);
            msgQuery.setMsgContent("");
            msgQuery.setGroupId(imGroupId);
            msgQuery.setFromUserId("administrator");
            imService.sendCustomGroupMsg(msgQuery);
        }
        return R.ok();
    }

    @Override
    //@CacheEvict(key = "#dto.liveId", value = CacheConstants.ROOM_WATCH_BASE_INFO)
    public R updateGiftReward(Long comId, Long accountId, UpdateGiftRewardDto dto) {
        Long liveId = dto.getLiveId();
        //删除缓存
        clearcache.delLiveCache(comId, liveId);

        LiveConfig config = getLiveConfig(comId, liveId);
        if (ObjectUtils.isEmpty(config)) {
            return R.failed("操作的直播间不存在");
        }
        // 红包是否可用
        config.setRedpackEnable(dto.getRedpackEnable());
        // 送礼物是否可用
        config.setGiftGivingEnable(dto.getGiftGivingEnable());
        // 打赏
        config.setRewardEnable(dto.getRewardEnable());
        // 开启快捷打赏
        config.setRewardQuickEnable(dto.getRewardQuickEnable());
        // 快捷打赏金额
        config.setRewardMoney(dto.getRewardMoney());
        this.baseMapper.updateById(config);
        return R.ok();
    }

    @Override
    public R getCommodityEnable(Long comId, Long liveId) {
        LiveConfig liveConfig = this.baseMapper.selectOne(new LambdaQueryWrapper<LiveConfig>()
                .select(LiveConfig::getCommodityEnable)
                .eq(LiveConfig::getComId, comId)
                .eq(LiveConfig::getLiveId, liveId)
                .eq(LiveConfig::getDeleteFlg, false));
        return R.ok(liveConfig);
    }
}
