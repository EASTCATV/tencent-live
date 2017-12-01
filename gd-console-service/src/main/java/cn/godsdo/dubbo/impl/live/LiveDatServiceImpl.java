package cn.godsdo.dubbo.impl.live;

import cn.godsdo.base.BasePage;
import cn.godsdo.constant.MaxOnlineConstants;
import cn.godsdo.constant.MongoConstant;
import cn.godsdo.constant.RedisConstants;
import cn.godsdo.dto.assistan.SendNoticeDto;
import cn.godsdo.dto.live.AddLiveDto;
import cn.godsdo.dto.live.GetLiveListDto;
import cn.godsdo.dto.live.tencent.PushStreamDTO;
import cn.godsdo.dto.live.tencent.StopPublishCdnStreamDto;
import cn.godsdo.dto.msg.LiveUserStateChange;
import cn.godsdo.dubbo.LiveRecordDatService;
import cn.godsdo.dubbo.cache.ClearCache;
import cn.godsdo.dubbo.camp.TrainingCampLiveService;
import cn.godsdo.dubbo.com.BlackService;
import cn.godsdo.dubbo.com.ComDefultSettingService;
import cn.godsdo.dubbo.com.ComUrlConfigService;
import cn.godsdo.dubbo.crossService.RedisDubboService;
import cn.godsdo.dubbo.im.ImService;
import cn.godsdo.dubbo.live.LiveDatService;
import cn.godsdo.dubbo.live.LiveRoomCdnService;
import cn.godsdo.dubbo.live.LiveTagService;
import cn.godsdo.dubbo.live.TliveService;
import cn.godsdo.dubbo.tencent.TencentTaskService;
import cn.godsdo.entity.AccountDat;
import cn.godsdo.entity.LiveRecordDat;
import cn.godsdo.entity.TencentTask;
import cn.godsdo.entity.camp.TrainingCampLive;
import cn.godsdo.entity.channel.ChannelLiveInfo;
import cn.godsdo.entity.com.ComDefultSetting;
import cn.godsdo.entity.com.ComLiveVideo;
import cn.godsdo.entity.com.ComUrlConfig;
import cn.godsdo.entity.com.LiveBackVideo;
import cn.godsdo.entity.coupon.ComCouponLiveDat;
import cn.godsdo.entity.im.CreateImGroup;
import cn.godsdo.entity.im.TencentImUserSign;
import cn.godsdo.entity.intelligent.IntelligentLiveRecord;
import cn.godsdo.entity.live.*;
import cn.godsdo.enums.CdnPullTypeEnum;
import cn.godsdo.enums.DataPermissionsEnun;
import cn.godsdo.enums.camp.LiveTypeEnum;
import cn.godsdo.enums.im.TIMSendTypeEnum;
import cn.godsdo.enums.live.*;
import cn.godsdo.mapper.AccountDatMapper;
import cn.godsdo.mapper.AccountProjectDatMapper;
import cn.godsdo.mapper.camp.TrainingCampLiveMapper;
import cn.godsdo.mapper.channel.ChannelLiveInfoMapper;
import cn.godsdo.mapper.com.ComAccountProjectMapper;
import cn.godsdo.mapper.com.LiveBackVideoMapper;
import cn.godsdo.mapper.coupon.ComCouponLiveDatMapper;
import cn.godsdo.mapper.intelligent.IntelligentLiveRecordMapper;
import cn.godsdo.mapper.live.*;
import cn.godsdo.query.im.ImMsgQuery;
import cn.godsdo.util.R;
import cn.godsdo.util.ShortUtil;
import cn.godsdo.util.im.ImUtil;
import cn.godsdo.util.tencent.ImHelperUtil;
import cn.godsdo.vo.ComOnlineSumVo;
import cn.godsdo.vo.GetHomeInfoVo;
import cn.godsdo.vo.assistant.GetRoomDatByAssistanVo;
import cn.godsdo.vo.live.*;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONObject;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencentyun.TLSSigAPIv2;
import com.y20y.constant.Constants;
import com.y20y.interfaces.IdService;
import com.y20y.pushFlow.Obs;
import com.y20y.trtc.TencentLiveCloudUtil;
import com.y20y.utils.DesHelper;
import com.y20y.utils.ToBase62;
import io.github.doocs.im.constant.ApplyJoinOption;
import io.github.doocs.im.constant.GroupType;
import io.github.doocs.im.model.request.CreateGroupRequest;
import io.github.doocs.im.model.response.CreateGroupResult;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static cn.godsdo.constant.RedisConstants.INTELLIGENT_LIVE_STATUS;
import static com.baomidou.mybatisplus.core.toolkit.ObjectUtils.isNotNull;

/**
 * <p>
 * 房间基础信息表 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2018/11/18
 */
@Slf4j
@DubboService
@RefreshScope
public class LiveDatServiceImpl extends ServiceImpl<LiveDatMapper, LiveDat> implements LiveDatService {

    @DubboReference(check = false)
    ComDefultSettingService comDefultSettingService;
    @DubboReference(check = false)
    LiveRoomCdnService liveRoomCdnService;
    @DubboReference(check = false)
    private TliveService tliveService;
    @DubboReference(check = false)
    LiveTagService liveTagService;
    @DubboReference
    IdService idService;
    @DubboReference(check = false)
    ComUrlConfigService comUrlConfigService;
    @DubboReference(check = false)
    ClearCache clearcache;
    @DubboReference
    RedisDubboService redisService;
    @DubboReference
    BlackService blackService;

    @Resource
    LiveConfigMapper liveConfigMapper;
    @Resource
    LiveTagMapper liveTagMapper;
    @Resource
    ComLiveVideoMapper comLiveVideoMapper;
    @Resource
    LiveMarqueeMapper liveMarqueeMapper;
    @Resource
    LiveDatMapper liveDatMapper;
    @Resource
    LiveWatchChatMapper liveWatchChatMapper;
    @Resource
    LiveCountDownMapper liveCountDownMapper;
    @Resource
    LiveCommodityMapper liveCommodityMapper;
    @Resource
    ChannelLiveInfoMapper channelLiveInfoMapper;
    @Resource
    AccountDatMapper accountDatMapper;
    @Resource
    TrtcInfoMapper trtcInfoMapper;
    @Resource
    ComAccountProjectMapper comAccountProjectMapper;
    @Resource
    LiveBackVideoMapper liveBackVideoMapper;
    @Resource
    LiveShareMapper liveShareMapper;
    @Resource
    IntelligentLiveRecordMapper intelligentLiveRecordMapper;
    @Value(value = "${defaultRequestHeader}")
    public String defaultRequestHeader;
    @Value(value = "${requestAuthUrl}")
    public String requestAuthUrl;
    @Value(value = "${defaultRequestAppId}")
    public String defaultRequestAppId;
    @Value(value = "${shortDomain}")
    public String shortDomain;
    @Value(value = "${WATCH_ROOM_KEY}")
    public String watchRoomKey;
    @Value(value = "${CREATE_GROUP_URL}")
    public String createGroupUrl;
    @Value(value = "${LINK_API_KEY}")
    public String linkApiKey;
    @Value(value = "${Tencent.trtc.appname}")
    public String trtcAppName;
    @Value(value = "${Tencent.live.obsKey}")
    public String obsKey;
    @Value(value = "${Tencent.live.tpush}")
    public String tpush;
    @Resource
    @Qualifier("TimBean")
    private ImHelperUtil imhelperutil;

    @Value("${Tencent.im.appId}")
    private long sdkAppId;

    @Value("${Tencent.im.key}")
    private String key;
    @DubboReference(check = false, retries = 0)
    private ImService imService;

    @DubboReference(check = false)
    private LiveRecordDatService liveRecordDatService;

    @Autowired
    private MongoTemplate mongoTemplate;
    @DubboReference(check = false)
    TencentTaskService tencentTaskService;

    @DubboReference(check = false)
    private TrainingCampLiveService trainingCampLiveService;


    @Resource
    private TrainingCampLiveMapper trainingCampLiveMapper;

    @Resource
    private AccountProjectDatMapper accountProjectDatMapper;
    @Resource
    private ComCouponLiveDatMapper comCouponLiveDatMapper;
    @Resource
    private LiveBindBackMapper liveBindBackMapper;

