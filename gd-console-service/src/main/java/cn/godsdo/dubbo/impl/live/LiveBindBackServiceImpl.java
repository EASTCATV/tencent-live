package cn.godsdo.dubbo.impl.live;

import cn.godsdo.dubbo.live.LiveBindBackService;
import cn.godsdo.entity.live.LiveBindBack;
import cn.godsdo.mapper.live.LiveBindBackMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * <p>
 * 直播回看绑定表 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2024-04-30
 */
@DubboService
public class LiveBindBackServiceImpl extends ServiceImpl<LiveBindBackMapper, LiveBindBack> implements LiveBindBackService {

    @Override
    public Boolean checkBindRoomBack(Long comId, Long videoId) {
        Long count = this.baseMapper.getCountByVideoId(videoId,comId);
        if(count>0){
            return true;
        }
        return false;
    }

    @Override
    public void delete(Long comId, Long videoId) {
        this.baseMapper.delete(Wrappers.<LiveBindBack>update().lambda().eq(LiveBindBack::getVideoId, videoId).eq(LiveBindBack::getComId, comId));
    }
}
