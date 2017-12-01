package cn.godsdo.dubbo.impl.camp;

import cn.godsdo.dubbo.camp.TrainingComTaskErrorsService;
import cn.godsdo.entity.camp.TrainingComTaskErrors;
import cn.godsdo.mapper.camp.TrainingComTaskErrorsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 训练营任务失败记录表 服务实现类
 * </p>
 *
 * @author hdk
 * @since 2024-07-06
 */
@Slf4j
@DubboService
public class TrainingComTaskErrorsServiceImpl extends ServiceImpl<TrainingComTaskErrorsMapper, TrainingComTaskErrors> implements TrainingComTaskErrorsService {

    @Override
    public List<TrainingComTaskErrors> selectByTaskId(Long taskId) {
        return this.baseMapper.selectList(new LambdaQueryWrapper<TrainingComTaskErrors>()
                .select(TrainingComTaskErrors::getMsg)
                .eq(TrainingComTaskErrors::getTaskId, taskId));
    }

}