    /**
     * 仅自己及下属项目数据
     * https://wx1a542d1ec1dae7aa.sx.godsdo.com/A12DDEACC3181BBA567A334B56416478FFDA2C65D3E2C1EA
     * https://wx181f4bf416f0313a.sx.godsdo.com/17qOb0zvna
     */

//    所有项目 等同于admin权限
    // 本项目及下属项目 自己所属项目的直播间，及所有项目的直播间，
    // 获取项目ID，然后判断父级id时候为0，如果不是0查询一次，把下属的项目ID查出来
    // 如果=0查询两次把所有的id查出来
    // 自定义   
    // 从表里查询所有的项目ID，
    @Override
    public R getLiveList(Long comId, Long accountId, Boolean isAdmin, GetLiveListDto dto) {
        Long page = dto.getPage();
        if (page == 1) {
            refreshLiveKeyByRedis(comId);
        }
        // 当不是管理员账号的时候
        if (!isAdmin) {
//            // 根据账户ID查询账户信息
            AccountDat accountDat = accountDatMapper.selectById(accountId);
            // 获取数据权限
            Integer dataPermissions = accountDat.getDataPermissions();
            List<Long> ids = new ArrayList<>();
            // 本项目及下属项目数据
            if (DataPermissionsEnun.THIS_PROJECT.getValue() == dataPermissions) {
                // 获取项目ID
                Long project = accountDat.getProject();

                if (project != 0) {
                    ids.add(project);
                    // 根据项目ID查询账户项目
//                    ComAccountProject comAccountProject = comAccountProjectMapper.selectById(project);
                    // 获取子项目ID列表
                    List<Long> childrenIds = getChildrenIds(comId, project);
                    if (ObjectUtils.isNotEmpty(childrenIds)) {
                        // 将子项目ID添加到总ID列表中
                        ids.addAll(childrenIds);
                        for (Long childId : childrenIds) {
                            // 获取下一级子项目ID列表
                            List<Long> childrenIds1 = getChildrenIds(comId, childId);
                            if (ObjectUtils.isNotEmpty(childrenIds)) {
                                // 将下一级子项目ID添加到总ID列表中
                                ids.addAll(childrenIds1);
                                for (Long childId1 : childrenIds1) {
                                    // 获取再下一级子项目ID列表
                                    List<Long> childrenIds2 = getChildrenIds(comId, childId1);
                                    if (ObjectUtils.isNotEmpty(childrenIds)) {
                                        // 将再下一级子项目ID添加到总ID列表中
                                        ids.addAll(childrenIds2);
                                    }

                                }
                            }

                        }
                    }
                    dto.setIds(ids);
                }
            } else if (DataPermissionsEnun.CUSTOM.getValue() == dataPermissions) {
                // 自定义权限
                ids = accountProjectDatMapper.getAccountProject(comId, accountId);
                dto.setIds(ids);
            }
//            // 获取项目ID
//            Long project = accountDat.getProject();
//            List<Long> ids = new ArrayList<>();
//            // 如果项目为总项目
//            if (project == 0) {
            // 如果权限为仅自己及下属项目数据
//                if (DataPermissionsEnun.ONESELF.getValue() == dataPermissions) {
//                    // 获取当前项目及下属项目的所有ID
//                    List<Long> childrenIds = getChildrenIds(comId, project);
//                    ids.addAll(childrenIds);
//                    // 遍历下属项目，获取所有ID
//                    for (Long childId : childrenIds) {
//                        List<Long> childrenIds1 = getChildrenIds(comId, childId);
//                        ids.addAll(childrenIds1);
//                        for (Long childId1 : childrenIds1) {
//                            List<Long> childrenIds2 = getChildrenIds(comId, childId1);
//                            ids.addAll(childrenIds2);
//                        }
//                    }
//                    List<Long> accountIdsByProject = new ArrayList<>();
//                    if (ObjectUtils.isNotEmpty(ids)) {
//                        // 查询项目下所有的用户ID
//                        accountIdsByProject = accountDatMapper.getAccountIdByProject(ids, comId);
//                        if (ObjectUtils.isEmpty(accountIdsByProject)) {
//                            accountIdsByProject.add(accountId);
//                        } else {
//                            accountIdsByProject = new ArrayList<>();
//                        }
//                    }
//                    accountIdsByProject.add(accountId);
//
//                    dto.setIds(accountIdsByProject);
//                }
//            } else {
//                ComAccountProject comAccountProject = comAccountProjectMapper.selectById(project);
//                // 判断是否为二级ID
//                if (comAccountProject != null && comAccountProject.getParentId() == 0) {
//                    List<Long> childrenIds = getChildrenIds(comId, project);
//                    ids.addAll(childrenIds);
//                    // 获取下属项目的所有ID
//                    for (Long childId1 : childrenIds) {
//                        List<Long> childrenIds1 = getChildrenIds(comId, childId1);
//                        ids.addAll(childrenIds1);
//                    }
//                } else {
//                    List<Long> childrenIds = getChildrenIds(comId, project);
//                    ids = childrenIds;
//                }
//                if (DataPermissionsEnun.ONESELF.getValue() == dataPermissions) {
//                    List<Long> accountIdsByProject = new ArrayList<>();
//                    if (ObjectUtils.isNotEmpty(ids)) {
//                        accountIdsByProject = accountDatMapper.getAccountIdByProject(ids, comId);
//                    }
//                    // 查询项目下所有的用户ID
//                    accountIdsByProject.add(accountId);
//                    dto.setIds(accountIdsByProject);
//                } else {
//                    // 加入当前项目的ID
//                    ids.add(project);
//                    // 查询项目下所有的用户ID
//                    List<Long> accountIdsByProject = accountDatMapper.getAccountIdByProject(ids, comId);
//                    if (ObjectUtils.isEmpty(accountIdsByProject)) {
//                        accountIdsByProject = new ArrayList<>();
//                        accountIdsByProject.add(accountId);
//                    }
//                    dto.setIds(accountIdsByProject);
//                }
//
//            }
        }

        Page<LiveDat> ipage = new Page<>(dto.getPage(), dto.getPageSize());
        if (StringUtils.isNotBlank(dto.getStartTime())) {
            dto.setStartTime(dto.getStartTime() + " 00:00:01");
            if (StringUtils.isNotBlank(dto.getEndTime())) {
                dto.setEndTime(dto.getEndTime() + " 23:59:59");
            }
        }
        IPage<GetLiveListVo> result = this.baseMapper.getLiveList(ipage, dto, comId);
        List<GetLiveListVo> list = result.getRecords().stream().map(e -> {
            if (LiveStatusEnum.LIVING.getValue() == e.getLiveStatus()) {
                LiveRecordDat liveDat = liveRecordDatService.getLiveDat(comId, e.getLiveId());
                if (ObjectUtils.isNotEmpty(liveDat)) {
                    e.setLiveType(liveDat.getVideoType());
                }
            }
            return e;
        }).toList();
        return R.ok(list, result.getTotal());
    }

    @Override
    public R getLiveListByHome(Long comId, GetLiveListDto dto) {
        Long page = dto.getPage();
//        if (page == 1) {
//            refreshLiveKeyByRedis(comId);
//        }
        Page<LiveDat> ipage = new Page<>(page, dto.getPageSize());
        IPage<GetLiveListVo> result = this.baseMapper.getLiveList(ipage, dto, comId);
        return R.ok(result.getRecords(), result.getTotal());
    }


    /**
     * 获取子集项目
     *
     * @param comId
     * @param project
     * @return
     */
    private List<Long> getChildrenIds(Long comId, Long project) {
        return comAccountProjectMapper.getChildrenIds(project, comId);
    }

    @Override
    public R addRoom(Long comId, Long accountId, AddLiveDto dto) {
//        Integer authorityStatus = companyDatService.getAuthorityStatus(comId);
        Integer maxOnlineNum = MaxOnlineConstants.DEFAULT_VALUE;
        // 未绑定域名的情况下，最多只能创建2个房间，一个房间最多10个人
//        if (2 != authorityStatus) {
//            Long roomCount = this.baseMapper.selectCount(new LambdaQueryWrapper<LiveDat>()
//                    .eq(LiveDat::getComId, comId)
//                    .eq(LiveDat::getDeleteFlg, 0));
//            if (roomCount >= 2) {
//                return R.failed("创建房间失败，请联系商务");
//            }
//            maxOnlineNum = MaxOnlineConstants.AUTHORITY_MAX_VALUE;
//        }
        LiveDat liveDat = new LiveDat();
        Long id = idService.nextId();
        // 获取trtc操作
        TrtcInfo trtcInfo = new TrtcInfo();
        trtcInfo.setLiveId(id);
        trtcInfoMapper.insert(trtcInfo);
        String showId = ToBase62.encodeToBase62(id);
        liveDat.setId(id);
        liveDat.setShowId("gd-" + showId);
        liveDat.setComId(comId);
        liveDat.setTrtcId(trtcInfo.getTrtcId());
        liveDat.setName(dto.getLiveName());
        liveDat.setCoverImage(dto.getCoverImage());
        liveDat.setContent(dto.getContent());
        liveDat.setProject(dto.getProject());
        liveDat.setLecturerId(dto.getLecturerId());
        liveDat.setStartPlayTime(dto.getStartPlayTime());
        liveDat.setCreateBy(accountId);

        if (isNotNull(dto.getPeriodId())) {
            liveDat.setLiveType(LiveTypeEnum.CAMP.getValue());
            CompletableFuture.runAsync(() -> trainingCampLiveService.addCampLive(dto.getPeriodId(), id));
        }

        this.baseMapper.insert(liveDat);
        // 远程调用获取imId
        CompletableFuture.runAsync(() -> createIMGroup(liveDat, comId, id));
        insertRommSetting(comId, accountId, id);
        // 插入房间配置表
        getLiveConfigByRoomAdd(dto, comId, accountId, id, maxOnlineNum);
        // 插入房间菜单配置表
        insertDefaultTag(id, dto.getLiveTheme(), comId, accountId);
        return R.ok();
    }

    /**
     * 插入直播间配置
     *
     * @param comId
     * @param accountId
     * @param id
     */
    private void insertRommSetting(Long comId, Long accountId, Long id) {
        //  房间倒计时配置
        LiveCountDown lrcd = new LiveCountDown(idService.nextId(), comId, id, 0, 0, "立即下单", "已售罄", accountId);
        liveCountDownMapper.insert(lrcd);
        // 房间跑马灯
        LiveMarquee liveMarquee = new LiveMarquee();
        liveMarquee.setId(idService.nextId());
        liveMarquee.setComId(comId);
        liveMarquee.setContentType(1);
        liveMarquee.setMode(1);
        liveMarquee.setMarqueeEnable(false);
        liveMarquee.setLiveId(id);
        liveMarquee.setCreateBy(accountId);
        liveMarqueeMapper.insert(liveMarquee);
        // 房间聊天
        LiveWatchChat liveWatchChat = new LiveWatchChat(idService.nextId(), comId, id, false, false,
                false, false, false, false, "", "", accountId);
        liveWatchChatMapper.insert(liveWatchChat);
        // 插入分享设置
        LiveShare liveShare = new LiveShare(idService.nextId(), comId, id, "", "", "", "", accountId);
        liveShareMapper.insert(liveShare);
    }


