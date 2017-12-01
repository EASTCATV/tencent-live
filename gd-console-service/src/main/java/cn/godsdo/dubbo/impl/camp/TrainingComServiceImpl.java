package cn.godsdo.dubbo.impl.camp;

import cn.godsdo.dubbo.camp.TrainingComService;
import cn.godsdo.entity.camp.TrainingCom;
import cn.godsdo.mapper.camp.TrainingComMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

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
public class TrainingComServiceImpl extends ServiceImpl<TrainingComMapper, TrainingCom> implements TrainingComService {

}
