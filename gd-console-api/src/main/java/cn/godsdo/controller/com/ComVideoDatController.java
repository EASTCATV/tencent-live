package cn.godsdo.controller.com;


import cn.godsdo.dto.com.GetComVideoShareUrlDto;
import cn.godsdo.dto.mediaLibrary.GetAllBackVideoListDto;
import cn.godsdo.dto.mediaLibrary.UpdateBackVideoDatGroupDto;
import cn.godsdo.dubbo.LiveJobService;
import cn.godsdo.dubbo.LiveStatisticsDatService;
import cn.godsdo.dubbo.LiveXxlJobService;
import cn.godsdo.dubbo.com.ComChunkInfoService;
import cn.godsdo.dubbo.com.ComFileInfoService;
import cn.godsdo.dubbo.com.ComVideoDatService;
import cn.godsdo.dubbo.live.ComLiveVideoService;
import cn.godsdo.dubbo.live.LiveBindBackService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import com.y20y.interfaces.IdService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 《客户视频素材管理表》 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2018-04-11
 */
@Slf4j
@RestController
@RequestMapping("/comVideoDat")
@CrossOrigin
public class ComVideoDatController {

    @DubboReference
    ComVideoDatService comVideoDatService;
    @DubboReference
    ComLiveVideoService comLiveVideoService;
    @DubboReference
    IdService idService;

    @Value("${uploadFolder}")
    private String uploadFolder;


    @DubboReference(check = false)
    ComChunkInfoService comChunkInfoService;

    @DubboReference
    ComFileInfoService comFileInfoService;
    @DubboReference
    LiveBindBackService LiveBindBackService;

    @DubboReference(retries = 0,timeout = 15000)
    LiveJobService liveJobService;
    @DubboReference(retries = 0,timeout = 15000)
    LiveStatisticsDatService liveStatisticsService;

    @DubboReference(check = false)
    private LiveXxlJobService liveXxlJobService;

    /**
     * 获取媒体库视频列表(素材中心)
     *
     * @return
     */
    @PostMapping("/getComVideoList")
    public R getComVideoList(@RequestBody GetAllBackVideoListDto dto) {
        Long comId = ShiroUtil.getComId();
        return comVideoDatService.getComVideoList(dto, comId);
    }

    /**
     * 删除媒体库视频(素材中心)
     * 视频删除时
     *
     * @param id 视频id
     * @return
     */
    @GetMapping("/deleteComVideo")
    public R deleteComVideo(@RequestParam("id") Long id) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        Boolean b = LiveBindBackService.checkBindRoomBack(comId, id);
        if (b) {
            return R.failed("该视频已被直播间引用，不可删除");
        }
        LiveBindBackService.delete(comId, id);
        // 删除视频与直播间的绑定
        comLiveVideoService.deleteRoomVideo(id, comId);
        // TODo 删除云端视频
//        aliyunDelete(accountId, ossHelper, id);
        return comVideoDatService.deleteComVideo(id, comId, accountId);
    }


    /**
     * 获取媒体库视频列表(直播间装修)
     *
     * @return
     */
    @PostMapping("/getComVideoListByFitment")
    public R getComVideoListByFitment(@RequestBody GetAllBackVideoListDto dto) {
        Long comId = ShiroUtil.getComId();
        return comVideoDatService.getComVideoListByFitment(dto, comId);
    }





    /**
     * 批量删除视频(素材中心)
     *
     * @return
     */
    @PostMapping("/comVideoDatDeleteBatch")
    public R comVideoDatDeleteBatch(@RequestBody UpdateBackVideoDatGroupDto dto) {
        List<Long> ids = dto.getIds();
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        for (Long id : ids) {
            // 删除视频与直播间的绑定
            comLiveVideoService.deleteRoomVideo(id, comId);
        }
//        OSSHelper ossHelper = new OSSHelper(endpoint, accessKeyId, accessKeySecret);
//        for (Long id : ids) {
//            aliyunDelete(accountId, ossHelper, id);
//        }
        return comVideoDatService.comVideoDatDeleteBatch(ids, comId, accountId);
    }

