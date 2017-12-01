package cn.godsdo.dubbo.impl;

import cloud.tianai.captcha.common.util.CollectionUtils;
import cn.godsdo.constant.MongoConstant;
import cn.godsdo.dto.LiveWatchRecordDto;
import cn.godsdo.dto.QueryLiveUserWatchDto;
import cn.godsdo.dto.StaticOnlineDto;
import cn.godsdo.dto.msg.LiveMessage;
import cn.godsdo.dto.msg.LiveUserStateChange;
import cn.godsdo.dubbo.LiveRecordDatService;
import cn.godsdo.dubbo.LiveStatisticsDatService;
import cn.godsdo.dubbo.camp.TrainingCampLiveUserService;
import cn.godsdo.dubbo.channel.ChannelGroupAdminService;
import cn.godsdo.dubbo.channel.ComChannelDatService;
import cn.godsdo.dubbo.channel.ComChannelGroupService;
import cn.godsdo.dubbo.crossService.RedisDubboService;
import cn.godsdo.dubbo.user.UserDatService;
import cn.godsdo.entity.LiveRecordDat;
import cn.godsdo.entity.camp.TrainingCampLiveUser;
import cn.godsdo.entity.channel.ChannelGroupAdmin;
import cn.godsdo.entity.channel.ComChannelDat;
import cn.godsdo.entity.user.UserDat;
import cn.godsdo.mapper.channel.ComChannelDatMapper;
import cn.godsdo.util.R;
import cn.godsdo.util.TimeUtil;
import cn.godsdo.vo.*;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.NumberUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

/**
 * <p>
 * 直播统计中间表 服务实现类
 * </p>
 *
 * @author hdk
 * @since 2024-06-04
 */
@Slf4j
@DubboService
public class LiveStatisticsDatServiceImpl implements LiveStatisticsDatService {

    @DubboReference(check = false)
    UserDatService userDatService;
    @DubboReference(check = false)
    RedisDubboService redisService;
    @DubboReference(check = false)
    private ChannelGroupAdminService channelGroupAdminService;
    @DubboReference(check = false)
    private ComChannelDatService comChannelDatService;
    @DubboReference(check = false)
    private ComChannelGroupService comChannelGroupService;
    @DubboReference(check = false)
    private LiveRecordDatService liveRecordDatService;
    @DubboReference(check = false)
    private TrainingCampLiveUserService trainingCampLiveUserService;

    @Resource
    private MongoTemplate mongoTemplate;
    @Resource
    private ComChannelDatMapper comChannelDatMapper;

    @Override
    public void StaticOnLinePersonStatic(String date) {
        // 判断表是否存在
        if (mongoTemplate.collectionExists(MongoConstant.LIVE_USER_STATE_CHANGE_DAY)) {
            List<LiveUserStateChange> liveUserStateChanges = mongoTemplate.find(new Query(), LiveUserStateChange.class, MongoConstant.LIVE_USER_STATE_CHANGE_DAY);
            if (CollectionUtil.isNotEmpty(liveUserStateChanges)) {
                Set<Long> liveIds = new HashSet<>();
                for (LiveUserStateChange item : liveUserStateChanges) {
                    if (liveIds.contains(item.getLiveId())) {
                        continue;
                    }

                    Criteria criteria = Criteria.where("liveId").is(item.getLiveId());
                    Aggregation aggregation = Aggregation.newAggregation(
                            Aggregation.match(criteria),
                            Aggregation.sort(Sort.Direction.DESC, "_id"),
                            Aggregation.group("liveUserId")
                                    .first("liveUserId").as("liveUserId")
                                    .first("liveUsername").as("liveUsername")
                                    .first("userType").as("userType")
                                    .first("isNew").as("isNew")
                                    .first("headImgUrl").as("headImgUrl")
                                    .first("eventType").as("eventType"),
                            Aggregation.project()
                                    .and("liveUsername").previousOperation()//必须添加一个字段的 previousOperation 否则会报错
                                    .and("liveUserId").as("liveUserId")
                                    .and("liveUsername").as("liveUsername")
                                    .and("userType").as("userType")
                                    .and("isNew").as("isNew")
                                    .and("headImgUrl").as("headImgUrl")
                                    .and("eventType").as("eventType"),
                            Aggregation.sort(Sort.Direction.DESC, "liveUserId")
                    );
                    AggregationResults<LiveUserStateChangeVo> results = mongoTemplate.aggregate(aggregation, MongoConstant.LIVE_USER_STATE_CHANGE_DAY, LiveUserStateChangeVo.class);

                    List<LiveUserStateChangeVo> sumOnLineUsers = new ArrayList<>();
                    List<LiveUserStateChangeVo> newOnLineUsers = new ArrayList<>();
                    List<LiveUserStateChangeVo> audienceLineUsers = new ArrayList<>();
                    List<LiveUserStateChangeVo> outLineUsers = new ArrayList<>();//Logout / Disconnect
//userType; // assistant : 助理, anchor : 主播, robot : 机器人, audience : 观众
                    for (LiveUserStateChangeVo liveUserStateChange : results.getMappedResults()) {
                        if ("Login".equals(liveUserStateChange.getEventType())) {
                            sumOnLineUsers.add(liveUserStateChange);
                            if ("audience".equals(liveUserStateChange.getUserType())) {
                                if (null != liveUserStateChange.getIsNew() && liveUserStateChange.getIsNew() == 1) {
                                    newOnLineUsers.add(liveUserStateChange);
                                }
                                audienceLineUsers.add(liveUserStateChange);
                            }
                        } else {
                            outLineUsers.add(liveUserStateChange);
                        }
                    }
                    StaticOnLineUserStateVO staticOnLineUserStateVO = new StaticOnLineUserStateVO();
                    staticOnLineUserStateVO.setSumOnLineUser(sumOnLineUsers.size());
                    staticOnLineUserStateVO.setNewOnLineUser(newOnLineUsers.size());
                    staticOnLineUserStateVO.setAudienceLineUser(audienceLineUsers.size());
                    staticOnLineUserStateVO.setOutLineUser(outLineUsers.size());

                    staticOnLineUserStateVO.setStatisticsDate(date);
                    staticOnLineUserStateVO.setLiveId(item.getLiveId());
                    staticOnLineUserStateVO.setComId(item.getComId());

                    staticOnLineUserStateVO.setCreateAt(new Date());
                    mongoTemplate.insert(staticOnLineUserStateVO, MongoConstant.STATIC_ONLINE_USER_STATUE);
                    liveIds.add(item.getLiveId());

                    redisService.setex("SumOnLine_" + item.getLiveId(), sumOnLineUsers.size() + "", 60 * 2);
                }
            }
        } else {
            log.info("mongo中没有集合：{}", MongoConstant.LIVE_USER_STATE_CHANGE_DAY);
        }
    }

