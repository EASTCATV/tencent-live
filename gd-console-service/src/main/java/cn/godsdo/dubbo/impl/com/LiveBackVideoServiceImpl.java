package cn.godsdo.dubbo.impl.com;

import cn.godsdo.dto.live.tencent.VodMetaData;
import cn.godsdo.dto.mediaLibrary.GetAllBackVideoListDto;
import cn.godsdo.dubbo.cache.ClearCache;
import cn.godsdo.dubbo.com.LiveBackVideoService;
import cn.godsdo.dubbo.tencent.TVodFileService;
import cn.godsdo.entity.TencentTask;
import cn.godsdo.entity.com.LiveBackVideo;
import cn.godsdo.entity.live.LiveConfig;
import cn.godsdo.entity.live.LiveDat;
import cn.godsdo.enums.live.LiveBackStatusEnums;
import cn.godsdo.enums.live.LiveThemeEnum;
import cn.godsdo.mapper.TencentTaskMapper;
import cn.godsdo.mapper.com.LiveBackVideoMapper;
import cn.godsdo.mapper.live.LiveConfigMapper;
import cn.godsdo.mapper.live.LiveDatMapper;
import cn.godsdo.util.R;
import cn.godsdo.vo.GetBackVideoBySplitScreen;
import cn.godsdo.vo.channel.GetBackVideoListVo;
import cn.hutool.core.thread.ThreadUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.base.ProcessMediaByProcedureInfo;
import com.y20y.interfaces.IdService;
import com.y20y.vod.ProcessMediaByProcedure;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static cn.godsdo.util.TimeUtil.convertSecondsToHMS;

/**
 * <p>
 * 直播间回看视频表 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2018-04-11
 */
@Slf4j
@DubboService
public class LiveBackVideoServiceImpl extends ServiceImpl<LiveBackVideoMapper, LiveBackVideo> implements LiveBackVideoService {

    @Resource
    LiveDatMapper liveDatMapper;
    @Resource
    LiveConfigMapper liveConfigMapper;
    @Resource
    TencentTaskMapper tencentTaskMapper;
    @DubboReference
    IdService idService;
    //@DubboReference
    //TencentApiService tencentApiService;
    @DubboReference(check = false)
    TVodFileService tvodfileservice;
    @Value("${Tencent.secretId}")
    private String secretId;

    @Value("${Tencent.secretKey}")
    private String secretKey;
    @Value("${Tencent.vod.subAppId}")
    private Long subAppId;
    @DubboReference(check = false)
    ClearCache clearcache;

