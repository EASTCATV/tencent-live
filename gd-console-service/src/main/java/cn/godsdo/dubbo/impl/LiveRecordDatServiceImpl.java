package cn.godsdo.dubbo.impl;

import cn.godsdo.dubbo.LiveRecordDatService;
import cn.godsdo.dubbo.cache.ClearCache;
import cn.godsdo.entity.LiveRecordDat;
import cn.godsdo.enums.live.LiveStatusEnum;
import cn.godsdo.mapper.LiveRecordDatMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.interfaces.IdService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * 直播开播记录表 服务实现类
 * </p>
 *
 * @author hdk
 * @since 2024-06-04
 */
@Slf4j
@DubboService
public class LiveRecordDatServiceImpl extends ServiceImpl<LiveRecordDatMapper, LiveRecordDat> implements LiveRecordDatService {

    @DubboReference(check = false)
    private IdService idService;
    @DubboReference(check = false)
    private ClearCache clearCache;

    //缓存房间直播状态
    @Override
    public Long saveDat(LiveRecordDat dat) {
        clearCache.delLiveRecordInfo(dat.getComId(),dat.getLiveId());
//        dat.setId(idService.nextId());
        dat.setState(LiveStatusEnum.LIVING.getValue());
        save(dat);
        return dat.getId();
    }

    @Override
    public boolean updateDat(LiveRecordDat recordDat) {
        clearCache.delLiveRecordInfo(recordDat.getComId(),recordDat.getLiveId());
        return this.update(new LambdaUpdateWrapper<LiveRecordDat>()
                .set(LiveRecordDat::getEndTime, recordDat.getEndTime())
                .set(LiveRecordDat::getUpdateBy, recordDat.getUpdateBy())
                .set(LiveRecordDat::getState, LiveStatusEnum.NOT_STARTED.getValue())
                .eq(LiveRecordDat::getLiveId, recordDat.getLiveId())
                .eq(LiveRecordDat::getState, LiveStatusEnum.LIVING.getValue())
        );
    }

    @Override
    public LiveRecordDat getByLiveId(Long liveId,String taskId) {
        return this.baseMapper.selectOne(new LambdaQueryWrapper<LiveRecordDat>()
                .eq(LiveRecordDat::getLiveId, liveId)
                .eq(LiveRecordDat::getReqTaskId, taskId)
                .eq(LiveRecordDat::getState, LiveStatusEnum.LIVING.getValue()));
    }

    @Override
    public void updateByLiveId(LiveRecordDat dto) {
        clearCache.delLiveRecordInfo(dto.getComId(),dto.getLiveId());
        this.baseMapper.update(null, new LambdaUpdateWrapper<LiveRecordDat>()
                .set(LiveRecordDat::getState, LiveStatusEnum.NOT_STARTED.getValue())
                .set(LiveRecordDat::getEndTime, dto.getEndTime())
                .set(LiveRecordDat::getUpdateBy, dto.getUpdateBy())
                .eq(LiveRecordDat::getLiveId, dto.getLiveId())
                .eq(LiveRecordDat::getState, LiveStatusEnum.LIVING.getValue())
        );
    }

    @Override
    public List<LiveRecordDat> getListBayDat(Long comId, Long liveId, String queryDate) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date start = formatter.parse(queryDate + " 00:00:00");
            Date end = formatter.parse(queryDate + " 23:59:59");
            LambdaQueryWrapper<LiveRecordDat> wrapper = new LambdaQueryWrapper<>();
            if (null != comId) {
                wrapper.eq(LiveRecordDat::getComId, comId);
            }
            if (null != liveId) {
                wrapper.eq(LiveRecordDat::getLiveId, liveId);
            }
            wrapper.ge(LiveRecordDat::getStartTime, start);
            wrapper.le(LiveRecordDat::getStartTime, end);
            return this.baseMapper.selectList(wrapper);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public LiveRecordDat getLiveDat(Long comId, Long liveId) {
        LiveRecordDat liveRecordDat = this.baseMapper.selectOne(new LambdaQueryWrapper<LiveRecordDat>()
                .eq(LiveRecordDat::getLiveId, liveId)
                .eq(LiveRecordDat::getComId, comId)
                .eq(LiveRecordDat::getState, LiveStatusEnum.LIVING.getValue())
                .orderByDesc(LiveRecordDat::getStartTime).last("limit 1"));
        return liveRecordDat;
    }

}
