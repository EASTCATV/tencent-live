package cn.godsdo.controller.camp.assistant;

import cn.godsdo.dto.assistan.BindLiveBotDto;
import cn.godsdo.dto.assistan.GetBotListByAssistanDto;
import cn.godsdo.dto.assistan.SendMsgByBotDto;
import cn.godsdo.dubbo.com.ComBotService;
import cn.godsdo.dubbo.live.LiveBotService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * @author W~Y~H
 * @Date : 2018/05/18
 */
@RestController
@RequestMapping("camp/assistanBot")
@CrossOrigin
public class CampAssistanBotController {
    @DubboReference
    ComBotService comBotService;
    @DubboReference
    LiveBotService liveBotService;

    /***
     * 获取机器人列表
     * @param dto
     * @return
     */
    @PostMapping("getBotListByAssistan")
    public R getBotListByAssistan(@RequestBody GetBotListByAssistanDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comBotService.getBotListByAssistan(dto, accountId, comId);


    }

    /**
     * 绑定机器人
     *
     * @return
     */
    @PostMapping("bindLiveBot")
    public R bindRoomBot(@RequestBody BindLiveBotDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveBotService.bindRoomBot(dto, comId, accountId);
    }

    /**
     * 解除绑定机器人
     *
     * @return
     */
    @GetMapping("unBindLiveBot")
    public R unBindLiveBot(@RequestParam("liveId") Long liveId, @RequestParam("botId") Long botId) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveBotService.unBindLiveBot(liveId, botId, comId, accountId);
    }
    /**
     * 清空机器人绑定列表
     *
     * @return
     */
    @GetMapping("clearBindLiveBot")
    public R clearBindLiveBot(@RequestParam("liveId") Long liveId) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveBotService.clearBindLiveBot(liveId,  comId, accountId);
    }
    /**
     * 获取已绑定的机器人列表
     *
     * @return
     */
    @GetMapping("getBindList")
    public R getBindList(@RequestParam("liveId") Long liveId) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveBotService.getBindList(liveId, comId, accountId);
    }

    /**
     * 机器人发送消息（单个）
     *
     * @return
     */
    @PostMapping("/aloneSend")
    public R aloneSend(@RequestBody SendMsgByBotDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comBotService.aloneSend(dto, accountId, comId);
    }

    /**
     * 机器人发送消息（多个）
     *
     * @return
     */
    @PostMapping("/listSend")
    public R listSend(@RequestBody SendMsgByBotDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comBotService.listSend(dto, accountId, comId);
    }

    /**
     * 获取倒计时
     *
     * @return
     */
    @GetMapping("/getListSendCountdown")
    public R getListSendCountdown(@RequestParam("liveId") Long liveId) {
        return comBotService.getListSendCountdown(liveId);
    }
}