    @Override
    public R getBackVideoList(GetAllBackVideoListDto dto, Long comId) {
//        Integer mobileThemeType = dto.getMobileThemeType();
//        LiveThemeEnum.SPLIT_SCREEN.getValue() == mobileThemeType
        if (false) {
            // 创建分页对象，指定当前页和每页记录数
            IPage<LiveBackVideo> iPage = new Page<>(dto.getPage(), dto.getPageSize());
            // 调用baseMapper的getBackVideoList方法获取回放视频列表
            IPage<LiveBackVideo> vo = this.baseMapper.getBackLiveKeyList(iPage, dto, comId);
            List<LiveBackVideo> records = vo.getRecords();
            List<GetBackVideoBySplitScreen> getBackVideoBySplitScreenList = new ArrayList<>();
            if (ObjectUtils.isNotEmpty(records)) {
                getBackVideoBySplitScreenList = records.stream().map(e -> {
                    GetBackVideoBySplitScreen list = new GetBackVideoBySplitScreen();
                    if (ObjectUtils.isNotEmpty(e)) {
                        Long liveKey = e.getLiveKey();

                        List<LiveBackVideo> liveBackVideos = this.baseMapper.selectList(new LambdaQueryWrapper<LiveBackVideo>()
                                .eq(LiveBackVideo::getLiveKey, liveKey).eq(LiveBackVideo::getComId, comId));
                        if (ObjectUtils.isNotEmpty(liveBackVideos)) {
                            LiveBackVideo liveBackVideo = liveBackVideos.get(0);
                            list.setId(liveKey);
                            list.setLiveName(liveBackVideo.getLiveName());
                            list.setRecordEndTime(liveBackVideo.getRecordEndTime());
                            list.setRecordStartTime(liveBackVideo.getRecordStartTime());
                            list.setShowId(liveBackVideo.getLiveShowId());
                            Integer videoDuration = liveBackVideo.getVideoDuration();

                            String s = convertSecondsToHMS(videoDuration.longValue());
                            list.setTime(s);
                            ArrayList<GetBackVideoListVo> children = new ArrayList<>();
                            for (LiveBackVideo backVideo : liveBackVideos) {
                                String fileId = backVideo.getFileId();
                                // 再次判断是否存在图片
                                String image = backVideo.getImage();
                                if (StringUtils.isBlank(image) && StringUtils.isNotEmpty(fileId)) {
                                    VodMetaData describemediainfos = tvodfileservice.describemediainfos(fileId);
                                    if (ObjectUtils.isNotEmpty(describemediainfos)) {
                                        String coverUrl = describemediainfos.getCoverUrl();
                                        if (StringUtils.isNotBlank(coverUrl)) {
                                            backVideo.setImage(describemediainfos.getCoverUrl());
                                            this.baseMapper.updateById(backVideo);
                                        } else {
                                            asynchronousGetCoverUrl(fileId);
                                        }
                                    }
                                }
                                GetBackVideoListVo getBackVideoListVo = new GetBackVideoListVo();
                                BeanUtils.copyProperties(backVideo, getBackVideoListVo);
                                getBackVideoListVo.setTime(s);
                                children.add(getBackVideoListVo);
                            }
                            list.setChildren(children);
                        }
                    }

                    return list;
                }).toList();

            }
            return R.ok(getBackVideoBySplitScreenList, vo.getTotal());
        } else {
            // 创建分页对象，指定当前页和每页记录数
            IPage<LiveBackVideo> iPage = new Page<>(dto.getPage(), dto.getPageSize());
            // 调用baseMapper的getBackVideoList方法获取回放视频列表
            IPage<LiveBackVideo> vo = this.baseMapper.getBackVideoList(iPage, dto, comId);
            List<GetBackVideoListVo> collect = vo.getRecords().stream().map(e -> {
                GetBackVideoListVo getBackVideoListVo = new GetBackVideoListVo();
                BeanUtils.copyProperties(e, getBackVideoListVo);
                Integer videoDuration = e.getVideoDuration();
                String fileId = e.getFileId();
                if (ObjectUtils.isNotEmpty(videoDuration)) {
                    String s = convertSecondsToHMS(videoDuration.longValue());
                    getBackVideoListVo.setTime(s);
                } else {

                    if (StringUtils.isNotEmpty(fileId)) {
                        VodMetaData describemediainfos = tvodfileservice.describemediainfos(fileId);
                        if (ObjectUtils.isNotEmpty(describemediainfos)) {
                            Integer duration = describemediainfos.getDuration();
                            e.setVideoDuration(duration);
                            e.setVideoSize(describemediainfos.getSize());
                            e.setImage(describemediainfos.getCoverUrl());
                            this.baseMapper.updateById(e);
                            getBackVideoListVo.setVideoDuration(duration);
                            String s = convertSecondsToHMS(duration.longValue());
                            getBackVideoListVo.setTime(s);
                        } else {
                            getBackVideoListVo.setVideoDuration(0);
                            getBackVideoListVo.setTime("0");
                        }

                    }
                }
                // 再次判断是否存在图片
                String image = e.getImage();
                if (StringUtils.isBlank(image)) {
                    VodMetaData describemediainfos = tvodfileservice.describemediainfos(fileId);
                    if (ObjectUtils.isNotEmpty(describemediainfos)) {
                        String coverUrl = describemediainfos.getCoverUrl();
                        if (StringUtils.isNotBlank(coverUrl)) {
                            e.setImage(describemediainfos.getCoverUrl());
                            this.baseMapper.updateById(e);
                        } else {
                            asynchronousGetCoverUrl(fileId);
                        }
                    }
                }

                return getBackVideoListVo;
            }).collect(Collectors.toList());
            // 封装查询结果并返回
            return R.ok(collect, vo.getTotal());
        }
    }

