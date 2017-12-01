package cn.godsdo.dubbo.impl.channel;

import cn.godsdo.constant.MongoConstant;
import cn.godsdo.dto.LiveWatchRecordDto;
import cn.godsdo.dto.channel.AddGroupAdminDto;
import cn.godsdo.dto.channel.GetAccountChannelListDto;
import cn.godsdo.dto.channel.GetChannelListDto;
import cn.godsdo.dto.channel.UpdateChannelDto;
import cn.godsdo.dubbo.channel.ComChannelDatService;
import cn.godsdo.dubbo.live.LiveDatService;
import cn.godsdo.entity.channel.ChannelLiveInfo;
import cn.godsdo.entity.channel.ComChannelDat;
import cn.godsdo.entity.channel.ComChannelGroup;
import cn.godsdo.mapper.channel.ChannelGroupAdminMapper;
import cn.godsdo.mapper.channel.ChannelLiveInfoMapper;
import cn.godsdo.mapper.channel.ComChannelDatMapper;
import cn.godsdo.mapper.channel.ComChannelGroupMapper;
import cn.godsdo.util.R;
import cn.godsdo.vo.channel.GetAccountChannelListVo;
import cn.godsdo.vo.channel.GetChannelListVo;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.constant.Constants;
import com.y20y.interfaces.IdService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * <p>
 * 《渠道信息表》 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2019-04-22
 */
@DubboService
public class ComChannelDatServiceImpl extends ServiceImpl<ComChannelDatMapper, ComChannelDat> implements ComChannelDatService {

    @DubboReference
    IdService idService;
    @Resource
    ComChannelGroupMapper comChannelGroupMapper;
    @Resource
    ChannelGroupAdminMapper channelGroupAdminMapper;
    @Resource
    ChannelLiveInfoMapper channelLiveInfoMapper;
    @DubboReference
    LiveDatService liveDatService;
    @Value(value = "${WATCH_ROOM_KEY}")
    public String watchRoomKey;
    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public R addChannel(Long comId, Long accountId, String name, Long groupId, Long userId) {

        // 创建 ComChannelDat 对象并设置属性
        ComChannelDat comChannelDat = new ComChannelDat();
        comChannelDat.setId(idService.nextId());
        comChannelDat.setComId(comId);
        comChannelDat.setAccountId(userId);
        comChannelDat.setChannelNo(getRandomNum(comId));
        comChannelDat.setName(name);
        comChannelDat.setGroupId(groupId);
        comChannelDat.setCreateBy(accountId);

        // 将 comChannelDat 对象插入数据库
        this.baseMapper.insert(comChannelDat);

        return R.ok(comChannelDat);
    }

    @Override
    public R deleteChannel(Long comId, Long accountId, Long id) {
        // 从数据库中删除指定条件的渠道数据
        ComChannelDat comChannelDat = this.baseMapper.selectOne(new LambdaQueryWrapper<ComChannelDat>()
                .eq(ComChannelDat::getComId, comId).eq(ComChannelDat::getId, id).eq(ComChannelDat::getDeleteFlag, false));
        if (ObjectUtil.isNull(comChannelDat)) {
            return R.failed("渠道不存在");
        }
        // 删除所有渠道绑定的直播间
        channelLiveInfoMapper.update(null, new LambdaUpdateWrapper<ChannelLiveInfo>()
                .set(ChannelLiveInfo::getDeleteFlag, true)
                .set(ChannelLiveInfo::getUpdateBy, accountId)
                .eq(ChannelLiveInfo::getDeleteFlag, false)
                .eq(ChannelLiveInfo::getComId, comId)
                .eq(ChannelLiveInfo::getChannelId, id)
        );
        // 将渠道数据的删除标志设置为true，并更新删除者的账户ID
        comChannelDat.setDeleteFlag(true);
        comChannelDat.setUpdateBy(accountId);
        // 更新渠道数据
        this.baseMapper.updateById(comChannelDat);

        return R.ok();
    }

