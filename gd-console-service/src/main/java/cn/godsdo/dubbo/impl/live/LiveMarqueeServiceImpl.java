package cn.godsdo.dubbo.impl.live;

import cn.godsdo.constant.CacheConstants;
import cn.godsdo.dto.live.UpdateRoomMarqueeDto;
import cn.godsdo.dubbo.cache.ClearCache;
import cn.godsdo.dubbo.live.LiveMarqueeService;
import cn.godsdo.entity.live.LiveDat;
import cn.godsdo.entity.live.LiveMarquee;
import cn.godsdo.mapper.live.LiveDatMapper;
import cn.godsdo.mapper.live.LiveMarqueeMapper;
import cn.godsdo.util.R;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.interfaces.IdService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

/**
 * <p>
 * 房间跑马灯表 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2018/10/12
 */
@DubboService
public class LiveMarqueeServiceImpl extends ServiceImpl<LiveMarqueeMapper, LiveMarquee> implements LiveMarqueeService {
    @Resource
    LiveDatMapper liveDatMapper;
    @DubboReference
    IdService idService;
    @DubboReference(check = false)
    ClearCache clearcache;


    @Override
    public R getInfo(Long comId, Long accountId, Long liveId) {
        LiveMarquee liveMarquee = this.baseMapper.selectOne(new LambdaQueryWrapper<LiveMarquee>()
                .eq(LiveMarquee::getComId, comId).eq(LiveMarquee::getLiveId, liveId));
        // 不存在，则新建
        if (ObjectUtils.isEmpty(liveMarquee)) {
            liveMarquee = new LiveMarquee();
            liveMarquee.setId(idService.nextId());
            liveMarquee.setComId(comId);
            liveMarquee.setContentType(1);
            liveMarquee.setMode(1);
            liveMarquee.setMarqueeEnable(false);
            liveMarquee.setLiveId(liveId);
            liveMarquee.setCreateBy(accountId);
            this.baseMapper.insert(liveMarquee);
        }
        return R.ok(liveMarquee);
    }

    @Override
    //@Caching(evict = {@CacheEvict(key = "#dto.getLiveId()", value = CacheConstants.ROOM_MARQUEE_VALUE)})
    public R updateMarquee(Long comId, Long accountId, UpdateRoomMarqueeDto dto) {
        // 根据直播间ID判断直播间是否存在
        Long liveId = dto.getLiveId();
        //删除缓存
        clearcache.delLiveCache(comId, liveId);
        LiveDat liveDat = liveDatMapper.selectOne(new LambdaQueryWrapper<LiveDat>()
                .eq(LiveDat::getComId, comId).eq(LiveDat::getId, liveId).eq(LiveDat::getDeleteFlg, false));
        if (ObjectUtils.isEmpty(liveDat)) {
            return R.failed("直播间不存在，请确实后重试！！");
        }

        // 查询直播间跑马灯信息
        LiveMarquee marquee = this.baseMapper.selectOne(new LambdaQueryWrapper<LiveMarquee>()
                .eq(LiveMarquee::getComId, comId).eq(LiveMarquee::getLiveId, liveId));
        if (ObjectUtils.isEmpty(marquee)) {
            // 如果跑马灯信息不存在则新建，并设置相应字段值
            marquee = new LiveMarquee();
            marquee.setId(idService.nextId());
            marquee.setComId(comId);
            marquee.setLiveId(liveId);
            marquee.setContentType(dto.getContentType());
            marquee.setCustomContent(dto.getCustomContent());
            marquee.setMode(dto.getMode());
            marquee.setFontSize(dto.getFontSize());
            marquee.setFontColor(dto.getFontColor());
            marquee.setRollSpeed(dto.getRollSpeed());
            marquee.setCreateBy(accountId);
            marquee.setMarqueeEnable(dto.getMarqueeEnable());
            this.baseMapper.insert(marquee);
        } else {
            // 如果跑马灯信息存在则更新相应字段值
            marquee.setContentType(dto.getContentType());
            marquee.setCustomContent(dto.getCustomContent());
            marquee.setMode(dto.getMode());
            marquee.setFontSize(dto.getFontSize());
            marquee.setFontColor(dto.getFontColor());
            marquee.setRollSpeed(dto.getRollSpeed());
            marquee.setUpdateBy(accountId);
            marquee.setMarqueeEnable(dto.getMarqueeEnable());
            this.baseMapper.updateById(marquee);
        }
        return R.ok();
    }

}