    @Override
    public R getBackVideoListByFitment(GetAllBackVideoListDto dto, Long comId) {
//        Integer mobileThemeType = dto.getMobileThemeType();
//        LiveThemeEnum.SPLIT_SCREEN.getValue() == mobileThemeType
        if (false) {
            // 创建分页对象，指定当前页和每页记录数
            IPage<LiveBackVideo> iPage = new Page<>(dto.getPage(), dto.getPageSize());
            // 调用baseMapper的getBackVideoList方法获取回放视频列表
            IPage<LiveBackVideo> vo = this.baseMapper.getBackLiveKeyList(iPage, dto, comId);
            List<LiveBackVideo> records = vo.getRecords();
            List<GetBackVideoBySplitScreen> getBackVideoBySplitScreenList = new ArrayList<>();
            if (ObjectUtils.isNotEmpty(records)) {
                getBackVideoBySplitScreenList = records.stream().map(e -> {
                    GetBackVideoBySplitScreen list = new GetBackVideoBySplitScreen();
                    if (ObjectUtils.isNotEmpty(e)) {
                        Long liveKey = e.getLiveKey();

                        List<LiveBackVideo> liveBackVideos = this.baseMapper.selectList(new LambdaQueryWrapper<LiveBackVideo>()
                                .eq(LiveBackVideo::getLiveKey, liveKey).eq(LiveBackVideo::getComId, comId));
                        if (ObjectUtils.isNotEmpty(liveBackVideos)) {
                            LiveBackVideo liveBackVideo = liveBackVideos.get(0);
                            list.setId(liveKey);
                            list.setLiveName(liveBackVideo.getLiveName());
                            list.setRecordEndTime(liveBackVideo.getRecordEndTime());
                            list.setRecordStartTime(liveBackVideo.getRecordStartTime());
                            list.setShowId(liveBackVideo.getLiveShowId());
                            Integer videoDuration = liveBackVideo.getVideoDuration();

                            String s = convertSecondsToHMS(videoDuration.longValue());
                            list.setTime(s);
                            ArrayList<GetBackVideoListVo> children = new ArrayList<>();
                            for (LiveBackVideo backVideo : liveBackVideos) {
                                String fileId = backVideo.getFileId();
                                // 再次判断是否存在图片
                                String image = backVideo.getImage();
                                if (StringUtils.isBlank(image) && StringUtils.isNotEmpty(fileId)) {
                                    VodMetaData describemediainfos = tvodfileservice.describemediainfos(fileId);
                                    if (ObjectUtils.isNotEmpty(describemediainfos)) {
                                        String coverUrl = describemediainfos.getCoverUrl();
                                        if (StringUtils.isNotBlank(coverUrl)) {
                                            backVideo.setImage(describemediainfos.getCoverUrl());
                                            this.baseMapper.updateById(backVideo);
                                        } else {
                                            asynchronousGetCoverUrl(fileId);
                                        }
                                    }
                                }
                                GetBackVideoListVo getBackVideoListVo = new GetBackVideoListVo();
                                BeanUtils.copyProperties(backVideo, getBackVideoListVo);
                                getBackVideoListVo.setTime(s);
                                children.add(getBackVideoListVo);
                            }
                            list.setChildren(children);
                        }
                    }

                    return list;
                }).toList();

            }
            return R.ok(getBackVideoBySplitScreenList, vo.getTotal());
        } else {
            Page<LiveBackVideo> page = new Page<>(dto.getPage(), dto.getPageSize());
            IPage<LiveBackVideo> roomBackVideoIPage = this.baseMapper.getBackVideoListByFitment(page, dto, comId);
            List<GetBackVideoListVo> collect = roomBackVideoIPage.getRecords().stream().map(e -> {
                GetBackVideoListVo getBackVideoListVo = new GetBackVideoListVo();
                BeanUtils.copyProperties(e, getBackVideoListVo);
                Integer videoDuration = e.getVideoDuration();

                String fileId = e.getFileId();
                if (ObjectUtils.isNotEmpty(videoDuration)) {
                    String s = convertSecondsToHMS(videoDuration.longValue());
                    getBackVideoListVo.setTime(s);
                } else {
                    VodMetaData describemediainfos = tvodfileservice.describemediainfos(fileId);
                    if (ObjectUtils.isNotEmpty(describemediainfos)) {
                        Integer duration = describemediainfos.getDuration();
                        e.setVideoDuration(duration);
                        e.setVideoSize(describemediainfos.getSize());
                        e.setImage(describemediainfos.getCoverUrl());
                        this.baseMapper.updateById(e);
                        getBackVideoListVo.setVideoDuration(duration);
                        String s = convertSecondsToHMS(duration.longValue());
                        getBackVideoListVo.setTime(s);
                    } else {
                        getBackVideoListVo.setVideoDuration(0);
                        getBackVideoListVo.setTime("0");
                    }
                }
                // 再次判断是否存在图片
                String image = e.getImage();
                if (StringUtils.isBlank(image)) {
                    VodMetaData describemediainfos = tvodfileservice.describemediainfos(fileId);
                    if (ObjectUtils.isNotEmpty(describemediainfos)) {
                        String coverUrl = describemediainfos.getCoverUrl();
                        if (StringUtils.isNotBlank(coverUrl)) {
                            e.setImage(describemediainfos.getCoverUrl());
                            this.baseMapper.updateById(e);
                        } else {
                            asynchronousGetCoverUrl(fileId);
                        }
                    }
                }

                return getBackVideoListVo;
            }).collect(Collectors.toList());
            return R.ok(collect, roomBackVideoIPage.getTotal());
        }
    }