    @Override
    public R updateChannel(Long comId, Long accountId, UpdateChannelDto dto) {
        // 根据渠道ID和dto的ID查询ComChannelDat对象
        ComChannelDat comChannelDat = this.baseMapper.selectOne(new LambdaQueryWrapper<ComChannelDat>()
                .eq(ComChannelDat::getComId, comId).eq(ComChannelDat::getId, dto.getId()).eq(ComChannelDat::getDeleteFlag, false));
        // 如果comChannelDat为空，则返回渠道不存在的错误信息
        if (ObjectUtil.isNull(comChannelDat)) {
            return R.failed("渠道不存在");
        }
        // 更新comChannelDat的名称、组ID、账户ID和更新者ID
        comChannelDat.setName(dto.getName());
        comChannelDat.setGroupId(dto.getGroupId());
        comChannelDat.setAccountId(dto.getUserId());
        comChannelDat.setUpdateBy(accountId);
        // 通过ID更新comChannelDat对象
        this.baseMapper.updateById(comChannelDat);
        return R.ok();
    }

    @Override
    public R getChannelList(Long comId, Long accountId, Boolean isAdmin, GetChannelListDto dto) {
        Long groupId = dto.getGroupId();
        List<Long> ids = new ArrayList<>();
        Boolean isGroupAdmin = false;
        if (isAdmin) {
            isGroupAdmin = true;
            // 不是默认分组的情况下，需要拉取下一级分组的权限
            ids = getIds(comId, groupId, ids);
            if (ObjectUtil.isEmpty(ids)) {
                return R.failed("分组信息不存在！！！");
            }
        } else {
            Set<Long> groupByAdmin = channelGroupAdminMapper.getGroupByAdmin(comId, accountId);
            if (ObjectUtil.isNotEmpty(groupByAdmin)) {
                // 是管理员
                if (groupByAdmin.contains(groupId)) {
                    isGroupAdmin = true;
//                    ids = getIds(comId, groupId, ids);
//                    if(ObjectUtil.isEmpty(ids)){
//                        return R.failed("分组信息不存在！！！");
//                    }

                } else if (!Constants.DEFAULT_GROUP_VALUE.equals(groupId)) {
                    // 判断是否为父级管理员
                    ComChannelGroup comChannelGroup = comChannelGroupMapper.selectById(groupId);
                    if (ObjectUtil.isEmpty(comChannelGroup)) {
                        return R.failed("分组信息不存在！！！");
                    } else {
                        Long parentId = comChannelGroup.getParentId();
                        if (parentId != 0) {
                            ComChannelGroup comChannelGroup1 = comChannelGroupMapper.selectById(parentId);
                            if (ObjectUtil.isEmpty(comChannelGroup1)) {
                                return R.failed("分组信息不存在！！！");
                            }
                            if (groupByAdmin.contains(parentId) || groupByAdmin.contains(comChannelGroup1.getParentId())) {
                                isGroupAdmin = true;

                            }
                        }

                    }
                }
                if (isGroupAdmin) {
                    ids = getIds(comId, groupId, ids);
                    if (ObjectUtil.isEmpty(ids)) {
                        return R.failed("分组信息不存在！！！");
                    }
                }
            } else {
                // 不是管理员的情况下，点击上级分组，获取子集分组的数据
                List<Long> childrenIds = comChannelGroupMapper.getChildrenIds(groupId, comId);
                ids.add(groupId);
                if (ObjectUtil.isNotEmpty(childrenIds)) {
                    ids.addAll(childrenIds);
                    for (Long childrenId : childrenIds) {
                        List<Long> childrenIds1 = comChannelGroupMapper.getChildrenIds(childrenId, comId);
                        if (ObjectUtil.isNotEmpty(childrenIds1)) {
                            ids.addAll(childrenIds1);
                            for (Long l : childrenIds1) {
                                List<Long> childrenIds2 = comChannelGroupMapper.getChildrenIds(l, comId);
                                if (ObjectUtil.isNotEmpty(childrenIds2)) {
                                    ids.addAll(childrenIds2);
                                }
                            }
                        }
                    }
                }
            }
        }

        dto.setChannelGroupIds(ids);
//        dto.setIsGroupAdmin(isGroupAdmin);
        Page<ComChannelDat> page = new Page<>(dto.getPage(), dto.getPageSize());
        List<GetChannelListVo> records = new ArrayList<>();
        Long total = 0L;
        if (isGroupAdmin) {
            IPage<GetChannelListVo> vo = this.baseMapper.getChannelListByAdmin(page, dto, comId, accountId);
            records = vo.getRecords();
            total = vo.getTotal();
        } else {
            IPage<GetChannelListVo> vo = this.baseMapper.getChannelList(page, dto, comId, accountId);
            records = vo.getRecords();
            total = vo.getTotal();
        }
        List<GetChannelListVo> list = records.stream().map(e -> {
            Criteria criteria = Criteria.where("comId").is(comId).and("channelId").is(e.getId());
            Query query = new Query(criteria);
            Long sum = mongoTemplate.count(query, LiveWatchRecordDto.class, MongoConstant.LIVE_WATCH_RECORD_USER);
            e.setSum(sum);
            e.setChannelInfo(getChannelInfo(e.getGroupId()));
            return e;
        }).toList();

        return R.ok(list, total);
    }