//    /**
//     * 上传文件块
//     *
//     * @param chunk
//     * @return
//     */
//    @PostMapping("/chunk")
//    public String uploadChunk( ChunkInfoDto chunk) {
//        Long comId = ShiroUtil.getComId();
//        String apiRlt = "200";
//
//        MultipartFile file = chunk.getUpfile();
//        log.info("file originName: {}, chunkNumber: {}", file.getOriginalFilename(), chunk.getChunkNumber());
//
//        try {
//            byte[] bytes = file.getBytes();
//            Path path = Paths.get(FileInfoUtils.generatePath(uploadFolder, chunk, comId));
//            //文件写入指定路径
//            Files.write(path, bytes);
//            chunk.setComId(comId);
//            chunk.setId(idService.nextId() + "");
//            //chunk.setChunkNumber(chunk.getChunkNumber());
//            if (comChunkInfoService.saveChunk(chunk) < 0) {
//                apiRlt = "415";
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//            apiRlt = "415";
//        }
//        return apiRlt;
//    }
//
//    @GetMapping("/chunk")
//    public UploadResult checkChunk( ChunkInfoDto chunk, HttpServletResponse response) {
//        Long comId = ShiroUtil.getComId();
//        UploadResult ur = new UploadResult();
//
//        //默认返回其他状态码，前端不进去checkChunkUploadedByResponse函数，正常走标准上传
//        response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
//
//        String file = uploadFolder + File.separator + comId + File.separator + chunk.getIdentifier() + File.separator + chunk.getFilename();
//
//        //先判断整个文件是否已经上传过了，如果是，则告诉前端跳过上传，实现秒传
//        if (FileInfoUtils.fileExists(file)) {
//            ur.setSkipUpload(true);
//            ur.setLocation(file);
//            response.setStatus(HttpServletResponse.SC_OK);
//            ur.setMessage("完整文件已存在，直接跳过上传，实现秒传" + uploadFolder);
//            return ur;
//        }
//
//        ComVideoDat comVideoDat = comVideoDatService.getComVideoByIdentifier(comId, chunk.getIdentifier());
//        if (comVideoDat != null) {
//            ur.setSkipUpload(true);
//            ur.setLocation(file);
//            response.setStatus(HttpServletResponse.SC_OK);
//            ur.setMessage("完整文件已存在，直接跳过上传，实现秒传");
//            return ur;
//        }
//
//        //如果完整文件不存在，则去数据库判断当前哪些文件块已经上传过了，把结果告诉前端，跳过这些文件块的上传，实现断点续传
//        chunk.setComId(comId);
//        List<Integer> list = comChunkInfoService.checkChunk(comId, chunk.getIdentifier());
//        if (list != null && list.size() > 0) {
//            ur.setSkipUpload(false);
//            ur.setUploadedChunks(list);
//            response.setStatus(HttpServletResponse.SC_OK);
//            ur.setMessage("部分文件块已存在，继续上传剩余文件块，实现断点续传");
//            return ur;
//        }
//        return ur;
//    }

    /**
     * 多选移动媒体库视频分组
     *
     * @return
     */
    @PostMapping("/updateComVideoDatGroupBatch")
    public R updateComVideoDatGroupBatch(@RequestBody UpdateBackVideoDatGroupDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comVideoDatService.updateComVideoDatGroupBatch(dto.getIds(), dto.getGroupId(), comId, accountId);
    }