    @Override
    public R deleteBackVideo(Long id, Long comId, Long accountId) {
        // 根据条件查询回放视频
        LiveBackVideo liveBackVideo = this.baseMapper.selectOne(new LambdaUpdateWrapper<LiveBackVideo>()
                .eq(LiveBackVideo::getComId, comId)
                .eq(LiveBackVideo::getId, id)
                .eq(LiveBackVideo::getDeleteFlg, false));
        // 如果回放视频不存在，则返回失败信息
        if (ObjectUtils.isEmpty(liveBackVideo)) {
            return R.failed("删除失败，视频不存在");
        }
        // 将回放视频的删除标记设置为true，并设置更新者的账户ID
        liveBackVideo.setDeleteFlg(1);
        liveBackVideo.setUpdateBy(accountId);
        // 更新回放视频信息
        this.baseMapper.updateById(liveBackVideo);
        // 返回操作成功信息
        return R.ok();
    }

    @Override
    public R backVideoDatDeleteBatch(List<Long> ids, Long comId, Long accountId) {
        this.baseMapper.backVideoDatDeleteBatch(ids, comId, accountId);
        return R.ok();
    }

    @Override
    public R updateBackVideoDatGroupBatch(List<Long> ids, Long groupId, Long comId, Long accountId) {
        // 使用传入的 ids、groupId、comId 和 accountId 调用 baseMapper 的方法进行批量更新视频数据的分组信息
        this.baseMapper.updateBackVideoDatGroupBatch(ids, groupId, comId, accountId);

        // 返回更新操作的成功状态
        return R.ok();
    }

    @Override
    public R updateBackVideo(Long id, String videoName, Long comId, Long accountId) {
        // 根据条件查询回放视频
        LiveBackVideo liveBackVideo = this.baseMapper.selectOne(new LambdaUpdateWrapper<LiveBackVideo>()
                .eq(LiveBackVideo::getComId, comId)
                .eq(LiveBackVideo::getId, id)
                .eq(LiveBackVideo::getDeleteFlg, false));
        if (ObjectUtils.isEmpty(liveBackVideo)) {
            return R.failed("修改失败，视频不存在");
        }
        liveBackVideo.setVideoName(videoName);
        liveBackVideo.setUpdateBy(accountId);
        this.baseMapper.updateById(liveBackVideo);
        return R.ok();
    }