    @Override
    public void StaticChannelOnLinePersonStatic(String date) {
        // 判断表是否存在
        if (mongoTemplate.collectionExists(MongoConstant.LIVE_USER_STATE_CHANGE_DAY)) {
            Criteria criteria1 = Criteria.where("channelId").exists(true).ne("").ne(null).ne(0);
            List<LiveUserStateChange> liveUserStateChanges = mongoTemplate.find(new Query(criteria1).with(Sort.by(Sort.Direction.DESC, "eventTime")), LiveUserStateChange.class, MongoConstant.LIVE_USER_STATE_CHANGE_DAY);
            if (CollectionUtil.isNotEmpty(liveUserStateChanges)) {
                Set<Long> channelIds = new HashSet<>();
                for (LiveUserStateChange item : liveUserStateChanges) {
                    if (channelIds.contains(item.getChannelId())) {
                        continue;
                    }

                    // 查询所有渠道为空id
                    Criteria criteria = Criteria.where("liveId").is(item.getLiveId()).and("channelId").is(item.getChannelId());
                    Aggregation aggregation = Aggregation.newAggregation(
                            Aggregation.match(criteria),
                            Aggregation.sort(Sort.Direction.DESC, "_id"),
                            Aggregation.group("liveUserId")
                                    .first("liveUserId").as("liveUserId")
                                    .first("liveUsername").as("liveUsername")
                                    .first("userType").as("userType")
                                    .first("isNew").as("isNew")
                                    .first("channelId").as("channelId")
                                    .first("headImgUrl").as("headImgUrl")
                                    .first("eventType").as("eventType"),
                            Aggregation.project()
                                    //必须添加一个字段的 previousOperation 否则会报错
                                    .and("liveUsername").previousOperation()
                                    .and("liveUserId").as("liveUserId")
                                    .and("liveUsername").as("liveUsername")
                                    .and("userType").as("userType")
                                    .and("isNew").as("isNew")
                                    .and("channelId").as("channelId")
                                    .and("headImgUrl").as("headImgUrl")
                                    .and("eventType").as("eventType"),
                            Aggregation.sort(Sort.Direction.DESC, "liveUserId")
                    );
                    AggregationResults<LiveUserStateChangeVo> results = mongoTemplate.aggregate(aggregation, MongoConstant.LIVE_USER_STATE_CHANGE_DAY, LiveUserStateChangeVo.class);
                    // 总观众人数
                    List<LiveUserStateChangeVo> sumOnLineUsers = new ArrayList<>();
                    List<LiveUserStateChangeVo> newOnLineUsers = new ArrayList<>();
                    List<LiveUserStateChangeVo> audienceLineUsers = new ArrayList<>();
                    List<LiveUserStateChangeVo> outLineUsers = new ArrayList<>();//Logout / Disconnect
//userType; // assistant : 助理, anchor : 主播, robot : 机器人, audience : 观众
                    for (LiveUserStateChangeVo liveUserStateChange : results.getMappedResults()) {
                        if ("Login".equals(liveUserStateChange.getEventType())) {
                            sumOnLineUsers.add(liveUserStateChange);
                            if ("audience".equals(liveUserStateChange.getUserType())) {
                                if (null != liveUserStateChange.getIsNew() && liveUserStateChange.getIsNew() == 1) {
                                    newOnLineUsers.add(liveUserStateChange);
                                }
                                audienceLineUsers.add(liveUserStateChange);
                            }
                        } else {
                            outLineUsers.add(liveUserStateChange);
                        }
                    }
                    StaticChannelOnLineUserStateVO vo = new StaticChannelOnLineUserStateVO();

                    Long channelId = item.getChannelId();
                    StaticChannelOnLineUserStateVO groupParantId = comChannelGroupService.getGroupParantId(channelId);
                    if (ObjectUtils.isNotEmpty(groupParantId)) {
                        vo = groupParantId;
                    }
                    vo.setSumOnLineUser(sumOnLineUsers.size());

                    vo.setNewOnLineUser(newOnLineUsers.size());
                    vo.setAudienceLineUser(audienceLineUsers.size());
                    vo.setOutLineUser(outLineUsers.size());

                    vo.setStatisticsDate(date);
                    vo.setLiveId(item.getLiveId());
                    vo.setComId(item.getComId());
                    vo.setChannelId(channelId);
                    vo.setCreateAt(new Date());
                    mongoTemplate.insert(vo, MongoConstant.STATIC_CHANNEL_ONLINE_USER_STATUE);
                    channelIds.add(item.getChannelId());
                }
            }
        } else {
            log.info("mongo中没有channel集合：{}", MongoConstant.LIVE_USER_STATE_CHANGE_DAY);
        }
    }

    @Override
    public R onLineData(StaticOnlineDto dto) {
        Criteria criteria = Criteria.where("liveId").is(dto.getLiveId());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        if (null != dto.getDateType() && dto.getDateType() > 0) {
            calendar.add(Calendar.HOUR_OF_DAY, dto.getDateType() * -1);
            Date newDate = calendar.getTime();
            criteria.and("createAt").gte(newDate);
        } else {
            String dateSs = DateFormatUtils.format(new Date(), "yyyy-MM-dd") + " 00:00:00";
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try {
                Date start = formatter.parse(dateSs);
                criteria.and("createAt").gte(start);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        List<StaticOnLineUserStateVO> staticOnLineUserStateVO = new ArrayList<>();

        // 训练营协助人
        if (StringUtils.isNotBlank(dto.getChannelIds())) {
            String[] channelIds = dto.getChannelIds().split(",");
            List<Long> channelIdList = new ArrayList<>();
            for (String channelId : channelIds) {
                if (NumberUtil.isLong(channelId) && !"1".equals(channelId)) {
                    channelIdList.add(Long.parseLong(channelId));
                }
            }
            if (CollectionUtils.isEmpty(channelIdList)) {
                return R.ok(staticOnLineUserStateVO, 0);
            } else {
                criteria.and("channelId").in(channelIdList);
                if (NumberUtil.isLong(dto.getChannelId()) && !"0".equals(dto.getChannelId())) {
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
                            criteria.and("liveUserId").in(ids);
                        }
                    }
                }

                Aggregation aggregation = Aggregation.newAggregation(
                        Aggregation.match(criteria),
                        Aggregation.group("statisticsDate").sum("sumOnLineUser").as("sumOnLineUser")
                                .sum("newOnLineUser").as("newOnLineUser")
                                .sum("audienceLineUser").as("audienceLineUser")
                                .sum("outLineUser").as("outLineUser")
                                .first("statisticsDate").as("statisticsDate"),
                        Aggregation.project()
                                .and("statisticsDate").previousOperation()//必须添加一个字段的 previousOperation 否则会报错
                                .and("statisticsDate").as("statisticsDate")
                                .and("sumOnLineUser").as("sumOnLineUser")
                                .and("newOnLineUser").as("newOnLineUser")
                                .and("audienceLineUser").as("audienceLineUser")
                                .and("outLineUser").as("outLineUser")
                        ,
                        Aggregation.sort(Sort.Direction.ASC, "statisticsDate")
                );
                AggregationResults<StaticOnLineUserStateVO> results = mongoTemplate.aggregate(aggregation, MongoConstant.STATIC_CHANNEL_ONLINE_USER_STATUE, StaticOnLineUserStateVO.class);
                staticOnLineUserStateVO = results.getMappedResults();
            }
        } else if (NumberUtil.isLong(dto.getChannelId()) && !"0".equals(dto.getChannelId())) {
            //是否是管理员
            List<ChannelGroupAdmin> list = channelGroupAdminService.getListByChannelId(dto.getChannelId());
            if (CollectionUtils.isEmpty(list)) {
                //不是管理员
                criteria.and("channelId").is(Long.parseLong(dto.getChannelId()));
                Query query = new Query(criteria);
                staticOnLineUserStateVO = mongoTemplate.find(query, StaticOnLineUserStateVO.class, MongoConstant.STATIC_CHANNEL_ONLINE_USER_STATUE);
            } else {
                //是管理员
                List<Long> principalList = new ArrayList<>();
                for (ChannelGroupAdmin admin : list) {
                    principalList.add(admin.getGroupId());
                }
                List<ComChannelDat> channelList = comChannelDatService.getChannelListByGroupIds(principalList);
                if (!CollectionUtils.isEmpty(channelList)) {
                    List<Long> channelIdList = channelList.stream().map(ComChannelDat::getId).toList();
                    criteria.and("channelId").in(channelIdList);
                }

                Aggregation aggregation = Aggregation.newAggregation(
                        Aggregation.match(criteria),
                        Aggregation.group("statisticsDate").sum("sumOnLineUser").as("sumOnLineUser")
                                .sum("newOnLineUser").as("newOnLineUser")
                                .sum("audienceLineUser").as("audienceLineUser")
                                .sum("outLineUser").as("outLineUser")
                                .first("statisticsDate").as("statisticsDate"),
                        Aggregation.project()
                                .and("statisticsDate").previousOperation()//必须添加一个字段的 previousOperation 否则会报错
                                .and("statisticsDate").as("statisticsDate")
                                .and("sumOnLineUser").as("sumOnLineUser")
                                .and("newOnLineUser").as("newOnLineUser")
                                .and("audienceLineUser").as("audienceLineUser")
                                .and("outLineUser").as("outLineUser")
                        ,
                        Aggregation.sort(Sort.Direction.ASC, "statisticsDate")
                );
                AggregationResults<StaticOnLineUserStateVO> results = mongoTemplate.aggregate(aggregation, MongoConstant.STATIC_CHANNEL_ONLINE_USER_STATUE, StaticOnLineUserStateVO.class);
                staticOnLineUserStateVO = results.getMappedResults();
            }
        } else {
            Query query = new Query(criteria);
            staticOnLineUserStateVO = mongoTemplate.find(query, StaticOnLineUserStateVO.class, MongoConstant.STATIC_ONLINE_USER_STATUE);
        }

         return R.ok(staticOnLineUserStateVO, staticOnLineUserStateVO.size());
    }

