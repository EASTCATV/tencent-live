package cn.godsdo.dubbo.impl;

import cn.godsdo.dubbo.LiveOrderControlClickService;
import cn.godsdo.entity.LiveOrderControlClick;
import cn.godsdo.mapper.LiveOrderControlClickMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * <p>
 * 订单场控按钮点击记录表 服务实现类
 * </p>
 *
 * @author hdk
 * @since 2024-07-15
 */
@DubboService
public class LiveOrderControlClickServiceImpl extends ServiceImpl<LiveOrderControlClickMapper, LiveOrderControlClick> implements LiveOrderControlClickService {

}