    /**
     * 获取渠道所有的分组名称
     * @param groupId
     * @return
     */
    private String getChannelInfo(Long groupId) {
        if (Constants.DEFAULT_GROUP_VALUE.equals(groupId)) {
            return Constants.DEFAULT_GROUP;
        }
        ComChannelGroup comChannelGroup = comChannelGroupMapper.selectById(groupId);
        if (ObjectUtil.isNotEmpty(comChannelGroup)) {
            Long parentId = comChannelGroup.getParentId();
            if (parentId == 0) {
                return comChannelGroup.getName();
            } else {
                ComChannelGroup comChannelGroup1 = comChannelGroupMapper.selectById(parentId);
                if (ObjectUtil.isNotEmpty(comChannelGroup1)) {
                    Long parentId1 = comChannelGroup1.getParentId();
                    if (parentId1 == 0) {
                        return comChannelGroup1.getName() + "->" + comChannelGroup.getName();
                    } else {
                        ComChannelGroup comChannelGroup2 = comChannelGroupMapper.selectById(parentId1);
                        if (ObjectUtil.isNotEmpty(comChannelGroup2)) {
                            return comChannelGroup2.getName() +  "->"  + comChannelGroup1.getName() +  "->"  + comChannelGroup.getName();
                        }
                    }
                }
            }

        }
        return "";
    }

    private @Nullable List<Long> getIds(Long comId, Long groupId, List<Long> ids) {
        if (!Constants.DEFAULT_GROUP_VALUE.equals(groupId)) {
            ComChannelGroup comChannelGroup = comChannelGroupMapper.selectById(groupId);
            if (ObjectUtil.isNotEmpty(comChannelGroup)) {
                Long parentId = comChannelGroup.getParentId();
                if (parentId == 0) {
                    List<Long> childrenIds = comChannelGroupMapper.getChildrenIds(groupId, comId);
                    ids.addAll(childrenIds);
                    for (Long childrenId : childrenIds) {
                        List<Long> childrenIds1 = comChannelGroupMapper.getChildrenIds(childrenId, comId);
                        ids.addAll(childrenIds1);
                    }
                } else {
                    List<Long> childrenIds = comChannelGroupMapper.getChildrenIds(groupId, comId);
                    ids.addAll(childrenIds);
                }
                ids.add(groupId);
            } else {
                return null;
            }
        } else {
            ids.add(groupId);
        }
        return ids;
    }

    @Override
    public R moveComChannelGroupByList(AddGroupAdminDto dto, Long comId, Long accountId) {
        // 获取DTO中的ids列表
        List<Long> ids = dto.getIds();
        // 若ids列表为空，则返回移动失败信息
        if (ObjectUtil.isEmpty(ids)) {
            return R.failed("请选中需要移动的渠道");
        }

        // 根据comId、dto中的ids以及deleteFlag筛选符合条件的渠道数据
        List<ComChannelDat> comChannelDats = this.baseMapper.selectList(
                new LambdaQueryWrapper<ComChannelDat>()
                        .eq(ComChannelDat::getComId, comId)
                        .in(ComChannelDat::getId, ids)
                        .eq(ComChannelDat::getDeleteFlag, false)
        );

        // 若筛选后的渠道数据为空，则返回移动失败信息
        if (ObjectUtil.isEmpty(comChannelDats)) {
            return R.failed("请先选择需要移动的渠道");
        }

        // 遍历筛选后的渠道数据列表，更新对应的分组ID和更新者账户ID
        for (ComChannelDat comChannelDat : comChannelDats) {
            comChannelDat.setGroupId(dto.getGroupId());
            comChannelDat.setUpdateBy(accountId);
        }

        // 批量更新渠道数据
        this.updateBatchById(comChannelDats);
        return R.ok();
    }

