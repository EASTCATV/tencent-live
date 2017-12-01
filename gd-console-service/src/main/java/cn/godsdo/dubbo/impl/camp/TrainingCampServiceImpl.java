package cn.godsdo.dubbo.impl.camp;

import cn.godsdo.dto.live.CampDto;
import cn.godsdo.dto.live.GetLiveListDto;
import cn.godsdo.dubbo.camp.TrainingCampService;
import cn.godsdo.entity.TencentTask;
import cn.godsdo.entity.camp.TrainingCamp;
import cn.godsdo.entity.camp.TrainingCampLive;
import cn.godsdo.entity.camp.TrainingCampPeriod;
import cn.godsdo.entity.camp.TrainingCom;
import cn.godsdo.mapper.camp.TrainingCampLiveMapper;
import cn.godsdo.mapper.camp.TrainingCampMapper;
import cn.godsdo.mapper.camp.TrainingCampPeriodMapper;
import cn.godsdo.mapper.camp.TrainingComMapper;
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

import java.util.List;

/**
 * <p>
 * 训练营表 服务实现类
 * </p>
 *
 * @author hdk
 * @since 2024-07-05
 */
@Slf4j
@DubboService
public class TrainingCampServiceImpl extends ServiceImpl<TrainingCampMapper, TrainingCamp> implements TrainingCampService {

    @DubboReference(check = false)
    private IdService idService;

    @Resource
    private TrainingCampPeriodMapper trainingCampPeriodMapper;

    @Resource
    private TrainingComMapper trainingComMapper;

    @Override
    public R getCampList(CampDto dto) {
        LambdaQueryWrapper<TrainingCamp> queryWrapper = new LambdaQueryWrapper<TrainingCamp>()
                .eq(TrainingCamp::getComId, dto.getComId())
                .eq(TrainingCamp::getDeleteFlg, false);
        Page<TrainingCamp> page = this.baseMapper.selectPage(new Page<>(dto.getPage(), dto.getPageSize()), queryWrapper);
        // 处理分页结果
        List<TrainingCamp> records = page.getRecords();
        return R.ok(records, page.getTotal());
    }

    @Override
    public void addCamp(CampDto dto) {
        //存数据库
        TrainingCamp camp = TrainingCamp.builder()
                .id(idService.nextId())
                .comId(dto.getComId())
                .campName(dto.getName())
                .createBy(dto.getAccountId())
                .deleteFlg(0)
                .build();
        this.save(camp);
    }

    @Override
    public R editCamp(CampDto dto) {
        try {
            Assert.notNull(dto.getComId(), "客户id不能为空");
            Assert.notNull(dto.getCampId(), "训练营id不能为空");
            Assert.notNull(dto.getName(), "训练营名称不能为空");
        } catch (IllegalArgumentException e) {
            return R.failed(e.getMessage());
        }

        this.update(new LambdaUpdateWrapper<TrainingCamp>()
                .set(TrainingCamp::getCampName, dto.getName())
                .eq(TrainingCamp::getComId, dto.getComId())
                .eq(TrainingCamp::getId, dto.getCampId())
                .eq(TrainingCamp::getDeleteFlg, false));
        return R.ok();
    }

    @Override
    public R delCamp(CampDto dto) {
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
        Long count = this.trainingCampPeriodMapper.selectCount(queryWrapper);

        if (count > 0) {
            return R.failed("该训练营下有营期，不能删除");
        }

        this.update(new LambdaUpdateWrapper<TrainingCamp>()
                .set(TrainingCamp::getDeleteFlg, true)
                .set(TrainingCamp::getUpdateBy, dto.getAccountId())
                .eq(TrainingCamp::getComId, dto.getComId())
                .eq(TrainingCamp::getId, dto.getCampId())
                .eq(TrainingCamp::getDeleteFlg, false));
        return R.ok();
    }

    @Override
    public List<TrainingCom> getKeyByComId(Long comId) {
        return trainingComMapper.selectList(new LambdaQueryWrapper<TrainingCom>()
                .eq(TrainingCom::getComId, comId));
    }

}