    /**
     * 新增im群组
     * PRIVATE("Private", "好友工作群"),
     * PUBLIC("Public", "陌生人社交群"),
     * CHAT_ROOM("ChatRoom", "会议群"),
     * AV_CHAT_ROOM("AVChatRoom", "直播群"),
     * COMMUNITY("Community", "社群");
     */
    @SneakyThrows
    private void createIMGroup(LiveDat liveDat, Long comId, Long liveId) {
        // 在线程中创建IM群组
//        ThreadUtil.execute(new Runnable() {
//            @Override
//            public void run() {
        try {
            // 创建IM群组
            CreateImGroup createImGroup = new CreateImGroup();
//                    createImGroup.setName("communityGroupId111");
            String name = liveDat.getName();
            if (name.length() > 10) {
                name = name.substring(0, 10);
            }
            // 获取社区群组ID
            String communityGroupId = ImUtil.getLiveGroupId(comId, liveId);
            createImGroup.setGroupId(communityGroupId);  // 设置群组ID
            // 生成随机数
            Integer random = RandomUtils.nextInt(0, 999999999);
            CreateGroupRequest request = CreateGroupRequest.builder()
                    .type(GroupType.AV_CHAT_ROOM) // 直播群
//                            .type(GroupType.PUBLIC) // 陌生人社交群
                    // 设置群组名称
                    .name(name)
                    // 设置群主账号
                    .ownerAccount(createImGroup.getOwnerAccount())
                    // 设置群组ID
                    .groupId(createImGroup.getGroupId())
//                .memberList(createGroupInfo(query.getMemberList())  )\
                    // 设置加入群组的申请选项为自由加入
                    .applyJoinOption(ApplyJoinOption.FREE_ACCESS)
                    .build();
            CreateGroupResult result = imhelperutil.group().createGroup(request, random);
            String groupId = result.getGroupId();
            // 设置IM群组ID
            liveDat.setImGroupId(groupId);
            // 更新数据
            liveDatMapper.updateById(liveDat);

        } catch (Exception e) {
            e.printStackTrace();
        }
//            }
//        });
    }

    @Override
    //@CacheEvict(key = "#liveId", value = CacheConstants.ROOM_WATCH_BASE_INFO)
    public R delRoom(Long comId, Long accountId, Long liveId) {
        //删除缓存
        clearcache.delLiveCache(comId, liveId);

        LiveDat liveDat = this.baseMapper.selectById(liveId);
        if (liveDat.getDeleteFlg() == 1) {
            return R.failed("操作的直播间不存在");
        }
        if (LiveStatusEnum.LIVING.getValue() == liveDat.getLiveStatus()) {
            return R.failed("该直播间正在直播，请您关播后再进行删除操作！");
        }
        // 查询是否绑定渠道，如果有绑定返回删除失败
        Long bindChannelCount = channelLiveInfoMapper.selectCount(new LambdaQueryWrapper<ChannelLiveInfo>().eq(ChannelLiveInfo::getComId, comId)
                .eq(ChannelLiveInfo::getLiveId, liveId).eq(ChannelLiveInfo::getDeleteFlag, false));
        if (bindChannelCount > 0) {
            return R.failed("该直播间已绑定渠道，不可删除");
        }
        this.update(new LambdaUpdateWrapper<LiveDat>()
                .set(LiveDat::getDeleteFlg, true)
                .set(LiveDat::getUpdateBy, accountId)
                .eq(LiveDat::getId, liveId));

        //删除redisKey
//        redisService.del(RedisKeyConstants.LOOK_BASE_ROOM + dto.getRoomId());
        if (LiveTypeEnum.CAMP.getValue().equals(liveDat.getLiveType())) {
            this.trainingCampLiveMapper.delete(new LambdaQueryWrapper<TrainingCampLive>().eq(TrainingCampLive::getLiveId, liveId));
        }

        // 直播间删除后清除绑定过的直播间，与绑定过的优惠券
//        liveCommodityMapper.update()
        liveCommodityMapper.update(null, new LambdaUpdateWrapper<LiveCommodity>()
                .set(LiveCommodity::getUpdateBy, accountId)
                .set(LiveCommodity::getDeleteFlag, true)
                .eq(LiveCommodity::getLiveId, liveId)
                .eq(LiveCommodity::getComId, comId)
        );
        comCouponLiveDatMapper.update(null, new LambdaUpdateWrapper<ComCouponLiveDat>()
                .set(ComCouponLiveDat::getUpdateBy, accountId)
                .set(ComCouponLiveDat::getDeleteFlag, true)
                .eq(ComCouponLiveDat::getLiveId, liveId)
                .eq(ComCouponLiveDat::getComId, comId)
        );
        return R.ok();
    }

    @Override
    public R getLiveDetail(Long comId, Long liveId) {
        LiveDat liveDat = this.baseMapper.selectOne(new LambdaQueryWrapper<LiveDat>().select(LiveDat::getId, LiveDat::getComId,
                        LiveDat::getName, LiveDat::getCoverImage, LiveDat::getStartPlayTime, LiveDat::getLecturerId, LiveDat::getProject)
                .eq(LiveDat::getComId, comId).eq(LiveDat::getId, liveId));
        GetLiveDatVo vo = new GetLiveDatVo();
        BeanUtils.copyProperties(liveDat, vo);
        LiveConfig lrc = getLiveConfig(liveId, comId);
        vo.setMobileThemeType(lrc.getMobileThemeType());
        return R.ok(vo);
    }

    @Override
//    @Cacheable(key = "#roomId", value = CacheConstants.ROOM_WATCH_BASE_INFO, unless = "#roomId==null")
    public R getLiveDat(Long comId, Long liveId) {

        log.info("getLiveDat=comId={}=liveId={}", comId, liveId);
        GetLiveDatVo liveDat = this.baseMapper.getLiveDat(comId, liveId);
        log.info(liveDat.toString());
        // 判断是否存在课件
        LiveTag liveTag = liveTagMapper.selectOne(new LambdaQueryWrapper<LiveTag>().eq(LiveTag::getLiveId, liveId)
                .eq(LiveTag::getComId, comId).eq(LiveTag::getTagType, TagTypeEnum.CLASS.getValue()));
        if (ObjectUtils.isNotEmpty(liveTag)) {
            liveDat.setIsOpenTeduBoard(true);
        } else {
            liveDat.setIsOpenTeduBoard(false);
        }

        return R.ok(liveDat);
    }

    @Override
//    @CacheEvict(key = "#dto.liveId", value = CacheConstants.ROOM_WATCH_BASE_INFO)
    public R updateBasicInfo(Long comId, Long accountId, AddLiveDto dto) {
        Long liveId = dto.getLiveId();
        //删除缓存
        clearcache.delLiveCache(comId, liveId);

        LiveDat liveDat = getDat(comId, liveId);
        if (ObjectUtils.isEmpty(liveDat)) {
            return R.failed("操作的直播间不存在");
        }
        liveDat.setName(dto.getLiveName());
        liveDat.setProject(dto.getProject());
        liveDat.setCoverImage(dto.getCoverImage());
        liveDat.setLecturerId(dto.getLecturerId());
        liveDat.setContent(dto.getContent());
        liveDat.setStartPlayTime(dto.getStartPlayTime());
        this.baseMapper.updateById(liveDat);
        return R.ok();
    }

    @Override
    public R getLivingRoom(BasePage basePage, Long comId) {
        Long page = basePage.getPage();
        if (page == 1) {
            refreshLiveKeyByRedis(comId);
        }
        IPage<LiveDat> iPage = new Page<>(page, basePage.getPageSize());
        IPage<GetLivingRoomVo> vo = this.baseMapper.getLivingRoom(iPage, comId);
        List<GetLivingRoomVo> collect = vo.getRecords().stream().map(x -> {
//            // todo 获取拉流转推地址
//            x.setUploadUrl(null);
            Object value = redisService.get("SumOnLine_" + x.getLiveId());
            if (ObjectUtils.isEmpty(value)) {
                x.setCount("0");
            } else {
                x.setCount(value.toString());
            }
            LiveRecordDat liveDat = liveRecordDatService.getLiveDat(comId, x.getLiveId());
            if (ObjectUtils.isNotEmpty(liveDat)) {
                x.setLiveType(liveDat.getVideoType());
            }
            return x;
        }).collect(Collectors.toList());
        return R.ok(collect, vo.getTotal());
    }

    @Override
    public R copyLive(Long liveId, String name, Long comId, Long accountId) {
        LiveDat liveDat = this.baseMapper.selectOne(new LambdaQueryWrapper<LiveDat>().eq(LiveDat::getComId, comId).eq(LiveDat::getId, liveId)
                .eq(LiveDat::getDeleteFlg, 0));
        if (ObjectUtils.isEmpty(liveDat)) {
            return R.failed("直播间不存在");
        }
        Long newLiveId = idService.nextId();
        String showId = ToBase62.encodeToBase62(newLiveId);
        // 获取trtc操作
        TrtcInfo trtcInfo = new TrtcInfo();
        trtcInfo.setLiveId(newLiveId);
        trtcInfoMapper.insert(trtcInfo);
        liveDat.setShowId("gd-" + showId);
        liveDat.setTrtcId(trtcInfo.getTrtcId());
        liveDat.setId(newLiveId);
        liveDat.setName(name);
        liveDat.setShowId("gd-" + ToBase62.encodeToBase62(newLiveId));
        // 直播状态改为未开始
        liveDat.setLiveStatus(0);
        liveDat.setImGroupId(null);
        liveDat.setUpdateBy(accountId);
        liveDat.setCreateAt(null);
        this.baseMapper.insert(liveDat);
//        createIMGroup(liveDat, comId, accountId);
        CompletableFuture.runAsync(() -> createIMGroup(liveDat, comId, newLiveId));
        LiveConfig lrc = getLiveConfig(liveId, comId);
        lrc.setId(idService.nextId());
        lrc.setLiveId(newLiveId);
        lrc.setUpdateBy(accountId);
        lrc.setCreateAt(null);
        liveConfigMapper.insert(lrc);
        // 处理直播间装修菜单
        insertRoomFitment(liveId, comId, newLiveId);
        // 回放开启
        insertBindBack(liveId, comId, accountId, lrc, newLiveId);
        // 绑定的商品
        insertCommdityBind(liveId, comId, accountId, newLiveId);
        // 聊天设置
        insertWatchChat(liveId, comId, accountId, newLiveId);
        // 跑马灯
        insretMarquee(liveId, comId, accountId, newLiveId);
        // 插入分享设置
        insretLiveShare(liveId, comId, accountId, newLiveId);
        // 营期
        if (LiveTypeEnum.CAMP.getValue().equals(liveDat.getLiveType())) {
            LambdaQueryWrapper<TrainingCampLive> queryWrapper = new LambdaQueryWrapper<TrainingCampLive>()
                    .eq(TrainingCampLive::getLiveId, liveId);
            List<TrainingCampLive> list = this.trainingCampLiveMapper.selectList(queryWrapper);
            if (list.size() == 1) {
                CompletableFuture.runAsync(() -> trainingCampLiveService.addCampLive(list.get(0).getPeriodId(), newLiveId));
            } else {
                log.error("直播间{}不存在对应的营期信息", liveId);
            }
        }

        return R.ok();
    }

