package cn.godsdo.dubbo.impl;

import cloud.tianai.captcha.common.util.CollectionUtils;
import cn.godsdo.constant.CacheConstants;
import cn.godsdo.dto.GetPayOrdersDto;
import cn.godsdo.dto.push.AddPushInterfaceDto;
import cn.godsdo.dto.push.PushInterfaceBaseDto;
import cn.godsdo.dubbo.ExternalInterfaceService;
import cn.godsdo.dubbo.PayOrderService;
import cn.godsdo.dubbo.camp.TrainingCampLiveUserService;
import cn.godsdo.dubbo.camp.TrainingComService;
import cn.godsdo.dubbo.channel.ChannelGroupAdminService;
import cn.godsdo.dubbo.channel.ComChannelDatService;
import cn.godsdo.dubbo.user.UserDatService;
import cn.godsdo.entity.PayOrder;
import cn.godsdo.entity.camp.TrainingCampLiveUser;
import cn.godsdo.entity.camp.TrainingCom;
import cn.godsdo.entity.channel.ChannelGroupAdmin;
import cn.godsdo.entity.channel.ComChannelDat;
import cn.godsdo.entity.user.UserDat;
import cn.godsdo.mapper.PayOrderMapper;
import cn.godsdo.mapper.channel.ComChannelDatMapper;
import cn.godsdo.util.R;
import cn.hutool.core.util.NumberUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author hdk
 * @since 2024-05-15
 */
@DubboService
public class PayOrderServiceImpl extends ServiceImpl<PayOrderMapper, PayOrder> implements PayOrderService {

    @DubboReference(check = false)
    UserDatService userDatService;
    @DubboReference(check = false)
    private ChannelGroupAdminService channelGroupAdminService;
    @DubboReference(check = false)
    private ComChannelDatService comChannelDatService;
    @DubboReference(check = false)
    private TrainingCampLiveUserService trainingCampLiveUserService;

    @DubboReference(check = false)
    TrainingComService trainingComService;

    @DubboReference(check = false)
    ExternalInterfaceService externalInterfaceService;

    @Resource
    private ComChannelDatMapper comChannelDatMapper;

    @Override
    //@Cacheable(key = "#orderNum",value = CacheConstants.PAY_ORDER,unless = "#result==null")
    public PayOrder getOrder(Integer status, Integer type, String orderNum) {
        //基础信息
        LambdaQueryWrapper<PayOrder> wrapper = new LambdaQueryWrapper<PayOrder>()
                .eq(PayOrder::getStatus, status)
                .eq(PayOrder::getType, type)
                .eq(PayOrder::getOrderNum, orderNum);
        return this.baseMapper.selectOne(wrapper);
    }

    @Override
    //@CachePut(key = "#order.orderNum",value = CacheConstants.PAY_ORDER,unless = "#result==null")
    public PayOrder updateOrder(PayOrder order, Integer value, String transactionId) {
        order.setStatus(value);
        order.setTransactionId(transactionId);
        int res = baseMapper.updateById(order);

        if ("商品".equals(order.getTitle())) {
            CompletableFuture.runAsync(() -> pushProductOrder(order));
        }

        if (res == 1) {
            return order;
        } else {
            return null;
        }
    }

    private void pushProductOrder(PayOrder order) {
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

    @Override
    public R queryPayOrderData(GetPayOrdersDto dto) {
        LambdaQueryWrapper<PayOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PayOrder::getTitle, "商品");
        wrapper.eq(PayOrder::getLiveId, dto.getLiveId());
        if (dto.getStatus() != null) {
            wrapper.eq(PayOrder::getStatus, dto.getStatus());
        }
        if (StringUtils.isNotBlank(dto.getNickname())) {
            wrapper.eq(PayOrder::getNickname, dto.getNickname());
        }

        if (StringUtils.isNotBlank(dto.getChannelIds())) {
            // 训练营协助人
            String[] channelIds = dto.getChannelIds().split(",");
            List<Long> channelIdList = new ArrayList<>();
            for (String channelId : channelIds) {
                if (NumberUtil.isLong(channelId) && !"1".equals(channelId)) {
                    channelIdList.add(Long.parseLong(channelId));
                }
            }
            if (!CollectionUtils.isEmpty(channelIdList)) {
                wrapper.in(PayOrder::getChannelId, channelIdList);
            } else {
                return R.ok(new ArrayList<>(), 0);
            }
            if (StringUtils.isNotBlank(dto.getChannelId()) && !"0".equals(dto.getChannelId())) {
                String employeeNum = comChannelDatMapper.getEmployeeByChannelId(dto.getComId(), dto.getChannelId());
                List<TrainingCampLiveUser> list = trainingCampLiveUserService.list(new LambdaQueryWrapper<TrainingCampLiveUser>()
                        .select(TrainingCampLiveUser::getUnionid)
                        .eq(TrainingCampLiveUser::getComId, dto.getComId())
                        .eq(TrainingCampLiveUser::getLiveId, dto.getLiveId())
                        .eq(TrainingCampLiveUser::getRepeatType, 1)
                        .eq(TrainingCampLiveUser::getEmployeeNum, employeeNum)
                        .eq(TrainingCampLiveUser::getDeleteFlg, false));
                if (!CollectionUtils.isEmpty(list)) {
                    List<String> unionIds = list.stream().map(TrainingCampLiveUser::getUnionid).toList();
                    List<UserDat> userList = userDatService.list(new LambdaQueryWrapper<UserDat>()
                            .select(UserDat::getId)
                            .eq(UserDat::getComId, dto.getComId())
                            .in(UserDat::getUnionid, unionIds)
                            .eq(UserDat::getDeleteFlag, false)
                    );
                    if (!CollectionUtils.isEmpty(userList)) {
                        List<Long> ids = userList.stream().map(UserDat::getId).toList();
                        wrapper.in(PayOrder::getUnionid, ids);
                    }
                }
            }
        } else if (StringUtils.isNotBlank(dto.getChannelId()) && !"0".equals(dto.getChannelId())) {
            //是否是管理员
            List<ChannelGroupAdmin> list = channelGroupAdminService.getListByChannelId(dto.getChannelId());
            if (CollectionUtils.isEmpty(list)) {
                //不是管理员
                wrapper.eq(PayOrder::getChannelId, Long.parseLong(dto.getChannelId()));
            } else {
                //是管理员
                List<Long> principalList = new ArrayList<>();
                for (ChannelGroupAdmin admin : list) {
                    principalList.add(admin.getGroupId());
                }
                List<ComChannelDat> channelList = comChannelDatService.getChannelListByGroupIds(principalList);
                if (!CollectionUtils.isEmpty(channelList)) {
                    List<Long> channelIdList = channelList.stream().map(ComChannelDat::getId).toList();
                    wrapper.in(PayOrder::getChannelId, channelIdList);
                }
            }
        }

        wrapper.orderByDesc(PayOrder::getCreateAt);
        // 创建一个分页对象
        Page<PayOrder> datPage = new Page<>(dto.getPage(), dto.getPageSize());
        Page<PayOrder> vo = this.baseMapper.selectPage(datPage, wrapper);
        // 获取查询结果的记录列表和总数
        List<PayOrder> records = vo.getRecords();
        // 返回结果
        return R.ok(records, vo.getTotal());
    }

