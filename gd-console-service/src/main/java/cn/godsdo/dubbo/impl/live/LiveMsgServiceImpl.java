package cn.godsdo.dubbo.impl.live;

import cloud.tianai.captcha.common.util.CollectionUtils;
import cn.godsdo.constant.MongoConstant;
import cn.godsdo.constant.MsgTypeConstants;
import cn.godsdo.constant.TextType;
import cn.godsdo.dto.QueryLiveUserWatchDto;
import cn.godsdo.dto.assistan.GetActionUsersDto;
import cn.godsdo.dto.intelligent.GetTemplateInfoDto;
import cn.godsdo.dto.intelligent.ImportMessagesDataDto;
import cn.godsdo.dto.intelligent.UpdateTemplateDto;
import cn.godsdo.dto.msg.*;
import cn.godsdo.dubbo.camp.TrainingCampLiveUserService;
import cn.godsdo.dubbo.channel.ChannelGroupAdminService;
import cn.godsdo.dubbo.channel.ComChannelDatService;
import cn.godsdo.dubbo.crossService.RedisDubboService;
import cn.godsdo.dubbo.im.ImService;
import cn.godsdo.dubbo.live.LiveMsgService;
import cn.godsdo.dubbo.user.UserDatService;
import cn.godsdo.entity.camp.TrainingCampLiveUser;
import cn.godsdo.entity.channel.ChannelGroupAdmin;
import cn.godsdo.entity.channel.ComChannelDat;
import cn.godsdo.entity.com.ComCommodity;
import cn.godsdo.entity.user.UserDat;
import cn.godsdo.enums.im.AuditEnum;
import cn.godsdo.enums.im.SendUserRole;
import cn.godsdo.mapper.channel.ComChannelDatMapper;
import cn.godsdo.mapper.live.ComCommodityMapper;
import cn.godsdo.query.im.ImMsgQuery;
import cn.godsdo.util.R;
import cn.godsdo.util.TimeUtil;
import cn.godsdo.vo.LiveUserStateChangeVo;
import cn.godsdo.vo.intelligent.CommodityMsgVo;
import cn.godsdo.vo.intelligent.LiveCommodityMsgVo;
import cn.godsdo.vo.intelligent.LiveInteractMsgVo;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.extra.emoji.EmojiUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.y20y.constant.Constants;
import com.y20y.interfaces.IdService;
import com.y20y.utils.AESUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

/**
 * @Author: CR7
 * @Date: 2019/5/5 15:19
 * @Description:
 */
@Slf4j
@DubboService
public class LiveMsgServiceImpl implements LiveMsgService {

    @DubboReference(check = false)
    private RedisDubboService redisService;

    @DubboReference(check = false, retries = 0)
    private ImService imService;

    @DubboReference(check = false)
    private IdService idService;

    @DubboReference(check = false)
    private ComChannelDatService comChannelDatService;

    @DubboReference(check = false)
    private ChannelGroupAdminService channelGroupAdminService;

    @DubboReference(check = false)
    private TrainingCampLiveUserService trainingCampLiveUserService;

    @DubboReference(check = false)
    UserDatService userDatService;

    @Resource
    private ComCommodityMapper comCommodityMapper;

    @Resource
    private ComChannelDatMapper comChannelDatMapper;

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public R audit(AuditMsgDTO messageDTO) {
        log.info("审核消息内容：{}", JSON.toJSONString(messageDTO));
        String key = messageDTO.getLiveId() + "_" + messageDTO.getMsgSeq();
        if (isNull(redisService.get(key))) {
            redisService.setex(key, "0", 60 * 60);
            CompletableFuture.runAsync(() -> auditMessage(messageDTO));
            return R.ok("消息已发送至群组");
        } else {
            return R.ok("消息已被审核,请勿重复操作");
        }
    }

    private void auditMessage(AuditMsgDTO messageDTO) {
        ImMsgQuery msgQuery = new ImMsgQuery();
        MyCustomData text = new MyCustomData();
        MyCustomData.UserInfo userInfo = new MyCustomData.UserInfo();

        userInfo.setSendUserAvatar(messageDTO.getLiveUserAvatar());
        userInfo.setSendNickName(messageDTO.getLiveUsername());
        userInfo.setSendUserId(messageDTO.getLiveUserId());
        userInfo.setOperateUserId(messageDTO.getUserId());
        userInfo.setOperateNickName("");
        userInfo.setSendUserRole("audience");
        userInfo.setComId(messageDTO.getComId());

        MyCustomData.MessageInfo messageInfo = new MyCustomData.MessageInfo();
        messageInfo.setMsgSeq(messageDTO.getMsgSeq());
        messageInfo.setType(messageDTO.getOptType());
        messageInfo.setNoticeType(0);

        text.setUserInfo(userInfo);
        text.setMessageInfo(messageInfo);

        String json = JSON.toJSONString(text);
        msgQuery.setCloudCustomData(json);
        msgQuery.setMsgContent(messageDTO.getMessage());
        msgQuery.setMsgType(MsgTypeConstants.TIM_TEXT_ELEM);
        msgQuery.setGroupId(messageDTO.getImGroupId());
        msgQuery.setFromUserId(String.valueOf(messageDTO.getLiveUserId()));

        imService.sendGroupMsg(msgQuery);
    }

