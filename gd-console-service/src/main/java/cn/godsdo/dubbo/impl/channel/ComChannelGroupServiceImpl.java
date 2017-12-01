package cn.godsdo.dubbo.impl.channel;

import cn.godsdo.constant.MongoConstant;
import cn.godsdo.dto.LiveWatchRecordDto;
import cn.godsdo.dto.channel.GetChannelOnLineDto;
import cn.godsdo.dubbo.channel.ComChannelGroupService;
import cn.godsdo.dubbo.crossService.RedisDubboService;
import cn.godsdo.entity.LiveRecordDat;
import cn.godsdo.entity.channel.ComChannelDat;
import cn.godsdo.entity.channel.ComChannelGroup;
import cn.godsdo.enums.live.LiveStatusEnum;
import cn.godsdo.mapper.channel.ChannelGroupAdminMapper;
import cn.godsdo.mapper.channel.ComChannelDatMapper;
import cn.godsdo.mapper.channel.ComChannelGroupMapper;
import cn.godsdo.util.R;
import cn.godsdo.vo.ComOnlineSumVo;
import cn.godsdo.vo.StaticChannelOnLineUserStateVO;
import cn.godsdo.vo.channel.GetAllDataGroupsVo;
import cn.godsdo.vo.channel.GetAllGroupsVo;
import cn.godsdo.vo.channel.GetChannelOnLineVo;
import cn.godsdo.vo.channel.GetComChannelGroupAndChannelVo;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.excel.util.StringUtils;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.constant.Constants;
import com.y20y.interfaces.IdService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static cn.godsdo.constant.RedisConstants.CHANNEL_GROUP_PARENT_INFO;

/**
 * <p>
 * 《渠道分组表》 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2019-04-22
 */
@Slf4j
@DubboService
public class ComChannelGroupServiceImpl extends ServiceImpl<ComChannelGroupMapper, ComChannelGroup> implements ComChannelGroupService {

    @Resource
    ComChannelDatMapper comChannelDatMapper;
    @Resource
    ChannelGroupAdminMapper channelGroupAdminMapper;
    @Resource
    ComChannelGroupMapper comChannelGroupMapper;

    @DubboReference
    IdService idService;
    @DubboReference(check = false)
    RedisDubboService redisDubboService;
    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public R getAllGroups(Long comId, Long accountId, Boolean isAdmin) {
        return getGroupsInfo(comId, accountId, isAdmin);
    }

