package cn.godsdo.dubbo.impl.camp;

import cn.godsdo.constant.CacheConstants;
import cn.godsdo.dto.assistan.GetCampUsersDto;
import cn.godsdo.entity.AccountDat;
import cn.godsdo.mapper.channel.ComChannelDatMapper;
import cn.godsdo.util.R;
import cn.hutool.core.util.NumberUtil;
import com.alibaba.nacos.common.utils.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import cn.godsdo.dubbo.camp.TrainingCampLiveUserService;
import cn.godsdo.entity.camp.TrainingCampLiveUser;
import cn.godsdo.mapper.camp.TrainingCampLiveUserMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 训练营直播间学员表 服务实现类
 * </p>
 *
 * @author hdk
 * @since 2024-07-08
 */
@Slf4j
@DubboService
public class TrainingCampLiveUserServiceImpl extends ServiceImpl<TrainingCampLiveUserMapper, TrainingCampLiveUser> implements TrainingCampLiveUserService {

    @Resource
    private ComChannelDatMapper comChannelDatMapper;

    @Override
    @Cacheable(key = "#liveId + '_' + #unionId",value = CacheConstants.WATCH_CAMP_USER,unless = "#result==null")
    public TrainingCampLiveUser getCampLiveUserByCache(Long comId, Long liveId, String unionId) {
        return this.baseMapper.selectOne(new LambdaQueryWrapper<TrainingCampLiveUser>()
                .eq(TrainingCampLiveUser::getComId, comId)
                .eq(TrainingCampLiveUser::getLiveId, liveId)
                .eq(TrainingCampLiveUser::getUnionid, unionId)
                .eq(TrainingCampLiveUser::getRepeatType, 0)
                .eq(TrainingCampLiveUser::getDeleteFlg, false));
    }

    @Override
    @CachePut(key = "#liveId + '_' + #unionId",value = CacheConstants.WATCH_CAMP_USER,unless = "#result==null")
    public TrainingCampLiveUser updateCampLiveUserCache(TrainingCampLiveUser campLiveUser, Long liveId, String unionId) {
        this.baseMapper.updateById(campLiveUser);
        return campLiveUser;
    }

    @Override
    public R getSignIn(GetCampUsersDto dto) {
        List<TrainingCampLiveUser> rlist = new ArrayList<>();

        LambdaQueryWrapper<TrainingCampLiveUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TrainingCampLiveUser::getComId, dto.getComId());
        queryWrapper.eq(TrainingCampLiveUser::getLiveId, dto.getLiveId());
        queryWrapper.eq(TrainingCampLiveUser::getRepeatType, 0);
        queryWrapper.eq(TrainingCampLiveUser::getDeleteFlg, false);
        if (null != dto.getSingType()) {
            queryWrapper.eq(TrainingCampLiveUser::getSignIn, dto.getSingType());
        }
        if (StringUtils.isNotEmpty(dto.getNickName())) {
            queryWrapper.like(TrainingCampLiveUser::getNickName, dto.getNickName());
        }

        if (StringUtils.isNotEmpty(dto.getChannelIds())) {
            String[] channelIdsArr = dto.getChannelIds().split(",");
            List<String> employeeIds = comChannelDatMapper.getEmployeeByChannelIds(dto.getComId(), channelIdsArr);
            if (!employeeIds.isEmpty()) {
                queryWrapper.in(TrainingCampLiveUser::getEmployeeNum, employeeIds);
            } else {
                return R.ok(rlist, 0);
            }
            if (NumberUtil.isLong(dto.getChannelId()) && !"0".equals(dto.getChannelId())) {
                String employeeNum = comChannelDatMapper.getEmployeeByChannelId(dto.getComId(), dto.getChannelId());
                List<TrainingCampLiveUser> list = this.baseMapper.selectList(new LambdaQueryWrapper<TrainingCampLiveUser>()
                                .select(TrainingCampLiveUser::getUnionid)
                                .eq(TrainingCampLiveUser::getComId, dto.getComId())
                                .eq(TrainingCampLiveUser::getLiveId, dto.getLiveId())
                                .eq(TrainingCampLiveUser::getRepeatType, 1)
                                .eq(TrainingCampLiveUser::getEmployeeNum, employeeNum)
                                .eq(TrainingCampLiveUser::getDeleteFlg, false));
                if (!list.isEmpty()) {
                    List<String> unionIds = list.stream().map(TrainingCampLiveUser::getUnionid).toList();
                    queryWrapper.in(TrainingCampLiveUser::getUnionid, unionIds);
                }
            }
        } else if (NumberUtil.isLong(dto.getChannelId()) && !"0".equals(dto.getChannelId())) {
            String[] channelIdsArr = dto.getChannelId().split(",");
            List<String> employeeIds = comChannelDatMapper.getEmployeeByChannelIds(dto.getComId(), channelIdsArr);
            if (!employeeIds.isEmpty()) {
                queryWrapper.in(TrainingCampLiveUser::getEmployeeNum, employeeIds);
            }
        }
        rlist = this.baseMapper.selectList(queryWrapper);
        return R.ok(rlist, rlist.size());
    }

    @Override
    public R getChannelIds(GetCampUsersDto dto) {
        List<Long> channelIds = comChannelDatMapper.getChannelIds(dto.getComId(), dto.getLiveId(), dto.getChannelId());
        if (channelIds.isEmpty()) {
            return R.ok("");
        } else {
            StringBuilder channelIdsStr = new StringBuilder();
            for (Long channelId : channelIds) {
                channelIdsStr.append(channelId).append(",");
            }
            channelIdsStr = new StringBuilder(channelIdsStr.substring(0, channelIdsStr.length() - 1));
            return R.ok(channelIdsStr.toString());
        }
    }

}
