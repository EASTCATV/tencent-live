package cn.godsdo.dubbo.impl.camp;

import cn.godsdo.dto.live.CampDto;
import cn.godsdo.dto.live.CampUserDto;
import cn.godsdo.dubbo.cache.ClearCache;
import cn.godsdo.dubbo.camp.*;
import cn.godsdo.dubbo.channel.ChannelLiveInfoService;
import cn.godsdo.dubbo.channel.ComChannelDatService;
import cn.godsdo.entity.AccountDat;
import cn.godsdo.entity.camp.*;
import cn.godsdo.entity.channel.ChannelLiveInfo;
import cn.godsdo.entity.channel.ComChannelDat;
import cn.godsdo.enums.camp.ComTaskEnum;
import cn.godsdo.mapper.AccountDatMapper;
import cn.godsdo.mapper.camp.*;
import cn.godsdo.mapper.channel.ComChannelDatMapper;
import cn.godsdo.util.R;
import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.interfaces.IdService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 训练营营期学员表 服务实现类
 * </p>
 *
 * @author hdk
 * @since 2024-07-05
 */
@Slf4j
@DubboService
public class TrainingCampUserServiceImpl extends ServiceImpl<TrainingCampUserMapper, TrainingCampUser> implements TrainingCampUserService {

    @DubboReference(check = false)
    IdService idService;
    @DubboReference(check = false)
    private TrainingComTaskService trainingComTaskService;
    @DubboReference(check = false)
    private TrainingComTaskErrorsService trainingComTaskErrorsService;
    @DubboReference(check = false)
    private TrainingCampPeriodService trainingCampPeriodService;
    @DubboReference(check = false)
    private TrainingPeriodChannelInfoService trainingPeriodChannelInfoService;
    @DubboReference(check = false)
    private ComChannelDatService comChannelDatService;
    @DubboReference(check = false)
    private ChannelLiveInfoService channelLiveInfoService;
    @DubboReference(check = false)
    private TrainingCampLiveUserService trainingCampLiveUserService;
    @DubboReference(check = false)
    ClearCache clearcache;
    @Resource
    private TrainingComMapper trainingComMapper;
    @Resource
    private AccountDatMapper accountDatMapper;
    @Resource
    private ComChannelDatMapper comChannelDatMapper;

    @Resource
    private TrainingCampPeriodMapper trainingCampPeriodMapper;
    @Resource
    private TrainingPeriodChannelInfoMapper trainingPeriodChannelInfoMapper;
    @Resource
    private TrainingCampLiveMapper trainingCampLiveMapper;
    @Resource
    private TrainingCampLiveUserMapper trainingCampLiveUserMapper;

    @Override
    public R getLiveCampList(CampDto dto) {
        try {
            Assert.notNull(dto.getComId(), "客户id不能为空");
            Assert.notNull(dto.getPeriodId(), "营期id不能为空");
            Assert.notNull(dto.getPage(), "查询页码不能为空");
            Assert.notNull(dto.getPageSize(), "查询页大小不能为空");
        } catch (IllegalArgumentException e) {
            return R.failed(e.getMessage());
        }
        LambdaQueryWrapper<TrainingCampUser> queryWrapper = new LambdaQueryWrapper<TrainingCampUser>()
                .eq(TrainingCampUser::getComId, dto.getComId())
                .eq(TrainingCampUser::getPeriodId, dto.getPeriodId())
                .eq(TrainingCampUser::getDeleteFlg, false)
                .orderByDesc(TrainingCampUser::getCreateAt);
        Page<TrainingCampUser> page = this.baseMapper.selectPage(new Page<>(dto.getPage(), dto.getPageSize()), queryWrapper);
        // 处理分页结果
        List<TrainingCampUser> records = page.getRecords();
        return R.ok(records, page.getTotal());
    }

    @Override
    public boolean checkComKey(Long comId, String comKey) {
        Long count = trainingComMapper.selectCount(new LambdaQueryWrapper<TrainingCom>()
                .eq(TrainingCom::getComId, comId)
                .eq(TrainingCom::getComKey, comKey));
        return count == 1;
    }

