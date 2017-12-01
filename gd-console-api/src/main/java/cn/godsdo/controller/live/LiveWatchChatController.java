package cn.godsdo.controller.live;


import cn.godsdo.dto.live.LiveWatchChatDto;
import cn.godsdo.dubbo.live.LiveWatchChatService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 房间聊天设置 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2018/10/18
 */
@RestController
@CrossOrigin
@RequestMapping("/liveWatchChat")
public class LiveWatchChatController {
    @DubboReference
    LiveWatchChatService liveWatchChatService;

    @PostMapping("/save")
    public R save(@RequestBody LiveWatchChatDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveWatchChatService.updateLiveChat(dto, comId, accountId);
    }

    /**
     * 获取聊天设置
     *
     * @param liveId
     * @return
     */
    @GetMapping("/getLiveChatInfo")
    public R getLiveChatInfo(@RequestParam("liveId") Long liveId) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveWatchChatService.getLiveChatInfo(liveId, comId, accountId);
    }
}
