package cn.godsdo.dubbo.impl.camp;

import cn.godsdo.dubbo.camp.TrainingCampLiveService;
import cn.godsdo.dubbo.camp.TrainingCampLiveUserService;
import cn.godsdo.dubbo.channel.ChannelLiveInfoService;
import cn.godsdo.entity.camp.TrainingCampLive;
import cn.godsdo.entity.camp.TrainingCampLiveUser;
import cn.godsdo.entity.camp.TrainingCampUser;
import cn.godsdo.entity.camp.TrainingPeriodChannelInfo;
import cn.godsdo.entity.channel.ChannelLiveInfo;
import cn.godsdo.mapper.camp.TrainingCampLiveMapper;
import cn.godsdo.mapper.camp.TrainingCampUserMapper;
import cn.godsdo.mapper.camp.TrainingPeriodChannelInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.interfaces.IdService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author hdk
 * @since 2024-07-05
 */
@Slf4j
@DubboService
public class TrainingCampLiveServiceImpl extends ServiceImpl<TrainingCampLiveMapper, TrainingCampLive> implements TrainingCampLiveService {

    @DubboReference(check = false)
    private IdService idService;
    @DubboReference(check = false)
    private ChannelLiveInfoService channelLiveInfoService;

    @DubboReference(check = false)
    private TrainingCampLiveUserService trainingCampLiveUserService;

    @Resource
    private TrainingPeriodChannelInfoMapper trainingPeriodChannelInfoMapper;

    @Resource
    private TrainingCampUserMapper trainingCampUserMapper;

    @Override
    public void addCampLive(Long periodId, Long liveId) {
        TrainingCampLive trainingCampLive = new TrainingCampLive();
        trainingCampLive.setPeriodId(periodId);
        trainingCampLive.setLiveId(liveId);
        this.save(trainingCampLive);

        // 同步渠道
        List<TrainingPeriodChannelInfo> list = trainingPeriodChannelInfoMapper.selectList(new LambdaQueryWrapper<TrainingPeriodChannelInfo>()
                .eq(TrainingPeriodChannelInfo::getPeriodId, periodId)
               .eq(TrainingPeriodChannelInfo::getDeleteFlag, false)
        );
        if (!list.isEmpty()) {
            List<ChannelLiveInfo> result = new ArrayList<>();
            for (TrainingPeriodChannelInfo trainingPeriodChannelInfo : list) {
                ChannelLiveInfo cga = new ChannelLiveInfo();
                cga.setId(idService.nextId());
                cga.setChannelId(trainingPeriodChannelInfo.getChannelId());
                cga.setComId(trainingPeriodChannelInfo.getComId());
                cga.setLiveId(liveId);
                cga.setCreateBy(0L);
                result.add(cga);
            }
            channelLiveInfoService.saveBatch(result);
        }

        // 同步用户
        List<TrainingCampUser> userList = trainingCampUserMapper.selectList(new LambdaQueryWrapper<TrainingCampUser>()
                .eq(TrainingCampUser::getPeriodId, periodId)
                .eq(TrainingCampUser::getDeleteFlg, false)
        );
        if (!userList.isEmpty()) {
            List<TrainingCampLiveUser> liveUsers = new ArrayList<>();
            for (TrainingCampUser user : userList) {
                TrainingCampLiveUser liveUser = new TrainingCampLiveUser();
                BeanUtils.copyProperties(user, liveUser);
                liveUser.setId(idService.nextId());
                liveUser.setLiveId(liveId);
                liveUsers.add(liveUser);
            }
            trainingCampLiveUserService.saveBatch(liveUsers);
        }
    }

}
