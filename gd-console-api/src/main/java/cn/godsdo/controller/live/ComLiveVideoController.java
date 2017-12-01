package cn.godsdo.controller.live;


import cn.godsdo.dto.live.AddLiveVideoDto;
import cn.godsdo.dubbo.live.ComLiveVideoService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 用户直播间视频配置表 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2018-04-12
 */
@RestController
@CrossOrigin
@RequestMapping("/comLiveVideo")
public class ComLiveVideoController {

    @DubboReference
    ComLiveVideoService comLiveVideoService;

    /**
     * 新增视频列表
     * @return
     */
    @PostMapping("/addLiveVideo")
    public R addLiveVideo(@RequestBody AddLiveVideoDto addLiveVideoDto) {
        Long comId = ShiroUtil.getComId();
        Long  accountId= ShiroUtil.getAccountId();
        return comLiveVideoService.addLiveVideo(comId,accountId,addLiveVideoDto);
    }
    /**
     * 获取直播间选中视频列表
     *
     * @return
     */
    @GetMapping("/getCheckVideoList")
    public R getCheckVideoList(@RequestParam("liveId") Long liveId) {
        Long comId = ShiroUtil.getComId();
        return comLiveVideoService.getCheckVideoList(comId,liveId);
    }

    /**
     * 删除直播间选中视频
     *
     * @return
     */
    @GetMapping("/delRoomVideoByFitment")
    public R delRoomVideoByFitment(@RequestParam("liveId") Long liveId) {
        Long comId = ShiroUtil.getComId();
        return comLiveVideoService.delRoomVideoByFitment(comId,liveId);
    }
}
