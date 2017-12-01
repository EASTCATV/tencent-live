package cn.godsdo.dubbo.impl.crossServerImpl;

import cn.godsdo.dto.push.AddPushInterfaceDto;
import cn.godsdo.dto.push.PushInterfaceBaseDto;
import cn.godsdo.dto.assistan.LiveOrderControlDto;
import cn.godsdo.dubbo.ExternalInterfaceService;
import cn.godsdo.dubbo.LiveOrderControlService;
import cn.godsdo.dubbo.camp.TrainingComService;
import cn.godsdo.dubbo.crossService.LiveDubboService;
import cn.godsdo.entity.PayOrder;
import cn.godsdo.entity.camp.TrainingCom;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * @Author: CR7
 * @Date: 2019/5/5 10:53
 * @Description:
 */
@Slf4j
@DubboService
public class LiveDubboServiceImpl implements LiveDubboService {
    @DubboReference(check = false)
    ExternalInterfaceService externalInterfaceService;

    @DubboReference(check = false)
    LiveOrderControlService liveOrderControlService;

    @DubboReference(check = false)
    TrainingComService trainingComService;

    @Override
    public void sendOrderController(PayOrder order) {

        if ("商品".equals(order.getTitle())) {
            LiveOrderControlDto orderControlDto = new LiveOrderControlDto();
            orderControlDto.setComId(order.getComId());
            orderControlDto.setLiveId(order.getLiveId());
            orderControlDto.setNickName(order.getNickname());
            orderControlDto.setLiveAccount(order.getAccount());
            liveOrderControlService.sendOrderController(orderControlDto);

            //接口推送
            TrainingCom trainingCom = trainingComService.getOne(new LambdaQueryWrapper<TrainingCom>()
                    .eq(TrainingCom::getComId, order.getComId()));
            if (trainingCom != null && StringUtils.isNotBlank(trainingCom.getPath()) && StringUtils.isNotBlank(trainingCom.getComKey())) {
                PushInterfaceBaseDto dto = new PushInterfaceBaseDto();
                dto.setUrl(trainingCom.getPath());
                dto.setKey(trainingCom.getComKey());
                AddPushInterfaceDto<PayOrder> pushInterfaceDto= new AddPushInterfaceDto<>();
                pushInterfaceDto.setData(order);
                pushInterfaceDto.setDto(dto);
                externalInterfaceService.pushProductOrder(pushInterfaceDto);
            }
        }

    }

}
