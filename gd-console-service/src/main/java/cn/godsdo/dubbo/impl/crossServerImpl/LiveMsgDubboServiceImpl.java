package cn.godsdo.dubbo.impl.crossServerImpl;

import cn.godsdo.dto.msg.LiveMessage;
import cn.godsdo.dubbo.crossService.LiveMsgDubboService;
import cn.godsdo.dubbo.live.LiveMsgService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

/**
 * @Author: CR7
 * @Date: 2019/5/5 0:36
 * @Description:
 */
@DubboService
public class LiveMsgDubboServiceImpl implements LiveMsgDubboService {

    @DubboReference(check = false)
    LiveMsgService liveMsgService;

    @Override
    public List<LiveMessage> getHistoryMessage(Long liveId) {
        return liveMsgService.getHistoryMessage(liveId);
    }
}
