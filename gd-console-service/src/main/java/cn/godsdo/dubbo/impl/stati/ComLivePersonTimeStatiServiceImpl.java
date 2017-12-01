package cn.godsdo.dubbo.impl.stati;

import cn.godsdo.dubbo.stati.ComLivePersonTimeStatiService;
import cn.godsdo.entity.stati.ComLivePersonTimeStati;
import cn.godsdo.mapper.stati.ComLivePersonTimeStatiMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.dubbo.config.annotation.DubboService;

import java.time.LocalDate;

/**
 * <p>
 * 用户房间直播统计表 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2024-07-25
 */
@DubboService
public class ComLivePersonTimeStatiServiceImpl extends ServiceImpl<ComLivePersonTimeStatiMapper, ComLivePersonTimeStati> implements ComLivePersonTimeStatiService {

}