//    @PostMapping("/mergeFile")
//    public Object mergeFile(@RequestBody TFileInfoVO fileInfoVO, HttpServletRequest request) {
//        // 获取公司ID
//        Long comId = ShiroUtil.getComId();
//        // 获取账户ID
//        Long accountId = ShiroUtil.getAccountId();
//        // 创建JSON对象
//        JSONObject jsonObject = new JSONObject();
//        // 设置code为200
//        jsonObject.put("code", 200);
//        // 设置message为"上传成功"
//        jsonObject.put("message", "上传成功");
//
//        // 根据公司ID和唯一标识获取视频数据
//        ComVideoDat tComVideoDat = comVideoDatService.getComVideoByIdentifier(comId, fileInfoVO.getUniqueIdentifier());
//
//        // 如果视频数据不为空
//        if (tComVideoDat != null) {
//            // 设置code为200
//            jsonObject.put("code", 200);
//            // 设置message为"视频已上传，无需重复上传!"
//            jsonObject.put("message", "视频已上传，无需重复上传!");
//            // 返回JSON对象
//            return jsonObject;
//        }
//        // 前端组件参数转换为model对象
//        // 创建ComFileInfo对象
//        ComFileInfo fileInfo = new ComFileInfo();
//        fileInfo.setId(idService.nextId());
//        // 设置公司ID
//        fileInfo.setComId(comId);
//        // 设置文件名
//        fileInfo.setFilename(fileInfoVO.getName());
//        // 设置唯一标识
//        fileInfo.setIdentifier(fileInfoVO.getUniqueIdentifier());
//        // 设置总大小
//        fileInfo.setTotalSize(fileInfoVO.getSize());
//
//        // 进行文件的合并操作
//        // 获取文件名
//        String filename = fileInfo.getFilename();
//        // 设置文件路径
//        String file = uploadFolder + File.separator + comId + File.separator + fileInfo.getIdentifier() + File.separator + filename;
//        log.info( "=======file:{}", file);
//        // 设置文件夹路径
//        String folder = uploadFolder + File.separator + comId + File.separator + fileInfo.getIdentifier();
//        log.info( "=====设置文件夹路径:{}", folder);
//        // 调用文件合并方法
//        String fileSuccess = FileInfoUtils.merge(file, folder, filename);
//// 设置文件位置
//        fileInfo.setLocation(file);
//        log.info( "=====fileInfo:{}", fileInfo);
//        // 如果文件合并失败
//        if ("500".equals(fileSuccess)) {
//            log.info( "==上传失败===file:{}", file);
//            // 设置code为500
//            jsonObject.put("code", 500);
//            // 设置message为"上传失败"
//            jsonObject.put("message", "上传失败");
//            // 返回JSON对象
//            return jsonObject;
//        }
//
//        // 初始化标志为false
//        boolean flag = false;
//        log.info( "=====fileSuccess:{}", fileSuccess);
//        // 如果文件合并成功
//        if ("200".equals(fileSuccess)) {
//            // 设置标志为true
//            flag = true;
//        }
//        // 如果文件部分合并成功
//        if ("300".equals(fileSuccess)) {
//
//            // 根据参数查询文件信息
//            List<ComFileInfo> tfList = comFileInfoService.selectFileByParams(fileInfo);
//            // 如果文件信息为空
//            if (tfList != null && tfList.size() == 0) {
//                // 设置标志为true
//                flag = true;
//            }
//        }
//
//        // 如果标志为true
//        if (flag) {
//            CompletableFuture.runAsync(() -> asyncMergeFile( fileInfo,  filename,  folder,  comId,  accountId,  fileInfoVO));
//        }
//
//        return jsonObject;
//    }
//
//    private void asyncMergeFile(ComFileInfo fileInfo, String filename, String folder, Long comId, Long accountId, TFileInfoVO fileInfoVO) {
////        fileInfo
//        log.info( "===添加文件成功==fileInfo:{}", fileInfo);
//        // 如果添加文件信息成功
//        if (comFileInfoService.addFileInfo(fileInfo) > 0) {
//
//            // 文件合并成功后，将数据添加到com_video_dat表中
//            // 创建ComVideoDat对象
//            ComVideoDat comVideoDat = new ComVideoDat();
//            comVideoDat.setId(idService.nextId());
//            // 设置公司ID
//            comVideoDat.setComId(comId);
//            // 设置标题
//            comVideoDat.setVideoName(fileInfoVO.getName());
//            // 设置大小
//            comVideoDat.setVolume(fileInfoVO.getSize());
//            // 设置原始文件名
//            comVideoDat.setOriginalFile(fileInfoVO.getName());
//            // 设置状态为"UPLOAD"
//            comVideoDat.setStatus("UPLOAD");
//            // 设置创建时间为当前时间
//            comVideoDat.setCreateAt(new Date());
//            // 设置创建人为当前账户ID
//            comVideoDat.setCreateBy(accountId);
//            // 设置删除标志为false
//            comVideoDat.setDeleteFlg(0);
//            // 设置唯一标识
//            comVideoDat.setIdentifier(fileInfo.getIdentifier());
//            // 设置组ID
//            comVideoDat.setGroupId(Long.parseLong(fileInfoVO.getRefProjectId()));
//            // 添加视频数据
//            comVideoDatService.addComVideoDat(comVideoDat);
//
//            // 创建JSON对象param
//            JSONObject param = new JSONObject();
//            // 设置视频ID
//            param.put("videoId", comVideoDat.getId());
//            param.put("fileName", filename);
//            param.put("folder", folder);
//            // 获取文件扩展名
////            String ext = FilenameUtils.getExtension(filename);
//            // 设置路径，对路径进行编码
//            param.put("path", fileInfo.getLocation());
////                param.put("path", URLEncoder.createDefault().encode(fileInfo.getLocation(), Charset.forName("utf-8")));
//            log.info( "===执行job==fileInfo:{}", param.toString());
//            liveJobService.addJob(param.toString(), "VideoJob");
//        }
//    }

    /**
     * 媒体库编辑视频
     *
     * @param id 视频id
     * @return
     */
    @GetMapping("/updateVideoName")
    public R updateVideoName(@RequestParam("id") Long id, @RequestParam("videoName") String videoName) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        // 修改视频绑定名称
        comLiveVideoService.updateVideoName(accountId, comId, id, videoName);
        return comVideoDatService.updateVideoName(id, videoName, comId, accountId);
    }


}