    @Override
    public R onLineDataHistory(StaticOnlineDto dto) {
        Criteria criteria = Criteria.where("liveId").is(dto.getLiveId());
        if (StringUtils.isNotBlank(dto.getStartTime()) && StringUtils.isNotBlank(dto.getEndTime())) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateStart = dto.getStartTime() + " 00:00:00";
            String dateEnd = dto.getEndTime() + " 23:59:59";
            Date start0;
            Date start24;
            try {
                start0 = formatter.parse(dateStart);
                start24 = formatter.parse(dateEnd);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            criteria.andOperator(
                    Criteria.where("enterTime").gte(start0),
                    Criteria.where("enterTime").lte(start24)
            );
        } else {
            String dateSs = DateFormatUtils.format(new Date(), "yyyy-MM");
            criteria.and("statisticsDate").regex("^.*" + dateSs + ".*$");
        }

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group("statisticsDate").count().as("sum")
                        .first("statisticsDate").as("statisticsDate"),
                Aggregation.project()
                        .and("statisticsDate").previousOperation()//必须添加一个字段的 previousOperation 否则会报错
                        .and("statisticsDate").as("statisticsDate")
                        .and("sum").as("sum"),
                Aggregation.sort(Sort.Direction.ASC, "statisticsDate")
        );
        AggregationResults<ComOnlineSumVo> results = mongoTemplate.aggregate(aggregation, MongoConstant.LIVE_USER_WATCH_TIME_STATISTICS, ComOnlineSumVo.class);
        List<ComOnlineSumVo> resultList = results.getMappedResults();
        return R.ok(resultList);
    }

    @Override
    public R queryLiveUserWatchAll(QueryLiveUserWatchDto watchQuery) {
        try {
//            Assert.notNull(watchQuery.getQueryDate(), "查询时间不能为空");
            Assert.notNull(watchQuery.getUserId(), "用户id不能为空");
            Assert.notNull(watchQuery.getPageNum(), "查询页码不能为空");
            Assert.notNull(watchQuery.getPageSize(), "查询页大小不能为空");
        } catch (IllegalArgumentException e) {
            return R.failed(e.getMessage());
        }
        String date = DateFormatUtils.format(new Date(), "yyyy-MM-dd");
        Criteria criteria = Criteria.where("userId").is(watchQuery.getUserId());

        if (StringUtils.isNotBlank(watchQuery.getQueryDate())) {
            criteria.and("statisticsDate").is(watchQuery.getQueryDate());
        }

        long total;
        List<QueryLiveUserWatchVO> queryLiveUserWatchVOS = new ArrayList<>();
        // 判断时间是否统计当天直播数据
        if (StringUtils.equals(watchQuery.getQueryDate(), date)) {
            // 查询当天数据
            Query queryUser = new Query(criteria);
            total = mongoTemplate.count(queryUser, LiveWatchRecordDto.class, MongoConstant.LIVE_WATCH_RECORD_USER);
            if (total > 0) {
                // 排序
                Sort sort = Sort.by(Sort.Direction.DESC, "createAt");
                queryUser.with(sort);
                // 分页 (当前页-1)*每页大小，每页大小
                queryUser.skip((long) (watchQuery.getPageNum() - 1) * watchQuery.getPageSize()).limit(watchQuery.getPageSize());

                List<LiveWatchRecordDto> userList = mongoTemplate.find(queryUser, LiveWatchRecordDto.class, MongoConstant.LIVE_WATCH_RECORD_USER);

                List<LiveRecordDat> liveRecordDatList = liveRecordDatService.getListBayDat(null, watchQuery.getLiveId(), watchQuery.getQueryDate());
                for (LiveWatchRecordDto watchRecord : userList) {
                    QueryLiveUserWatchVO queryLiveUserWatchVO = new QueryLiveUserWatchVO();
                    queryLiveUserWatchVO.setLiveId(watchQuery.getLiveId());
                    queryLiveUserWatchVO.setCity(watchRecord.getCity());
                    queryLiveUserWatchVO.setIp(watchRecord.getIp());
                    queryLiveUserWatchVO.setOperators(watchRecord.getOperators());
                    queryLiveUserWatchVO.setBrowser(watchRecord.getBrowser());
                    queryLiveUserWatchVO.setSystemOs(watchRecord.getSystemOs());
                    queryLiveUserWatchVO.setEnterTime(watchRecord.getEnterTime());
                    queryLiveUserWatchVO.setOutTime(watchRecord.getOutTime());
                    queryLiveUserWatchVO.setStatisticsDate(watchQuery.getQueryDate());
                    queryLiveUserWatchVO.setUserId(watchRecord.getUserId());
                    queryLiveUserWatchVO.setNickName(watchRecord.getNickName());
                    queryLiveUserWatchVO.setHeadImgUrl(watchRecord.getHeadImgUrl());
                    queryLiveUserWatchVO.setChannelId(watchRecord.getChannelId());
                    queryLiveUserWatchVO.setChannelName(watchRecord.getChannelName());
                    String key = "IM_" + watchRecord.getUserId();
                    String userInfo = (String) redisService.get(key);
                    if (StringUtils.isNotBlank(userInfo)) {
                        String[] split = StringUtils.split(userInfo, "###");
                        String isNew = split[5];
                        if (StringUtils.isNotBlank(isNew)) {
                            queryLiveUserWatchVO.setIsNew(Integer.parseInt(isNew));
                        }
                    }

                    long enterOutNum = 0L;
                    long messageCount = 0L;
                    // 获取观看时长
                    List<LiveWatchRecordDto> listAll = new ArrayList<>();

                    for (LiveRecordDat liveRecordDat : liveRecordDatList) {
                        // 获取用户观看进入次数
                        Query query = new Query(Criteria.where("liveRecordId").is(liveRecordDat.getId())
                                .and("userId").is(watchRecord.getUserId()));
                        enterOutNum += mongoTemplate.count(query, LiveWatchRecordDto.class, MongoConstant.LIVE_WATCH_RECORD_PERSISTENCE);

                        // 获取观看时长
                        List<LiveWatchRecordDto> list = mongoTemplate.find(query, LiveWatchRecordDto.class, MongoConstant.LIVE_WATCH_RECORD_PERSISTENCE);
                        listAll.addAll(list);

                        long endTime = null == watchRecord.getOutTime() ? new Date().getTime() / 1000 : watchRecord.getOutTime().getTime() / 1000;
                        // 获取用户观看发言次数
                        Query messageQuery = new Query(Criteria.where("liveId").is(watchQuery.getLiveId())
                                .and("liveUserId").is(watchRecord.getUserId())
                                .andOperator(
                                        Criteria.where("msgTime").gte(watchRecord.getEnterTime().getTime() / 1000),
                                        Criteria.where("msgTime").lte(endTime)
                                )
                        );
                        messageCount += mongoTemplate.count(messageQuery, LiveMessage.class, MongoConstant.LIVE_MESSAGE);
                    }
                    String watchTime = statisticalUserWatchTime(listAll);
                    queryLiveUserWatchVO.setWatchDurations(watchTime);
                    queryLiveUserWatchVO.setSpeakCount((int) messageCount);
                    queryLiveUserWatchVO.setEnterNum((int) enterOutNum);
                    queryLiveUserWatchVOS.add(queryLiveUserWatchVO);
                }

            }
        } else {
            // 查询历史数据
            Query query = new Query(criteria);
            total = mongoTemplate.count(query, QueryLiveUserWatchVO.class, MongoConstant.LIVE_USER_WATCH_TIME_STATISTICS);
            if (total > 0) {
                // 排序
                Sort sort = Sort.by(Sort.Direction.DESC, "enterTime");
                query.with(sort);
                // 分页 (当前页-1)*每页大小，每页大小
                query.skip((long) (watchQuery.getPageNum() - 1) * watchQuery.getPageSize()).limit(watchQuery.getPageSize());

                queryLiveUserWatchVOS = mongoTemplate.find(query, QueryLiveUserWatchVO.class, MongoConstant.LIVE_USER_WATCH_TIME_STATISTICS);
            }
        }

        return R.ok(queryLiveUserWatchVOS, total);

    }

    @Override
    public List<QueryLiveUserWatchVO> getWatchDataTodayList(QueryLiveUserWatchDto watchQuery) {
        Criteria criteria = Criteria.where("comId").is(watchQuery.getComId()).and("statisticsDate").is(watchQuery.getQueryDate()).and("roomId").is(watchQuery.getLiveId());
        return this.getWatchDataToday(criteria,watchQuery);
    }


