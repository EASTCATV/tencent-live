package cn.godsdo.controller.com;


import cn.godsdo.dto.mediaLibrary.GetAllBackVideoListDto;
import cn.godsdo.dto.mediaLibrary.UpdateBackVideoDatGroupDto;
import cn.godsdo.dubbo.com.LiveBackVideoService;
import cn.godsdo.dubbo.live.ComLiveVideoService;
import cn.godsdo.dubbo.live.LiveBindBackService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 直播间回看视频表 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2018-04-11
 */
@RestController
@RequestMapping("/liveBackVideo")
@CrossOrigin
public class LiveBackVideoController {


    @DubboReference
    LiveBackVideoService liveBackVideoService;


    @DubboReference
    ComLiveVideoService comLiveVideoService;
    @DubboReference
    LiveBindBackService LiveBindBackService;

    /**
     * 获取回看视频列表(素材中心)
     *
     * @return
     */
    @PostMapping("/getBackVideoList")
    public R getBackVideoList(@RequestBody GetAllBackVideoListDto dto) {
        Long comId = ShiroUtil.getComId();
        return liveBackVideoService.getBackVideoList(dto, comId);
    }

    /**
     * 获取回看视频列表(直播间装修)
     *
     * @return
     */
    @PostMapping("/getBackVideoListByFitment")
    public R getBackVideoListByFitment(@RequestBody GetAllBackVideoListDto dto) {
        Long comId = ShiroUtil.getComId();
        return liveBackVideoService.getBackVideoListByFitment(dto, comId);
    }

    /**
     * 删除回看视频(素材中心)
     *
     * @param id 视频id
     * @return
     */
    @GetMapping("/deleteBackVideo")
    public R deleteBackVideo(@RequestParam("id") Long id) {
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
        return liveBackVideoService.deleteBackVideo(id, comId, accountId);
    }

    /**
     * 编辑视频
     *
     * @param id 视频id
     * @return
     */
    @GetMapping("/updateBackVideo")
    public R updateBackVideo(@RequestParam("id") Long id, @RequestParam("videoName") String videoName) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        // 修改视频绑定名称
        comLiveVideoService.updateVideoName(accountId, comId, id, videoName);
        return liveBackVideoService.updateBackVideo(id, videoName, comId, accountId);
    }

    /**
     * 多选删除回看视频
     *
     * @return
     */
    @PostMapping("/backVideoDatDeleteBatch")
    public R backVideoDatDeleteBatch(@RequestBody UpdateBackVideoDatGroupDto dto) {
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
        return liveBackVideoService.backVideoDatDeleteBatch(ids, comId, accountId);
    }

    /**
     * 多选移动回看视频分组
     *
     * @return
     */
    @PostMapping("/updateBackVideoDatGroupBatch")
    public R updateBackVideoDatGroupBatch(@RequestBody UpdateBackVideoDatGroupDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveBackVideoService.updateBackVideoDatGroupBatch(dto.getIds(), dto.getGroupId(), comId, accountId);
    }

}