    @Override
    public R auditList(AuditMessageQuery messageQuery) {
        Sort sort = Sort.by(Sort.Direction.ASC, "createAt");
        Query query = new Query(Criteria.where("liveId").is(messageQuery.getLiveId())
                .and("status").is(0))
                .with(sort).limit(200);
        val messageList = mongoTemplate.find(query, LiveAuditMessage.class, MongoConstant.LIVE_AUDIT_MESSAGE);
        return R.ok(messageList);
    }


    @Override
    public void statStateChange(JSONObject jsonObject) {
        JSONObject info = jsonObject.getJSONObject("Info");
        String account = info.getString("To_Account");
        String action = info.getString("Action");
        String reason = info.getString("Reason");
        String key = "IM_" + account;
        String keyCamp = "IM_CAMP_" + account;

        String userInfo = (String) redisService.get(key);
        String userInfoCamp = (String) redisService.get(keyCamp);

        if (StringUtils.isNotBlank(userInfo)) {
            LiveUserStateChange liveUserStateChange = new LiveUserStateChange();

            String[] split = StringUtils.split(userInfo, "###");
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
            liveUserStateChange.setLiveUserId(Long.valueOf(account));
            liveUserStateChange.setClientIp(jsonObject.getString("ClientIP"));
            liveUserStateChange.setLiveUsername(name);
            liveUserStateChange.setUserType(userType);
            liveUserStateChange.setEventType(action);
            liveUserStateChange.setEventReason(reason);
            liveUserStateChange.setEventTime(new Date());
            if (StringUtils.isNotBlank(isNew)) {
                liveUserStateChange.setIsNew(Integer.parseInt(isNew));
            }
            mongoTemplate.insert(liveUserStateChange, MongoConstant.LIVE_USER_STATE_CHANGE_DAY);

            // 训练营签到
            if ("Login".equals(action) && StringUtils.isNotBlank(userInfoCamp)) {
                String[] splitCamp = StringUtils.split(userInfoCamp, "###");
                if (splitCamp.length > 1 && !"0".equals(splitCamp[1])) {
                    String liveType = splitCamp[0];
                    String unionId = splitCamp[1];
                    if ("2".equals(liveType)) {
                        updateCampLiveUser(Long.parseLong(comId), Long.parseLong(liveId), unionId);
                    }
                }
            }
        }
    }

    private void updateCampLiveUser(Long comId, Long liveId, String unionId) {
        TrainingCampLiveUser campLiveUser = trainingCampLiveUserService.getCampLiveUserByCache(comId, liveId, unionId);
        if (null != campLiveUser && campLiveUser.getSignIn() == 0) {
            campLiveUser.setSignIn(1);
            trainingCampLiveUserService.updateCampLiveUserCache(campLiveUser, liveId, unionId);
        }
    }

