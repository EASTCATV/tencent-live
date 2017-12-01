package cn.godsdo.controller.live;


import cn.godsdo.dto.live.UpdateLiveShareDto;
import cn.godsdo.dubbo.LiveJobService;
import cn.godsdo.dubbo.live.LiveShareService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import org.springframework.stereotype.Controller;

/**
 * <p>
 * 直播间分享配置表 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2024-07-21
 */
@RestController
@CrossOrigin
@RequestMapping("/liveShare")
public class LiveShareController {
    @DubboReference
    LiveShareService liveShareService;
    /**
     * 获取微信分享设置
     *
     * @return
     */
    @GetMapping("/getWxShareInfo")
    public R getWxShareInfo(@RequestParam("liveId") Long liveId) {
        Long comId = ShiroUtil.getComId();
        return liveShareService.getWxShareInfo(comId, liveId);
    }

    /**
     * 修改微信分享设置
     * @param dto
     * @return
     */
    @PostMapping("/updateWxShareInfo")
    public R updateWxShareInfo(@RequestBody UpdateLiveShareDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveShareService.updateWxShareInfo(comId, accountId,dto);
    }
    /**
     * 获取渠道分享设置
     *
     * @return
     */
    @GetMapping("/getChannelShareInfo")
    public R getChannelShareInfo(@RequestParam("liveId") Long liveId) {
        Long comId = ShiroUtil.getComId();
        return liveShareService.getChannelShareInfo(comId, liveId);
    }

    /**
     * 修改渠道分享设置
     * @param dto
     * @return
     */
    @PostMapping("/updateChannelShareInfo")
    public R updateChannelShareInfo(@RequestBody UpdateLiveShareDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveShareService.updateChannelShareInfo(comId, accountId,dto);
    }
}
