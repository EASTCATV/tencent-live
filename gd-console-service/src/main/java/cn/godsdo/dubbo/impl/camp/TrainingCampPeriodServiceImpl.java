package cn.godsdo.dubbo.impl.camp;

import cn.godsdo.dto.live.CampDto;
import cn.godsdo.dubbo.camp.TrainingCampPeriodService;
import cn.godsdo.dubbo.camp.TrainingCampService;
import cn.godsdo.entity.PayProductClick;
import cn.godsdo.entity.camp.TrainingCamp;
import cn.godsdo.entity.camp.TrainingCampLive;
import cn.godsdo.entity.camp.TrainingCampPeriod;
import cn.godsdo.entity.live.LiveDat;
import cn.godsdo.enums.PayEnum;
import cn.godsdo.mapper.camp.TrainingCampLiveMapper;
import cn.godsdo.mapper.camp.TrainingCampPeriodMapper;
import cn.godsdo.mapper.com.ComChunkInfoMapper;
import cn.godsdo.util.R;
import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.interfaces.IdService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.Date;
import java.util.List;

/**
 * <p>
 * 训练营营期表 服务实现类
 * </p>
 *
 * @author hdk
 * @since 2024-07-05
 */
@Slf4j
@DubboService
public class TrainingCampPeriodServiceImpl extends ServiceImpl<TrainingCampPeriodMapper, TrainingCampPeriod> implements TrainingCampPeriodService {

    @DubboReference(check = false)
    private IdService idService;

    @Resource
    private TrainingCampLiveMapper trainingCampLiveMapper;

    @Resource
    private TrainingCampService trainingCampService;

    @Override
    public R getPeriodList(CampDto dto) {
        try {
            Assert.notNull(dto.getComId(), "客户id不能为空");
            Assert.notNull(dto.getCampId(), "训练营id不能为空");
        } catch (IllegalArgumentException e) {
            return R.failed(e.getMessage());
        }
        LambdaQueryWrapper<TrainingCampPeriod> queryWrapper = new LambdaQueryWrapper<TrainingCampPeriod>()
                .eq(TrainingCampPeriod::getComId, dto.getComId())
                .eq(TrainingCampPeriod::getCampId, dto.getCampId())
                .eq(TrainingCampPeriod::getDeleteFlg, false);
        Page<TrainingCampPeriod> page = this.baseMapper.selectPage(new Page<>(dto.getPage(), dto.getPageSize()), queryWrapper);
        // 处理分页结果
        List<TrainingCampPeriod> records = page.getRecords();
        return R.ok(records, page.getTotal());
    }

    @Override
    public R addPeriod(CampDto dto) {
        try {
            Assert.notNull(dto.getComId(), "客户id不能为空");
            Assert.notNull(dto.getCampId(), "训练营id不能为空");
        } catch (IllegalArgumentException e) {
            return R.failed(e.getMessage());
        }

        LambdaQueryWrapper<TrainingCampPeriod> queryWrapper = new LambdaQueryWrapper<TrainingCampPeriod>()
                .eq(TrainingCampPeriod::getComId, dto.getComId())
                .eq(TrainingCampPeriod::getCampId, dto.getCampId());
        Long count = this.baseMapper.selectCount(queryWrapper);

        //存数据库
        TrainingCampPeriod camp = TrainingCampPeriod.builder()
                .id(idService.nextId())
                .comId(dto.getComId())
                .campId(dto.getCampId())
                .periodNum((int) (count + 1))
                .periodName(dto.getName())
                .periodUserNum(0)
                .createBy(dto.getAccountId())
                .deleteFlg(0)
                .build();
        this.save(camp);

        TrainingCamp trainingCamp = trainingCampService.getById(dto.getCampId());
        if (null == trainingCamp.getTotal()) {
            trainingCamp.setTotal(1);
        } else {
            trainingCamp.setTotal(trainingCamp.getTotal() + 1);
        }
        trainingCampService.updateById(trainingCamp);

        return R.ok();
    }

    @Override
    public R delPeriod(CampDto dto) {
        try {
            Assert.notNull(dto.getComId(), "客户id不能为空");
            Assert.notNull(dto.getCampId(), "训练营id不能为空");
            Assert.notNull(dto.getId(), "营期id不能为空");
        } catch (IllegalArgumentException e) {
            return R.failed(e.getMessage());
        }

        LambdaQueryWrapper<TrainingCampLive> queryWrapper = new LambdaQueryWrapper<TrainingCampLive>()
                .eq(TrainingCampLive::getPeriodId, dto.getId());
        Long count = this.trainingCampLiveMapper.selectCount(queryWrapper);

        if (count > 0) {
            return R.failed("该营期下有课程，不能删除");
        }
        this.update(new LambdaUpdateWrapper<TrainingCampPeriod>()
                .set(TrainingCampPeriod::getDeleteFlg, true)
                .set(TrainingCampPeriod::getUpdateBy, dto.getAccountId())
                .eq(TrainingCampPeriod::getComId, dto.getComId())
                .eq(TrainingCampPeriod::getCampId, dto.getCampId())
                .eq(TrainingCampPeriod::getId, dto.getId())
                .eq(TrainingCampPeriod::getDeleteFlg, false));

        TrainingCamp trainingCamp = trainingCampService.getById(dto.getCampId());
        if (null == trainingCamp.getTotal()) {
            trainingCamp.setTotal(0);
        } else {
            trainingCamp.setTotal(trainingCamp.getTotal() - 1);
        }
        trainingCampService.updateById(trainingCamp);

        return R.ok();
    }

    @Override
    public R editPeriod(CampDto dto) {
        try {
            Assert.notNull(dto.getComId(), "客户id不能为空");
            Assert.notNull(dto.getCampId(), "训练营id不能为空");
            Assert.notNull(dto.getId(), "营期id不能为空");
            Assert.notNull(dto.getName(), "营期名称不能为空");
        } catch (IllegalArgumentException e) {
            return R.failed(e.getMessage());
        }

        this.update(new LambdaUpdateWrapper<TrainingCampPeriod>()
                .set(TrainingCampPeriod::getPeriodName, dto.getName())
                .set(TrainingCampPeriod::getUpdateBy, dto.getAccountId())
                .eq(TrainingCampPeriod::getComId, dto.getComId())
                .eq(TrainingCampPeriod::getCampId, dto.getCampId())
                .eq(TrainingCampPeriod::getId, dto.getId())
                .eq(TrainingCampPeriod::getDeleteFlg, false));

        return R.ok();

    }
}