    @Override
    public R getAccountChannelList(GetAccountChannelListDto dto, Long comId, Long accountId) {
        Page<ComChannelDat> page = new Page<>(dto.getPage(), dto.getPageSize());
        IPage<GetAccountChannelListVo> vo = this.baseMapper.getAccountChannelList(page, dto, comId, accountId);
        return R.ok(vo.getRecords(), vo.getTotal());
    }


    @Override
    public R getChannelListByAccount(Long comId, Long accountId) {
        List<ComChannelDat> list = this.baseMapper.selectList(new LambdaQueryWrapper<ComChannelDat>()
                .select(ComChannelDat::getId, ComChannelDat::getName)
                .eq(ComChannelDat::getComId, comId)
                .eq(ComChannelDat::getDeleteFlag, false)
                .eq(ComChannelDat::getAccountId, accountId));
//        ComChannelDat comChannelDat = new ComChannelDat();
//        comChannelDat.setId(0L);
//        comChannelDat.setName("全部");
//        list.add(comChannelDat);
        return R.ok(list);
    }

    @Override
    public R getAllChannelByLiveId(Long liveId, Long comId) {
        List<ComChannelDat> list = this.baseMapper.getAllChannelByLiveId(comId, liveId);
        return R.ok(list);
    }

    @Override
    public List<ComChannelDat> getChannelListByGroupIds(List<Long> principalList) {
        return this.baseMapper.selectList(new LambdaQueryWrapper<ComChannelDat>()
                .select(ComChannelDat::getId)
                .eq(ComChannelDat::getDeleteFlag, false)
                .in(ComChannelDat::getGroupId, principalList));
    }

    @Override
    public R getNotBindChannel(Long comId, Long groupId, Long liveId) {
        if (groupId != 0 && !Constants.DEFAULT_GROUP_VALUE.equals(groupId)) {
            // 根据groupId查询ComChannelGroup
            ComChannelGroup comChannelGroup = comChannelGroupMapper.selectById(groupId);
            if (ObjectUtil.isEmpty(comChannelGroup)) {
                return R.ok();
            }
            List<Long> ids = getChannelIds(comId, groupId, comChannelGroup.getParentId());
            // 获取绑定到直播组的频道列表
            List<Long> bindChannelByLiveGroup = channelLiveInfoMapper.getNotBindChannelByLiveGroupList(comId, liveId, ids);
            if (ObjectUtil.isEmpty(bindChannelByLiveGroup)) {
                return R.ok();
            }
            List<ComChannelDat> comChannelDats = this.baseMapper.selectList(new LambdaQueryWrapper<ComChannelDat>().eq(ComChannelDat::getComId, comId).eq(ComChannelDat::getDeleteFlag, false)
                    .in(ComChannelDat::getGroupId, ids).in(ObjectUtil.isNotEmpty(bindChannelByLiveGroup), ComChannelDat::getId, bindChannelByLiveGroup));
            return R.ok(comChannelDats);
        } else {
            // 获取绑定到直播组的频道列表
            List<Long> bindChannelByLiveGroup = channelLiveInfoMapper.getBindChannelByLiveGroup(comId, liveId, groupId);
            List<ComChannelDat> comChannelDats = this.baseMapper.selectList(new LambdaQueryWrapper<ComChannelDat>().eq(ComChannelDat::getComId, comId).eq(ComChannelDat::getDeleteFlag, false)
                    .eq(groupId != 0, ComChannelDat::getGroupId, groupId).notIn(ObjectUtil.isNotEmpty(bindChannelByLiveGroup), ComChannelDat::getId, bindChannelByLiveGroup));
            return R.ok(comChannelDats);
        }
    }