//    @Override
//    public void SynchronizationUserData() {
//        List<LiveUserStateChange> liveUserStateChanges = mongoTemplate.find(new Query(), LiveUserStateChange.class, MongoConstant.LIVE_USER_STATE_CHANGE_DAY);
//        if(CollectionUtil.isNotEmpty(liveUserStateChanges)){
//            for(LiveUserStateChange liveUserStateChange : liveUserStateChanges){
//                mongoTemplate.insert(liveUserStateChange, MongoConstant.LIVE_USER_STATE_USER_DATA);
//            }
//            mongoTemplate.remove(new Query(), MongoConstant.LIVE_USER_STATE_CHANGE_DAY);
//        }
//    }

    /**
     * 同步用户数据
     */
    private void synchronizationUserData(String statisticalDate) {
        try {
            Criteria criteria = Criteria.where("statisticsDate").is(statisticalDate);
            Query queryUser = new Query(criteria);
            List<LiveUserStateChange> liveUserStateChanges = mongoTemplate.find(queryUser, LiveUserStateChange.class, MongoConstant.LIVE_USER_STATE_CHANGE_DAY);
            if (CollectionUtil.isNotEmpty(liveUserStateChanges)) {
                mongoTemplate.insert(liveUserStateChanges, MongoConstant.LIVE_USER_STATE_USER_DATA);
                mongoTemplate.remove(queryUser, MongoConstant.LIVE_USER_STATE_CHANGE_DAY);
            }
        } catch (Exception e) {
            log.error("同步用户数据失败", e);
        }
    }

    @Override
    public void statisticalUserWatchData() {
        //获取昨天时间
        LocalDate localDate = LocalDate.now().minusDays(1);
        String statisticalDate = String.valueOf(localDate);

        //同步用户数据
        synchronizationUserData(statisticalDate);

        //同步观看数据
        synchronizationWatchData(statisticalDate);

    }

    private void synchronizationWatchData(String statisticalDate) {
        Criteria criteria = Criteria.where("statisticsDate").is(statisticalDate);
        Query queryUser = new Query(criteria);
        List<LiveWatchRecordDto> userList = mongoTemplate.find(queryUser, LiveWatchRecordDto.class, MongoConstant.LIVE_WATCH_RECORD_USER);

        List<LiveRecordDat> liveRecordDatList = liveRecordDatService.getListBayDat(null ,null ,statisticalDate);
        Map<String, String> map = new HashMap<>();
        for (LiveRecordDat liveRecordDat : liveRecordDatList) {
            if (map.containsKey(liveRecordDat.getLiveId() + "")) {
                map.put(liveRecordDat.getLiveId() + "", liveRecordDat.getId() + "," + map.get(liveRecordDat.getLiveId() + ""));
            } else {
                map.put(liveRecordDat.getLiveId() + "", liveRecordDat.getId() + "");
            }
        }

        for (LiveWatchRecordDto watchRecord : userList) {
            long enterOutNum = 0L;
            long messageCount = 0L;
            // 获取观看时长
            List<LiveWatchRecordDto> listAll = new ArrayList<>();
            if (null != map.get(watchRecord.getLiveId() + "") && map.get(watchRecord.getLiveId() + "").contains(",")) {
                String[] split = map.get(watchRecord.getLiveId() + "").split(",");
                for (String id : split) {
                    // 获取用户观看进入次数
                    Query query = new Query(Criteria.where("liveRecordId").is(id)
                            .and("userId").is(watchRecord.getUserId()));
                    enterOutNum += mongoTemplate.count(query, LiveWatchRecordDto.class, MongoConstant.LIVE_WATCH_RECORD_PERSISTENCE);

                    // 获取观看时长
                    List<LiveWatchRecordDto> list = mongoTemplate.find(query, LiveWatchRecordDto.class, MongoConstant.LIVE_WATCH_RECORD_PERSISTENCE);
                    listAll.addAll(list);

                    long endTime = null == watchRecord.getOutTime() ? new Date().getTime() / 1000 : watchRecord.getOutTime().getTime() / 1000;
                    // 获取用户观看发言次数
                    Query messageQuery = new Query(Criteria.where("liveId").is(watchRecord.getLiveId())
                            .and("liveUserId").is(watchRecord.getUserId())
                            .andOperator(
                                    Criteria.where("msgTime").gte(watchRecord.getEnterTime().getTime() / 1000),
                                    Criteria.where("msgTime").lte(endTime)
                            )
                    );
                    messageCount += mongoTemplate.count(messageQuery, LiveMessage.class, MongoConstant.LIVE_MESSAGE);
                }
            } else {
                // 获取用户观看进入次数
                Query query = new Query(Criteria.where("liveRecordId").is(watchRecord.getLiveRecordId())
                        .and("userId").is(watchRecord.getUserId()));
                enterOutNum = mongoTemplate.count(query, LiveWatchRecordDto.class, MongoConstant.LIVE_WATCH_RECORD_PERSISTENCE);

                // 获取观看时长
                List<LiveWatchRecordDto> list = mongoTemplate.find(query, LiveWatchRecordDto.class, MongoConstant.LIVE_WATCH_RECORD_PERSISTENCE);
                listAll.addAll(list);

                long endTime = null == watchRecord.getOutTime() ? new Date().getTime() / 1000 : watchRecord.getOutTime().getTime() / 1000;
                // 获取用户观看发言次数
                Query messageQuery = new Query(Criteria.where("liveId").is(watchRecord.getLiveId())
                        .and("liveUserId").is(watchRecord.getUserId())
                        .andOperator(
                                Criteria.where("msgTime").gte(watchRecord.getEnterTime().getTime() / 1000),
                                Criteria.where("msgTime").lte(endTime)
                        )
                );
                messageCount = mongoTemplate.count(messageQuery, LiveMessage.class, MongoConstant.LIVE_MESSAGE);
            }
//            statisticalUserWatchTime(listAll);
            long seconds = statisticalUserWatchTimeSeconds(listAll);
            String watchTime =  TimeUtil.convertSecondsToHMS(seconds);
            //根据用户id查询用户信息
            QueryLiveUserWatchVO queryLiveUserWatchVO = new QueryLiveUserWatchVO();
            queryLiveUserWatchVO.setComId(watchRecord.getComId());
            queryLiveUserWatchVO.setLiveId(watchRecord.getLiveId());

            queryLiveUserWatchVO.setWatchDurations(watchTime);
            queryLiveUserWatchVO.setWatchDurationsSeconds(seconds);
            queryLiveUserWatchVO.setSpeakCount((int) messageCount);
            queryLiveUserWatchVO.setEnterNum((int) enterOutNum);
            queryLiveUserWatchVO.setCity(watchRecord.getCity());
            queryLiveUserWatchVO.setIp(watchRecord.getIp());
            queryLiveUserWatchVO.setOperators(watchRecord.getOperators());
            queryLiveUserWatchVO.setBrowser(watchRecord.getBrowser());
            queryLiveUserWatchVO.setSystemOs(watchRecord.getSystemOs());
            queryLiveUserWatchVO.setEnterTime(watchRecord.getEnterTime());
            queryLiveUserWatchVO.setOutTime(watchRecord.getOutTime());
            queryLiveUserWatchVO.setStatisticsDate(statisticalDate);
            queryLiveUserWatchVO.setUserId(watchRecord.getUserId());

            queryLiveUserWatchVO.setNickName(watchRecord.getNickName());
            queryLiveUserWatchVO.setHeadImgUrl(watchRecord.getHeadImgUrl());
            queryLiveUserWatchVO.setChannelId(watchRecord.getChannelId());
            queryLiveUserWatchVO.setChannelName(watchRecord.getChannelName());

            String key = "IM_" + watchRecord.getUserId();
            String userInfo = (String) redisService.get(key);
            if (StringUtils.isNotBlank(userInfo)) {
                String[] split = StringUtils.split(userInfo, "###");
                String isNew = split[5];
                if (StringUtils.isNotBlank(isNew)) {
                    queryLiveUserWatchVO.setIsNew(Integer.parseInt(isNew));
                }
            }

            //放入monogo中
            mongoTemplate.insert(queryLiveUserWatchVO, MongoConstant.LIVE_USER_WATCH_TIME_STATISTICS);
        }
    }

    @Override
    public R queryLiveUserWatchQuery(QueryLiveUserWatchDto watchQuery) {
        try {
            Assert.notNull(watchQuery.getQueryDate(), "查询时间不能为空");
            Assert.notNull(watchQuery.getLiveId(), "查询直播间不能为空");
            Assert.notNull(watchQuery.getPageNum(), "查询页码不能为空");
            Assert.notNull(watchQuery.getPageSize(), "查询页大小不能为空");
        } catch (IllegalArgumentException e) {
            return R.failed(e.getMessage());
        }
        String date = DateFormatUtils.format(new Date(), "yyyy-MM-dd");
        Criteria criteria = Criteria.where("liveId").is(watchQuery.getLiveId());
        if (StringUtils.isNotBlank(watchQuery.getNickName())) {
            criteria.and("nickName").regex("^.*" + watchQuery.getNickName() + ".*$");
        }
        if (StringUtils.isNotBlank(watchQuery.getQueryDate())) {
            criteria.and("statisticsDate").is(watchQuery.getQueryDate());
        }

        List<QueryLiveUserWatchVO> queryLiveUserWatchVOS = new ArrayList<>();

        if (StringUtils.isNotEmpty(watchQuery.getChannelIds())) {
            // 训练营协助人
            String[] channelIds = watchQuery.getChannelIds().split(",");
            List<Long> channelIdList = new ArrayList<>();
            for (String channelId : channelIds) {
                if (NumberUtil.isLong(channelId) && !"1".equals(channelId)) {
                    channelIdList.add(Long.parseLong(channelId));
                }
            }
            if (!CollectionUtils.isEmpty(channelIdList)) {
                criteria.and("channelId").in(channelIdList);
            } else {
                return R.ok(queryLiveUserWatchVOS, 0);
            }
            if (StringUtils.isNotBlank(watchQuery.getChannelId()) && !"0".equals(watchQuery.getChannelId())) {
                String employeeNum = comChannelDatMapper.getEmployeeByChannelId(watchQuery.getComId(), watchQuery.getChannelId());
                List<TrainingCampLiveUser> list = trainingCampLiveUserService.list(new LambdaQueryWrapper<TrainingCampLiveUser>()
                        .select(TrainingCampLiveUser::getUnionid)
                        .eq(TrainingCampLiveUser::getComId, watchQuery.getComId())
                        .eq(TrainingCampLiveUser::getLiveId, watchQuery.getLiveId())
                        .eq(TrainingCampLiveUser::getRepeatType, 1)
                        .eq(TrainingCampLiveUser::getEmployeeNum, employeeNum)
                        .eq(TrainingCampLiveUser::getDeleteFlg, false));
                if (!CollectionUtils.isEmpty(list)) {
                    List<String> unionIds = list.stream().map(TrainingCampLiveUser::getUnionid).toList();
                    List<UserDat> userList = userDatService.list(new LambdaQueryWrapper<UserDat>()
                            .select(UserDat::getId)
                            .eq(UserDat::getComId, watchQuery.getComId())
                            .in(UserDat::getUnionid, unionIds)
                            .eq(UserDat::getDeleteFlag, false)
                    );
                    if (!CollectionUtils.isEmpty(userList)) {
                        List<Long> ids = userList.stream().map(UserDat::getId).toList();
                        criteria.and("userId").in(ids);
                    }
                }
            }
        } else if (StringUtils.isNotBlank(watchQuery.getChannelId()) && !"0".equals(watchQuery.getChannelId())) {
            //是否是管理员
            List<ChannelGroupAdmin> list = channelGroupAdminService.getListByChannelId(watchQuery.getChannelId());
            if (CollectionUtils.isEmpty(list)) {
                //不是管理员
                criteria.and("channelId").is(Long.parseLong(watchQuery.getChannelId()));
            } else {
                //是管理员
                List<Long> principalList = new ArrayList<>();
                for (ChannelGroupAdmin admin : list) {
                    principalList.add(admin.getGroupId());
                }
                List<ComChannelDat> channelList = comChannelDatService.getChannelListByGroupIds(principalList);
                if (!CollectionUtils.isEmpty(channelList)) {
                    List<Long> channelIdList = channelList.stream().map(ComChannelDat::getId).toList();
                    criteria.and("channelId").in(channelIdList);
                }
            }
        }


        // 判断时间是否统计当天直播数据
        if (StringUtils.equals(watchQuery.getQueryDate(), date)) {
            queryLiveUserWatchVOS = getWatchDataToday(criteria, watchQuery);
        } else {
            // 查询历史数据
            Query query = new Query(criteria);
            long total = mongoTemplate.count(query, QueryLiveUserWatchVO.class, MongoConstant.LIVE_USER_WATCH_TIME_STATISTICS);
            if (total > 0) {
                // 排序
                Sort sort = Sort.by(Sort.Direction.DESC, "enterTime");
                query.with(sort);
                // 分页 (当前页-1)*每页大小，每页大小
                query.skip((long) (watchQuery.getPageNum() - 1) * watchQuery.getPageSize()).limit(watchQuery.getPageSize());

                queryLiveUserWatchVOS = mongoTemplate.find(query, QueryLiveUserWatchVO.class, MongoConstant.LIVE_USER_WATCH_TIME_STATISTICS);
            }
        }

        return R.ok(queryLiveUserWatchVOS, queryLiveUserWatchVOS.size());
    }

    /**
     * 统计当天直播数据
     * @param criteria 条件
     * @param watchQuery 查询条件
     * @return
     */
    private List<QueryLiveUserWatchVO> getWatchDataToday(Criteria criteria, QueryLiveUserWatchDto watchQuery) {
        List<QueryLiveUserWatchVO> queryLiveUserWatchVOS = new ArrayList<>();
        // 查询当天数据
        Query queryUser = new Query(criteria);
        long total = mongoTemplate.count(queryUser, LiveWatchRecordDto.class, MongoConstant.LIVE_WATCH_RECORD_USER);
        if (total > 0) {
            // 排序
            Sort sort = Sort.by(Sort.Direction.DESC, "createAt");
            queryUser.with(sort);
            // 分页 (当前页-1)*每页大小，每页大小
            queryUser.skip((long) (watchQuery.getPageNum() - 1) * watchQuery.getPageSize()).limit(watchQuery.getPageSize());

            List<LiveWatchRecordDto> userList = mongoTemplate.find(queryUser, LiveWatchRecordDto.class, MongoConstant.LIVE_WATCH_RECORD_USER);

            List<LiveRecordDat> liveRecordDatList = liveRecordDatService.getListBayDat(null, watchQuery.getLiveId(), watchQuery.getQueryDate());
            for (LiveWatchRecordDto watchRecord : userList) {
                QueryLiveUserWatchVO queryLiveUserWatchVO = new QueryLiveUserWatchVO();
                queryLiveUserWatchVO.setLiveId(watchQuery.getLiveId());
                queryLiveUserWatchVO.setCity(watchRecord.getCity());
                queryLiveUserWatchVO.setIp(watchRecord.getIp());
                queryLiveUserWatchVO.setOperators(watchRecord.getOperators());
                queryLiveUserWatchVO.setBrowser(watchRecord.getBrowser());
                queryLiveUserWatchVO.setSystemOs(watchRecord.getSystemOs());
                queryLiveUserWatchVO.setEnterTime(watchRecord.getEnterTime());
                queryLiveUserWatchVO.setOutTime(watchRecord.getOutTime());
                queryLiveUserWatchVO.setStatisticsDate(watchQuery.getQueryDate());
                queryLiveUserWatchVO.setUserId(watchRecord.getUserId());
                queryLiveUserWatchVO.setNickName(watchRecord.getNickName());
                queryLiveUserWatchVO.setHeadImgUrl(watchRecord.getHeadImgUrl());
                queryLiveUserWatchVO.setChannelId(watchRecord.getChannelId());
                queryLiveUserWatchVO.setChannelName(watchRecord.getChannelName());
                String key = "IM_" + watchRecord.getUserId();
                String userInfo = (String) redisService.get(key);
                if (StringUtils.isNotBlank(userInfo)) {
                    String[] split = StringUtils.split(userInfo, "###");
                    String isNew = split[5];
                    if (StringUtils.isNotBlank(isNew)) {
                        queryLiveUserWatchVO.setIsNew(Integer.parseInt(isNew));
                    }
                }

                long enterOutNum = 0L;

                // 获取观看时长
                List<LiveWatchRecordDto> listAll = new ArrayList<>();

                for (LiveRecordDat liveRecordDat : liveRecordDatList) {
                    // 获取用户观看进入次数
                    Query query = new Query(Criteria.where("liveRecordId").is(liveRecordDat.getId())
                            .and("userId").is(watchRecord.getUserId()));
                    enterOutNum += mongoTemplate.count(query, LiveWatchRecordDto.class, MongoConstant.LIVE_WATCH_RECORD_PERSISTENCE);

                    // 获取观看时长
                    List<LiveWatchRecordDto> list = mongoTemplate.find(query, LiveWatchRecordDto.class, MongoConstant.LIVE_WATCH_RECORD_PERSISTENCE);
                    listAll.addAll(list);
                }

                long endTime = null == watchRecord.getOutTime() ? new Date().getTime() / 1000 : watchRecord.getOutTime().getTime() / 1000;
                // 获取用户观看发言次数
                Query messageQuery = new Query(Criteria.where("liveId").is(watchQuery.getLiveId())
                        .and("liveUserId").is(watchRecord.getUserId())
                        .andOperator(
                                Criteria.where("msgTime").gte(watchRecord.getEnterTime().getTime() / 1000),
                                Criteria.where("msgTime").lte(endTime)
                        )
                );
                long messageCount = mongoTemplate.count(messageQuery, LiveMessage.class, MongoConstant.LIVE_MESSAGE);

//                String watchTime = statisticalUserWatchTime(listAll);
                long totalWatchTime = statisticalUserWatchTimeSeconds(listAll);
                long hours = totalWatchTime / 3600; // 小时数
                long minutes = (totalWatchTime % 3600) / 60; // 分钟数
                long remainingSeconds = totalWatchTime % 60; // 剩余秒数
                String watchTime = String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);

                queryLiveUserWatchVO.setTime(totalWatchTime);
                queryLiveUserWatchVO.setWatchDurations(watchTime);
                queryLiveUserWatchVO.setSpeakCount((int) messageCount);
                queryLiveUserWatchVO.setEnterNum((int) enterOutNum);
                queryLiveUserWatchVOS.add(queryLiveUserWatchVO);
            }
        }
        return queryLiveUserWatchVOS;
    }

    @Override
    public List<QueryLiveUserWatchVO> queryLiveUserWatchDetail(QueryLiveUserWatchDto watchQuery) {
        Assert.notNull(watchQuery.getQueryDate(), "查询时间不能为空");
        String date = DateFormatUtils.format(new Date(), "yyyy-MM-dd");
        Criteria criteria = Criteria.where("liveId").is(watchQuery.getLiveId()).and("userId").is(watchQuery.getUserId());
        if (StringUtils.isNotBlank(watchQuery.getQueryDate())) {
            criteria.and("statisticsDate").is(watchQuery.getQueryDate());
        }
        Query query = new Query(criteria);
        List<QueryLiveUserWatchVO> queryLiveUserWatchVOS;
        // 判断时间是否统计当天直播数据
        if (StringUtils.equals(watchQuery.getQueryDate(), date)) {
//            // 获取用户信息
//            UserDat userInfo = userDatMapper.selectById(watchQuery.getUserId());

            // 查询当天数据
            List<LiveWatchRecordDto> list = mongoTemplate.find(query, LiveWatchRecordDto.class, MongoConstant.LIVE_WATCH_RECORD_PERSISTENCE);
            queryLiveUserWatchVOS = statisticalUserWatchTimeDetail(list);
        } else {
            queryLiveUserWatchVOS = mongoTemplate.find(query, QueryLiveUserWatchVO.class, MongoConstant.LIVE_USER_WATCH_TIME_STATISTICS);
        }
        return queryLiveUserWatchVOS;
    }

    @Override
    public R queryLiveUserWatchSum(QueryLiveUserWatchDto watchQuery) {
        try {
            Assert.notNull(watchQuery.getQueryDate(), "查询时间不能为空");
            Assert.notNull(watchQuery.getLiveId(), "查询直播间不能为空");
        } catch (IllegalArgumentException e) {
            return R.failed(e.getMessage());
        }

        Map<String, Object> map = new HashMap<>();
        int sumOnLineUsers = 0; //累计人数
        int msgCount = 0; //互动人数
        int outLineUsers = 0; //离线人数
        int newOnLineUsers = 0; //新用户

        String date = DateFormatUtils.format(new Date(), "yyyy-MM-dd");
        Criteria criteria = Criteria.where("liveId").is(watchQuery.getLiveId());
        if (StringUtils.isNotBlank(watchQuery.getQueryDate())) {
            criteria.and("statisticsDate").is(watchQuery.getQueryDate());
        }

        if (StringUtils.isNotEmpty(watchQuery.getChannelIds())) {
            // 训练营协助人
            String[] channelIds = watchQuery.getChannelIds().split(",");
            List<Long> channelIdList = new ArrayList<>();
            for (String channelId : channelIds) {
                if (NumberUtil.isLong(channelId) && !"1".equals(channelId)) {
                    channelIdList.add(Long.parseLong(channelId));
                }
            }
            if (!CollectionUtils.isEmpty(channelIdList)) {
                criteria.and("channelId").in(channelIdList);
            } else {
                map.put("sumOnLineUsers", sumOnLineUsers);
                map.put("msgCount", msgCount);
                map.put("outLineUsers", outLineUsers);
                map.put("newOnLineUsers", newOnLineUsers);
                return R.ok(map);
            }
            if (NumberUtil.isLong(watchQuery.getChannelId()) && !"0".equals(watchQuery.getChannelId())) {
                String employeeNum = comChannelDatMapper.getEmployeeByChannelId(watchQuery.getComId(), watchQuery.getChannelId());
                List<TrainingCampLiveUser> list = trainingCampLiveUserService.list(new LambdaQueryWrapper<TrainingCampLiveUser>()
                        .select(TrainingCampLiveUser::getUnionid)
                        .eq(TrainingCampLiveUser::getComId, watchQuery.getComId())
                        .eq(TrainingCampLiveUser::getLiveId, watchQuery.getLiveId())
                        .eq(TrainingCampLiveUser::getRepeatType, 1)
                        .eq(TrainingCampLiveUser::getEmployeeNum, employeeNum)
                        .eq(TrainingCampLiveUser::getDeleteFlg, false));
                if (!CollectionUtils.isEmpty(list)) {
                    List<String> unionIds = list.stream().map(TrainingCampLiveUser::getUnionid).toList();
                    List<UserDat> userList = userDatService.list(new LambdaQueryWrapper<UserDat>()
                            .select(UserDat::getId)
                            .eq(UserDat::getComId, watchQuery.getComId())
                            .in(UserDat::getUnionid, unionIds)
                            .eq(UserDat::getDeleteFlag, false)
                    );
                    if (!CollectionUtils.isEmpty(userList)) {
                        List<Long> ids = userList.stream().map(UserDat::getId).toList();
                        criteria.and("liveUserId").in(ids);
                    }
                }
            }
        } else if (NumberUtil.isLong(watchQuery.getChannelId()) && !"0".equals(watchQuery.getChannelId())) {
            //是否是管理员
            List<ChannelGroupAdmin> list = channelGroupAdminService.getListByChannelId(watchQuery.getChannelId());
            if (CollectionUtils.isEmpty(list)) {
                //不是管理员
                criteria.and("channelId").is(Long.parseLong(watchQuery.getChannelId()));
            } else {
                //是管理员
                List<Long> principalList = new ArrayList<>();
                for (ChannelGroupAdmin admin : list) {
                    principalList.add(admin.getGroupId());
                }
                List<ComChannelDat> channelList = comChannelDatService.getChannelListByGroupIds(principalList);
                if (!CollectionUtils.isEmpty(channelList)) {
                    List<Long> channelIdList = channelList.stream().map(ComChannelDat::getId).toList();
                    criteria.and("channelId").in(channelIdList);
                }
            }
        }


        // 判断时间是否统计当天直播数据
        if (StringUtils.equals(watchQuery.getQueryDate(), date)) {
            if (mongoTemplate.collectionExists(MongoConstant.LIVE_USER_STATE_CHANGE_DAY)) {
                Aggregation aggregation = Aggregation.newAggregation(
                        Aggregation.match(criteria),
                        Aggregation.sort(Sort.Direction.DESC, "_id"),
                        Aggregation.group("liveUserId")
                                .first("liveUserId").as("liveUserId")
                                .first("liveUsername").as("liveUsername")
                                .first("userType").as("userType")
                                .first("isNew").as("isNew")
                                .first("headImgUrl").as("headImgUrl")
                                .first("eventType").as("eventType"),
                        Aggregation.project()
                                .and("liveUsername").previousOperation()//必须添加一个字段的 previousOperation 否则会报错
                                .and("liveUserId").as("liveUserId")
                                .and("liveUsername").as("liveUsername")
                                .and("userType").as("userType")
                                .and("isNew").as("isNew")
                                .and("headImgUrl").as("headImgUrl")
                                .and("eventType").as("eventType"),
                        Aggregation.sort(Sort.Direction.DESC, "liveUserId")
                );
                AggregationResults<LiveUserStateChangeVo> results = mongoTemplate.aggregate(aggregation, MongoConstant.LIVE_USER_STATE_CHANGE_DAY, LiveUserStateChangeVo.class);

                sumOnLineUsers = results.getMappedResults().size(); //累计人数

                String dateSs = date + " 00:00:00";
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                try {
                    Date start = formatter.parse(dateSs);
                    for (LiveUserStateChangeVo liveUserStateChange : results.getMappedResults()) {
                        // 获取用户观看发言次数
                        Query query = new Query(Criteria.where("liveId").is(watchQuery.getLiveId())
                                .and("liveUserId").is(liveUserStateChange.getLiveUserId())
                                .and("msgTime").gte(start.getTime() / 1000)
                        );
                        long messageCount = mongoTemplate.count(query, LiveMessage.class, MongoConstant.LIVE_MESSAGE);
                        if (messageCount > 0) {
                            msgCount++;
                        }

                        if (!"Login".equals(liveUserStateChange.getEventType())) {
                            outLineUsers++;
                        }
                        if ("audience".equals(liveUserStateChange.getUserType()) && null != liveUserStateChange.getIsNew() && liveUserStateChange.getIsNew() == 1) {
                            newOnLineUsers++;
                        }
                    }
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            } else {
                log.info("mongo中没有集合：{}", MongoConstant.LIVE_USER_STATE_CHANGE_DAY);
            }
        } else {
            log.info("历史数据暂未统计");
        }

        map.put("sumOnLineUsers", sumOnLineUsers);
        map.put("msgCount", msgCount);
        map.put("outLineUsers", outLineUsers);
        map.put("newOnLineUsers", newOnLineUsers);
        return R.ok(map);
    }

    @Override
    public List<QueryLiveUserWatchVO> downLiveUserWatchList(QueryLiveUserWatchDto watchQuery) {
        List<QueryLiveUserWatchVO> queryLiveUserWatchVOS = new ArrayList<>();
        try {
            Assert.notNull(watchQuery.getQueryDate(), "查询时间不能为空");
            Assert.notNull(watchQuery.getLiveId(), "查询直播间不能为空");
        } catch (IllegalArgumentException e) {
            return queryLiveUserWatchVOS;
        }
        String date = DateFormatUtils.format(new Date(), "yyyy-MM-dd");
        Criteria criteria = Criteria.where("liveId").is(watchQuery.getLiveId());
//        if (StringUtils.isNotBlank(watchQuery.getNickName())) {
//            criteria.and("nickName").regex("^.*"+watchQuery.getNickName()+".*$");
//        }
        if (StringUtils.isNotBlank(watchQuery.getQueryDate())) {
            criteria.and("statisticsDate").is(watchQuery.getQueryDate());
        }
        // 判断时间是否统计当天直播数据
        if (StringUtils.equals(watchQuery.getQueryDate(), date)) {
            // 查询当天数据
            Query queryUser = new Query(criteria);
            // 排序
            Sort sort = Sort.by(Sort.Direction.DESC, "createAt");
            queryUser.with(sort);
            List<LiveWatchRecordDto> userList = mongoTemplate.find(queryUser, LiveWatchRecordDto.class, MongoConstant.LIVE_WATCH_RECORD_USER);

            List<LiveRecordDat> liveRecordDatList = liveRecordDatService.getListBayDat(null, watchQuery.getLiveId(), watchQuery.getQueryDate());
            for (LiveWatchRecordDto watchRecord : userList) {
                QueryLiveUserWatchVO queryLiveUserWatchVO = new QueryLiveUserWatchVO();
                queryLiveUserWatchVO.setLiveId(watchQuery.getLiveId());
                queryLiveUserWatchVO.setCity(watchRecord.getCity());
                queryLiveUserWatchVO.setIp(watchRecord.getIp());
                queryLiveUserWatchVO.setOperators(watchRecord.getOperators());
                queryLiveUserWatchVO.setBrowser(watchRecord.getBrowser());
                queryLiveUserWatchVO.setSystemOs(watchRecord.getSystemOs());
                queryLiveUserWatchVO.setEnterTime(watchRecord.getEnterTime());
                queryLiveUserWatchVO.setOutTime(watchRecord.getOutTime());
                queryLiveUserWatchVO.setStatisticsDate(watchQuery.getQueryDate());
                queryLiveUserWatchVO.setUserId(watchRecord.getUserId());
                queryLiveUserWatchVO.setNickName(watchRecord.getNickName());
                queryLiveUserWatchVO.setHeadImgUrl(watchRecord.getHeadImgUrl());
                queryLiveUserWatchVO.setChannelId(watchRecord.getChannelId());
                queryLiveUserWatchVO.setChannelName(watchRecord.getChannelName());
                String key = "IM_" + watchRecord.getUserId();
                String userInfo = (String) redisService.get(key);
                if (StringUtils.isNotBlank(userInfo)) {
                    String[] split = StringUtils.split(userInfo, "###");
                    String isNew = split[5];
                    if (StringUtils.isNotBlank(isNew)) {
                        queryLiveUserWatchVO.setIsNew(Integer.parseInt(isNew));
                    }
                }

                long enterOutNum = 0L;
                long messageCount = 0L;
                // 获取观看时长
                List<LiveWatchRecordDto> listAll = new ArrayList<>();

                for (LiveRecordDat liveRecordDat : liveRecordDatList) {
                    // 获取用户观看进入次数
                    Query query = new Query(Criteria.where("liveRecordId").is(liveRecordDat.getId())
                            .and("userId").is(watchRecord.getUserId()));
                    enterOutNum += mongoTemplate.count(query, LiveWatchRecordDto.class, MongoConstant.LIVE_WATCH_RECORD_PERSISTENCE);

                    // 获取观看时长
                    List<LiveWatchRecordDto> list = mongoTemplate.find(query, LiveWatchRecordDto.class, MongoConstant.LIVE_WATCH_RECORD_PERSISTENCE);
                    listAll.addAll(list);

                    long endTime = null == watchRecord.getOutTime() ? new Date().getTime() / 1000 : watchRecord.getOutTime().getTime() / 1000;
                    // 获取用户观看发言次数
                    Query messageQuery = new Query(Criteria.where("liveId").is(watchQuery.getLiveId())
                            .and("liveUserId").is(watchRecord.getUserId())
                            .andOperator(
                                    Criteria.where("msgTime").gte(watchRecord.getEnterTime().getTime() / 1000),
                                    Criteria.where("msgTime").lte(endTime)
                            )
                    );
                    messageCount += mongoTemplate.count(messageQuery, LiveMessage.class, MongoConstant.LIVE_MESSAGE);
                }
                String watchTime = statisticalUserWatchTime(listAll);
                queryLiveUserWatchVO.setWatchDurations(watchTime);
                queryLiveUserWatchVO.setSpeakCount((int) messageCount);
                queryLiveUserWatchVO.setEnterNum((int) enterOutNum);
                queryLiveUserWatchVOS.add(queryLiveUserWatchVO);
            }

        } else {
            // 查询历史数据
            Query query = new Query(criteria);
            // 排序
            Sort sort = Sort.by(Sort.Direction.DESC, "enterTime");
            query.with(sort);
            queryLiveUserWatchVOS = mongoTemplate.find(query, QueryLiveUserWatchVO.class, MongoConstant.LIVE_USER_WATCH_TIME_STATISTICS);
        }
        return queryLiveUserWatchVOS;
    }

    @Override
    public R queryLiveUserWatchGeneral(QueryLiveUserWatchDto watchQuery) {
        String date = DateFormatUtils.format(new Date(), "yyyy-MM-dd");
        Criteria criteria = Criteria.where("liveId").is(watchQuery.getLiveId());
        if (StringUtils.isNotBlank(watchQuery.getQueryDate())) {
            criteria.and("statisticsDate").is(watchQuery.getQueryDate());
        }
        Map<String, Object> map = new HashMap<>();
        int sumOnLineUsers = 0; //累计观看人数
        int maxOnLineUsers = 0; //最高在线人数
        int interactionRate = 0; //互动率
        int newOnLineUsers = 0; //新用户数
        long sumWatchTime = 0; //总观看时长
        String avgWatchTime = "00:00:00"; //平均观看时长

        int msgCount = 0; //互动人数

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.sort(Sort.Direction.DESC, "_id"),
                Aggregation.group("liveUserId")
                        .first("liveUserId").as("liveUserId")
                        .first("liveUsername").as("liveUsername")
                        .first("userType").as("userType")
                        .first("isNew").as("isNew")
                        .first("headImgUrl").as("headImgUrl")
                        .first("eventType").as("eventType"),
                Aggregation.project()
                        .and("liveUsername").previousOperation()//必须添加一个字段的 previousOperation 否则会报错
                        .and("liveUserId").as("liveUserId")
                        .and("liveUsername").as("liveUsername")
                        .and("userType").as("userType")
                        .and("isNew").as("isNew")
                        .and("headImgUrl").as("headImgUrl")
                        .and("eventType").as("eventType"),
                Aggregation.sort(Sort.Direction.DESC, "liveUserId")
        );

        String userConstant = MongoConstant.LIVE_USER_STATE_USER_DATA;
        String msgConstant = MongoConstant.LIVE_MESSAGE + "_" + watchQuery.getQueryDate();

        // 判断时间是否统计当天直播数据
        if (StringUtils.equals(watchQuery.getQueryDate(), date) && mongoTemplate.collectionExists(MongoConstant.LIVE_USER_STATE_CHANGE_DAY)) {
            userConstant = MongoConstant.LIVE_USER_STATE_CHANGE_DAY;
            msgConstant = MongoConstant.LIVE_MESSAGE;
        }

        AggregationResults<LiveUserStateChangeVo> results = mongoTemplate.aggregate(aggregation, userConstant, LiveUserStateChangeVo.class);
        sumOnLineUsers = results.getMappedResults().size(); //累计人数

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date start = formatter.parse(watchQuery.getQueryDate() + " 00:00:00");
            for (LiveUserStateChangeVo liveUserStateChange : results.getMappedResults()) {
                // 获取观看时长
                Query queryT = new Query(Criteria.where("statisticsDate").is(watchQuery.getQueryDate())
                        .and("userId").is(liveUserStateChange.getLiveUserId())
                        .and("liveId").is(watchQuery.getLiveId())
                );
                List<LiveWatchRecordDto> list = mongoTemplate.find(queryT, LiveWatchRecordDto.class, MongoConstant.LIVE_WATCH_RECORD_PERSISTENCE);
                long watchTimeSeconds = statisticalUserWatchTimeSeconds(list);
                System.out.println("watchTimeSeconds = " + watchTimeSeconds);

                sumWatchTime += watchTimeSeconds;

                Query query = new Query(Criteria.where("liveId").is(watchQuery.getLiveId())
                        .and("liveUserId").is(liveUserStateChange.getLiveUserId())
                        .and("msgTime").gte(start.getTime() / 1000)
                );
                long messageCount = mongoTemplate.count(query, LiveMessage.class, msgConstant);
                if (messageCount > 0) {
                    msgCount++;
                }
                if ("audience".equals(liveUserStateChange.getUserType()) && null != liveUserStateChange.getIsNew() && liveUserStateChange.getIsNew() == 1) {
                    newOnLineUsers++;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 计算最高在线人数
        String dateSs1 = watchQuery.getQueryDate() + " 00:00:00";
        String dateSs2 = watchQuery.getQueryDate() + " 23:59:59";
        try {
            Date start0 = formatter.parse(dateSs1);
            Date start24 = formatter.parse(dateSs2);
            Query query = new Query(Criteria.where("liveId").is(watchQuery.getLiveId())
                    .andOperator(
                            Criteria.where("createAt").gte(start0),
                            Criteria.where("createAt").lte(start24)
                    )
            );
            List<StaticOnLineUserStateVO> staticOnLineUserStateVO = mongoTemplate.find(query, StaticOnLineUserStateVO.class, MongoConstant.STATIC_ONLINE_USER_STATUE);
            maxOnLineUsers = staticOnLineUserStateVO.stream().mapToInt(StaticOnLineUserStateVO::getSumOnLineUser).max().orElse(0);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        // 计算互动率
        if (sumOnLineUsers > 0 && sumOnLineUsers >= msgCount) {
            interactionRate = (int) (msgCount * 100.0 / sumOnLineUsers);
        }

        long hours = sumWatchTime / 3600; // 小时数
        if (sumOnLineUsers > 0) {
            long totalWatchTime = sumWatchTime / sumOnLineUsers; //平均观看时长
            long hoursAvg = totalWatchTime / 3600; // 小时数
            long minutesAvg = (totalWatchTime % 3600) / 60; // 分钟数
            long secondsAvg = totalWatchTime % 60; // 剩余秒数
            avgWatchTime = String.format("%02d:%02d:%02d", hoursAvg, minutesAvg, secondsAvg);
        }

        map.put("sumOnLineUsers", sumOnLineUsers);//累计观看人数
        map.put("maxOnLineUsers", maxOnLineUsers);//最高在线人数
        map.put("interactionRate", interactionRate);//互动率
        map.put("newOnLineUsers", newOnLineUsers);//新用户数
        map.put("sumWatchTime", hours);//总观看时长
        map.put("avgWatchTime", avgWatchTime);//平均观看时长

        return R.ok(map);
    }

    public static void main(String[] args) {
        long totalWatchTime = 55;

    }

    private List<QueryLiveUserWatchVO> statisticalUserWatchTimeDetail(List<LiveWatchRecordDto> list) {
        List<QueryLiveUserWatchVO> resultList = new ArrayList<>();
        QueryLiveUserWatchVO queryLiveUserWatchDetailVo;
        for (LiveWatchRecordDto watchRecord : list) {
            queryLiveUserWatchDetailVo = new QueryLiveUserWatchVO();
            BeanUtils.copyProperties(watchRecord, queryLiveUserWatchDetailVo);

            Date outDate = watchRecord.getOutTime() == null ? watchRecord.getLatestUploadTime() : watchRecord.getOutTime();
            Date enterDate = watchRecord.getEnterTime() == null ? watchRecord.getCreateAt() : watchRecord.getEnterTime();
            long times = (outDate.getTime() - enterDate.getTime()) / 1000;
            long hours = times / 3600; // 小时数
            long minutes = (times % 3600) / 60; // 分钟数
            long remainingSeconds = times % 60; // 剩余秒数
            String watchTime = String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
            queryLiveUserWatchDetailVo.setWatchDurations(watchTime);
            queryLiveUserWatchDetailVo.setNickName(watchRecord.getNickName());
            queryLiveUserWatchDetailVo.setHeadImgUrl(watchRecord.getHeadImgUrl());
            resultList.add(queryLiveUserWatchDetailVo);
        }
        return resultList;
    }

    /**
     * 计算用户观看时长
     */
    public String statisticalUserWatchTime(List<LiveWatchRecordDto> dataList) {
        // 初始化总观看时长
        long totalWatchTime = 0;
        // 计算每个用户的观看时长并累加总观看时长
        for (LiveWatchRecordDto watchRecord : dataList) {
            Date outDate = watchRecord.getOutTime() == null ? watchRecord.getLatestUploadTime() : watchRecord.getOutTime();
            Date enterDate = watchRecord.getEnterTime() == null ? watchRecord.getCreateAt() : watchRecord.getEnterTime();
            long times = (outDate.getTime() - enterDate.getTime()) / 1000;
            totalWatchTime += times;
        }
        long hours = totalWatchTime / 3600; // 小时数
        long minutes = (totalWatchTime % 3600) / 60; // 分钟数
        long remainingSeconds = totalWatchTime % 60; // 剩余秒数
        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
    }

    /**
     * 计算用户观看时长-秒
     */
    public long statisticalUserWatchTimeSeconds(List<LiveWatchRecordDto> dataList) {
        // 初始化总观看时长
        long totalWatchTime = 0;
        // 计算每个用户的观看时长并累加总观看时长
        for (LiveWatchRecordDto watchRecord : dataList) {
            Date outDate = watchRecord.getOutTime() == null ? watchRecord.getLatestUploadTime() : watchRecord.getOutTime();
            Date enterDate = watchRecord.getEnterTime() == null ? watchRecord.getCreateAt() : watchRecord.getEnterTime();
            long times = (outDate.getTime() - enterDate.getTime()) / 1000;
            totalWatchTime += times;
        }
        return totalWatchTime;
//        long hours = totalWatchTime / 3600; // 小时数
//        long minutes = (totalWatchTime % 3600) / 60; // 分钟数
//        long remainingSeconds = totalWatchTime % 60; // 剩余秒数
//        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
    }

}
