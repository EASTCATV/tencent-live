package cn.godsdo.dubbo.impl.camp;

import cn.godsdo.dubbo.camp.TrainingComTaskService;
import cn.godsdo.entity.camp.TrainingComTask;
import cn.godsdo.mapper.camp.TrainingComTaskMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 训练营任务表 服务实现类
 * </p>
 *
 * @author hdk
 * @since 2024-07-05
 */
@Slf4j
@DubboService
public class TrainingComTaskServiceImpl extends ServiceImpl<TrainingComTaskMapper, TrainingComTask> implements TrainingComTaskService {

}
