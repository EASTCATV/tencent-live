package cn.godsdo.dubbo.impl;

import cn.godsdo.dto.GetBaseDto;
import cn.godsdo.dubbo.LiveOrderControlRobotService;
import cn.godsdo.entity.LiveOrderControlRobot;
import cn.godsdo.entity.LiveRecordDat;
import cn.godsdo.entity.PayProductClick;
import cn.godsdo.entity.camp.TrainingCampPeriod;
import cn.godsdo.enums.PayEnum;
import cn.godsdo.mapper.LiveOrderControlRobotMapper;
import cn.godsdo.util.R;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.Date;
import java.util.List;

/**
 * <p>
 * 直播间订单场控机器人表 服务实现类
 * </p>
 *
 * @author hdk
 * @since 2024-07-15
 */
@DubboService
public class LiveOrderControlRobotServiceImpl extends ServiceImpl<LiveOrderControlRobotMapper, LiveOrderControlRobot> implements LiveOrderControlRobotService {

    @Override
    public R getOrderControlRobotList(GetBaseDto dto) {
        LambdaQueryWrapper<LiveOrderControlRobot> queryWrapper = new LambdaQueryWrapper<LiveOrderControlRobot>()
                .select(LiveOrderControlRobot::getId, LiveOrderControlRobot::getNickName)
                .eq(LiveOrderControlRobot::getComId, dto.getComId())
                .eq(LiveOrderControlRobot::getLiveId, dto.getLiveId())
                .eq(LiveOrderControlRobot::getDeleteFlg, false)
                .orderByDesc(LiveOrderControlRobot::getId);
        Page<LiveOrderControlRobot> page = this.baseMapper.selectPage(new Page<>(dto.getPage(), dto.getPageSize()), queryWrapper);
        // 处理分页结果
        List<LiveOrderControlRobot> records = page.getRecords();
        return R.ok(records, page.getTotal());
    }

    @Override
    public R clearByRoomId(GetBaseDto dto) {
        this.baseMapper.delete(new LambdaQueryWrapper<LiveOrderControlRobot>()
               .eq(LiveOrderControlRobot::getComId, dto.getComId())
               .eq(LiveOrderControlRobot::getLiveId, dto.getLiveId()));
        return R.ok();
    }

    @Override
    public R updateOrderControlRobot(GetBaseDto dto, Long accountId) {
        this.update(new LambdaUpdateWrapper<LiveOrderControlRobot>()
                .set(LiveOrderControlRobot::getNickName, dto.getName())
                .set(LiveOrderControlRobot::getUpdateBy, accountId)
                .set(LiveOrderControlRobot::getUpdateAt, new Date())
                .eq(LiveOrderControlRobot::getComId, dto.getComId())
                .eq(LiveOrderControlRobot::getLiveId, dto.getLiveId())
                .eq(LiveOrderControlRobot::getId, dto.getId()));
        return R.ok();
    }

    @Override
    public R delOrderControlRobotOne(GetBaseDto dto) {
        this.baseMapper.delete(new LambdaQueryWrapper<LiveOrderControlRobot>()
                .eq(LiveOrderControlRobot::getComId, dto.getComId())
                .eq(LiveOrderControlRobot::getLiveId, dto.getLiveId())
                .eq(LiveOrderControlRobot::getId, dto.getId()));
        return R.ok();
    }

}