    @Override
    public void insertBack(String trtcId, String taskId) {
        LiveDat liveDat = liveDatMapper.selectOne(new LambdaUpdateWrapper<LiveDat>().eq(LiveDat::getTrtcId, trtcId));
        if (ObjectUtils.isNotEmpty(liveDat)) {
            Long comId = liveDat.getComId();
            LiveBackVideo lbv = this.baseMapper.selectOne(new LambdaUpdateWrapper<LiveBackVideo>()
                    .eq(LiveBackVideo::getComId, comId)
                    .eq(LiveBackVideo::getTaskId, taskId)
                    .eq(LiveBackVideo::getDeleteFlg, false));
            Long id = liveDat.getId();
            if (ObjectUtils.isEmpty(lbv)) {
                LiveBackVideo liveBackVideo = new LiveBackVideo();
                liveBackVideo.setId(idService.nextId());
                liveBackVideo.setLiveId(id);
                liveBackVideo.setLiveName(liveDat.getName());
                liveBackVideo.setComId(comId);
                liveBackVideo.setTaskId(taskId);
                liveBackVideo.setVideoName(taskId);
                liveBackVideo.setVideoType(2);
                // 状态为合成中
                liveBackVideo.setStatus(LiveBackStatusEnums.InSynthesis.getValue());
                this.baseMapper.insert(liveBackVideo);
                log.info("视频task记录入库完成，task::{},liveId:{}", taskId, id);
            } else {
                log.info("视频task记录已存在，task::{},liveId:{}", taskId, id);
            }

        }
    }

    @Override
    public void updateBack(String trtcId, String mediaId, String taskId, String fileId, String videoUrl, String cacheFile, Long startTimeStamp, Long endTimeStamp) {
        log.info("updateBack回调完成开始修改数据库，task::{}", taskId);
        // 一个taskID只能生成一个回放视频，如果已存在直接return
        List<LiveBackVideo> liveBackVideos = this.baseMapper.selectList(new LambdaUpdateWrapper<LiveBackVideo>()
                .eq(LiveBackVideo::getTaskId, taskId)
                .eq(LiveBackVideo::getDeleteFlg, false));
        if (ObjectUtils.isNotEmpty(liveBackVideos)) {
            return;
        }
        // 如果没有录制任务，不入库
        List<TencentTask> tencentTasks1 = tencentTaskMapper.findByTaskIds(taskId);
        if (ObjectUtils.isEmpty(tencentTasks1)) {
            return;
        }
//        LiveBackVideo lbv = this.baseMapper.selectOne(new LambdaUpdateWrapper<LiveBackVideo>()
//                .eq(LiveBackVideo::getTaskId, taskId)
//                .eq(LiveBackVideo::getDeleteFlg, false));
        LiveDat liveDat = liveDatMapper.selectOne(new LambdaUpdateWrapper<LiveDat>().eq(LiveDat::getTrtcId, trtcId));
        Long liveId = liveDat.getId();
        LiveConfig liveConfig = liveConfigMapper.selectOne(new LambdaUpdateWrapper<LiveConfig>().eq(LiveConfig::getLiveId, liveId));

//        if (ObjectUtils.isEmpty(lbv)) {
//            log.info("视频task记录不存在，task::{}", taskId);
//        } else {
        LiveBackVideo lbv = new LiveBackVideo();
        lbv.setId(idService.nextId());
        lbv.setLiveShowId(liveDat.getShowId());
        lbv.setLiveId(liveId);
        lbv.setMediaId(mediaId);
        lbv.setComId(liveDat.getComId());
        List<TencentTask> tencentTasks = tencentTaskMapper.selectList(new LambdaUpdateWrapper<TencentTask>().eq(TencentTask::getLiveId, liveId).eq(TencentTask::getTaskId, taskId));
        if (ObjectUtils.isNotEmpty(tencentTasks)) {
            TencentTask tencentTask = tencentTasks.get(0);
            lbv.setLiveKey(tencentTask.getLiveKey());
        }
        lbv.setVideoType(2);
        lbv.setLiveName(liveDat.getName());
        lbv.setTaskId(taskId);
        log.info("updateBack回调完成开始修改数据库，task::{}", JSONObject.toJSONString(lbv));
        lbv.setStatus(LiveBackStatusEnums.OK.getValue());
        //lbv.setVideoName(cacheFile);
        lbv.setVideoName(liveDat.getName());
        if (videoUrl.endsWith(".mp4")) {
            lbv.setOssUrl(videoUrl);
        } else {
            lbv.setM3u8Url(videoUrl);
        }

        lbv.setMobileThemeType(liveConfig.getMobileThemeType());
        lbv.setFileId(fileId);
        // 多路同一时录制
        lbv.setTaskType(3);
        lbv.setRecordStartTime(new Date(startTimeStamp));
        lbv.setRecordEndTime(new Date(endTimeStamp));

        VodMetaData describemediainfos = tvodfileservice.describemediainfos(fileId);
        String coverUrl = describemediainfos.getCoverUrl();
        if (ObjectUtils.isNotEmpty(describemediainfos)) {
            lbv.setVideoDuration(describemediainfos.getDuration());
            lbv.setVideoSize(describemediainfos.getSize());
            lbv.setImage(coverUrl);
        }
        this.baseMapper.insert(lbv);
        log.info("updateBack回调完成修改数据库完成，task::{}", JSONObject.toJSONString(lbv));

        log.info("删除回看缓存，comId::{},liveId:{}", liveDat.getComId(), liveId);
        //删除缓存
        clearcache.delLiveCache(liveDat.getComId(), liveId);
        if (StringUtils.isBlank(coverUrl)) {
            // 获取图片地址
            asynchronousGetCoverUrl(fileId);
        }

//        }
    }

