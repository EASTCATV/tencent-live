package cn.godsdo.dubbo.impl.com;

import cn.godsdo.dto.mediaLibrary.GetAllBackVideoListDto;
import cn.godsdo.dto.video.MediaUploadDTO;
import cn.godsdo.dto.video.UploadResp;
import cn.godsdo.dubbo.com.ComChunkInfoService;
import cn.godsdo.dubbo.com.ComVideoDatService;
import cn.godsdo.dubbo.wx.TencentApiService;
import cn.godsdo.entity.com.ComChunkInfo;
import cn.godsdo.entity.com.ComFileInfo;
import cn.godsdo.entity.com.ComVideoDat;
import cn.godsdo.mapper.com.ComChunkInfoMapper;
import cn.godsdo.mapper.com.ComFileInfoMapper;
import cn.godsdo.mapper.com.ComVideoDatMapper;
import cn.godsdo.util.R;
import cn.godsdo.util.VideoUtil;
import cn.godsdo.vo.GetComVideoListVo;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencentcloudapi.vod.v20180717.models.DescribeTaskDetailResponse;
import com.tencentcloudapi.vod.v20180717.models.ProcessMediaByProcedureResponse;
import com.y20y.base.DescribeTaskDetailRequestinfo;
import com.y20y.base.ProcessMediaByProcedureInfo;
import com.y20y.vod.DescribeTaskDetail;
import com.y20y.vod.ProcessMediaByProcedure;
import jakarta.annotation.Resource;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static cn.godsdo.util.TimeUtil.convertSecondsToHMS;

/**
 * <p>
 * 《客户视频素材管理表》 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2018-04-11
 */
@Slf4j
@DubboService
public class ComVideoDatServiceImpl extends ServiceImpl<ComVideoDatMapper, ComVideoDat> implements ComVideoDatService {

    @DubboReference(check = false)
    ComChunkInfoService comChunkInfoService;

    @DubboReference(check = false)
    private TencentApiService tencentApiService;
    @Resource
    VideoUtil videoutil;
    @Resource
    ComFileInfoMapper comFileInfoMapper;
     @Resource
     ComChunkInfoMapper comChunkInfoMapper;
    @Value("${Tencent.secretId}")
    private String secretId;

    @Value("${Tencent.secretKey}")
    private String secretKey;
    @Value("${Tencent.vod.subAppId}")
    private Long subAppId;
    @Override
    public R getComVideoList(GetAllBackVideoListDto dto, Long comId) {
        // 创建分页对象，指定当前页和每页记录数
        IPage<ComVideoDat> iPage = new Page<>(dto.getPage(), dto.getPageSize());
        IPage<ComVideoDat> vo = this.baseMapper.getComVideoList(iPage, dto, comId);
        List<GetComVideoListVo> list = vo.getRecords().stream().map(e -> {
            GetComVideoListVo getComVideoListVo = new GetComVideoListVo();
            BeanUtils.copyProperties(e, getComVideoListVo);
            Long timeLength = e.getTimeLength();
            if(ObjectUtils.isNotEmpty(timeLength)){
                String time = convertSecondsToHMS(timeLength);
                getComVideoListVo.setTime(time);
            }else {
                getComVideoListVo.setTime("0");
            }
            return getComVideoListVo;
        }).toList();
        return R.ok(list, vo.getTotal());
    }

    @Override
    public R getComVideoListByFitment(GetAllBackVideoListDto dto, Long comId) {
        IPage<ComVideoDat> iPage = new Page<>(dto.getPage(), dto.getPageSize());
        IPage<ComVideoDat> vo = this.baseMapper.getComVideoListByFitment(iPage, dto, comId);
        List<GetComVideoListVo> list = vo.getRecords().stream().map(e -> {
            GetComVideoListVo getComVideoListVo = new GetComVideoListVo();
            BeanUtils.copyProperties(e, getComVideoListVo);
            if(ObjectUtils.isNotEmpty(e.getTimeLength())){
                String time = convertSecondsToHMS(e.getTimeLength());
                getComVideoListVo.setTime(time);
            }else {
                getComVideoListVo.setTime("0");
            }
            return getComVideoListVo;
        }).toList();
        return R.ok(list, vo.getTotal());
    }

    @Override
    public R deleteComVideo(Long id, Long comId, Long accountId) {
        // 根据条件查询视频
        ComVideoDat comVideoDat = this.baseMapper.selectOne(new LambdaUpdateWrapper<ComVideoDat>()
                .eq(ComVideoDat::getComId, comId)
                .eq(ComVideoDat::getId, id)
                .eq(ComVideoDat::getDeleteFlg, false));
        // 如果视频不存在，则返回失败信息
        if (ObjectUtils.isEmpty(comVideoDat)) {
            return R.ok();
        }
        // 将回放视频的删除标记设置为true，并设置更新者的账户ID
        comVideoDat.setDeleteFlg(1);
        comVideoDat.setUpdateBy(accountId);
        // 更新回放视频信息
        this.baseMapper.updateById(comVideoDat);
        String identifier = comVideoDat.getIdentifier();
        // 删除上传记录
        comFileInfoMapper.delete(new LambdaUpdateWrapper<ComFileInfo>()
                .eq(ComFileInfo::getComId, comId)
                .eq(ComFileInfo::getIdentifier, identifier));
        comChunkInfoMapper.delete(new LambdaUpdateWrapper<ComChunkInfo>()
                .eq(ComChunkInfo::getComId, comId)
                .eq(ComChunkInfo::getIdentifier, identifier));
        // 返回操作成功信息
        return R.ok();
    }