    private void insertBindBack(Long liveId, Long comId, Long accountId, LiveConfig lrc, Long newLiveId) {
        if(lrc.getReplayEnable()){
            LiveBindBack liveBindBack = liveBindBackMapper.selectOne(new LambdaQueryWrapper<LiveBindBack>().eq(LiveBindBack::getComId, comId).eq(LiveBindBack::getLiveId, liveId));
            if(ObjectUtils.isNotEmpty(liveBindBack)){
                liveBindBack.setId(idService.nextId());
                liveBindBack.setLiveId(newLiveId);
                liveBindBack.setCreateBy(accountId);
                liveBindBackMapper.insert(liveBindBack);
            }

        }
    }

    private LiveConfig getLiveConfig(Long liveId, Long comId) {
        LiveConfig lrc = liveConfigMapper.selectOne(new LambdaQueryWrapper<LiveConfig>()
                .eq(LiveConfig::getComId, comId).eq(LiveConfig::getLiveId, liveId)
                .eq(LiveConfig::getDeleteFlg, 0));
        return lrc;
    }

    @Override
    public R getWatchUrlByRoomId(Long liveId) {

        LiveDat liveDat = this.baseMapper.selectOne(new LambdaQueryWrapper<LiveDat>().eq(LiveDat::getId, liveId)
                .eq(LiveDat::getDeleteFlg, 0));
        if (ObjectUtils.isEmpty(liveDat)) {
            return R.failed("直播间不存在");
        }
        // live_tag
        List<LiveTag> liveTags = liveTagMapper.selectList(new LambdaQueryWrapper<LiveTag>().eq(LiveTag::getLiveId, liveId)
                .eq(LiveTag::getComId, liveDat.getComId()).eq(LiveTag::getTagType, TagTypeEnum.CLASS.getValue()));

        Boolean showTiw = getThreeScreen(liveId);
        GetWatchUrlByLiveVo vo = getCdnData(liveId, liveDat, showTiw);
        if (ObjectUtils.isEmpty(vo)) {
            return R.ok("获取cdn失败");
        }
        return R.ok(vo);
    }

    /**
     * 获取观看地址详细消息
     *
     * @param liveId
     * @param liveDat
     * @return
     */
    public GetWatchUrlByLiveVo getCdnData(Long liveId, LiveDat liveDat, Boolean showTiw) {
        // 根据直播间ID获取观看地址对象
        GetWatchUrlByLiveVo vo = new GetWatchUrlByLiveVo();
        Integer trtcId = liveDat.getTrtcId();
        // 获取绑定cnd数据
        ComDefultSetting comDefultSetting = comDefultSettingService.getCdnInfoByComId(liveDat.getComId());
        if (ObjectUtils.isEmpty(comDefultSetting)) {
            //            cdn数据不存在
            return null;
        }
        // 获取房间cnd配置
        LiveRoomCdn liveRoomCdn = liveRoomCdnService.getByCdnId(comDefultSetting.getPushCdnId());
        if (ObjectUtils.isEmpty(liveRoomCdn)) {
            //      直播间cdn配置不存在
            return null;
        }
        Long time = (System.currentTimeMillis() + 30 * 60 * 1000) / 1000;
        String expireDate = time.toString();
        Long lecturerId = liveDat.getLecturerId();
        // 腾讯云
        if (comDefultSetting.getPushCdnId().contains(Constants.TENCENT_CLOUD)) {
            String rtmpUrl = getPlayUrl(CdnPullTypeEnum.RTMP, liveRoomCdn, trtcId.toString(), expireDate);
            String flvUrl = getPlayUrl(CdnPullTypeEnum.FLV, liveRoomCdn, trtcId.toString(), expireDate);
            String m3u8Url = getPlayUrl(CdnPullTypeEnum.M3U8, liveRoomCdn, trtcId.toString(), expireDate);
            String webrtcUrl = getPlayUrl(CdnPullTypeEnum.WEBRTC, liveRoomCdn, trtcId.toString(), expireDate);
            if (showTiw) {
                String pushTiwStreamId = TencentLiveCloudUtil.getTiwPullStreamName(sdkAppId, trtcId.longValue(), lecturerId.toString());
                String tiwUrl = getPlayUrl(CdnPullTypeEnum.M3U8, liveRoomCdn, pushTiwStreamId, expireDate);
                vo.setTiwUrl(tiwUrl);
            }
            // rtmp路径
            vo.setRtmpUrl(rtmpUrl);
            vo.setRtmpTurnUrl(rtmpUrl);
            vo.setFlvUrl(flvUrl);
            vo.setM3u8Url(m3u8Url);
            // 低延迟路径
            vo.setLowLatencyUrl(webrtcUrl);
            vo.setType(Constants.TENCENT_CLOUD);
        }
        return vo;
    }


    @Override
    public R getRoomViewLink(Long liveId, Long comId, Long accountId) {
        GetRoomViewLinkVo vo = new GetRoomViewLinkVo();
        String domain = this.getShareComDomainUrl(comId, accountId);
        //加密
//        DesHelper desHelper = new DesHelper(watchRoomKey);

        DesHelper.getDesHelper(watchRoomKey);
        String key = DesHelper.getEncString(LiveRoleEnum.visitor.getValue() + Constants.UNDERLINE + liveId);
        String url = domain + Constants.SLASH + key;
        vo.setWatchurl(url);
        //获取短链接
        vo.setWatchShortUrl(getLiveShortUrl(liveId, url, shortDomain));
        return R.ok(vo);
    }

    public String getLiveShortUrl(Long liveId, String url, String shortDomain) {
        // 取出链接地址
        String[] uslList = shortDomain.split(",");
        String result = "";
        LiveDat liveDat = this.baseMapper.selectById(liveId);
        if (ObjectUtils.isEmpty(liveDat)) {
            return result;
        }
        if (StringUtils.isBlank(liveDat.getShortUrl())) {
            result = ShortUtil.create(blackService, url, uslList[new Random().nextInt(uslList.length)]);
            //更新短链接
            this.update(new LambdaUpdateWrapper<LiveDat>()
                    .set(LiveDat::getShortUrl, result)
                    .eq(LiveDat::getId, liveId));
        } else {
            result = liveDat.getShortUrl();
            // key加冒号
            String key = liveDat.getShortUrl().substring(liveDat.getShortUrl().lastIndexOf(Constants.SLASH) + 1);
            Object value = blackService.get(key);
            if (ObjectUtils.isNotEmpty(value)) {
                blackService.setShare(key, url);
            }
        }
        return result;
    }

    /**
     * 根据公司ID和用户账户ID获取分享客户端域名URL
     *
     * @param comId     公司ID
     * @param accountId 用户账户ID
     * @return 分享客户端域名URL
     */
    @Override
    public String getShareComDomainUrl(Long comId, Long accountId) {
        // 根据公司ID获取公司URL配置信息
        ComUrlConfig info = comUrlConfigService.getInfo(comId);
        String shareClientDomain = "";
        if (ObjectUtils.isEmpty(info)) {
            ComDefultSetting cdnInfoByComId = comDefultSettingService.getCdnInfoByComId(comId);
            String appId = defaultRequestAppId;
            if (ObjectUtils.isNotEmpty(cdnInfoByComId)) {
                String wxJsapiAppid = cdnInfoByComId.getWxJsapiAppid();
                if (StringUtils.isNotBlank(wxJsapiAppid)) {
                    appId = wxJsapiAppid;
                }
            }
            // 如果公司URL配置信息为空，则生成默认的路径
            shareClientDomain = defaultRequestHeader + appId + Constants.dot + requestAuthUrl;
            // 创建默认的公司URL配置信息
            info = new ComUrlConfig();
            info.setId(idService.nextId());
            info.setComId(comId);
            info.setShareClientDomain(shareClientDomain);
            info.setCreateBy(accountId);
            comUrlConfigService.save(info);
        } else {
            // 如果公司URL配置信息不为空，则获取已配置的分享客户端域名
            shareClientDomain = info.getShareClientDomain();
        }
        // 返回分享客户端域名URL
        return shareClientDomain;
    }

    @Override
    public R getHomeInfo(Long comId) {
//        Set<String> strings = refreshLiveKeyByRedis(comId);
        GetHomeInfoVo vo = new GetHomeInfoVo();
        List<LiveDat> liveDatList = this.baseMapper.selectList(new LambdaQueryWrapper<LiveDat>().eq(LiveDat::getComId, comId)
                .eq(LiveDat::getLiveStatus, LiveStatusEnum.LIVING.getValue())
                .eq(LiveDat::getDeleteFlg, false));
        vo.setLiveStreamingCount(liveDatList.size());
        Long liveCount = this.baseMapper.selectCount(new LambdaQueryWrapper<LiveDat>().eq(LiveDat::getComId, comId).eq(LiveDat::getDeleteFlg, false));
        vo.setLiveCount(liveCount);
        Long backCount = liveBackVideoMapper.selectCount(new LambdaQueryWrapper<LiveBackVideo>().eq(LiveBackVideo::getComId, comId).eq(LiveBackVideo::getDeleteFlg, false));
        vo.setBackCount(backCount);

        long livePeopleTotal = 0L;
        for (LiveDat liveDat : liveDatList) {
            Object value = redisService.get("SumOnLine_" + liveDat.getId());
            if (ObjectUtils.isNotEmpty(value)) {
                livePeopleTotal += Long.parseLong(value.toString());
            }
        }

        //总人数
        vo.setLivePeopleTotal(livePeopleTotal);
        return R.ok(vo);
    }