    @Override
    public R getOfflineUserList(GetActionUsersDto dto) {
        Assert.notNull(dto.getLiveId(), "liveId不能为空");
        Assert.notNull(dto.getEventType(), "事件类型不能为空");
        // 获取在线离线用户列表
        Criteria criteria = Criteria.where("liveId").is(dto.getLiveId());
        if (StringUtils.isNotEmpty(dto.getNickname())) {
            // 模糊查询
            criteria.and("liveUsername").regex("^.*" + dto.getNickname() + ".*$");
        }
        if (!"Login".equals(dto.getEventType()) && dto.getTime() != null && dto.getTime() > 0) {
            criteria.and("eventTime").gte(DateUtils.addMinutes(new Date(), -dto.getTime()));
        }

        if (StringUtils.isNotEmpty(dto.getChannelIds())) {
            // 训练营协助人
            String[] channelIds = dto.getChannelIds().split(",");
            List<Long> channelIdList = new ArrayList<>();
            for (String channelId : channelIds) {
                if (NumberUtil.isLong(channelId) && !"1".equals(channelId)) {
                    channelIdList.add(Long.parseLong(channelId));
                }
            }
            if (!CollectionUtils.isEmpty(channelIdList)) {
                criteria.and("channelId").in(channelIdList);
            } else {
                return R.ok(new ArrayList<>());
            }
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
        } else if (NumberUtil.isLong(dto.getChannelId()) && !"0".equals(dto.getChannelId())) {
            //是否是管理员
            List<ChannelGroupAdmin> list = channelGroupAdminService.getListByChannelId(dto.getChannelId());
            if (CollectionUtils.isEmpty(list)) {
                //不是管理员
                criteria.and("channelId").is(Long.parseLong(dto.getChannelId()));
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

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.sort(Sort.Direction.DESC, "_id"),
                Aggregation.group("liveUserId")
                        .first("liveUserId").as("liveUserId")
                        .first("liveUsername").as("liveUsername")
                        .first("isNew").as("isNew")
                        .first("channelId").as("channelId")
                        .first("channelName").as("channelName")
                        .first("userType").as("userType")
                        .first("headImgUrl").as("headImgUrl")
                        .first("eventType").as("eventType"),
                Aggregation.project()
                        .and("liveUsername").previousOperation()//必须添加一个字段的 previousOperation 否则会报错
                        .and("liveUserId").as("liveUserId")
                        .and("liveUsername").as("liveUsername")
                        .and("isNew").as("isNew")
                        .and("channelId").as("channelId")
                        .and("channelName").as("channelName")
                        .and("userType").as("userType")
                        .and("headImgUrl").as("headImgUrl")
                        .and("eventType").as("eventType"),
                Aggregation.sort(Sort.Direction.DESC, "liveUserId")
        );
        AggregationResults<LiveUserStateChangeVo> results = mongoTemplate.aggregate(aggregation, MongoConstant.LIVE_USER_STATE_CHANGE_DAY, LiveUserStateChangeVo.class);
        List<LiveUserStateChangeVo> liveUserStateChangesLogin = new ArrayList<>();
        List<LiveUserStateChangeVo> liveUserStateChanges = new ArrayList<>();//Logout / Disconnect
        for (LiveUserStateChangeVo liveUserStateChange : results.getMappedResults()) {
            if ("Login".equals(liveUserStateChange.getEventType())) {
                liveUserStateChangesLogin.add(liveUserStateChange);
            } else {
                liveUserStateChanges.add(liveUserStateChange);
            }
        }
        if (dto.getEventType().equals("Login")) {
            return R.ok(liveUserStateChangesLogin);
        } else {
            return R.ok(liveUserStateChanges);
        }
    }

    @Override
    public R getTemplateMsg(Long comId, GetTemplateInfoDto dto) {
        Query query = null;
        Criteria textType = Criteria.where("textType").in(TextType.TEXT, TextType.TEXT_FLOWER, TextType.TEXT_LIKE, TextType.TEXT_GIFT, TextType.TEXT_REPLY);

        Query queryTotal = new Query(textType);
        Long total = mongoTemplate.count(queryTotal, LiveInteractMsgVo.class, MongoConstant.LIVE_TEMPLATE_MESSAGE + "_" + dto.getTemplateId());
        if(total<1){
            return R.ok();
        }
        query = new Query(textType);
        // 排序
        Sort sort = Sort.by(Sort.Direction.ASC, "sendTime");
        query.with(sort);
        query.skip((long) (dto.getPage() - 1) * dto.getPageSize()).limit(dto.getPageSize());
        val messageList = mongoTemplate.find(query, LiveInteractMsgVo.class, MongoConstant.LIVE_TEMPLATE_MESSAGE + "_" + dto.getTemplateId());

        if (ObjectUtils.isEmpty(messageList)) {
            return R.ok(null,total);
        }
        // 查询历史数据

        List<LiveInteractMsgVo> list = messageList.stream().map(e -> {
            String username = e.getUsername();
            if(StringUtils.isNotEmpty(username)){
                e.setUsername(EmojiUtil.toUnicode(username));
            }
            return e;
        }).toList();

//        messageList.parallelStream().forEach(message -> {
//            val jsonObject = JSON.parseObject(message.getMessage());
//            if (TextType.TEXT.equals(message.getTextType())) {
//                message.setText(jsonObject.getString("Text"));
//                //文本消息
//            }
//            if (TextType.TEXT_GIFT.equals(message.getTextType())) {
//                //礼物消息
//                val data = jsonObject.getString("Data");
//                val object = JSON.parseObject(data);
//                message.setGiftId(object.getLong("giftId"));
//                message.setGiftName(object.getString("giftName"));
//                message.setGiftImgUrl(object.getString("imgUrl"));
//            } else if (TextType.TEXT_REPLY.equals(message.getTextType())) {
//                //回复消息
//                message.setText(jsonObject.getString("Data"));
//                message.setReplyText(jsonObject.getString("Ext"));
//            }
//        });
        return R.ok(list, total);
    }

    @Override
    public void saveImMsg(JSONObject jsonObject) {
        try {
            CallbackMessage tencentMessage = new CallbackMessage();
            tencentMessage.setClientIp(jsonObject.getString("ClientIP"));
            tencentMessage.setOptPlatform(jsonObject.getString("OptPlatform"));
            val groupId = jsonObject.getString("GroupId");
            val fromAccount = jsonObject.getString("From_Account");
            val cloudCustomData = jsonObject.getString("CloudCustomData");
            val jsonArray = jsonObject.getJSONArray("MsgBody");
            val jsonObj = jsonArray.getJSONObject(0);
            val msgType = jsonObj.getString("MsgType");
            val msgContent = jsonObj.getString("MsgContent");
            val msgSeq = jsonObject.getInteger("MsgSeq");
            val msgTime = jsonObject.getInteger("MsgTime");
            tencentMessage.setFromAccount(fromAccount);
            tencentMessage.setMsgTime(msgTime);
            tencentMessage.setMsgSeq(msgSeq);
            tencentMessage.setCloudCustomData(cloudCustomData);
            tencentMessage.setGroupId(groupId);
            tencentMessage.setMsgType(msgType);
            tencentMessage.setMsgContent(msgContent);

            log.info("CallbackAfterSendMsg 消息回调：{}", JSON.toJSONString(tencentMessage));

            val split = StringUtils.split(groupId, "_");
            val comId = Long.parseLong(split[1]);
            val liveId = Long.parseLong(split[2]);

            //解析消息类型
            MyCustomData customData = JSON.parseObject(cloudCustomData, MyCustomData.class);

            Date date = DateUtils.addHours(new Date(tencentMessage.getMsgTime() * 1000L), 8);

            // 待审核消息
            boolean pendingApprovalMessage = false;
            // 审核通过消息
            boolean passMessage = false;
            // 审核拒绝消息
            boolean rejectMessage = false;

            int textType = 0;
            if (MsgTypeConstants.TIM_TEXT_ELEM.equals(msgType)) {
                if (Objects.nonNull(customData) && Objects.nonNull(customData.getMessageInfo()) && StringUtils.isNotBlank(customData.getMessageInfo().getType())) {
                    switch (customData.getMessageInfo().getType()) {
                        case "PASS" -> passMessage = true;
                        case "REJECT", "FORBID", "BAN" -> rejectMessage = true;
                        default -> pendingApprovalMessage = true;
                    }
                }
            }

            Integer notifyType = null;
            if (MsgTypeConstants.TIM_CUSTOM_ELEM.equals(msgType)) {
                if (Objects.nonNull(msgContent)) {
                    MsgContent content = JSON.parseObject(msgContent, MsgContent.class);
                    if (Objects.nonNull(content) && StringUtils.isNotBlank(content.getData())) {
                        if (MsgDesc.GIFT.equals(content.getDesc())) {
                            //礼物消息
                            textType = TextType.TEXT_GIFT;
                            String msgData = content.getData();
                            MsgContent.MsgData data = JSON.parseObject(msgData, MsgContent.MsgData.class);
                            if (Objects.nonNull(data)) {
                                notifyType = data.getNotifyType();
                            }
                        }
                        //消息回复
                        if (MsgDesc.RECOVER.equals(content.getDesc())) {
                            textType = TextType.TEXT_REPLY;
                        }

                        if (MsgDesc.ROOM_PRODUCT_NOT_SOLD_OUT.equals(content.getDesc())
                                || MsgDesc.ROOM_PRODUCT_OFF_SALE.equals(content.getDesc())
                                || MsgDesc.ROOM_PRODUCT_ON_SALE.equals(content.getDesc())
                                || MsgDesc.ROOM_PRODUCT_SOLD_OUT.equals(content.getDesc())
                                || MsgDesc.ROOM_PRODUCT_RECOMMEND.equals(content.getDesc())
                                || MsgDesc.ROOM_PRODUCT_UN_RECOMMEND.equals(content.getDesc())
                        ) {
                            //商品消息
                            textType = TextType.TEXT_PRODUCT;
                            String msgData = content.getData();
                            MsgContent.MsgData data = JSON.parseObject(msgData, MsgContent.MsgData.class);
                            if (Objects.nonNull(data)) {
                                notifyType = data.getNotifyType();
                            }
                        }
                    }
                }
            }

            Long liveUserId = null;
            String liveUserName = null;
            Long operateUserId = null;
            String sendUserRole = null;
            String sendUserAvatar = null;
            String channelId = null;
            String channelName = null;
            if (Objects.nonNull(customData) && Objects.nonNull(customData.getUserInfo())) {
                liveUserId = customData.getUserInfo().getSendUserId();
                liveUserName = customData.getUserInfo().getSendNickName();
                operateUserId = customData.getUserInfo().getOperateUserId();
                sendUserRole = customData.getUserInfo().getSendUserRole();
                sendUserAvatar = customData.getUserInfo().getSendUserAvatar();
                channelId = customData.getUserInfo().getChannelId();
                channelName = customData.getUserInfo().getChannelName();
            }
            if (isNull(liveUserId)) {
                try {
                    liveUserId = Long.valueOf(tencentMessage.getFromAccount());
                } catch (Exception e) {
                    log.error("long valueOf error! fromAccount:{}", tencentMessage.getFromAccount());
                }
            }

            if (pendingApprovalMessage) {
                // 待审核消息
                LiveAuditMessage liveAuditMessage = new LiveAuditMessage();
                liveAuditMessage.setLiveId(liveId);
                liveAuditMessage.setComId(comId);
                liveAuditMessage.setLiveUserId(liveUserId);
                liveAuditMessage.setLiveUserName(liveUserName);
                //增加头像
                liveAuditMessage.setLiveUserAvatar(sendUserAvatar);
                liveAuditMessage.setOperateUserId(operateUserId);
                liveAuditMessage.setMessage(msgContent);
                liveAuditMessage.setMsgType(msgType);
                liveAuditMessage.setMsgSeq(tencentMessage.getMsgSeq());
                liveAuditMessage.setCreateAt(date);
                liveAuditMessage.setClientIp(tencentMessage.getClientIp());
                liveAuditMessage.setOptPlatform(tencentMessage.getOptPlatform());
                //待审核
                liveAuditMessage.setStatus(AuditEnum.NOT_AUDIT.getValue());

                if (NumberUtil.isLong(channelId) && !"0".equals(channelId)) {
                    liveAuditMessage.setChannelId(Long.parseLong(channelId));
                    liveAuditMessage.setChannelName(channelName);
                }

                log.info("*************************** 待审核消息 新增字段 MsgContent ***************************");
                JSONObject jsonObjText = jsonObj.getJSONObject("MsgContent");
                log.info("jsonObjText ：{}", jsonObjText);
                liveAuditMessage.setMessageText(jsonObjText.getString("Text"));

                log.info("*************************** 待审核消息 入库 start ***************************");

                mongoTemplate.insert(liveAuditMessage, MongoConstant.LIVE_AUDIT_MESSAGE);
                log.info("*************************** 待审核消息 入库 end ***************************");
            }

            // 更新审核消息表
            if ((passMessage || rejectMessage) && Objects.nonNull(customData.getMessageInfo().getMsgSeq())) {
                Query query = new Query(Criteria.where("liveId").is(liveId)
                        .and("msgSeq").is(customData.getMessageInfo().getMsgSeq()));
                Update update = new Update();
                update.set("updateAt", date);
                update.set("updateBy", operateUserId);
                update.set("comId", comId);
                if (passMessage) {
                    // 审核通过消息
                    update.set("status", 1);
                }
                if (rejectMessage) {
                    // 审核拒绝消息
                    update.set("status", -1);
                }
                mongoTemplate.findAndModify(query, update,
                        LiveAuditMessage.class, MongoConstant.LIVE_AUDIT_MESSAGE);
            }

            // 除了待审核和审核拒绝消息 其余入库公屏消息表
            if (!pendingApprovalMessage && !rejectMessage) {
                LiveMessage liveMessage = new LiveMessage();
                liveMessage.setLiveId(liveId);
                liveMessage.setComId(comId);
                liveMessage.setLiveUserId(liveUserId);
                liveMessage.setLiveUsername(liveUserName);
                liveMessage.setMessage(msgContent);
                liveMessage.setClientIp(tencentMessage.getClientIp());
                liveMessage.setOptPlatform(tencentMessage.getOptPlatform());
                liveMessage.setCreateAt(date);
                liveMessage.setMsgSeq(tencentMessage.getMsgSeq());
                liveMessage.setMsgType(msgType);
                liveMessage.setMsgTime(tencentMessage.getMsgTime());
                liveMessage.setCloudCustomData(cloudCustomData);
                liveMessage.setLiveUserRole(sendUserRole);
                liveMessage.setLiveUserAvatar(sendUserAvatar);
                liveMessage.setNotifyType(notifyType);
                //文本类型
                liveMessage.setTextType(textType);
                //渠道
                if (NumberUtil.isLong(channelId) && !"0".equals(channelId)) {
                    liveMessage.setChannelId(Long.parseLong(channelId));
                    liveMessage.setChannelName(channelName);
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String statisticalDate = sdf.format(liveMessage.getMsgTime() * 1000);

                //消息内容 记录公屏消息 不记录白板消息
                if (MsgTypeConstants.TIM_TEXT_ELEM.equals(msgType)) {
                    JSONObject jsonObjText = jsonObj.getJSONObject("MsgContent");
                    liveMessage.setMessageText(jsonObjText.getString("Text"));
                    mongoTemplate.insert(liveMessage, MongoConstant.LIVE_MESSAGE);
                    mongoTemplate.insert(liveMessage, MongoConstant.LIVE_MESSAGE + "_" + statisticalDate);
                } else if (MsgTypeConstants.TIM_CUSTOM_ELEM.equals(msgType)) {
                    JSONObject jsonObjText = jsonObj.getJSONObject("MsgContent");
                    if (StringUtils.isBlank(jsonObjText.getString("Ext")) || !"TXWhiteBoardExt".equals(jsonObjText.getString("Ext"))) {
                        liveMessage.setMessageText("自定义消息");
                        mongoTemplate.insert(liveMessage, MongoConstant.LIVE_MESSAGE);
                        mongoTemplate.insert(liveMessage, MongoConstant.LIVE_MESSAGE + "_" + statisticalDate);
                    }
                }
            }
        } catch (Exception e) {
            log.error("handleCallbackAfterSendMsg error!", e);
        }
    }


    @Override
    public R addInteractMsg(Long comId, Long accountId, LiveInteractMsgVo dto) {
        try {
            Long templateId = dto.getTemplateId();
            dto.setCreateBy(accountId);
            dto.setComId(comId);
            dto.setId(null);
            // 插入消息列表
            mongoTemplate.insert(dto, MongoConstant.LIVE_TEMPLATE_MESSAGE + "_" + templateId);
        } catch (Exception e) {
            log.error("addInteractMsg error!", e);
        }
        return R.ok();
    }

    @Override
    public R updateInteractMsg(Long comId, Long accountId, LiveInteractMsgVo dto) {
        dto.setUpdateBy(accountId);

        Query query = null;
        // 查找id，及文本消息是否存在
        query = new Query(Criteria.where("_id").is(dto.getId()));
        // 修改id
        dto.setId(null);
        String collectionName = MongoConstant.LIVE_TEMPLATE_MESSAGE + "_" + dto.getTemplateId();
        mongoTemplate.remove(query, collectionName);
        mongoTemplate.insert(dto, collectionName);
        return R.ok();
    }

    @Override
    public R deleteInteractMsg(Long comId, Long accountId, Long templateId, String id) {
        Query query = null;
        // 查找id，及文本消息是否存在
        query = new Query(Criteria.where("_id").is(id));
        mongoTemplate.remove(query, MongoConstant.LIVE_TEMPLATE_MESSAGE + "_" + templateId);
        return R.ok();
    }

    @Override
    public R batchDeleteInteractMsg(Long comId, Long accountId, UpdateTemplateDto dto) {
        List<String> ids = dto.getIds();
        if (CollectionUtils.isEmpty(ids)) {
            return R.failed("操作的数据不能为空");
        }
        Long templateId = dto.getTemplateId();
        Query query = null;
        // 查找id，及文本消息是否存在
        query = new Query(Criteria.where("_id").in(ids));
        mongoTemplate.remove(query, MongoConstant.LIVE_TEMPLATE_MESSAGE + "_" + templateId);
        return R.ok();
    }

    @Override
    public R batchMoveInteractMsg(Long comId, Long accountId, UpdateTemplateDto dto) {
        // 获取DTO中的ids列表
        List<String> ids = dto.getIds();
        // 如果ids列表为空，返回错误响应
        if (CollectionUtils.isEmpty(ids)) {
            return R.failed("操作的数据不能为空");
        }
        // 构建查询条件
        Query query = null;
        query = new Query(Criteria.where("_id").in(ids));
        // 拼接集合名称
        String collectionName = MongoConstant.LIVE_TEMPLATE_MESSAGE + "_" + dto.getTemplateId();
        // 在指定集合中根据查询条件查找消息列表
        val messageList = mongoTemplate.find(query, LiveInteractMsgVo.class, collectionName);
        // 修改消息列表中的数据并存入新的列表
        List<LiveInteractMsgVo> list = messageList.stream().map(e -> {
            e.setId(null);
            Long second = dto.getSecond();
            Long l = 0L;
            if (dto.getType() == 0) {
                l = e.getSendTime() - second;
            } else {
                l = e.getSendTime() + second;
            }
            e.setSendTime(l);
            return e;
        }).toList();
        // 根据条件在集合中查找并删除消息
        mongoTemplate.remove(query, collectionName);
        // 将修改后的消息列表插入到集合中
        mongoTemplate.insert(list, collectionName);
        // 返回成功响应
        return R.ok();
    }

    @Override
    public R getProductMsg(Long comId, Long templateId) {
        // 时间使用秒
        // 创建查询条件对象
        Query query = new Query(Criteria.where("textType").is(TextType.TEXT_PRODUCT));
        // 排序
        Sort sort = Sort.by(Sort.Direction.ASC, "sendTime");
        query.with(sort);
        // 通过查询条件从数据库中获取消息列表
        val messageList = mongoTemplate.find(query, LiveInteractMsgVo.class, MongoConstant.LIVE_TEMPLATE_MESSAGE + "_" + templateId);
        // 如果消息列表为空，则返回操作成功的响应
        if (CollectionUtils.isEmpty(messageList)) {
            return R.ok();
        }
        // 将消息列表转换为商品消息实体对象列表
        val listMap = messageList.stream().map(message -> {
                    val jsonObject = JSON.parseObject(message.getMessage());
                    val data = jsonObject.getString("Data");
                    val desc = jsonObject.getString("Desc");
                    val object = JSON.parseObject(data);
                    return CommodityMsgVo.builder()
                            .commodityId(object.getLong("commodityId"))
                            .action(desc)
                            .commodityName(object.getString("commodityName"))
                            .image(object.getString("image"))
                            .commodityPrice(object.containsKey("commodityPrice") ? object.getString("commodityPrice") : null)
                            // 换算成秒，距离开始时间的秒数
                            .sendTime(message.getSendTime())
                            .notifyType(object.getInteger("notifyType"))
                            .payType(object.getInteger("payType"))
                            .jumpAddress(object.getString("jumpAddress"))
                            .liveUserRole(message.getLiveUserRole())
                            .message(message.getMessage())
                            .cloudCustomData(message.getCloudCustomData())
                            .comId(message.getComId())
                            .templateId(message.getTemplateId())
                            .textType(message.getTextType())
                            .build();
                }
        ).collect(Collectors.groupingBy(CommodityMsgVo::getCommodityId, Collectors.groupingBy(CommodityMsgVo::getCommodityName)));
        // 创建商品消息实体对象列表
        List<LiveCommodityMsgVo> list = new ArrayList<>(listMap.size());
        // 遍历商品消息映射对象，并转换为商品消息实体对象列表
        listMap.forEach((key, products) -> {
            val msgVO = LiveCommodityMsgVo.builder().commodityId(key).build();
            products.forEach((name, value) -> {
                msgVO.setCommodityName(name);
                msgVO.setMsgList(value);
            });
            list.add(msgVO);
        });

        return R.ok(list);
    }

    @Override
    public R saveProductMsg(Long comId, Long accountId, LiveCommodityMsgVo vo) {
        Long templateId = vo.getTemplateId();
        Query query = new Query(Criteria.where("textType").is(TextType.TEXT_PRODUCT));
        // 删除所有的商品记录
        mongoTemplate.remove(query, MongoConstant.LIVE_TEMPLATE_MESSAGE + "_" + templateId);
        if (CollectionUtils.isEmpty(vo.getMsgList())) {
            //没有消息
            return R.ok();
        }
        val messageList = vo.getMsgList().stream()
                .map(this::buildTemplateMessage).toList();

        // 新增记录
        mongoTemplate.insert(messageList, MongoConstant.LIVE_TEMPLATE_MESSAGE + "_" + templateId);
        return R.ok();
    }

    @Override
    public R queryMessage(QueryLiveUserWatchDto dto) {
        List<LiveMessage> messageList = new ArrayList<>();
        Criteria criteria = Criteria.where("liveId").is(dto.getLiveId())
                .and("msgType").is(MsgTypeConstants.TIM_TEXT_ELEM);
        if (StringUtils.isNotBlank(dto.getNickName())) {
            criteria.and("liveUsername").regex("^.*" + dto.getNickName() + ".*$");
        }
        Query query = new Query(criteria);

        long total = mongoTemplate.count(query, LiveMessage.class, MongoConstant.LIVE_MESSAGE + "_" + dto.getQueryDate());
        if (total > 0) {
            // 排序
            Sort sort = Sort.by(Sort.Direction.DESC, "msgTime");
            query.with(sort);
            // 分页 (当前页-1)*每页大小，每页大小
            if (null != dto.getPageSize()) {
                query.skip((long) (dto.getPageNum() - 1) * dto.getPageSize()).limit(dto.getPageSize());
            }
            messageList = mongoTemplate.find(query, LiveMessage.class, MongoConstant.LIVE_MESSAGE + "_" + dto.getQueryDate());
        }
        return R.ok(messageList, total);
    }

    @Override
    public List<LiveMessage> getHistoryMessage(Long liveId) {
        String statisticalDate = DateFormatUtils.format(new Date(), "yyyy-MM-dd");
        Sort sort = Sort.by(Sort.Direction.DESC, "msgTime");
        Query query = new Query(Criteria.where("liveId").is(liveId)
                .and("msgType").is(MsgTypeConstants.TIM_TEXT_ELEM))
                .with(sort).limit(10);
        return mongoTemplate.find(query, LiveMessage.class, MongoConstant.LIVE_MESSAGE + "_" + statisticalDate);
//        return mongoTemplate.find(query, LiveMessage.class, MongoConstant.LIVE_MESSAGE);
    }

    @Override
    public List<LiveAuditMessage> queryNotPassMessage(QueryLiveUserWatchDto dto) {
        Criteria criteria = Criteria.where("liveId").is(dto.getLiveId()).and("status").is(AuditEnum.NOT_PASS.getValue());
        if (StringUtils.isNotBlank(dto.getNickName())) {
            criteria.and("liveUserName").regex("^.*" + dto.getNickName() + ".*$");
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateStart = dto.getQueryDate() + " 00:00:00";
        String dateEnd = dto.getQueryDate() + " 23:59:59";
        Date start0;
        Date start24;
        try {
            start0 = formatter.parse(dateStart);
            start24 = formatter.parse(dateEnd);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        criteria.andOperator(
                Criteria.where("createAt").gte(start0),
                Criteria.where("createAt").lte(start24)
        );
        log.info("start 0 :" + start0);
        log.info("start24 :" + start24);
        log.info("new Date() :" + new Date());
        List<LiveAuditMessage> userList = new ArrayList<>();
        Query queryUser = new Query(criteria);
        long total = mongoTemplate.count(queryUser, LiveAuditMessage.class, MongoConstant.LIVE_AUDIT_MESSAGE);
        log.info("queryNotPassMessage total:{}", total);
        if (total > 0) {
            // 排序
            Sort sort = Sort.by(Sort.Direction.DESC, "createAt");
            queryUser.with(sort);
            // 分页 (当前页-1)*每页大小，每页大小
            queryUser.skip((long) (dto.getPageNum() - 1) * dto.getPageSize()).limit(dto.getPageSize());
            userList = mongoTemplate.find(queryUser, LiveAuditMessage.class, MongoConstant.LIVE_AUDIT_MESSAGE);
//            log.info("userList.get(0).getCreateAt() :" + userList.get(0).getCreateAt());
        }
        return userList;
    }

    @Override
    public List<LiveInteractMsgVo> downMessagesByTemplate(Long templateId) {
        List<LiveInteractMsgVo> messageList = new ArrayList<>();
        try {
            Assert.notNull(templateId, "模版id不能为空");
        } catch (IllegalArgumentException e) {
            return messageList;
        }
        try {
            Sort sort = Sort.by(Sort.Direction.ASC, "sendTime");
            Query query = new Query(Criteria.where("textType").is(0))
                    .with(sort);
            messageList = mongoTemplate.find(query, LiveInteractMsgVo.class, MongoConstant.LIVE_TEMPLATE_MESSAGE + "_" + templateId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return messageList;
    }

    @Override
    public List<LiveMessage> queryMessageAll(QueryLiveUserWatchDto dto) {
        Criteria criteria = Criteria.where("liveId").is(dto.getLiveId())
                .and("msgType").is(MsgTypeConstants.TIM_TEXT_ELEM);
        if (StringUtils.isNotBlank(dto.getNickName())) {
            criteria.and("liveUsername").regex("^.*" + dto.getNickName() + ".*$");
        }
        Query query = new Query(criteria);
        // 排序
        Sort sort = Sort.by(Sort.Direction.ASC, "msgTime");
        query.with(sort);
        return mongoTemplate.find(query, LiveMessage.class, MongoConstant.LIVE_MESSAGE + "_" + dto.getQueryDate());
    }

    @Override
    public R importMessagesData(Long templateId, List<ImportMessagesDataDto> list,Long comId ) {
//        mongoTemplate.find(query, LiveInteractMsgVo.class, MongoConstant.LIVE_MESSAGE + "_" + templateId);
        List<LiveInteractMsgVo> data = list.stream().map(message -> {
            Long userId = message.getUserId();
            String userName = message.getUserName();
            Criteria criteria = Criteria.where("liveUserId").is(userId)
                    .and("msgType").is(MsgTypeConstants.TIM_TEXT_ELEM);
            Query query = new Query(criteria);
            // 排序
            Sort sort = Sort.by(Sort.Direction.ASC, "msgTime");
            query.with(sort);
            // 分页 (当前页-1)*每页大小，每页大小
            // 只取一条
            query.skip(0).limit(1);
            List<LiveMessage> liveMessages = mongoTemplate.find(query, LiveMessage.class, MongoConstant.LIVE_MESSAGE);
            LiveInteractMsgVo vo = new LiveInteractMsgVo();
            if (ObjectUtils.isNotEmpty(liveMessages)) {
                LiveMessage liveMessage = liveMessages.get(0);
//                vo.setUsername(liveMessage.getLiveUserAvatar());
                vo.setUserAvatar(liveMessage.getLiveUserAvatar());
            }

            vo.setComId(comId);
            vo.setTemplateId(templateId);
            vo.setUserId(userId);
            vo.setUsername(userName);

            String msg = message.getMessage();
            vo.setMessageText(msg);
            vo.setLiveUserRole(SendUserRole.audience.getCode());
            vo.setTextType(TextType.TEXT);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("Text", msg);
            vo.setMessage(JSONObject.toJSONString(jsonObject));
            String time = message.getTime();
            vo.setSendTime(TimeUtil.returnSeconde(time));
            return vo;
        }).toList();
        mongoTemplate.insert(data, MongoConstant.LIVE_TEMPLATE_MESSAGE + "_" + templateId);
        return R.ok();
    }

    public static void main(String[] args) {
        try{
            String decryptPassword = AESUtil.Decrypt("ga4JaZdcU0MsUmYLza5MeQ==", "ocschool20200824");
            System.out.println(decryptPassword);
        }catch(Exception e){

        }

    }
    private CommodityMsgVo buildTemplateMessage(CommodityMsgVo vo) {
        // 构建模板消息
        val templateMessage = vo.builder()
                // 设置模板ID
                .templateId(vo.getTemplateId())
                // 设置用户ID
//                .userId(vo.getUserId())
//                // 设置用户名
//                .username(vo.getUsername())
//                // 设置用户头像
//                .userAvatar(vo.getUserAvatar())
                // 设置直播用户角色
                .liveUserRole(vo.getLiveUserRole())
                // 商品id
                .commodityId(vo.getCommodityId())
                // 设置直播用户角色
                .commodityName(vo.getCommodityName())
                // 商品主图
                .image(vo.getImage())
                .commodityLineationPrice(vo.getCommodityLineationPrice())
                // 支付类型] 0-跳转支付，1-在线支付）
                .payType(vo.getPayType())
                .notifyType(vo.getNotifyType())
                // 商品跳转地址
                .jumpAddress(vo.getJumpAddress())
                // 设置直播用户角色
                .liveUserRole(vo.getLiveUserRole())
                // 组装cloudCustomData
                // 构建自定义数据
                .cloudCustomData(buildCustomData(vo))
                // 设置发送时间
                .sendTime(vo.getSendTime())
                // 设置文本类型
                .textType(vo.getTextType())
                // 设置消息类型
                .msgType(vo.getMsgType())
                // 设置通知类型
                .notifyType(vo.getNotifyType())
                // 设置公司ID
                .comId(vo.getComId())
                .build();
        // 设置消息
        templateMessage.setMessage(buildProductMessage(vo));
        // 返回模板消息
        return templateMessage;

    }

    private String buildProductMessage(CommodityMsgVo vo) {
        // 查询商品详情
        ComCommodity comCommodity = comCommodityMapper.selectById(vo.getCommodityId());
        JSONObject jsonObject = new JSONObject();
        val message = CommodityMsgVo.builder()
                .commodityId(vo.getCommodityId())
                .commodityName(comCommodity.getCommodity())
                .commodityPrice(comCommodity.getCommodityPrice().toString())
//                .commodityLineationPrice(comCommodity.getCommodityLineationPrice().toString())
                .image(comCommodity.getImage())
                .notifyType(vo.getNotifyType())
                .payType(comCommodity.getPayType())
                .jumpAddress(comCommodity.getJumpAddress())
                .build();
        BigDecimal commodityLineationPrice = comCommodity.getCommodityLineationPrice();
        if(ObjectUtils.isNotEmpty(commodityLineationPrice)){
            message.setCommodityLineationPrice(commodityLineationPrice.toString());
        }
        message.setNotifyType(vo.getNotifyType());
        jsonObject.put("Data", JSON.toJSONString(message));
        jsonObject.put("Desc", vo.getAction());
        jsonObject.put("Ext", "url");
        jsonObject.put("Sound", "dingdong.aiff");
        return JSON.toJSONString(jsonObject);
    }

    private String buildCustomData(CommodityMsgVo vo) {
        // 构建自定义数据对象并设置用户信息和消息信息
        val customData = MyCustomData.builder()
                .userInfo(MyCustomData.UserInfo.builder()
                        // 设置发送用户ID
                        .sendUserId(vo.getUserId())
                        // 设置发送用户角色为助手
                        .sendUserRole(SendUserRole.assistant.getCode())
                        // 设置发送用户昵称
                        .sendNickName(vo.getUsername())
                        // 设置发送用户头像
                        .sendUserAvatar(vo.getUserAvatar())
                        .build())
                .messageInfo(MyCustomData.MessageInfo.builder()
                        // 检验类型为不检验
                        .type("NONE")
                        .build())
                .build();
        return JSON.toJSONString(customData);
    }
}
