package cn.godsdo.controller.live;


import cn.godsdo.dto.live.UpdateLiveTagsDto;
import cn.godsdo.dubbo.live.LiveTagService;
import cn.godsdo.entity.live.LiveTag;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import cn.godsdo.util.tencent.CosHelperUtil;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <p>
 * 房间自定义菜单配置表 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2018/10/11
 */
@RestController
@CrossOrigin
@RequestMapping("/liveTag")
public class LiveTagController {

    @DubboReference
    LiveTagService liveTagService;
    @Resource
    @Qualifier("CosBean")
    private CosHelperUtil cosHelperUtil;

    /**
     * 获取直播间装修功能
     *
     * @param liveId
     * @return
     */
    @GetMapping("/getRoomTags")
    public R getRoomConfigTags(@RequestParam("liveId") Long liveId) {
        List<LiveTag> list = liveTagService.getLiveTags(liveId);
        return R.ok(list);
    }

    @PostMapping("/updateRoomTags")
    public R updateRoomTags(@RequestBody UpdateLiveTagsDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveTagService.UpdateLiveTags(dto, comId, accountId);
    }


    /**
     * 上传图片
     *
     * @return
     */
    @PostMapping("/upload")
    public R upload(@RequestParam("file") MultipartFile file) {
        Long comId = ShiroUtil.getComId();
        try {
            byte[] bytes = file.getBytes();
            String[] split = file.getContentType().split("/");
            String suffix = "png";
            if (split.length > 1) {
                suffix = split[1];
            }
            String image = cosHelperUtil.uploadFile(comId, bytes, "baseSetting/", suffix);
            return R.ok(image);
        } catch (Exception exception) {
            return R.failed("上传失败");
        }

    }


}
