package cn.godsdo.controller.com;


import cn.godsdo.dto.com.UpdateWatchChatDto;
import cn.godsdo.dubbo.live.ComWatchChatService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2018-04-10
 */
@RestController
@RequestMapping("/comWatchChat")
@CrossOrigin
public class ComWatchChatController {

    @DubboReference
    private ComWatchChatService comWatchChatService;

    /**
     * 获取全局聊天设置
     *
     * @return
     */
    @GetMapping("/getComWatchChat")
    public R getComWatchChat() {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comWatchChatService.getComWatchChat(comId, accountId);
    }

    /**
     * 获取全局聊天设置
     *
     * @return
     */
    @GetMapping("/getComFree")
    public R getComFree() {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comWatchChatService.getComFree(comId, accountId);
    }


    /**
     * 获取全局聊天设置用于房间导入敏感词
     *
     * @return
     */
    @GetMapping("/getComSensitiveWord")
    public R getComSensitiveWord() {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comWatchChatService.getComSensitiveWord(comId, accountId);
    }
    /**
     * xxx更新全局聊天设置
     *
     * @return
     */
    @PostMapping("/updateComWatchChat")
    public R updateComWatchChat(@RequestBody UpdateWatchChatDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comWatchChatService.updateComWatchChat(comId, accountId, dto);
    }
}