    @Override
    public void importUser(CampUserDto dto) {
        TrainingComTask task = new TrainingComTask();
        task.setId(dto.getTaskId());
        task.setComId(dto.getComId());
        task.setTotalNum(dto.getList().size());
        task.setState(ComTaskEnum.IMPORTING.getValue());
        trainingComTaskService.save(task);

        int success = 0;
        int userNum = 0;
        int fail = 0;
        List<TrainingComTaskErrors> errorList = new ArrayList<>();
        TrainingComTaskErrors error;

        List<AccountDat> employeeNums = new ArrayList<>();
        try {
            List<TrainingCampUser> list = dto.getList();
            List<AccountDat> accountDats = accountDatMapper.selectList(new LambdaQueryWrapper<AccountDat>()
                    .eq(AccountDat::getComId, dto.getComId())
                    .isNotNull(AccountDat::getEmployeeNum)
                    .eq(AccountDat::getDeleteFlg, false)
            );
            Map<String, AccountDat> accountDatMap = accountDats.stream().collect(Collectors.toMap(AccountDat::getEmployeeNum, Function.identity()));

            List<TrainingCampUser> campUserList = this.baseMapper.selectList(new LambdaQueryWrapper<TrainingCampUser>()
                    .eq(TrainingCampUser::getComId, dto.getComId())
                    .eq(TrainingCampUser::getPeriodId, dto.getPeriodId())
                    .eq(TrainingCampUser::getRepeatType, 0)
                    .eq(TrainingCampUser::getDeleteFlg, false)
            );
            Map<String, TrainingCampUser> campUserMap = campUserList.stream().collect(Collectors.toMap(TrainingCampUser::getUnionid, Function.identity()));

            List<TrainingCampUser> insertList = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                TrainingCampUser user = list.get(i);
                if (ObjectUtils.isNull(user.getNickName())) {
                    error = new TrainingComTaskErrors();
                    error.setId(idService.nextId());
                    error.setComId(dto.getComId());
                    error.setTaskId(dto.getTaskId());
                    error.setMsg("第" + (i + 1) + "行，昵称不能为空");
                    errorList.add(error);
                    fail ++;
                } else if (ObjectUtils.isNull(user.getEmployeeNum())) {
                    error = new TrainingComTaskErrors();
                    error.setId(idService.nextId());
                    error.setComId(dto.getComId());
                    error.setTaskId(dto.getTaskId());
                    error.setMsg("第" + (i + 1) + "行，员工工号不能为空");
                    errorList.add(error);
                    fail ++;
                } else {
                    if (accountDatMap.containsKey(user.getEmployeeNum())){
                        user.setComId(dto.getComId());
                        user.setPeriodId(dto.getPeriodId());
                        AccountDat accountDat = accountDatMap.get(user.getEmployeeNum());
                        user.setEmployeeName(accountDat.getName());

                        if (user.getRepeatType() == 0 && campUserMap.containsKey(user.getUnionid())) {
                            TrainingCampUser existUser = campUserMap.get(user.getUnionid());
                            existUser.setNickName(user.getNickName());
                            existUser.setHeadImg(user.getHeadImg());
                            existUser.setRepeatType(user.getRepeatType());
                            existUser.setAddTime(user.getAddTime());
                            existUser.setEmployeeName(user.getEmployeeName());
                            existUser.setEmployeeNum(user.getEmployeeNum());
                            this.baseMapper.updateById(existUser);
                            CompletableFuture.runAsync(() -> asyncUpdateLive(user));
                            success ++;
                        } else {
                            try {
                                user.setId(idService.nextId());
                                this.save(user);
                                insertList.add(user);
                                employeeNums.add(accountDat);
                                if (user.getRepeatType() == 0) {
                                    campUserMap.put(user.getUnionid(), user);
                                    userNum ++;
                                }
                                success ++;
                            } catch (Exception e) {//联合索引报错更新 允许一人对应多个员工
                                if(e.getMessage().contains("unique_emp_user")) {
                                    TrainingCampUser existUser = this.baseMapper.selectOne(new LambdaQueryWrapper<TrainingCampUser>()
                                            .eq(TrainingCampUser::getComId, dto.getComId())
                                            .eq(TrainingCampUser::getEmployeeNum, user.getEmployeeNum())
                                            .eq(TrainingCampUser::getUnionid, user.getUnionid())
                                            .eq(TrainingCampUser::getPeriodId, dto.getPeriodId())
                                            .eq(TrainingCampUser::getDeleteFlg, false)
                                    );
                                    if (null != existUser) {
                                        existUser.setNickName(user.getNickName());
                                        existUser.setHeadImg(user.getHeadImg());
                                        existUser.setRepeatType(user.getRepeatType());
                                        existUser.setAddTime(user.getAddTime());
                                        this.baseMapper.updateById(existUser);
                                        CompletableFuture.runAsync(() -> asyncUpdateLive(user));
                                        if (user.getRepeatType() == 0 && existUser.getRepeatType() != 0) {
                                            campUserMap.put(user.getUnionid(), user);
                                            userNum ++;
                                        } else if (user.getRepeatType() != 0 && existUser.getRepeatType() == 0) {
                                            campUserMap.remove(user.getUnionid());
                                            userNum --;
                                        }
                                        success ++;
                                    } else {
                                        log.error("更新用户失败 comId:{},employeeNum:{},unionid:{}", dto.getComId(), user.getEmployeeNum(), user.getUnionid());
                                        error = new TrainingComTaskErrors();
                                        error.setId(idService.nextId());
                                        error.setComId(dto.getComId());
                                        error.setTaskId(dto.getTaskId());
                                        error.setMsg("第" + (i + 1) + "行，更新用户失败");
                                        errorList.add(error);
                                        fail ++;
                                    }
                                } else {
                                    log.error("导入用户失败", e);
                                    error = new TrainingComTaskErrors();
                                    error.setId(idService.nextId());
                                    error.setComId(dto.getComId());
                                    error.setTaskId(dto.getTaskId());
                                    error.setMsg("第" + (i + 1) + "行，导入数据库异常");
                                    errorList.add(error);
                                    fail ++;
                                }
                            }
                        }
                    } else {
                        error = new TrainingComTaskErrors();
                        error.setId(idService.nextId());
                        error.setComId(dto.getComId());
                        error.setTaskId(dto.getTaskId());
                        error.setMsg("第" + (i + 1) + "行，查询员工异常");
                        errorList.add(error);
                        fail ++;
                    }
                }
            }

            // 异步添加到直播间
            if (!insertList.isEmpty()) {
                CompletableFuture.runAsync(() -> asyncAddLive(insertList, dto.getPeriodId()));
            }

            log.info("导入成功{}条，失败{}条", success, fail);

            if (!errorList.isEmpty()) {
                trainingComTaskErrorsService.saveBatch(errorList);
            }

            if (userNum != 0) {
                TrainingCampPeriod period = trainingCampPeriodMapper.selectById(dto.getPeriodId());
                period.setPeriodUserNum(period.getPeriodUserNum() + userNum);
                trainingCampPeriodMapper.updateById(period);
            }

            task.setSuccessNum(success);
            task.setFailNum(fail);
            task.setState(ComTaskEnum.COMPLETED.getValue());
            trainingComTaskService.updateById(task);
        } catch (Exception e) {
            log.error("导入用户失败", e);
            task.setSuccessNum(success);
            task.setFailNum(fail);
            task.setState(ComTaskEnum.FAILED.getValue());
            trainingComTaskService.updateById(task);
        }

