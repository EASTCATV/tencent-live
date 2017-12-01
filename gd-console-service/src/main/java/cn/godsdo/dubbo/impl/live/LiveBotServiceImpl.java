package cn.godsdo.dubbo.impl.live;

import cn.godsdo.dto.assistan.BindLiveBotDto;
import cn.godsdo.dubbo.live.LiveBotService;
import cn.godsdo.entity.live.LiveBot;
import cn.godsdo.mapper.live.LiveBotMapper;
import cn.godsdo.util.R;
import cn.godsdo.vo.assistant.GetBindListVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.interfaces.IdService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 房间机器人绑定列表 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2024-05-29
 */
@DubboService
public class LiveBotServiceImpl extends ServiceImpl<LiveBotMapper, LiveBot> implements LiveBotService {

    @DubboReference
    IdService idService;

    @Override
    public R bindRoomBot(BindLiveBotDto dto, Long comId, Long accountId) {
        List<Long> bots = dto.getBots();
        Long liveId = dto.getLiveId();
        // 创建直播机器人列表
        List<LiveBot> liveBots = new ArrayList<>();
        // 遍历传入的机器人列表
        for (Long botId : bots) {
            // 创建直播机器人对象
            LiveBot liveBot = new LiveBot(idService.nextId(), comId, liveId, botId, accountId);
            // 将直播机器人添加到列表中
            liveBots.add(liveBot);
        }
        // 批量插入直播机器人信息至数据库
        this.baseMapper.insertBatch(liveBots);
        return R.ok();
    }

    @Override
    public R unBindLiveBot(Long liveId, Long botId, Long comId, Long accountId) {
        LiveBot liveBot = this.baseMapper.selectOne(new LambdaQueryWrapper<LiveBot>().eq(LiveBot::getComId, comId)
                .eq(LiveBot::getLiveId, liveId).eq(LiveBot::getBotId, botId).eq(LiveBot::getDeleteFlag, false));
        if (ObjectUtils.isEmpty(liveBot)) {
            return R.ok();
        }
        liveBot.setDeleteFlag(true);
        liveBot.setUpdateBy(accountId);
        this.baseMapper.updateById(liveBot);
        return R.ok();
    }

    @Override
    public R getBindList(Long liveId, Long comId, Long accountId) {
        List<GetBindListVo> list = this.baseMapper.getBindList(comId, liveId, accountId);
        return R.ok(list);
    }

    @Override
    public R clearBindLiveBot(Long liveId, Long comId, Long accountId) {
        this.baseMapper.update(null, new LambdaUpdateWrapper<LiveBot>().set(LiveBot::getDeleteFlag, true)
                .eq(LiveBot::getComId, comId)
                .eq(LiveBot::getLiveId, liveId).eq(LiveBot::getDeleteFlag, false));
        return null;
    }
}