    @Override
    public void updateBackByFail(String taskId) {
        LiveBackVideo lbv = this.baseMapper.selectOne(new LambdaUpdateWrapper<LiveBackVideo>()
                .eq(LiveBackVideo::getTaskId, taskId)
                .eq(LiveBackVideo::getDeleteFlg, false));
        if (ObjectUtils.isEmpty(lbv)) {
            log.info("视频task记录不存在，task::{}", taskId);
        } else {
            lbv.setStatus(LiveBackStatusEnums.FAIL.getValue());
            this.baseMapper.updateById(lbv);
        }
    }

    @Override
    public void insertMixRecord(JSONObject result) {
        log.info("========开始插入回调数据:{}", result);
        String streamId = result.getString("stream_id");
        String taskId = result.getString("task_id");
        // 一个taskID只能生成一个回放视频，如果已存在直接return
        List<LiveBackVideo> liveBackVideos = this.baseMapper.selectList(new LambdaUpdateWrapper<LiveBackVideo>()
                .eq(LiveBackVideo::getTaskId, taskId)
                .eq(LiveBackVideo::getDeleteFlg, false));
        if (ObjectUtils.isNotEmpty(liveBackVideos)) {
            return;
        }
        // 如果没有录制任务，不入库
        List<TencentTask> tencentTasks = tencentTaskMapper.findByTaskIds(taskId);
        if (ObjectUtils.isEmpty(tencentTasks)) {
            return;
        }


        //List<TencentTask> tencentTasks = tencentTaskMapper.selectList(new LambdaUpdateWrapper<TencentTask>().eq(TencentTask::getRoomId, streamId));
        //log.info("========查询数据结果:{}", JSONObject.toJSONString(tencentTasks));
        //if (ObjectUtils.isEmpty(tencentTasks)) {
        //    return;
        //}

        TencentTask tencentTask = tencentTasks.get(0);
        Long liveId = tencentTask.getLiveId();
//        String streamParam = result.getString("stream_param");
//        String[] split = streamParam.split("&");
////        String[] split = streamId.split("_");
//        if(split.length<2){
//            return;
//        }
//        String trtcId = "";
//        for (String s : split) {
//            if(s.startsWith("roomid=")){
//                String[] split1 = s.split("=");
//                trtcId= split1[1];
//            }
//        }
//        if(StringUtils.isBlank(trtcId)){
//            return;
//        }
//        String taskId = result.getString("task_id");
        LiveDat liveDat = liveDatMapper.selectOne(new LambdaUpdateWrapper<LiveDat>().eq(LiveDat::getId, liveId));
        if (ObjectUtils.isEmpty(liveDat)) {
            return;
        }
//        Long liveId = liveDat.getId();
        LiveBackVideo lbv = new LiveBackVideo();
        lbv.setId(idService.nextId());
//        lbv.setLiveId(liveId);
        lbv.setLiveId(liveId);
        lbv.setMediaId("mix");
        lbv.setVideoName(liveDat.getName());
        lbv.setLiveShowId(liveDat.getShowId());
        lbv.setComId(liveDat.getComId());
        LiveConfig liveConfig = liveConfigMapper.selectOne(new LambdaUpdateWrapper<LiveConfig>().eq(LiveConfig::getLiveId, liveId));
//        List<TencentTask> tencentTasks = tencentTaskMapper.selectList(new LambdaUpdateWrapper<TencentTask>()
//                .eq(TencentTask::getLiveId, liveId).eq(TencentTask::getTaskId, streamId));
//        if (ObjectUtils.isNotEmpty(tencentTasks)) {
//            TencentTask tencentTask = tencentTasks.get(0);
//            lbv.setLiveKey(tencentTask.getLiveKey());
//        }
        lbv.setLiveKey(tencentTask.getLiveKey());
        lbv.setVideoType(2);
        lbv.setLiveName(liveDat.getName());
        lbv.setTaskId(taskId);
        lbv.setStatus(LiveBackStatusEnums.OK.getValue());
        String fileFormat = result.getString("file_format");
        // 录制文件下载 URL
        String videoUrl = result.getString("video_url");
        if (fileFormat.endsWith("mp4")) {
            lbv.setOssUrl(videoUrl);
        } else {
            lbv.setM3u8Url(videoUrl);
        }
        // 录制任务启动时间，
        Long startTime = result.getLong("start_time")*1000;
        // 录制任务结束时间，
        Long endTime = result.getLong("end_time")*1000;


        String fileId = result.getString("file_id");
        Integer duration = result.getInteger("duration");
        // 录制文件大小，单位字节
        Long fileSize = result.getLong("file_size");

        lbv.setMobileThemeType(liveConfig.getMobileThemeType());
        lbv.setFileId(fileId);
        // 多路同一时录制
        lbv.setTaskType(4);
        lbv.setVideoDuration(duration);
        lbv.setVideoSize(fileSize);
        lbv.setRecordStartTime(new Date(startTime));
        lbv.setRecordEndTime(new Date(endTime));
        asynchronousGetCoverUrl(fileId);
        this.baseMapper.insert(lbv);
        log.info("混流录制回调，task::{}", JSONObject.toJSONString(lbv));
        log.info("删除回看缓存，comId::{},liveId:{}", liveDat.getComId(), liveId);
        //删除缓存
        clearcache.delLiveCache(liveDat.getComId(), liveId);
    }

    public static void main(String[] args) {

        // 获取当前时间的秒级Unix时间戳
        long unixTimestampSeconds = Instant.now().getEpochSecond();

        // 输出秒级Unix时间戳
        System.out.println("Current Unix Timestamp (seconds): " + unixTimestampSeconds);

        //减一个小时
        long unixTimestampSeconds1 = Instant.now().minusSeconds(3600).getEpochSecond();
        System.out.println("Current Unix Timestamp (seconds): " + unixTimestampSeconds1);




    }

    /**
     * 异步获取图片信息
     *
     * @param fileId
     */
    private void asynchronousGetCoverUrl(String fileId) {
        ThreadUtil.execute(new Runnable() {
            @Override
            public void run() {
                // 获取图片地址
                ProcessMediaByProcedureInfo info = ProcessMediaByProcedureInfo.builder()
                        .secretId(secretId)
                        .secretKey(secretKey)
                        .fileId(fileId)
                        .SubAppId(subAppId)
                        .procedureName("fengmian")
                        .build();
                ProcessMediaByProcedure.processMediaByProcedure(info);
            }
        });
    }
}
    