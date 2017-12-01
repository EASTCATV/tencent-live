//package cn.godsdo.controller.Media;
//
//import cn.godsdo.dubbo.mediaLibrary.ComVideoDatService;
//import cn.godsdo.entity.ComVideoDat;
//import cn.godsdo.entity.video.ChunkVideo;
//import cn.godsdo.util.FileInfoUtils;
//import cn.godsdo.util.ShiroUtil;
//import cn.godsdo.util.UploadResult;
//import com.alibaba.fastjson.JSONObject;
//import com.y20y.constant.Constants;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.dubbo.config.annotation.DubboReference;
//import org.apache.shiro.SecurityUtils;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.multipart.MultipartFile;
//
//import javax.servlet.http.HttpServletResponse;
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.ArrayList;
//
///**
// * @Author : yang
// * @Date : 2018/2/13
// * @ApiNote :
// */
//@RestController
//@RequestMapping("/Video")
//@Slf4j
//public class VideoController {
//    @Value("${vod.uploadFolder}")
//    private String uploadFolder;
//    @DubboReference(check = false)
//    private ComVideoDatService comVideoDatService;
//    @DubboReference(check = false)
//    private ClientChunkInfoService chunkService;
//
//
//    @GetMapping("/chunk")
//    public UploadResult checkChunk(ChunkVideo chunk, HttpServletResponse response) {
//
//
//        Long comId = ShiroUtil.getComId();
//        Long accountId = ShiroUtil.getAccountId();
//
//
//
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
//        ComVideoDat clientVideoDat = comVideoDatService.getComVideoByIdentifier(comId, chunk.getIdentifier());
//        if (clientVideoDat != null) {
//            ur.setSkipUpload(true);
//            ur.setLocation(file);
//            response.setStatus(HttpServletResponse.SC_OK);
//            ur.setMessage("完整文件已存在，直接跳过上传，实现秒传");
//            return ur;
//        }
//
//        //如果完整文件不存在，则去数据库判断当前哪些文件块已经上传过了，把结果告诉前端，跳过这些文件块的上传，实现断点续传
//        chunk.setComId(comId);
//        ArrayList<Integer> list = chunkService.checkChunk(chunk);
//        if (list != null && list.size() > 0) {
//            ur.setSkipUpload(false);
//            ur.setUploadedChunks(list);
//            response.setStatus(HttpServletResponse.SC_OK);
//            ur.setMessage("部分文件块已存在，继续上传剩余文件块，实现断点续传");
//            return ur;
//        }
//        return ur;
//    }
//
//
//}
