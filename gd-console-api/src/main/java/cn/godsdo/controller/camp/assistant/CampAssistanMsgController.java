package cn.godsdo.controller.camp.assistant;

import cn.godsdo.dto.msg.AuditMessageQuery;
import cn.godsdo.dto.msg.AuditMsgDTO;
import cn.godsdo.dubbo.live.LiveMsgService;
import cn.godsdo.dubbo.live.LiveWatchChatService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * @Author: CR7
 * @Date: 2019/5/5 15:17
 * @Description: 助理端消息接口
 */
@Slf4j
@RestController
@RequestMapping("camp/assistanMsg")
@CrossOrigin
public class CampAssistanMsgController {

    @DubboReference
    private LiveMsgService liveMsgService;

    @DubboReference
    private LiveWatchChatService liveWatchChatService;

    /**
     * 助理 审核消息
     */
    @PostMapping("/audit")
    public R audit(@Valid @RequestBody AuditMsgDTO messageDTO) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        messageDTO.setComId(comId);
        messageDTO.setUserId(accountId);
        return liveMsgService.audit(messageDTO);
    }

    /**
     * 助理端 待审核消息列表
     */
    @PostMapping("/auditList")
    public R auditList(@Valid @RequestBody AuditMessageQuery messageQuery) {
        Long comId = ShiroUtil.getComId();
        messageQuery.setComId(comId);
        return liveMsgService.auditList(messageQuery);
    }

}