    @Override
    public R getComChannelGroupAndChannel(Long liveId, String date, Long comId, Long accountId, Boolean isAdmin) {
        Set<Long> ids = new HashSet<>();
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date start0 = formatter.parse(date + " 00:00:01");
            Date start24 = formatter.parse(date + " 23:59:59");
            Criteria criteria = Criteria.where("liveId").is(liveId).and("comId").is(comId)
                    .and("channelId").ne("").ne(null).ne(0).and("createAt").gte(start0).lte(start24);
            Query query = new Query(criteria);
            List<LiveWatchRecordDto> queryLiveUserWatchVOS = mongoTemplate.find(query, LiveWatchRecordDto.class, MongoConstant.LIVE_WATCH_RECORD_USER);

            if (queryLiveUserWatchVOS.size() > 0) {
                for (LiveWatchRecordDto queryLiveUserWatchVO : queryLiveUserWatchVOS) {
                    Long channelId = queryLiveUserWatchVO.getChannelId();
                    ids.add(channelId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (ObjectUtil.isEmpty(ids)) {
            return R.ok();
        }
        // 获取所有渠道信息

        List<ComChannelDat> comChannelDats = comChannelDatMapper.selectList(new LambdaQueryWrapper<ComChannelDat>().eq(ComChannelDat::getComId, comId).in(ComChannelDat::getId, ids));
        Set<Long> groupIds = new HashSet<>();
        Set<Long> groupId2s = new HashSet<>();
        Boolean isDefultGroup = false;
        for (ComChannelDat comChannelDat : comChannelDats) {
            Long groupId = comChannelDat.getGroupId();
            if (Constants.DEFAULT_GROUP_VALUE == groupId) {
                isDefultGroup = true;
            } else {
                groupIds.add(groupId);
                groupId2s.add(groupId);
            }
        }

        // 循环查询上旬
        for (Long id : groupIds) {
            ComChannelGroup comChannelGroup = this.baseMapper.selectById(id);
            Long parentId = comChannelGroup.getParentId();
            if (parentId != 0) {
                ComChannelGroup comChannelGroup1 = this.baseMapper.selectById(parentId);
                groupId2s.add(parentId);
                Long parentId1 = comChannelGroup1.getParentId();
                if (parentId1 != 0) {
                    groupId2s.add(parentId);
                }
            }

        }
        // 获取所有渠道分组信息
        List<ComChannelGroup> groups = null;
        if (!CollectionUtils.isEmpty(groupId2s)) {
            groups = this.baseMapper.selectListAndTotalByList(comId, null, groupId2s);
        }
        // 用于存储处理后的分组信息
        List<GetComChannelGroupAndChannelVo> resultList = new ArrayList<>();
        // 若查询结果为null，则初始化为一个空列表
        if (groups == null) {
            groups = new ArrayList<>();
        }
        // 分析处理获取到的分组数据并统计总数
        Integer i = this.analysisGroupAndChannelData(resultList, comChannelDats, groups, 0L, 0);
        // 创建响应对象
        if (isDefultGroup) {
            // 计算默认组的数量
            Long defaultCount = this.getGroupCount(comId, Constants.DEFAULT_GROUP_VALUE);
            // 创建默认分组对象
            GetComChannelGroupAndChannelVo defaultGroup = new GetComChannelGroupAndChannelVo();
            defaultGroup.setId(Constants.DEFAULT_GROUP_VALUE);
            defaultGroup.setTitle(Constants.DEFAULT_GROUP);
            defaultGroup.setType("first");

            defaultGroup.setTotalItem(defaultCount.intValue());
            List<GetComChannelGroupAndChannelVo> defaultChannel = new ArrayList<>();
            comChannelDats.stream().filter(x -> x.getGroupId() == Constants.DEFAULT_GROUP_VALUE).forEach(e -> {
                GetComChannelGroupAndChannelVo getComChannelGroupAndChannelVo = new GetComChannelGroupAndChannelVo();
                getComChannelGroupAndChannelVo.setId(e.getId());
                getComChannelGroupAndChannelVo.setTitle(e.getName());
                getComChannelGroupAndChannelVo.setType("channel");
                defaultChannel.add(getComChannelGroupAndChannelVo);
            });
            defaultGroup.setChildren(defaultChannel);
            // 将默认分组信息添加到处理后的列表的首位
            resultList.add(0, defaultGroup);

        }
        return R.ok(resultList);
    }

    @Override
    public R getComChannelGroupByRoomDistribution(Long comId) {
        // 查询数据库获取公司的所有频道分组信息
        List<ComChannelGroup> comChannelGroups = this.baseMapper.selectListAndTotal(comId);
        // 用于存储处理后的分组信息
        List<GetAllGroupsVo> resultList = new ArrayList<>();
        // 若查询结果为null，则初始化为一个空列表
        if (comChannelGroups == null) {
            comChannelGroups = new ArrayList<>();
        }
        // 分析处理获取到的分组数据并统计总数
        Integer i = this.analysisGroupData(resultList, comChannelGroups, 0L, 0L);
        // 创建响应对象
        GetAllDataGroupsVo vo = new GetAllDataGroupsVo();
        // 计算默认组的数量
        Long defaultCount = this.getGroupCount(comId, Constants.DEFAULT_GROUP_VALUE);
        // 创建默认分组对象
        GetAllGroupsVo defaultGroup = new GetAllGroupsVo();
        defaultGroup.setId(Constants.DEFAULT_GROUP_VALUE);
        defaultGroup.setTitle(Constants.DEFAULT_GROUP);
        defaultGroup.setIsAdd(false);
        defaultGroup.setComId(comId);
        defaultGroup.setChildren(new ArrayList<>());
        defaultGroup.setTotalItem(defaultCount.intValue());
        // 将默认分组信息添加到处理后的列表的首位
        resultList.add(0, defaultGroup);
        // 创建默认分组对象
        GetAllGroupsVo wholeGroup = new GetAllGroupsVo();
        wholeGroup.setId(0L);
        wholeGroup.setTitle("全部");
        wholeGroup.setIsAdd(false);
        wholeGroup.setComId(comId);
        wholeGroup.setChildren(new ArrayList<>());
        wholeGroup.setTotalItem(0);
        // 将默认分组信息添加到处理后的列表的首位
        resultList.add(0, wholeGroup);
        vo.setTotal(i + defaultCount);

        vo.setTreeList(resultList);
        return R.ok(vo);
    }

    /**
     * 处理分组信息
     *
     * @return
     */
    private Integer analysisGroupAndChannelData(List<GetComChannelGroupAndChannelVo> resultList, List<ComChannelDat> comChannelDats, List<ComChannelGroup> groupList, Long parentId, Integer type) {
        int total = 0;
        for (ComChannelGroup group : groupList) {
            // 遍历组列表，找到parentId匹配的组
            if (group.getParentId().equals(parentId)) {
                int nowTotal = 0;
                GetComChannelGroupAndChannelVo getChannelGroupAllVo = new GetComChannelGroupAndChannelVo();
                BeanUtils.copyProperties(group, getChannelGroupAllVo);
                getChannelGroupAllVo.setChildren(new ArrayList<>());
                getChannelGroupAllVo.setTitle(group.getName());
                switch (type) {
//                    first一级 second二级 three  channel渠道
                    case 0:
                        getChannelGroupAllVo.setType("first");
                        break;
                    case 1:
                        getChannelGroupAllVo.setType("second");
                        break;
                    case 2:
                        getChannelGroupAllVo.setType("three");
                        break;
                    case 3:
                        getChannelGroupAllVo.setType("channel");
                        break;
                }

                // 获取子组的总数
                Long id = getChannelGroupAllVo.getId();
                Integer childrenTotal = this.analysisGroupAndChannelData(getChannelGroupAllVo.getChildren(), comChannelDats, groupList, id, type+1);
//                type++;
                // 计算当前组及其子组的总数
                nowTotal = getChannelGroupAllVo.getTotalItem() + childrenTotal;
                // 计算所有子组的总数
                total += nowTotal;
                getChannelGroupAllVo.setTotalItem(nowTotal);
                comChannelDats.stream().filter(x -> x.getGroupId().equals(id)).forEach(e -> {
                    GetComChannelGroupAndChannelVo getComChannelGroupAndChannelVo = new GetComChannelGroupAndChannelVo();
                    getComChannelGroupAndChannelVo.setId(e.getId());
                    getComChannelGroupAndChannelVo.setTitle(e.getName());
                    getComChannelGroupAndChannelVo.setType("channel");
                    getChannelGroupAllVo.getChildren().add(getComChannelGroupAndChannelVo);
                });
                resultList.add(getChannelGroupAllVo);
            }
        }
        // 按创建时间和ID对resultList进行排序
        resultList.sort(Comparator.comparing(GetComChannelGroupAndChannelVo::getId));
        return total;
    }


    @Override
    public R addGroup(Long comId, Long accountId, String name, Long parentId) {
        // 在数据库中插入新的通讯频道组
        ComChannelGroup ccg = this.baseMapper.selectOne(new LambdaQueryWrapper<ComChannelGroup>().eq(ComChannelGroup::getDeleteFlag, false)
                .eq(ComChannelGroup::getComId, comId).eq(ComChannelGroup::getParentId, parentId)
                .eq(ComChannelGroup::getName, name)
        );
        // 如果名称已存在，则返回失败响应
        if (ObjectUtil.isNotEmpty(ccg)) {
            return R.failed("分组名称已存在");
        }

        // 创建新的通讯频道组实例并设置属性值
        ComChannelGroup comChannelGroup = new ComChannelGroup();
        comChannelGroup.setId(idService.nextId());
        comChannelGroup.setComId(comId);
        comChannelGroup.setParentId(parentId);
        comChannelGroup.setName(name);
        comChannelGroup.setCreateBy(accountId);
        // 将新的通讯频道组插入数据库
        this.baseMapper.insert(comChannelGroup);

        return R.ok();
    }


    @Override
    public R updateGroup(Long groupId, String name, Long accountId, Long comId) {
        ComChannelGroup ccg = this.baseMapper.selectOne(new LambdaQueryWrapper<ComChannelGroup>().eq(ComChannelGroup::getDeleteFlag, false)
                .eq(ComChannelGroup::getComId, comId).eq(ComChannelGroup::getId, groupId)
        );
        if (ObjectUtil.isEmpty(ccg)) {
            return R.failed("分组不存在");
        }
        ccg.setName(name);
        ccg.setUpdateBy(accountId);
        this.baseMapper.updateById(ccg);
        return R.ok();
    }

    @Override
    public R deleteGroup(Long groupId, Long accountId, Long comId) {
        List<ComChannelGroup> ccgs = this.baseMapper.selectList(new LambdaQueryWrapper<ComChannelGroup>().eq(ComChannelGroup::getParentId, groupId)
                .eq(ComChannelGroup::getComId, comId).eq(ComChannelGroup::getDeleteFlag, false));
        if (ObjectUtil.isNotEmpty(ccgs)) {
            return R.failed("存在子分组，不可删除");
        }
        List<ComChannelDat> comChannelDats = comChannelDatMapper.selectList(new LambdaQueryWrapper<ComChannelDat>().eq(ComChannelDat::getGroupId, groupId)
                .eq(ComChannelDat::getComId, comId).eq(ComChannelDat::getDeleteFlag, false));
        if (ObjectUtil.isNotEmpty(comChannelDats)) {
            return R.failed("分组下存在渠道，不可删除");
        }
        ComChannelGroup ccg = this.baseMapper.selectOne(new LambdaQueryWrapper<ComChannelGroup>().eq(ComChannelGroup::getDeleteFlag, false)
                .eq(ComChannelGroup::getComId, comId).eq(ComChannelGroup::getId, groupId)
        );
        if (ObjectUtil.isEmpty(ccg)) {
            return R.failed("分组不存在");
        }
        ccg.setDeleteFlag(true);
        ccg.setUpdateBy(accountId);
        this.baseMapper.updateById(ccg);
        // 删除分组后移动渠道到其他直播间
//        comChannelDatMapper.update(null, new LambdaUpdateWrapper<ComChannelDat>()
//                .set(ComChannelDat::getGroupId, Constants.DEFAULT_GROUP_VALUE)
//                .set(ComChannelDat::getUpdateBy, accountId)
//                .eq(ComChannelDat::getGroupId, groupId)
//                .eq(ComChannelDat::getComId, comId)
//        );
        return R.ok();
    }

    @Override
    public R getChannelOnLine(Long comId, GetChannelOnLineDto dto) {
        Long liveId = dto.getLiveId();
        String date = dto.getDate();
        String type = dto.getType();
        List<Long> ids = dto.getIds();
        if (ObjectUtil.isEmpty(ids)) {
            return R.ok();
        }

        if (mongoTemplate.collectionExists(MongoConstant.STATIC_CHANNEL_ONLINE_USER_STATUE)) {

            try {
                List<GetChannelOnLineVo> list = new ArrayList<>();
                for (Long id : ids) {
                    GetChannelOnLineVo vo = new GetChannelOnLineVo();

                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(new Date());
                    String dateStart = date + " 00:00:00";
                    String dateEnd = date + " 23:59:59";
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Criteria criteria = Criteria.where("comId").is(comId);

                    Date start = formatter.parse(dateStart);
                    Date end = formatter.parse(dateEnd);
                    criteria.and("createAt").gte(start).lte(end);
                    criteria.and("liveId").is(liveId);
//                    if ("channel".equals(type)) {
//                        ComChannelDat comChannelDat = comChannelDatMapper.selectById(id);
//                        vo.setName(comChannelDat.getName());
////                        criteria.and("channel").is(id);
//                    }
                    String name = "";
                    if (Constants.DEFAULT_GROUP_VALUE.equals(id)) {
                        name = Constants.DEFAULT_GROUP;
                        criteria.and("channelFirstGroupId").is(id);
                    } else {
                        //  first一级 second二级 three  channel渠道
                        switch (type) {
                            case "channel":
                                ComChannelDat comChannelDat = comChannelDatMapper.selectById(id);
                                name = comChannelDat.getName();
                                criteria.and("channelId").is(id);
                                break;
                            case "first":
                                ComChannelGroup comChannelGroup = comChannelGroupMapper.selectById(id);
                                name = comChannelGroup.getName();
                                criteria.and("channelFirstGroupId").is(id);
                                break;
                            case "second":
                                ComChannelGroup comChannelGroup1 = comChannelGroupMapper.selectById(id);
                                name = comChannelGroup1.getName();
                                criteria.and("channelSecondGroupId").is(id);
                                break;
                            case "three":
                                ComChannelGroup comChannelGroup2 = comChannelGroupMapper.selectById(id);
                                name = comChannelGroup2.getName();
                                criteria.and("channelThreeGroupId").is(id);
                        }
                    }

                    if (StringUtils.isEmpty(name)) {
                        break;
                    }
                    vo.setName(name);

                    Aggregation aggregation = Aggregation.newAggregation(
                            Aggregation.match(criteria),
                            Aggregation.group("statisticsDate").sum("sumOnLineUser").as("sum")
                                    .first("statisticsDate").as("statisticsDate"),
                            Aggregation.project()
                                    .and("statisticsDate").previousOperation()//必须添加一个字段的 previousOperation 否则会报错
                                    .and("statisticsDate").as("statisticsDate")
                                    .and("sum").as("sum"),
                            Aggregation.sort(Sort.Direction.ASC, "statisticsDate")
                    );
                    AggregationResults<ComOnlineSumVo> results = mongoTemplate.aggregate(aggregation, MongoConstant.STATIC_CHANNEL_ONLINE_USER_STATUE, ComOnlineSumVo.class);
                    List<ComOnlineSumVo> resultList = results.getMappedResults();
                    vo.setData(resultList);

                    list.add(vo);
                }


                return R.ok(list);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        return R.ok();
    }

    public static void main(String[] args) {
        System.out.println(0L==0L);
    }
    @Override
    public StaticChannelOnLineUserStateVO getGroupParantId(Long channelId) {
        Object data = redisDubboService.get(1, CHANNEL_GROUP_PARENT_INFO + channelId);
        if (ObjectUtil.isNotEmpty(data)) {
            StaticChannelOnLineUserStateVO vo = JSONObject.parseObject(data.toString(), StaticChannelOnLineUserStateVO.class);
            return vo;
        } else {
            StaticChannelOnLineUserStateVO vo = new StaticChannelOnLineUserStateVO();
            ComChannelDat comChannelDat = comChannelDatMapper.selectById(channelId);

            if (ObjectUtils.isNotEmpty(comChannelDat)) {
                Long groupId = comChannelDat.getGroupId();
                if (Constants.DEFAULT_GROUP_VALUE.equals(groupId)) {
                    vo.setChannelFirstGroupId(groupId);
                    redisDubboService.setex(1, CHANNEL_GROUP_PARENT_INFO + channelId, JSONObject.toJSONString(vo), 60 * 60 * 24);
                    return vo;
                }
                // 判断渠道分组等级
                ComChannelGroup comChannelGroup = this.baseMapper.selectById(groupId);
                if (ObjectUtils.isNotEmpty(comChannelGroup)) {
                    Long parentId = comChannelGroup.getParentId();
                    if ( parentId==0L) {
                        vo.setChannelFirstGroupId(groupId);
                    } else {
                        ComChannelGroup comChannelGroup1 = this.baseMapper.selectById(parentId);
                        if (ObjectUtils.isNotEmpty(comChannelGroup1)) {
                            Long parentId1 = comChannelGroup1.getParentId();
                            if (parentId1==0L) {
                                vo.setChannelFirstGroupId(parentId);
                                vo.setChannelSecondGroupId(groupId);
                            } else {
                                vo.setChannelFirstGroupId(parentId1);
                                vo.setChannelSecondGroupId(parentId);
                                vo.setChannelThreeGroupId(groupId);
                            }
                        }
                    }
                    redisDubboService.setex(1, CHANNEL_GROUP_PARENT_INFO + channelId, JSONObject.toJSONString(vo), 60 * 60 * 24);
                    return vo;
                }
            }
        }
        return null;
    }

    public Long getGroupCount(Long comId, Long groupId) {
        return comChannelDatMapper.selectCount(new LambdaQueryWrapper<ComChannelDat>()
                .eq(ComChannelDat::getDeleteFlag, false)
                .eq(ComChannelDat::getComId, comId)
                .eq(ComChannelDat::getGroupId, groupId));
    }

    private Integer analysisGroupData(List<GetAllGroupsVo> resultList, List<ComChannelGroup> groupList, Long parentId, Long ppId) {
        int total = 0;
        for (ComChannelGroup group : groupList) {
            // 遍历组列表，找到parentId匹配的组
            if (group.getParentId().equals(parentId)) {
                int nowTotal = 0;
                GetAllGroupsVo getChannelGroupAllVo = new GetAllGroupsVo();
                BeanUtils.copyProperties(group, getChannelGroupAllVo);
                getChannelGroupAllVo.setChildren(new ArrayList<>());
                getChannelGroupAllVo.setTitle(group.getName());
                if (parentId == 0 || ppId == 0) {
                    getChannelGroupAllVo.setIsAdd(true);
                } else {
                    getChannelGroupAllVo.setIsAdd(false);
                }
                // 获取子组的总数
                Integer childrenTotal = this.analysisGroupData(getChannelGroupAllVo.getChildren(), groupList, getChannelGroupAllVo.getId(), parentId);
                // 计算当前组及其子组的总数
                nowTotal = getChannelGroupAllVo.getTotalItem() + childrenTotal;
                // 计算所有子组的总数
                total += nowTotal;
                getChannelGroupAllVo.setTotalItem(nowTotal);
                resultList.add(getChannelGroupAllVo);
            }
        }
        // 按创建时间和ID对resultList进行排序
//        resultList.sort(Comparator.comparing(GetAllGroupsVo::getCreateAt));
        return total;
    }


    /**
     * 获取所有的分组信息
     *
     * @param comId
     * @param accountId
     * @param isAdmin
     * @return
     */
    private R getGroupsInfo(Long comId, Long accountId, Boolean isAdmin) {
        if (isAdmin) {
            // 查询数据库获取公司的所有频道分组信息
            List<ComChannelGroup> comChannelGroups = this.baseMapper.selectListAndTotal(comId);
            return getGroupList(comId, comChannelGroups, true);
        } else {
            Boolean isDefault = false;
            // 查看是否为渠道管理员
            Set<Long> groupByAdmin = channelGroupAdminMapper.getGroupByAdmin(comId, accountId);
            // 不是渠道管理员
            if (ObjectUtil.isEmpty(groupByAdmin)) {
                Set<Long> groupIdList = comChannelDatMapper.getGroupIdList(comId, accountId);
                List<ComChannelGroup> comChannelGroups = new ArrayList<>();
                // 存在绑定渠道
                if (ObjectUtil.isNotEmpty(groupIdList)) {
                    if (groupIdList.contains(Constants.DEFAULT_GROUP_VALUE)) {
                        isDefault = true;
                        groupIdList.remove(Constants.DEFAULT_GROUP_VALUE);
                    } else {
                        groupIdList = groupIdList(groupIdList, comId);
                        comChannelGroups = this.baseMapper.selectListAndTotalByList(comId, accountId, groupIdList);
                    }
//                    List<ComChannelGroup> comChannelGroups = new ArrayList();
                    return getGroupList(comId, comChannelGroups, isDefault);
                }
            } else {
                HashSet<Long> groupIds = new HashSet<>();
                for (Long groupId : groupByAdmin) {
                    groupIds.add(groupId);
                    if (!groupId.equals(Constants.DEFAULT_GROUP_VALUE)) {
                        // 分组id不为第一级分组的情况下，取上级分组
                        ComChannelGroup comChannelGroup = this.baseMapper.selectById(groupId);
                        if (ObjectUtil.isNotEmpty(comChannelGroup)) {
                            Long parentId = comChannelGroup.getParentId();
                            if (parentId == 0) {
                                /**
                                 * 获取所有的分组id
                                 */
                                List<Long> childrenIds = this.baseMapper.getChildrenIds(groupId, comId);
                                if (ObjectUtil.isNotEmpty(childrenIds)) {
                                    for (Long childrenId : childrenIds) {
                                        groupIds.add(childrenId);
                                        List<Long> childrenChildrenIds = this.baseMapper.getChildrenIds(groupId, comId);
                                        if (ObjectUtil.isNotEmpty(childrenChildrenIds)) {
                                            for (Long childrenChildrenId : childrenChildrenIds) {
                                                groupIds.add(childrenChildrenId);
                                            }
                                        }
                                    }
                                }
                            } else {
                                ComChannelGroup comChannelGroup1 = this.baseMapper.selectById(parentId);
                                if (ObjectUtil.isNotEmpty(comChannelGroup1)) {
                                    Long parentId1 = comChannelGroup1.getParentId();
                                    if (parentId1 == 0) {
                                        groupIds.add(parentId);
                                        List<Long> childrenIds = this.baseMapper.getChildrenIds(groupId, comId);
                                        if (ObjectUtil.isNotEmpty(childrenIds)) {
                                            for (Long childrenId : childrenIds) {
                                                groupIds.add(childrenId);
                                            }
                                        }
                                    } else {
                                        ComChannelGroup comChannelGroup2 = this.baseMapper.selectById(parentId);
                                        if (ObjectUtil.isNotEmpty(comChannelGroup2)) {
                                            Long parentId2 = comChannelGroup2.getParentId();
                                            groupIds.add(parentId2);
                                        }
                                    }
                                }
                            }
                            // 获取所有的子id
                            List<Long> childrenIds = this.baseMapper.getChildrenIds(groupId, comId);
                            if (ObjectUtil.isNotEmpty(childrenIds)) {
                                for (Long childrenId : childrenIds) {
                                    groupIds.add(childrenId);
                                    List<Long> childrenChildrenIds = this.baseMapper.getChildrenIds(groupId, comId);
                                    if (ObjectUtil.isNotEmpty(childrenChildrenIds)) {
                                        for (Long childrenChildrenId : childrenChildrenIds) {
                                            groupIds.add(childrenChildrenId);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Set<Long> groupIdList = comChannelDatMapper.getGroupIdList(comId, accountId);
                if (groupIdList.contains(Constants.DEFAULT_GROUP_VALUE)) {
                    isDefault = true;
                    groupIdList.remove(Constants.DEFAULT_GROUP_VALUE);
                }
                if (ObjectUtil.isNotEmpty(groupIdList)) {
                    groupIdList = this.groupIdList(groupIdList, comId);
                    for (Long l : groupIdList) {
                        groupIds.add(l);
                    }
                }
                // 存在绑定渠道
                if (ObjectUtil.isNotEmpty(groupIds)) {

                    List<ComChannelGroup> comChannelGroups = this.baseMapper.selectListAndTotalByList(comId, null, groupIds);
//                            .eq(ComChannelGroup::getDeleteFlag, false).eq(ComChannelGroup::getComId, comId).in(ComChannelGroup::getId, groupIds));
                    return getGroupList(comId, comChannelGroups, isDefault);
                }
            }
            return R.ok();
        }
    }


    /**
     * 获取所有的父级id
     *
     * @param ids
     * @param comId
     * @return
     */
    private Set<Long> groupIdList(Set<Long> ids, Long comId) {

        List<ComChannelGroup> comChannelGroups = this.baseMapper.selectList(new LambdaQueryWrapper<ComChannelGroup>()
                .in(ComChannelGroup::getId, ids).ne(ComChannelGroup::getParentId, 0).eq(ComChannelGroup::getComId, comId));
        for (ComChannelGroup comChannelGroup : comChannelGroups) {
            Long parentId = comChannelGroup.getParentId();
            ids.add(parentId);
            ComChannelGroup comChannelGroup1 = this.baseMapper.selectById(parentId);
            if (ObjectUtil.isNotEmpty(comChannelGroup1)) {
                Long parentId1 = comChannelGroup1.getParentId();

                if (parentId1 != 0) {
                    ids.add(parentId1);
                }
            }
        }
        return ids;
    }


    /**
     * 处理分组信息
     *
     * @param comId
     * @param comChannelGroups
     * @param isDefault
     * @return
     */
    private R getGroupList(Long comId, List<ComChannelGroup> comChannelGroups, Boolean isDefault) {
        // 用于存储处理后的分组信息
        List<GetAllGroupsVo> resultList = new ArrayList<>();
        // 若查询结果为null，则初始化为一个空列表
        if (comChannelGroups == null) {
            comChannelGroups = new ArrayList<>();
        }
        // 分析处理获取到的分组数据并统计总数
        Integer i = this.analysisGroupData(resultList, comChannelGroups, 0L, 0L);
        // 创建响应对象
        GetAllDataGroupsVo vo = new GetAllDataGroupsVo();
        if (isDefault) {
            // 计算默认组的数量
            Long defaultCount = this.getGroupCount(comId, Constants.DEFAULT_GROUP_VALUE);
            // 创建默认分组对象
            GetAllGroupsVo defaultGroup = new GetAllGroupsVo();
            defaultGroup.setId(Constants.DEFAULT_GROUP_VALUE);
            defaultGroup.setTitle(Constants.DEFAULT_GROUP);
            defaultGroup.setIsAdd(false);
            defaultGroup.setComId(comId);
            defaultGroup.setChildren(new ArrayList<>());
            defaultGroup.setTotalItem(defaultCount.intValue());
            // 将默认分组信息添加到处理后的列表的首位
            resultList.add(0, defaultGroup);
            vo.setTotal(i + defaultCount);
        } else {
            vo.setTotal(i.longValue());
        }
        vo.setTreeList(resultList);
        return R.ok(vo);
    }
}
