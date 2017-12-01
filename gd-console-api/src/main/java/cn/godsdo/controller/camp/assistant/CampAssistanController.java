package cn.godsdo.controller.camp.assistant;

import cn.godsdo.dto.assistan.BanIpDto;
import cn.godsdo.dto.assistan.GetActionUsersDto;
import cn.godsdo.dto.assistan.SendNoticeDto;
import cn.godsdo.dubbo.channel.ComChannelDatService;
import cn.godsdo.dubbo.com.AccountDatService;
import cn.godsdo.dubbo.live.LiveDatService;
import cn.godsdo.dubbo.live.LiveMsgService;
import cn.godsdo.dubbo.live.LiveWatchChatService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * 获取
 *
 * @author W~Y~H
 * @Date : 2018/05/18
 */
@RestController
@RequestMapping("camp/assistan")
@CrossOrigin
public class CampAssistanController {

    @DubboReference
    ComChannelDatService comChannelDatService;
    @DubboReference
    LiveWatchChatService liveWatchChatService;
    @DubboReference
    LiveDatService liveDatService;
    @DubboReference
    AccountDatService accountDatService;

    @DubboReference
    LiveMsgService liveMsgService;

    /**
     * 获取直播间绑定的所有渠道
     */
    @GetMapping("getAllChannel")
    public R getAllChannel(@RequestParam("liveId") Long liveId) {
        Long comId = ShiroUtil.getComId();
        return comChannelDatService.getAllChannelByLiveId(liveId, comId);
    }

    /**
     * 获取免审词
     */
    @GetMapping("getWatchChat")
    public R getWatchChat(@RequestParam("liveId") Long liveId) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveWatchChatService.getLiveChatInfo(liveId, comId, accountId);
    }

    /**
     * 修改免审词开关
     */
    @GetMapping("updateWatchChatFree")
    public R updateWatchChatFree(@RequestParam("free") Boolean free, @RequestParam("liveId") Long liveId, @RequestParam("imGroupId") String imGroupId) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveWatchChatService.updateWatchChatFree(free, imGroupId, liveId, comId, accountId);
    }

    /**
     * 获取直播间信息
     */
    @GetMapping("getRoomDat")
    public R getRoomDatByAssistan(@RequestParam("liveId") Long liveId) {
        Long userId = ShiroUtil.getAccountId();
        Long comId = ShiroUtil.getComId();
        return liveDatService.getRoomDatByAssistan(userId, comId, liveId);
    }

    //    sendNotice
    @PostMapping("sendNotice")
    public R sendNotice(@RequestBody SendNoticeDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveDatService.sendNotice(comId, accountId, dto);
    }

    /**
     * 封禁ip
     */
    @PostMapping("/banIp")
    public R banIp(@RequestBody BanIpDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return accountDatService.banIp(comId, accountId, dto);
    }
    /**
     * 封禁ip列表
     */
    @GetMapping("/banIpList")
    public R banIpList(@RequestParam("liveId")Long liveId) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return accountDatService.banIpList(comId, accountId, liveId);
    }

    /**
     * 获取在线离线用户列表
     *
     */
    @PostMapping("/getActionUsers")
    public R getActionUsers(@RequestBody GetActionUsersDto dto) {
        Long comId = ShiroUtil.getComId();
        dto.setComId(comId);
        return liveMsgService.getOfflineUserList(dto);
    }
}
