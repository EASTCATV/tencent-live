package cn.godsdo.dubbo.impl.live;

import cn.godsdo.dubbo.live.LiveRoomCdnService;
import cn.godsdo.entity.live.LiveRoomCdn;
import cn.godsdo.mapper.live.LiveRoomCdnMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * <p>
 * 直播间cdn配置 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2024-05-01
 */
@DubboService
public class LiveRoomCdnServiceImpl extends ServiceImpl<LiveRoomCdnMapper, LiveRoomCdn> implements LiveRoomCdnService {

    @Override
    public LiveRoomCdn getByCdnId(String cdnId) {
        LiveRoomCdn liveRoomCdn = this.baseMapper.selectOne(new LambdaQueryWrapper<LiveRoomCdn>().eq(LiveRoomCdn::getCdnId, cdnId));
        return liveRoomCdn;
    }
}