    @Override
    public R comVideoDatDeleteBatch(List<Long> ids, Long comId, Long accountId) {

        List<ComVideoDat> list = this.baseMapper.selectBatchIds(ids);

        for (ComVideoDat comVideoDat : list) {
            comChunkInfoService.delChunk(comId, comVideoDat.getIdentifier());
        }

        this.baseMapper.comVideoDatDeleteBatch(ids, comId, accountId);
        return R.ok();
    }

    @Override
    public R updateComVideoDatGroupBatch(List<Long> ids, Long groupId, Long comId, Long accountId) {
        this.baseMapper.updateComVideoDatGroupBatch(ids, groupId, comId, accountId);
        return R.ok();
    }

    @Override
    public ComVideoDat getComVideoByIdentifier(Long comId, String identifier) {
        return this.baseMapper.selectOne(Wrappers.<ComVideoDat>lambdaQuery().eq(ComVideoDat::getComId, comId).eq(ComVideoDat::getIdentifier, identifier).eq(ComVideoDat::getDeleteFlg, false));
    }

    @Override
    public R addComVideoDat(ComVideoDat comVideoDat) {
        this.baseMapper.insert(comVideoDat);
        return R.ok();
    }

    @Override
    public R updateVideoName(Long id, String name, Long comId, Long accountId) {        // 根据条件查询视频
        ComVideoDat comVideoDat = this.baseMapper.selectOne(new LambdaUpdateWrapper<ComVideoDat>()
                .eq(ComVideoDat::getComId, comId)
                .eq(ComVideoDat::getId, id)
                .eq(ComVideoDat::getDeleteFlg, false));
        // 如果视频不存在，则返回失败信息
        if (ObjectUtils.isEmpty(comVideoDat)) {
            return R.failed("修改失败，视频不存在");
        }
        comVideoDat.setVideoName(name);
        comVideoDat.setUpdateBy(accountId);
        this.baseMapper.updateById(comVideoDat);
        return R.ok();
    }
    //
    //@Override
    //public void uploadVidoeToTencent(String parameter) {
    //    log.info("uploadVidoeToTencent 开始=========]");
    //    //CompletableFuture.runAsync(() -> asyncToTencent(parameter));
    //}

    @Override
    public void updateVod(ComVideoDat dat) {
        log.info("updateVod 开始========={}", dat);
        ComVideoDat comVideoDat = this.baseMapper.selectById(dat.getId());
        comVideoDat.setOssResource(dat.getOssResource());
        comVideoDat.setImage(dat.getImage());
        comVideoDat.setStatus("FINISH");
        comVideoDat.setFileId(dat.getFileId());
        comVideoDat.setTimeLength(dat.getTimeLength());
        this.baseMapper.updateById(comVideoDat);
    }

    @Override
    public String transcoding(ComVideoDat comVideoDat) {
        ProcessMediaByProcedureInfo info = ProcessMediaByProcedureInfo.builder()
                .secretId(secretId)
                .secretKey(secretKey)
                .fileId(comVideoDat.getFileId())
                .SubAppId(subAppId)
                .procedureName("2Hls-540")
                .build();
        ProcessMediaByProcedureResponse processMediaByProcedureResponse = ProcessMediaByProcedure.processMediaByProcedure(info);
        if(ObjectUtils.isNotEmpty(processMediaByProcedureResponse)){
            return processMediaByProcedureResponse.getTaskId();
        }
        return null;
    }

//    /**
//     * 上传视频到腾讯云点播
//     *
//     * @param parameter 上传参数
//     */
//    @Synchronized
//    private void asyncToTencent(String parameter) {
//        log.info("********** 上传视频到腾讯云点播 开始 **********{}", parameter);
//        JSONObject jsonObject = JSON.parseObject(parameter);
//        var videoId = jsonObject.get("videoId");
//        var filePath = jsonObject.getString("path");
//        var folder = jsonObject.getString("folder");
//        var fileName = jsonObject.getString("fileName");
//        String coverFilePath = "";
//        Long duration = 0L;
//        System.out.println("通过Javacv的方式获取视频截图");
//        //通过Javacv的方式获取视频截图
//        Map<String, Object> screenshot = videoutil.getScreenshot(filePath, folder, fileName);
//        if (CollectionUtil.isNotEmpty(screenshot)) {
//            coverFilePath = screenshot.get("imgPath").toString();
//            duration = (Long) screenshot.get("time");
//        } else {
//            log.info("通过Javacv的方式获取视频截图失败");
//        }
//        System.out.println("coverFilePath = " + coverFilePath);
////        //异步上传到云点播
//        MediaUploadDTO uploadDTO = new MediaUploadDTO();
//        uploadDTO.setMediaPath(filePath);
//        uploadDTO.setCoverPath(coverFilePath);
//        uploadDTO.setSlices(5);
//        try {
//            UploadResp uploadResp = tencentApiService.upload(uploadDTO); //分片上传加快处理速度
//            ComVideoDat comVideoDat = this.baseMapper.selectById(Long.valueOf(videoId.toString()));
//            comVideoDat.setOssResource(uploadResp.getMediaUrl());
//            comVideoDat.setImage(uploadResp.getCoverUrl());
//            comVideoDat.setStatus("FINISH");
//            comVideoDat.setFileId(uploadResp.getFileId());
//            comVideoDat.setTimeLength(duration);
//            this.baseMapper.updateById(comVideoDat);
//            log.info("异步上传到完成，修改数据库数据");
//            //删除服务器文件
//            log.info("删除文件 new ====={}", folder);
//            FileUtil.del(folder);
//        } catch (Exception e) {
//            log.error("上传视频到腾讯云点播失败", e);
//        }
//    }
}