        if (!employeeNums.isEmpty()) {
            CompletableFuture.runAsync(() -> bindChannelByPeriod(dto.getComId(),dto.getPeriodId(),employeeNums));
        }
    }

    private void asyncAddLive(List<TrainingCampUser> insertList, Long periodId) {
        // 查询营期下的直播间
        List<TrainingCampLive> campLives = trainingCampLiveMapper.selectList(new LambdaQueryWrapper<TrainingCampLive>()
                .eq(TrainingCampLive::getPeriodId, periodId)
        );
        if (!campLives.isEmpty()) {
            List<TrainingCampLiveUser> liveUsers = new ArrayList<>();
            for (TrainingCampLive campLive : campLives) {
                for (TrainingCampUser user : insertList) {
                    // 保存直播间用户
                    TrainingCampLiveUser liveUser = new TrainingCampLiveUser();
                    BeanUtils.copyProperties(user, liveUser);
                    liveUser.setId(idService.nextId());
                    liveUser.setLiveId(campLive.getLiveId());
                    liveUsers.add(liveUser);
                    //删除缓存---仅删除跟进人
                    if(user.getRepeatType() == 0){
                        clearcache.delCampLiveUserByCache(campLive.getLiveId(), user.getUnionid());
                    }
                }
            }
            if (!liveUsers.isEmpty()) {
                trainingCampLiveUserService.saveBatch(liveUsers);
            }
        }
    }

    private void asyncUpdateLive(TrainingCampUser user) {
        // 查询营期下的直播间
        List<TrainingCampLive> campLives = trainingCampLiveMapper.selectList(new LambdaQueryWrapper<TrainingCampLive>()
                .eq(TrainingCampLive::getPeriodId, user.getPeriodId())
        );
        if (!campLives.isEmpty()) {
            List<TrainingCampLiveUser> liveUsers = new ArrayList<>();
            for (TrainingCampLive campLive : campLives) {
                TrainingCampLiveUser existUser = this.trainingCampLiveUserMapper.selectOne(new LambdaQueryWrapper<TrainingCampLiveUser>()
                        .eq(TrainingCampLiveUser::getComId, user.getComId())
                        .eq(TrainingCampLiveUser::getPeriodId, user.getPeriodId())
                        .eq(TrainingCampLiveUser::getLiveId, campLive.getLiveId())
                        .eq(TrainingCampLiveUser::getEmployeeNum, user.getEmployeeNum())
                        .eq(TrainingCampLiveUser::getUnionid, user.getUnionid())
                        .eq(TrainingCampLiveUser::getDeleteFlg, false)
                );
                if (existUser != null) {
                    existUser.setNickName(user.getNickName());
                    existUser.setHeadImg(user.getHeadImg());
                    existUser.setRepeatType(user.getRepeatType());
                    existUser.setAddTime(user.getAddTime());
                    liveUsers.add(existUser);
                }
            }
            if (!liveUsers.isEmpty()) {
                trainingCampLiveUserService.updateBatchById(liveUsers);
            }
        }
    }

    /**
     * 绑定渠道
     */
    private void bindChannelByPeriod(Long comId, Long periodId, List<AccountDat> employeeNums) {
        // 查询账号
        List<Long> accountIds = employeeNums.stream().map(AccountDat::getId).toList();

        // 查询渠道
        List<ComChannelDat> channelDats = comChannelDatMapper.selectList(new LambdaQueryWrapper<ComChannelDat>()
                .eq(ComChannelDat::getComId, comId)
                .eq(ComChannelDat::getDeleteFlag, false)
                .in(ComChannelDat::getAccountId, accountIds)
        );

        if (!channelDats.isEmpty()) {
            List<Long> channelIds = channelDats.stream().map(ComChannelDat::getId).toList();

            List<TrainingPeriodChannelInfo> periodChannelInfos = trainingPeriodChannelInfoMapper.selectList(new LambdaQueryWrapper<TrainingPeriodChannelInfo>()
                    .eq(TrainingPeriodChannelInfo::getComId, comId)
                    .eq(TrainingPeriodChannelInfo::getPeriodId, periodId)
                    .eq(TrainingPeriodChannelInfo::getDeleteFlag, false)
                    .in(TrainingPeriodChannelInfo::getChannelId, channelIds)
            );

            // 绑定渠道 未绑定的去绑定
            List<Long> notBindChannelIds = channelIds.stream().filter(id -> periodChannelInfos.stream().noneMatch(info -> info.getChannelId().equals(id))).toList();

            // 查询营期下的直播间
            List<TrainingCampLive> campLives = trainingCampLiveMapper.selectList(new LambdaQueryWrapper<TrainingCampLive>()
                    .eq(TrainingCampLive::getPeriodId, periodId)
            );

            List<TrainingPeriodChannelInfo> insertList = new ArrayList<>();
            List<ChannelLiveInfo> result = new ArrayList<>();

            for (Long channelId : notBindChannelIds) {
                TrainingPeriodChannelInfo periodChannelInfo = new TrainingPeriodChannelInfo();
                periodChannelInfo.setId(idService.nextId());
                periodChannelInfo.setComId(comId);
                periodChannelInfo.setPeriodId(periodId);
                periodChannelInfo.setChannelId(channelId);
                insertList.add(periodChannelInfo);
                for (TrainingCampLive campLive : campLives) {
                    ChannelLiveInfo cga = new ChannelLiveInfo();
                    cga.setId(idService.nextId());
                    cga.setChannelId(channelId);
                    cga.setComId(comId);
                    cga.setLiveId(campLive.getLiveId());
                    cga.setCreateBy(0L);
                    result.add(cga);
                }
            }
            if (!insertList.isEmpty()) {
                trainingPeriodChannelInfoService.saveBatch(insertList);
            }
            if (!result.isEmpty()) {
                channelLiveInfoService.saveBatch(result);
            }
        }

    }

}
