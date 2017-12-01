package cn.godsdo.dubbo.impl.camp;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import cn.godsdo.dubbo.camp.TrainingPeriodChannelInfoService;
import cn.godsdo.entity.camp.TrainingPeriodChannelInfo;
import cn.godsdo.mapper.camp.TrainingPeriodChannelInfoMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * <p>
 * 渠道营期绑定表 服务实现类
 * </p>
 *
 * @author hdk
 * @since 2024-07-07
 */
@Slf4j
@DubboService
public class TrainingPeriodChannelInfoServiceImpl extends ServiceImpl<TrainingPeriodChannelInfoMapper, TrainingPeriodChannelInfo> implements TrainingPeriodChannelInfoService {

}