    @Override
    public R queryPayOrderSum(GetPayOrdersDto dto) {
        LambdaQueryWrapper<PayOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PayOrder::getTitle, "商品");
        wrapper.eq(PayOrder::getLiveId, dto.getLiveId());

        Map<String, Object> map = new HashMap<>();

        if (StringUtils.isNotBlank(dto.getChannelIds())) {
            // 训练营协助人
            String[] channelIds = dto.getChannelIds().split(",");
            List<Long> channelIdList = new ArrayList<>();
            for (String channelId : channelIds) {
                if (NumberUtil.isLong(channelId) && !"1".equals(channelId)) {
                    channelIdList.add(Long.parseLong(channelId));
                }
            }
            if (!CollectionUtils.isEmpty(channelIdList)) {
                wrapper.in(PayOrder::getChannelId, channelIdList);
            } else {
                map.put("userSum", 0);
                map.put("successSum", 0);
                map.put("moneySum", 0);
                return R.ok(map);
            }
            if (StringUtils.isNotBlank(dto.getChannelId()) && !"0".equals(dto.getChannelId())) {
                String employeeNum = comChannelDatMapper.getEmployeeByChannelId(dto.getComId(), dto.getChannelId());
                List<TrainingCampLiveUser> list = trainingCampLiveUserService.list(new LambdaQueryWrapper<TrainingCampLiveUser>()
                        .select(TrainingCampLiveUser::getUnionid)
                        .eq(TrainingCampLiveUser::getComId, dto.getComId())
                        .eq(TrainingCampLiveUser::getLiveId, dto.getLiveId())
                        .eq(TrainingCampLiveUser::getRepeatType, 1)
                        .eq(TrainingCampLiveUser::getEmployeeNum, employeeNum)
                        .eq(TrainingCampLiveUser::getDeleteFlg, false));
                if (!CollectionUtils.isEmpty(list)) {
                    List<String> unionIds = list.stream().map(TrainingCampLiveUser::getUnionid).toList();
                    List<UserDat> userList = userDatService.list(new LambdaQueryWrapper<UserDat>()
                            .select(UserDat::getId)
                            .eq(UserDat::getComId, dto.getComId())
                            .in(UserDat::getUnionid, unionIds)
                            .eq(UserDat::getDeleteFlag, false)
                    );
                    if (!CollectionUtils.isEmpty(userList)) {
                        List<Long> ids = userList.stream().map(UserDat::getId).toList();
                        wrapper.in(PayOrder::getUnionid, ids);
                    }
                }
            }
        } else if (StringUtils.isNotBlank(dto.getChannelId()) && !"0".equals(dto.getChannelId())) {
            //是否是管理员
            List<ChannelGroupAdmin> list = channelGroupAdminService.getListByChannelId(dto.getChannelId());
            if (CollectionUtils.isEmpty(list)) {
                //不是管理员
                wrapper.eq(PayOrder::getChannelId, Long.parseLong(dto.getChannelId()));
            } else {
                //是管理员
                List<Long> principalList = new ArrayList<>();
                for (ChannelGroupAdmin admin : list) {
                    principalList.add(admin.getGroupId());
                }
                List<ComChannelDat> channelList = comChannelDatService.getChannelListByGroupIds(principalList);
                if (!CollectionUtils.isEmpty(channelList)) {
                    List<Long> channelIdList = channelList.stream().map(ComChannelDat::getId).toList();
                    wrapper.in(PayOrder::getChannelId, channelIdList);
                }
            }
        }

        List<PayOrder> list = this.baseMapper.selectList(wrapper);

        int successSum = 0;
        BigDecimal moneySum = new BigDecimal(0);
        List<String> accountList = new ArrayList<>();
        for (PayOrder payOrder : list) {
            if (!accountList.contains(payOrder.getAccount())){
                accountList.add(payOrder.getAccount());
            }
            if (payOrder.getStatus() == 1) {
                successSum ++;
                moneySum = moneySum.add(payOrder.getTotalFee());
            }
        }

        map.put("userSum", accountList.size());
        map.put("successSum", successSum);
        map.put("moneySum", moneySum.divide(new BigDecimal(100), 2, RoundingMode.HALF_UP));
        return R.ok(map);
    }

}