    @Override
    public R getBindChannelByLiveId(Long comId, Long groupId, Long liveId) {
        if (groupId != 0 && !Constants.DEFAULT_GROUP_VALUE.equals(groupId)) {
            ComChannelGroup comChannelGroup = comChannelGroupMapper.selectById(groupId);
            if (ObjectUtil.isEmpty(comChannelGroup)) {
                return R.ok();
            }
            List<Long> ids = getChannelIds(comId, groupId, comChannelGroup.getParentId());
            // 获取绑定到直播组的频道列表
            List<Long> bindChannelByLiveGroup = channelLiveInfoMapper.getBindChannelByLiveGroupList(comId, liveId, ids);
            if (ObjectUtil.isEmpty(bindChannelByLiveGroup)) {
                return R.ok();
            }
            List<ComChannelDat> comChannelDats = this.baseMapper.selectList(new LambdaQueryWrapper<ComChannelDat>().eq(ComChannelDat::getComId, comId).eq(ComChannelDat::getDeleteFlag, false)
                    .in(ComChannelDat::getGroupId, ids).in(ComChannelDat::getId, bindChannelByLiveGroup));
            return R.ok(comChannelDats);
        } else {
            List<Long> bindChannelByLiveGroup = channelLiveInfoMapper.getBindChannelByLiveGroup(comId, liveId, groupId);
            if (ObjectUtil.isEmpty(bindChannelByLiveGroup)) {
                return R.ok();
            }
            List<ComChannelDat> comChannelDats = this.baseMapper.selectList(new LambdaQueryWrapper<ComChannelDat>().eq(ComChannelDat::getComId, comId).eq(ComChannelDat::getDeleteFlag, false)
                    .in(ComChannelDat::getId, bindChannelByLiveGroup));
            return R.ok(comChannelDats);
        }

    }


    /**
     * 生成随机渠道号
     *
     * @return
     */
    public String getRandomNum(Long comId) {
        // 生成随机数作为通道号
        int i = ThreadLocalRandom.current().nextInt(90000) + 10000;
        // 查询数据库检查通道号是否已存在
        ComChannelDat comChannelDat = this.baseMapper.selectOne(new LambdaQueryWrapper<ComChannelDat>()
                .eq(ComChannelDat::getComId, comId).eq(ComChannelDat::getChannelNo, i).eq(ComChannelDat::getDeleteFlag, false));
        // 如果通道号已存在，则递归调用获取新的随机数
        if (ObjectUtil.isNotEmpty(comChannelDat)) {
            return getRandomNum(comId);
        }
        // 返回未被占用的随机数作为通道号
        return String.valueOf(i);

    }

    private List<Long> getChannelIds(Long comId, Long groupId, Long parentId) {
        List<Long> ids = new ArrayList<>();
        ids.add(groupId);
        if (parentId == 0) {
            // 获取子节点ID列表
            List<Long> childrenIds = comChannelGroupMapper.getChildrenIds(groupId, comId);
            if (ObjectUtil.isNotEmpty(childrenIds)) {
                ids.addAll(childrenIds);
                for (Long childrenId : childrenIds) {
                    List<Long> childrenIds1 = comChannelGroupMapper.getChildrenIds(childrenId, comId);
                    if (ObjectUtil.isNotEmpty(childrenIds1)) {
                        ids.addAll(childrenIds1);
                        for (Long childrenId1 : childrenIds1) {
                            List<Long> childrenIds2 = comChannelGroupMapper.getChildrenIds(childrenId1, comId);
                            if (ObjectUtil.isNotEmpty(childrenIds2)) {
                                ids.addAll(childrenIds2);
                            }
                        }
                    }
                }
            }

        } else {
            // 获取子节点ID列表
            List<Long> childrenIds = comChannelGroupMapper.getChildrenIds(groupId, comId);
            if (ObjectUtil.isNotEmpty(childrenIds)) {
                ids.addAll(childrenIds);
                for (Long childrenId : childrenIds) {
                    List<Long> childrenIds1 = comChannelGroupMapper.getChildrenIds(childrenId, comId);
                    if (ObjectUtil.isNotEmpty(childrenIds1)) {
                        ids.addAll(childrenIds1);
                    }

                }
            }
        }
        return ids;
    }

}