    @Override
    public R getHomeOnLine(Long comId) {
//        Calendar calendar = Calendar.getInstance();
//        calendar.setTime(new Date());
        String dateSs = DateFormatUtils.format(new Date(), "yyyy-MM-dd") + " 00:00:00";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Criteria criteria = Criteria.where("comId").is(comId);
        try {
            Date start = formatter.parse(dateSs);
            criteria.and("createAt").gte(start);
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
            AggregationResults<ComOnlineSumVo> results = mongoTemplate.aggregate(aggregation, MongoConstant.STATIC_ONLINE_USER_STATUE, ComOnlineSumVo.class);
            List<ComOnlineSumVo> resultList = results.getMappedResults();
            return R.ok(resultList);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public R getHomeMonth(Long comId, String month) {
        Criteria criteria = Criteria.where("comId").is(comId);
        String dateSs = StringUtils.isNotBlank(month) ? month : DateFormatUtils.format(new Date(), "yyyy-MM");
        criteria.and("statisticsDate").regex("^.*" + dateSs + ".*$");
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group("statisticsDate", "userId")
                        .first("statisticsDate").as("statisticsDate")
                        .first("userId").as("userId"),
                Aggregation.project()
                        .and("statisticsDate").previousOperation()//必须添加一个字段的 previousOperation 否则会报错
                        .and("statisticsDate").as("statisticsDate")
                        .and("userId").as("userId"),
                Aggregation.sort(Sort.Direction.ASC, "statisticsDate")
        );
        AggregationResults<ComOnlineSumVo> results = mongoTemplate.aggregate(aggregation, MongoConstant.LIVE_USER_WATCH_TIME_STATISTICS, ComOnlineSumVo.class);
        List<ComOnlineSumVo> resultList = results.getMappedResults();
        List<ComOnlineSumVo> resultListNew = new ArrayList<>();
        int sum = 0;
        String statisticsDateList = "";
        //统计每天的数量
        for (ComOnlineSumVo comOnlineSumVo : resultList) {
            if (StringUtils.isBlank(statisticsDateList)) {
                statisticsDateList = comOnlineSumVo.getStatisticsDate();
                sum++;
            } else {
                if (comOnlineSumVo.getStatisticsDate().equals(statisticsDateList)) {
                    sum++;
                } else {
                    ComOnlineSumVo comOnlineSumVoNew = new ComOnlineSumVo();
                    comOnlineSumVoNew.setStatisticsDate(statisticsDateList);
                    comOnlineSumVoNew.setSum(sum);
                    resultListNew.add(comOnlineSumVoNew);
                    statisticsDateList = comOnlineSumVo.getStatisticsDate();
                    sum = 1;
                }
            }
        }
        if (sum > 0) {
            ComOnlineSumVo comOnlineSumVoNew = new ComOnlineSumVo();
            comOnlineSumVoNew.setStatisticsDate(statisticsDateList);
            comOnlineSumVoNew.setSum(sum);
            resultListNew.add(comOnlineSumVoNew);
        }
        return R.ok(resultListNew);
    }

    /**
     * //分页查询老师所在的直播间
     *
     * @param page
     * @param comId
     * @param lecturerId
     * @return
     */
    @Override
    public R getLivingRoomByLecturerId(BasePage page, Long comId, Long lecturerId) {
        IPage<LiveDat> iPage = new Page<>(page.getPage(), page.getPageSize());
        IPage<GetLivingRoomVo> vo = this.baseMapper.getLivingRoomByLecturerId(iPage, comId, lecturerId);
        //List<GetLivingRoomVo> collect = vo.getRecords().stream().map(x -> {
        //     todo 获取拉流转推地址
        //x.setUploadUrl(null);
        //return x;
        //}).collect(Collectors.toList());
        return R.ok(vo.getRecords(), vo.getTotal());
    }

    @Override
    public Long countLivingRoomByLecturerId(Long comId, Long lecturerId) {
        return this.baseMapper.countLivingRoomByLecturerId(comId, lecturerId);
    }

    @Override
    public R getRoomDatByAssistan(Long userId, Long comId, Long liveId) {
        LiveDat liveDat = this.baseMapper.selectById(liveId);
        if (ObjectUtils.isEmpty(liveDat)) {
            return R.failed("直播间信息不存在");
        }
        log.info("助理端获取直播间信息==={}", liveDat);
        GetRoomDatByAssistanVo vo = new GetRoomDatByAssistanVo();
        vo.setImGroupId(liveDat.getImGroupId());
        vo.setTrtcId(liveDat.getTrtcId());
        // 获取信令签名
        TLSSigAPIv2 tlsSigApi = new TLSSigAPIv2(sdkAppId, key);
        String userSig = tlsSigApi.genUserSig(userId.toString(), 86400);
        TencentImUserSign txCloudUserSign = new TencentImUserSign(userId.toString(), null, sdkAppId, userSig);
        // 删掉key
        txCloudUserSign.setKey(null);
        vo.setTxCloudUserSign(txCloudUserSign);
        Boolean showTiw = getThreeScreen(liveId);

        AccountDat accountDat = accountDatMapper.selectById(userId);
        String key = "IM_" + userId;

        // 多助理端登录导致统计人数异常修复
        String userInfo = (String) redisService.get(key);
        if (StringUtils.isNotBlank(userInfo)) {
            String[] split = StringUtils.split(userInfo, "###");
            String liveIdOld = split[2];
            if (!liveId.equals(Long.parseLong(liveIdOld))) {
                CompletableFuture.runAsync(() -> updateUserStateChange(split, userId));
            }
        }

        String value = "assistant###" + comId + "###" + liveId + "###" + accountDat.getName() + "###" + accountDat.getProfile() + "###2###0###";
        redisService.set(key, value);
        log.info("助理端获取直播间id==={}", liveId);
        // 获取跑马灯信息
        LiveMarquee liveMarquee = liveMarqueeMapper.selectOne(new LambdaQueryWrapper<LiveMarquee>()
                .eq(LiveMarquee::getComId, comId).eq(LiveMarquee::getLiveId, liveId));
        // 不存在，则新建
        if (ObjectUtils.isEmpty(liveMarquee)) {
            liveMarquee = new LiveMarquee();
            liveMarquee.setId(idService.nextId());
            liveMarquee.setComId(comId);
            liveMarquee.setContentType(1);
            liveMarquee.setMode(1);
            liveMarquee.setMarqueeEnable(false);
            liveMarquee.setLiveId(liveId);
            liveMarquee.setCreateBy(userId);
            liveMarqueeMapper.insert(liveMarquee);
        }
        vo.setLiveMarquee(liveMarquee);
        // 判断是否开播
        Object o = redisService.get(2, RedisConstants.LIVE_STATUS + comId + Constants.UNDERLINE + liveId);
        if (ObjectUtils.isNotEmpty(o)) {
            vo.setIsStart(true);
            vo.setGetWatchUrlByLiveVo(getCdnData(liveId, liveDat, showTiw));
            // 获取直播类型
            LiveRecordDat liveRecordDat = liveRecordDatService.getLiveDat(comId, liveId);
            if (null != liveRecordDat) {
                Integer videoType = liveRecordDat.getVideoType();
                if (videoType == 1) {
                    vo.setLiveStreamingType(videoType);
                }else{
                    vo.setLiveStreamingType(0);
                }
            }
            LiveConfig lrc = getLiveConfig(liveId, comId);
            vo.setMobileThemeType(lrc.getMobileThemeType());
        } else {
            vo.setIsStart(false);
        }
        // 获取是否禁言
        LiveWatchChat liveWatchChat = liveWatchChatMapper.selectOne(new LambdaQueryWrapper<LiveWatchChat>()
                .eq(LiveWatchChat::getComId, comId).eq(LiveWatchChat::getLiveId, liveId)
                .eq(LiveWatchChat::getDeleteFlg, 0));
        if (ObjectUtils.isEmpty(liveWatchChat)) {
            vo.setBannedAll(false);
        } else {
            vo.setBannedAll(liveWatchChat.getBannedAll());
        }
        return R.ok(vo);
    }

    private void updateUserStateChange(String[] split, Long userId) {
        LiveUserStateChange liveUserStateChange = new LiveUserStateChange();
        String userType = split[0];
        String comId = split[1];
        String liveId = split[2];
        String name = split[3];
        String headImg = split[4];
        String isNew = split[5];
        if (!"null".equals(headImg)) {
            liveUserStateChange.setHeadImgUrl(headImg);
        }
        if (split.length > 6 && !"0".equals(split[6])) {
            String channelId = split[6];
            liveUserStateChange.setChannelId(Long.parseLong(channelId));
        }
        if (split.length > 7) {
            String channelName = split[7];
            liveUserStateChange.setChannelName(channelName);
        }
        liveUserStateChange.setId(idService.nextId());
        liveUserStateChange.setStatisticsDate(DateFormatUtils.format(new Date(), "yyyy-MM-dd"));
        liveUserStateChange.setLiveId(Long.parseLong(liveId));
        liveUserStateChange.setComId(Long.parseLong(comId));
        liveUserStateChange.setLiveUserId(userId);
        liveUserStateChange.setClientIp("0.0.0.0");
        liveUserStateChange.setLiveUsername(name);
        liveUserStateChange.setUserType(userType);
        liveUserStateChange.setEventType("Disconnect");
        liveUserStateChange.setEventReason("LinkClose");
        liveUserStateChange.setEventTime(new Date());
        if (StringUtils.isNotBlank(isNew)) {
            liveUserStateChange.setIsNew(Integer.parseInt(isNew));
        }
        mongoTemplate.insert(liveUserStateChange, MongoConstant.LIVE_USER_STATE_CHANGE_DAY);
    }

    @Override
    public R sendNotice(Long comId, Long accountId, SendNoticeDto dto) {
        Long liveId = dto.getLiveId();
        String imGroupId = dto.getImGroupId();
        String content = dto.getContent();

        redisService.set(RedisConstants.LIVE_CONTEXT + liveId, content);
        System.out.println("res: " + redisService.get(RedisConstants.LIVE_CONTEXT + liveId));

        ImMsgQuery msgQuery = new ImMsgQuery();
        JSONObject json = new JSONObject();
        json.set("notice", content);
        json.set("userInfo", null);
        msgQuery.setNotifyTypeEnum(TIMSendTypeEnum.ANNOUNCEMENT);
        msgQuery.setCloudCustomData(JSON.toJSONString(json));
        msgQuery.setMsgContent(content);
        msgQuery.setGroupId(imGroupId);
        msgQuery.setFromUserId("administrator");
        imService.sendCustomGroupMsg(msgQuery);
        return R.ok();
    }

    //查询三分屏
    @Override
    public Boolean getThreeScreen(Long liveId) {
        List<LiveTag> liveTags = liveTagService.getLiveTags(liveId);
        if (ObjectUtils.isEmpty(liveTags)) {
            return false;
        }
        for (LiveTag liveTag : liveTags) {
            if (liveTag.getTagType().equals(TagTypeEnum.CLASS.getValue())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public R getLiveCampList(Long comId, Long accountId, GetLiveListDto dto) {
        try {
            Assert.notNull(dto.getPeriodId(), "营期id不能为空");
            Assert.notNull(dto.getPage(), "查询页码不能为空");
            Assert.notNull(dto.getPageSize(), "查询页大小不能为空");
        } catch (IllegalArgumentException e) {
            return R.failed(e.getMessage());
        }

        List<GetLiveListVo> records = new ArrayList<>();

        LambdaQueryWrapper<TrainingCampLive> queryWrapper = new LambdaQueryWrapper<TrainingCampLive>()
                .eq(TrainingCampLive::getPeriodId, dto.getPeriodId());
        List<TrainingCampLive> list = this.trainingCampLiveMapper.selectList(queryWrapper);
        if (ObjectUtils.isEmpty(list)) {
            return R.ok(records, 0);
        }
        List<Long> liveIds = list.stream().map(TrainingCampLive::getLiveId).toList();
        dto.setLiveIds(liveIds);
        dto.setTrainingCamp(LiveTypeEnum.CAMP.getValue());

        Long page = dto.getPage();
        if (page == 1) {
            refreshLiveKeyByRedis(comId);
        }
        Page<LiveDat> ipage = new Page<>(dto.getPage(), dto.getPageSize());
        if (StringUtils.isNotBlank(dto.getStartTime())) {
            dto.setStartTime(dto.getStartTime() + " 00:00:01");
            if (StringUtils.isNotBlank(dto.getEndTime())) {
                dto.setEndTime(dto.getEndTime() + " 23:59:59");
            }
        }
        IPage<GetLiveListVo> result = this.baseMapper.getLiveList(ipage, dto, comId);
        List<GetLiveListVo> vo = result.getRecords().stream().map(e -> {
            if (LiveStatusEnum.LIVING.getValue() == e.getLiveStatus()) {
                LiveRecordDat liveDat = liveRecordDatService.getLiveDat(comId, e.getLiveId());
                if (ObjectUtils.isNotEmpty(liveDat)) {
                    e.setLiveType(liveDat.getVideoType());
                }
            }
            return e;
        }).toList();
        return R.ok(vo, result.getTotal());
    }

    @Transactional
    @Override
    public R updateLiveStatus(Long comId, Long accountId, Long liveId, Integer status, Integer type) {
        //保存开播记录
        LiveDat liveDat = this.baseMapper.selectOne(new LambdaQueryWrapper<LiveDat>().eq(LiveDat::getId, liveId)
                .eq(LiveDat::getComId, comId).eq(LiveDat::getDeleteFlg, 0));
        ImMsgQuery msgQuery = new ImMsgQuery();
        JSONObject json = new JSONObject();
        json.set("liveStatus", status);
        if (status == 1) {
            Boolean showTiw = getThreeScreen(liveId);
            //插入新的直播记录
            GetWatchUrlByLiveVo cdnData = getCdnData(liveId, liveDat, showTiw);
            json.set("cdnData", cdnData);
            liveDat.setStartPlayTime(new Date());
            // 保存开播记录 , 特殊情况下可能开播了没调用这个接口, 需要上报时候校验一下缓存, 没有补录一条
            LiveRecordDat dat = new LiveRecordDat();
            dat.setStartTime(new Date());
            dat.setVideoType(type);
            dat.setLiveId(liveId);
            dat.setComId(comId);
            dat.setId(idService.nextId());
            dat.setState(LiveStatusEnum.LIVING.getValue());
            dat.setCreateBy(accountId);
            dat.setUpdateBy(accountId);
            liveRecordDatService.saveDat(dat);
            clearcache.delLiveRecordInfo(comId, liveId);
            redisService.setex("LIVE_RECORD_ID_" + liveId, dat.getId() + "", 60 * 60 * 24);


        } else {

            liveDat.setEndPlayTime(new Date());
            LiveRecordDat recordDat = new LiveRecordDat();
            recordDat.setState(LiveStatusEnum.NOT_STARTED.getValue());
            recordDat.setLiveId(liveId);
            recordDat.setComId(comId);
            recordDat.setEndTime(new Date());
            recordDat.setUpdateBy(accountId);
            liveRecordDatService.updateByLiveId(recordDat);
            clearcache.delLiveRecordInfo(comId, liveId);
        }
        liveDat.setLiveStatus(status);
        liveDat.setUpdateBy(accountId);
        this.baseMapper.updateById(liveDat);
        json.set("userInfo", null);
        msgQuery.setCloudCustomData(JSON.toJSONString(json));
        msgQuery.setNotifyTypeEnum(TIMSendTypeEnum.LIVE_STATUS_UPDATE);
        msgQuery.setMsgContent("");
        msgQuery.setGroupId(liveDat.getImGroupId());
        msgQuery.setFromUserId("administrator");
        imService.sendCustomGroupMsg(msgQuery);


        //LiveDat liveDat = this.baseMapper.selectOne(new LambdaQueryWrapper<LiveDat>().eq(LiveDat::getId, trtcId)
        //        .eq(LiveDat::getComId, comId).eq(LiveDat::getDeleteFlg, 0));
        //;
        //if (ObjectUtils.isEmpty(liveDat)) {
        //    return R.failed("直播间信息不存在");
        //}
        //Long liveId = liveDat.getId();
        ////
        //String liveStr = liveId.toString();
        //
        //if (1 == type) {
        //    //第一次更新
        //    // 无需修改，且不用发im消息
        //    if (!liveDat.getLiveStatus().equals(status)) {
        //        liveDat.setLiveStatus(status);
        //
        //        ImMsgQuery msgQuery = new ImMsgQuery();
        //        JSONObject json = new JSONObject();
        //        json.set("liveStatus", status);
        //        if (LiveStatusEnum.LIVING.getValue() == status) {
        //            GetWatchUrlByLiveVo cdnData = getCdnData(liveId, liveDat);
        //            json.set("cdnData", cdnData);
        //            liveDat.setStartPlayTime(new Date());
        //
        //            // 保存开播记录 , 特殊情况下可能开播了没调用这个接口, 需要上报时候校验一下缓存, 没有补录一条
        //            LiveRecordDat dat = new LiveRecordDat();
        //            dat.setStartTime(new Date());
        //            dat.setVideoType(0);
        //            dat.setLiveId(liveId);
        //            dat.setComId(comId);
        //            dat.setId(idService.nextId());
        //            dat.setState(LiveStatusEnum.LIVING.getValue());
        //            dat.setCreateBy(accountId);
        //            dat.setUpdateBy(accountId);
        //            liveRecordDatService.save(dat);
        //            redisService.setex("LIVE_RECORD_ID_" + liveId, dat.getId() + "", 60 * 60 * 24);
        //        } else {
        //            liveDat.setEndPlayTime(new Date());
        //
        //            LiveRecordDat recordDat = new LiveRecordDat();
        //            recordDat.setState(LiveStatusEnum.NOT_STARTED.getValue());
        //            recordDat.setLiveId(liveId);
        //            recordDat.setEndTime(new Date());
        //            recordDat.setUpdateBy(accountId);
        //            liveRecordDatService.updateByLiveId(recordDat);
        //        }
        //        liveDat.setUpdateBy(accountId);
        //        this.baseMapper.updateById(liveDat);
        //        json.set("userInfo", null);
        //        msgQuery.setCloudCustomData(JSON.toJSONString(json));
        //        msgQuery.setNotifyTypeEnum(TIMSendTypeEnum.LIVE_STATUS_UPDATE);
        //        msgQuery.setMsgContent("");
        //        msgQuery.setGroupId(liveDat.getImGroupId());
        //        msgQuery.setFromUserId("administrator");
        //        imService.sendCustomGroupMsg(msgQuery);
        //        //开始推流
        //        if (LiveStatusEnum.LIVING.getValue() == status) {
        //            System.out.println("======开始推流=======");
        //            LiveConfig conf = liveConfigMapper.selectByLiveIdAndComId(liveId, comId);
        //            Integer mobileThemeType = conf.getMobileThemeType();
        //            List<LiveTag> liveTags = liveTagService.getLiveTags(liveId);
        //            //Long comId, Long accountId, Long liveId
        //            R marquee = liveMarqueeService.getInfo(comId, accountId, liveId);
        //            LiveMarquee livemarquee = (LiveMarquee) marquee.getData();
        //            String customContent = livemarquee.getCustomContent();
        //            List<LiveTag> collect = liveTags.stream().filter(x -> x.getTagType() == 2).collect(Collectors.toList());
        //            boolean empty = collect.isEmpty();
        //            Long screenType = !empty ? 1L : 0L;
        //            // 开始推流
        //            PushStreamDTO pushStreamDTO =
        //                    PushStreamDTO.builder()
        //                            .groupId(liveDat.getImGroupId())
        //                            .accountId(liveDat.getLecturerId().toString())
        //                            .screenType(screenType)
        //                            .roomid(liveDat.getTrtcId().longValue())
        //                            .liveId(liveDat.getId())
        //                            .streamType(screenType)
        //                            .clarity(2)
        //                            .trtcAppName(trtcAppName)
        //                            .waterText(customContent)
        //                            .build();
        //            startPushStream(pushStreamDTO);
        //        } else {
        //            // 结束推流
        //            StopPublishCdnStreamDto stopPublishCdnStreamDto = StopPublishCdnStreamDto.builder()
        //                    .liveId(liveId)
        //                    .groupId(liveDat.getImGroupId())
        //                    .trtcAppName(trtcAppName)
        //                    .accountId(liveDat.getLecturerId().toString())
        //                    .trtcUserId(liveDat.getTrtcId().toString())
        //                    .roomid(liveDat.getTrtcId().longValue())
        //                    .build();
        //            stopPushStream(stopPublishCdnStreamDto);
        //        }
        //    }
        //}
        ////保存成功后,更新缓存
        //redisService.hset(2, RedisConstants.LIVE_STATUS_LIST + comId, liveStr, liveStr);
        //redisService.setex(2, RedisConstants.LIVE_STATUS + comId + Constants.UNDERLINE + liveStr, liveStr, 30);
        return R.ok();
    }

    @Override
    public LiveConfig selectByLiveIdAndComId(Long liveId, Long comId) {
        return liveConfigMapper.selectByLiveIdAndComId(liveId, comId);

    }

    private void stopPushStream(StopPublishCdnStreamDto stopPublishCdnStreamDto) {
        tliveService.stopPublishCdnStream(stopPublishCdnStreamDto);
    }

    private String startPushStream(PushStreamDTO pushStreamDTO) {
        return tliveService.startPublishCdnStream(pushStreamDTO);
    }

    /**
     * 重置redis直播间在线列表
     *
     * @param comId
     */
    public Set<String> refreshLiveKeyByRedis(Long comId) {
        try {
            Map<String, String> stringStringMap = redisService.hgetAll(2, RedisConstants.LIVE_STATUS_LIST + comId);
            Set<String> keys = stringStringMap.keySet();
            if (ObjectUtils.isNotEmpty(keys)) {
                for (String liveId : keys) {
                    Object o = redisService.get(2, RedisConstants.LIVE_STATUS + comId + Constants.UNDERLINE + liveId);
                    if (ObjectUtils.isEmpty(o)) {
                        keys.remove(liveId);
                        redisService.hDel(2, RedisConstants.LIVE_STATUS_LIST + comId, liveId);
                        LiveDat liveDat = this.baseMapper.selectById(liveId);
                        if (ObjectUtils.isNotEmpty(liveDat)) {
                            liveDat.setLiveStatus(LiveStatusEnum.NOT_STARTED.getValue());
                            this.baseMapper.updateById(liveDat);

                            // 异常断播更新直播记录表
                            LiveRecordDat recordDat = new LiveRecordDat();
                            recordDat.setState(LiveStatusEnum.NOT_STARTED.getValue());
                            recordDat.setLiveId(liveDat.getId());
                            recordDat.setEndTime(new Date());
                            recordDat.setComId(comId);
                            recordDat.setUpdateBy(0L);
                            liveRecordDatService.updateByLiveId(recordDat);
                        }
                    }
                }
            }
            return keys;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 增加绑定的商品
     *
     * @param liveId
     * @param comId
     * @param accountId
     * @param newLiveId
     */
    private void insertCommdityBind(Long liveId, Long comId, Long accountId, Long newLiveId) {
        // 从数据库中查询 LiveCountDown 对象
        LiveCountDown liveCountDown = liveCountDownMapper.selectOne(new LambdaQueryWrapper<LiveCountDown>()
                .eq(LiveCountDown::getComId, comId).eq(LiveCountDown::getLiveId, liveId));
        // 如果 liveCountDown 为空，则创建新的 LiveCountDown 对象，并插入到数据库中
        if (ObjectUtils.isEmpty(liveCountDown)) {
            liveCountDown = new LiveCountDown(idService.nextId(), comId, liveId, 0, 0, "立即下单", "已售罄", accountId);
            liveCountDownMapper.insert(liveCountDown);
        }
        // 更新 liveCountDown 对象的属性，并将其插入到数据库中
        liveCountDown.setId(idService.nextId());
        liveCountDown.setLiveId(newLiveId);
        liveCountDownMapper.insert(liveCountDown);
        // 从数据库中查询 liveCommodity 对象列表
        List<LiveCommodity> liveRoomCommodities = liveCommodityMapper.selectList(new LambdaQueryWrapper<LiveCommodity>().eq(LiveCommodity::getComId, comId)
                .eq(LiveCommodity::getLiveId, liveId).eq(LiveCommodity::getDeleteFlag, false));
        // 如果 liveRoomCommodities 不为空，则为每个 liveCommodity 对象设置新的属性，并批量插入到数据库中
        if (ObjectUtils.isNotEmpty(liveRoomCommodities)) {
            for (LiveCommodity liveCommodity : liveRoomCommodities) {
                liveCommodity.setId(idService.nextId());
                liveCommodity.setLiveId(newLiveId);
            }
            liveCommodityMapper.insertBatch(liveRoomCommodities);
        }

    }

    /**
     * 聊天设置
     *
     * @param liveId
     * @param comId
     * @param accountId
     * @param newLiveId
     */
    private void insertWatchChat(Long liveId, Long comId, Long accountId, Long newLiveId) {
        LiveWatchChat liveWatchChat = liveWatchChatMapper.selectOne(new LambdaQueryWrapper<LiveWatchChat>()
                .eq(LiveWatchChat::getComId, comId).eq(LiveWatchChat::getLiveId, liveId)
                .eq(LiveWatchChat::getDeleteFlg, 0));
        // 如果直播聊天信息为空，则创建新的直播聊天信息并插入数据库
        if (ObjectUtils.isEmpty(liveWatchChat)) {
            liveWatchChat = new LiveWatchChat(idService.nextId(), comId, liveId, false, false,
                    false, false, false, false, "", "", accountId);
            liveWatchChatMapper.insert(liveWatchChat);
        }
        liveWatchChat.setId(idService.nextId());
        liveWatchChat.setLiveId(newLiveId);
        liveWatchChatMapper.insert(liveWatchChat);
    }

    /**
     * 插入跑马灯
     *
     * @param liveId
     * @param comId
     * @param accountId
     * @param newLiveId
     */
    private void insretMarquee(Long liveId, Long comId, Long accountId, Long newLiveId) {
        // 查询liveMarquee表中符合条件的数据
        LiveMarquee liveMarquee = liveMarqueeMapper.selectOne(new LambdaQueryWrapper<LiveMarquee>().eq(LiveMarquee::getComId, comId).eq(LiveMarquee::getLiveId, liveId));
        // 如果liveMarquee为空，新建一个liveMarquee对象并插入数据库
        if (ObjectUtils.isEmpty(liveMarquee)) {
            liveMarquee = new LiveMarquee();
            liveMarquee.setId(idService.nextId());
            liveMarquee.setComId(comId);
            liveMarquee.setLiveId(liveId);
            liveMarquee.setCreateBy(accountId);
            liveMarqueeMapper.insert(liveMarquee);
        }
        // 为liveMarquee设置新的id、comId和roomId，并将其插入数据库
        liveMarquee.setId(idService.nextId());
        liveMarquee.setComId(comId);
        liveMarquee.setLiveId(newLiveId);
        liveMarqueeMapper.insert(liveMarquee);

    }

    /**
     * 插入分享设置
     *
     * @param liveId
     * @param comId
     * @param accountId
     * @param newLiveId
     */
    private void insretLiveShare(Long liveId, Long comId, Long accountId, Long newLiveId) {
        LiveShare liveShare = liveShareMapper.selectOne(new LambdaQueryWrapper<LiveShare>().eq(LiveShare::getComId, comId).eq(LiveShare::getLiveId, liveId));
        if (ObjectUtils.isEmpty(liveShare)) {
            // 插入分享设置
            liveShare = new LiveShare(idService.nextId(), comId, liveId, "", "", "", "", accountId);
            liveShareMapper.insert(liveShare);
        }
        // 为liveMarquee设置新的id、comId和roomId，并将其插入数据库
        liveShare.setId(idService.nextId());
        liveShare.setComId(comId);
        liveShare.setLiveId(newLiveId);
        liveShareMapper.insert(liveShare);

    }

    /**
     * 处理直播间装修菜单
     *
     * @param liveId
     * @param comId
     * @param newLiveId
     */
    private void insertRoomFitment(Long liveId, Long comId, Long newLiveId) {
        // 查询并更新直播间标签和视频信息
        List<LiveTag> liveTags = liveTagMapper.selectList(new LambdaQueryWrapper<LiveTag>().eq(LiveTag::getComId, comId).eq(LiveTag::getLiveId, liveId));
        for (LiveTag liveTag : liveTags) {
            liveTag.setId(idService.nextId());
            liveTag.setLiveId(newLiveId);
            // 如果直播间装修存在视频模块
            if (liveTag.getTagType().equals(TagTypeEnum.VIDEO.getValue())) {
                // 查询并更新直播间视频信息
                List<ComLiveVideo> comLiveVideos = comLiveVideoMapper.selectList(new LambdaQueryWrapper<ComLiveVideo>()
                        .eq(ComLiveVideo::getComId, comId).eq(ComLiveVideo::getLiveId, liveId));
                if (ObjectUtils.isNotEmpty(comLiveVideos)) {
                    for (ComLiveVideo comLiveVideo : comLiveVideos) {
                        comLiveVideo.setId(idService.nextId());
                        comLiveVideo.setLiveId(newLiveId);
                    }
                    // 批量插入更新后的直播间视频信息
                    comLiveVideoMapper.insertBatch(comLiveVideos);
                }
            }
        }
        // 批量插入更新后的直播间标签信息
        liveTagMapper.insertBatch(liveTags);

    }

    @Override
//    @Cacheable(key = "#comId + '_' + #liveId",value = CacheConstants.WATCH_BASE_INFO,unless = "#result==null")
    public LiveDat selectOne(Long comId, Long liveId) {
        return this.baseMapper.selectOne(new LambdaQueryWrapper<LiveDat>().eq(LiveDat::getComId, comId).eq(LiveDat::getId, liveId)
                .eq(LiveDat::getDeleteFlg, 0));
    }

    /**
     * 获取 OBS
     *
     * @param comId
     * @param accountId
     * @param isAdmin
     * @param dto
     * @return
     */
    @Override
    public R getObsUrl(Long comId, Long accountId, Boolean isAdmin, GetLiveListDto dto) {
        //获取房间信息
        LiveDat liveDat = this.getDat(comId, dto.getLiveId());
        Integer trtcId = liveDat.getTrtcId();
        Long time = System.currentTimeMillis() + 30 * 60 * 1000;
        //Long time = System.currentTimeMillis() + 30 * 60;
        String safeUrl = Obs.getSafeUrl(obsKey, trtcId.toString(), time / 1000);
        String obsUrl = "rtmp://" + tpush + "/obs/";
        ObsUrlVo obsUrlVo = new ObsUrlVo();
        obsUrlVo.setServer(obsUrl);
        obsUrlVo.setStreamKey(trtcId + "?" + safeUrl);
        return R.ok(obsUrlVo);
    }


    /**
     * 设置 OBS录制
     *
     * @param comId
     * @param accountId
     * @param isAdmin
     * @param dto
     * @return
     */

    @Override
    public R setObsRecord(Long comId, Long accountId, Boolean isAdmin, GetLiveListDto dto) {
        Integer liveStatus = dto.getLiveStatus();
        Long liveId = dto.getLiveId();
        LiveDat liveDat = this.baseMapper.selectById(liveId);
        String liveStr = liveId.toString();
        //删除缓存
        clearcache.delLiveCache(comId, liveId);
        if (liveStatus == 1) {

//            liveDat.setLiveStatus(liveStatus);
            //更新redis
            redisService.hset(2, RedisConstants.LIVE_STATUS_LIST + comId, liveStr, liveStr);
            redisService.set(2, RedisConstants.LIVE_STATUS + comId + Constants.UNDERLINE + liveStr, liveStr);
            // 新增直播记录，修改直播状态
            this.updateLiveStatus(comId, accountId, liveId, liveStatus, LiveRecordEnum.OBS.getValue());
        } else {
            //直播结束 结束推流
            StopPublishCdnStreamDto stopPublishCdnStreamDto = StopPublishCdnStreamDto.builder()
                    .liveId(liveId)
                    .groupId(liveDat.getImGroupId())
                    .trtcAppName(trtcAppName)
                    .accountId(liveDat.getLecturerId().toString())
                    .trtcUserId(liveDat.getTrtcId().toString())
                    .roomid(liveDat.getTrtcId().longValue())
                    .build();
            //todo yang 断波停止推流
//            stopPublishCdnStream(stopPublishCdnStreamDto);
            redisService.hDel(2, RedisConstants.LIVE_STATUS_LIST + comId, liveStr);
            redisService.del(2, RedisConstants.LIVE_STATUS + comId + Constants.UNDERLINE + liveStr);
            //todo yang 断波停止推流
//            deleteLiveTask(liveId);
        }
        // 新增直播记录，修改直播状态
        this.updateLiveStatus(comId, accountId, liveId, liveStatus, LiveRecordEnum.OBS.getValue());
        //todo yang 断波停止推流
        //存到数据库中,
        // 推流时间
        //存到redis中
        //启动录制任务
        //启动定时任务监控流状态
        return R.ok();
    }

    /**
     * 获取 OBS录制状态
     *
     * @param comId
     * @param accountId
     * @param isAdmin
     * @param dto
     * @return
     */
    @Override
    public R getObsRecordStatus(Long comId, Long accountId, Boolean isAdmin, GetLiveListDto dto) {
        //todo OBS录制状态

        return R.ok(0);
    }

    /**
     * 智能直播
     *
     * @param tetcId
     */

    @Override
    public void setLiveStatusByTetcId(String tetcId) {
        List<TencentTask> liveStreamBypushTypeAndRoomIdAndDeleteFlg = tencentTaskService.findLiveStreamBypushTypeAndRoomIdAndDeleteFlg(tetcId, 0);
        //判断当前直播间是否存在
        if (ObjectUtils.isEmpty(liveStreamBypushTypeAndRoomIdAndDeleteFlg)) {
            return;
        }
        //删除taskId
        for (TencentTask tencentTask : liveStreamBypushTypeAndRoomIdAndDeleteFlg) {
            tencentTask.setDeleteFlg(1);
            tencentTaskService.updateById(tencentTask);
        }

        LiveDat liveDat = this.baseMapper.selectOne(new LambdaQueryWrapper<LiveDat>()
                .eq(LiveDat::getTrtcId, tetcId)
                .eq(LiveDat::getDeleteFlg, false));
        if (ObjectUtils.isEmpty(liveDat)) {
            return;
        }
        liveDat.setLiveStatus(LiveStatusEnum.NOT_STARTED.getValue());
        this.updateById(liveDat);
        Long id = liveDat.getId();
        Long comId = liveDat.getComId();
        String liveStr = id.toString();
        //删除缓存
        clearcache.delLiveCache(comId, id);

        redisService.hDel(2, RedisConstants.LIVE_STATUS_LIST + comId, liveStr);
        redisService.del(2, RedisConstants.LIVE_STATUS + comId + Constants.UNDERLINE + liveStr);
        redisService.del("LIVE_RECORD_ID_" + liveStr);
        redisService.del(2, INTELLIGENT_LIVE_STATUS + liveStr);
//        // 修改智能直播记录状态
        intelligentLiveRecordMapper.update(null, new LambdaUpdateWrapper<IntelligentLiveRecord>()
                        .set(IntelligentLiveRecord::getStatus, 1)
//                .eq(IntelligentLiveRecord::getTemplateId, templateId)
                        .eq(IntelligentLiveRecord::getLiveId, id)
                        .eq(IntelligentLiveRecord::getComId, comId)
                        .eq(IntelligentLiveRecord::getDeleteFlg, false)
        );
        ImMsgQuery msgQuery = new ImMsgQuery();
        cn.hutool.json.JSONObject json = new cn.hutool.json.JSONObject();
        json.set("liveStatus", 0);
        json.set("userInfo", null);
        msgQuery.setCloudCustomData(JSON.toJSONString(json));
        msgQuery.setNotifyTypeEnum(TIMSendTypeEnum.LIVE_STATUS_UPDATE);
        msgQuery.setMsgContent("");
        msgQuery.setGroupId(liveDat.getImGroupId());
        msgQuery.setFromUserId("administrator");
        imService.sendCustomGroupMsg(msgQuery);

    }

    private LiveDat getDat(Long comId, Long liveId) {
        LiveDat liveDat = this.baseMapper.selectOne(new LambdaQueryWrapper<LiveDat>()
                .eq(LiveDat::getComId, comId)
                .eq(LiveDat::getId, liveId)
                .eq(LiveDat::getDeleteFlg, false));
        return liveDat;
    }

    public LiveConfig getLiveConfigByRoomAdd(AddLiveDto dto, Long comId, Long accountId, Long liveId, Integer maxOnlineNum) {
        // 插入房间配置表
        LiveConfig config = new LiveConfig();
        config.setId(idService.nextId());
        config.setComId(comId);
        config.setLiveId(liveId);
        config.setMobileThemeType(dto.getLiveTheme());
        config.setCreateBy(accountId);
        config.setMaxOnlineNum(maxOnlineNum);
        liveConfigMapper.insert(config);
        return config;
    }

    /**
     * 插入默认tag
     */
    private List<LiveTag> insertDefaultTag(Long liveId, Integer themeType, Long comId, Long accountId) {
        List<LiveTag> roomTagList = new ArrayList<>();
        //互动聊天
        LiveTag chat = new LiveTag(idService.nextId(), comId, liveId, TagTypeEnum.CHAT.getValue()
                , TagTypeEnum.CHAT.getDescription(), "", 0, accountId);
        roomTagList.add(chat);
//        //三分屏
        if (LiveThemeEnum.THREE_SPLIT_SCREEN.getValue() == themeType) {
            LiveTag three = new LiveTag(idService.nextId(), comId, liveId, TagTypeEnum.CLASS.getValue()
                    , TagTypeEnum.CLASS.getDescription(), "", 1, accountId);
            roomTagList.add(three);
        }
        // 批量插入
        liveTagMapper.insertBatch(roomTagList);
        return roomTagList;
    }

    //分页查询老师所在的直播间
    public static String getPlayUrl(CdnPullTypeEnum type, LiveRoomCdn liveRoomCdn, String trtcId, String expireDate) {
        String cdnSecretKey = liveRoomCdn.getCdnSecretKey();
        String url = getUrl(type, liveRoomCdn, trtcId, expireDate);
        if (ObjectUtil.isNotEmpty(cdnSecretKey) && ObjectUtil.isNotEmpty(expireDate)) {
            // 鉴权key不为空的话，计算鉴权参数并拼接
            String txSecretParams = getTxSecretParams(cdnSecretKey, trtcId, expireDate);
            url += txSecretParams;
        }
        return url;
    }

    private static String getUrl(CdnPullTypeEnum protocol, LiveRoomCdn liveRoomCdn, String trtcId, String httpPrefix) {
        // 地址组成
        // 播放域名+AppName+StreamName+鉴权信息
        StringBuilder urlBuilder = new StringBuilder();
        String defM3u8PullDomain = liveRoomCdn.getDefM3u8PullDomain();
        String webrtc = defM3u8PullDomain.replace("https", Constants.WEBRTC_HEADER);
        switch (protocol) {
            case RTMP ->
                //样例:rtmp://tpull.godsdo.com/live/10007
                    urlBuilder.append(liveRoomCdn.getDefRtmpPullDomain() + "/" + trtcId);
            case FLV ->
                //样例:https://tpull.godsdo.com/live/10007.flv
                    urlBuilder.append(liveRoomCdn.getDefFlvPullDomain() + "/" + trtcId + ".flv");
            case M3U8 ->
                //样例:https://tpull.godsdo.com/live/10007.m3u8
                    urlBuilder.append(liveRoomCdn.getDefM3u8PullDomain() + "/" + trtcId + Constants.TENCENT_M3U8_TEMPLATE + ".m3u8");
            case WEBRTC ->
                //样例:webrtc://live-test.miclass.net/AppName/StreamName
                    urlBuilder.append(webrtc + "/" + trtcId);
            default -> urlBuilder.append(webrtc + "/" + trtcId);


        }
        return urlBuilder.toString();
    }

    /**
     * 获取播放地址加密secret参数
     *
     * @param doMainKey  doMain鉴权 Key：用户自定义设置，支持大小写字母和数字。（配置路径：云直播 --> 域名管理 --> 鉴权配置）
     * @param streamName
     * @param expireDate
     * @return "?txSecret=xxxxxxxxx&txTime=xxxxx"
     */
    public static String getTxSecretParams(String doMainKey, String streamName, String expireDate) {
        // 获取16进制unix时间戳
//        String txTime = getUnixHexTimestamp(expireDate);
        StringBuilder paramsBuilder = new StringBuilder("?txSecret=");
        // 拉流 鉴权加密信息（key + streamName + txTime）
        String encodeData = doMainKey + streamName + Constants.TENCENT_M3U8_TEMPLATE + expireDate;
        // Md5加密
        String txSecret = DigestUtil.md5Hex(encodeData);
        paramsBuilder.append(txSecret);
        paramsBuilder.append("&txTime=");
        paramsBuilder.append(expireDate);
        return paramsBuilder.toString();
    }


}
