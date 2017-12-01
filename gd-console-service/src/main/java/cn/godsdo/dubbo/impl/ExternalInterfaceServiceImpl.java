package cn.godsdo.dubbo.impl;

import cn.godsdo.constant.MongoConstant;
import cn.godsdo.dto.LiveWatchRecordDto;
import cn.godsdo.dto.QueryLiveUserWatchDto;
import cn.godsdo.dto.msg.LiveMessage;
import cn.godsdo.dto.push.*;
import cn.godsdo.dubbo.ExternalInterfaceService;
import cn.godsdo.dubbo.LiveRecordDatService;
import cn.godsdo.dubbo.LiveStatisticsDatService;
import cn.godsdo.dubbo.camp.TrainingComService;
import cn.godsdo.dubbo.live.ComCommodityService;
import cn.godsdo.entity.LiveRecordDat;
import cn.godsdo.entity.PayOrder;
import cn.godsdo.entity.camp.TrainingCom;
import cn.godsdo.entity.com.ComCommodity;
import cn.godsdo.entity.live.LiveDat;
import cn.godsdo.entity.user.UserDat;
import cn.godsdo.enums.PushInterfaceType;
import cn.godsdo.mapper.live.LiveDatMapper;
import cn.godsdo.mapper.user.UserDatMapper;
import cn.godsdo.util.R;
import cn.godsdo.vo.PushWatchTimeAllVo;
import cn.godsdo.vo.QueryLiveUserWatchVO;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.alibaba.nacos.common.utils.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: CR7
 * @Date: 2019/5/5 16:41
 * @Description:
 */
@Slf4j
@DubboService
public class ExternalInterfaceServiceImpl implements ExternalInterfaceService {

    @DubboReference(check = false)
    TrainingComService trainingComService;

    @DubboReference(check = false)
    LiveStatisticsDatService liveStatisticsDatService;

    @DubboReference(check = false)
    LiveRecordDatService liveRecordDatService;

    @DubboReference(check = false)
    ComCommodityService comCommodityService;

    @Resource
    MongoTemplate mongoTemplate;

    @Resource
    LiveDatMapper liveDatMapper;

    @Resource
    UserDatMapper userDatMapper;

    @Override
    public void pushProductOrder(AddPushInterfaceDto<PayOrder> pushInterfaceDto) {
        String roomName = "";
        Long pushTime = System.currentTimeMillis();
        PushInterfaceDto dto = new PushInterfaceDto();
        PushInterfaceBaseDto pushInterfaceBaseDto = pushInterfaceDto.getDto();
        PayOrder req = pushInterfaceDto.getData();
        LiveDat room = liveDatMapper.selectById(req.getLiveId());
        if (room != null) {
            roomName = room.getName();
            //获取用户邮箱
//            dto.setUserEmail(appUserMapper.getUserEmailByUsrId(room.getAppUserId()));
        }
        dto.setAccount(req.getAccount());
        dto.setOpenId(req.getOpenId());
        dto.setNickname(req.getNickname());
        dto.setHeadUrl(req.getHeadUrl());
        dto.setUnionId(req.getUnionid());
        dto.setRoomId(req.getLiveId());
        dto.setChannelName(req.getChannelName());
        dto.setType(PushInterfaceType.COMMODITY_ORDER.getValue());
        dto.setRoomName(roomName);
        dto.setPushTime(pushTime);
        dto.setChannelNo(req.getChannelId() + "");
        //附加字段
        PushCommodityOrderDto orderDto = new PushCommodityOrderDto();
        orderDto.setOrderNum(req.getOrderNum());
        orderDto.setOrderType(req.getOrderType());
        orderDto.setCommodityName(req.getTitle());

        BigDecimal totalFee = req.getTotalFee();
        log.info("totalFee:{}", totalFee);
        log.info("totalFee to double :{}", totalFee.doubleValue());

        orderDto.setCommodityPrice(totalFee.doubleValue());
        orderDto.setCreateTime(req.getStatus() == 0 ? req.getCreateAt() : null);
        orderDto.setStatus(req.getStatus());
        orderDto.setTransactionId(req.getTransactionId());
        orderDto.setPayType(req.getType());
        orderDto.setPayTime(req.getStatus() == 0 ? null : req.getCreateAt());
        orderDto.setRecommendProductId(req.getProductId());

        ComCommodity cc = comCommodityService.getById(req.getProductId());
        if (ObjectUtils.isNotEmpty(cc) && StringUtils.isNotEmpty(cc.getOtherId())) {
            orderDto.setOtherId(cc.getOtherId());
        }

        dto.setExtra(JSONObject.toJSONString(orderDto));
        String parma = JSONObject.toJSONString(dto);
        //签名
        String sign = this.getPushSign(dto.getRoomId(), roomName, pushTime, PushInterfaceType.COMMODITY_ORDER.getValue(), pushInterfaceBaseDto.getKey());
        log.info("商品订单数据推送，推送时间：" + System.currentTimeMillis() + "推送参数：" + dto + "推送路径：" + pushInterfaceBaseDto.getUrl());
        String result = HttpRequest.post(pushInterfaceBaseDto.getUrl()).header("sign", sign).body(parma).timeout(3000).execute().body();
        log.info("商品订单数据推送，推送结束时间：" + System.currentTimeMillis() + "返回结果：" + result);

    }

    @Override
    public void pushWatchTimeData() {
        Long pushTime = System.currentTimeMillis();
        List<TrainingCom> trainingCom = trainingComService.list(new LambdaQueryWrapper<>());
        for (TrainingCom com : trainingCom) {
            if (com != null && StringUtils.isNotBlank(com.getPath()) && StringUtils.isNotBlank(com.getComKey())) {
                Long comId = com.getComId();
                //获取昨天时间
                LocalDate localDate = LocalDate.now().minusDays(1);
                String statisticalDate = String.valueOf(localDate);

                Criteria criteria = Criteria.where("comId").is(comId);
                criteria.and("statisticsDate").is(statisticalDate);
                List<Long> roomIdlist = mongoTemplate.findDistinct(new Query(criteria), "roomId", MongoConstant.LIVE_WATCH_RECORD_USER , LiveWatchRecordDto.class, Long.class);
                if (!roomIdlist.isEmpty()) {
                    List<UserDat> userDats = userDatMapper.selectList(new LambdaQueryWrapper<UserDat>().eq(UserDat::getComId, comId).eq(UserDat::getDeleteFlag, false));
                    //转map
                    Map<Long, UserDat> usetMap = userDats.stream().collect(Collectors.toMap(UserDat::getId, user -> user));

                    long count = 1000;
                    for (Long roomId : roomIdlist) {
                        //获取房间名称
                        LiveDat room = liveDatMapper.selectById(roomId);
                        String roomName = room != null ? room.getName() : "";
                        // 查询当天数据
                        Criteria criteriaUser = Criteria.where("comId").is(comId).and("statisticsDate").is(statisticalDate).and("roomId").is(roomId);
                        Query queryUser = new Query(criteriaUser);
                        long total = mongoTemplate.count(queryUser, LiveWatchRecordDto.class, MongoConstant.LIVE_WATCH_RECORD_USER);
                        long pageSize = total % count == 0 ? total / count : (total / count) + 1;
                        for (int i = 1; i <= pageSize; i++) {
                            QueryLiveUserWatchDto watchQuery = new QueryLiveUserWatchDto();
                            watchQuery.setComId(comId);
                            watchQuery.setQueryDate(statisticalDate);
                            watchQuery.setLiveId(roomId);
                            watchQuery.setPageNum(i);
                            watchQuery.setPageSize((int) count);
                            watchQuery.setRoomName(roomName);
                            List<QueryLiveUserWatchVO> liveUserWatchVOS = liveStatisticsDatService.getWatchDataTodayList(watchQuery);
                            List<PushWatchTimeAllVo> watchTimeAllVos = new ArrayList<>();
                            for (QueryLiveUserWatchVO queryLiveUserWatchVO : liveUserWatchVOS) {
                                PushWatchTimeAllVo vo = new PushWatchTimeAllVo();
                                vo.setTime(queryLiveUserWatchVO.getTime());
                                vo.setLogin(queryLiveUserWatchVO.getEnterTime().getTime());
                                vo.setLogout(queryLiveUserWatchVO.getOutTime() != null ? queryLiveUserWatchVO.getOutTime().getTime() : 0);
                                vo.setIp(queryLiveUserWatchVO.getIp());
                                vo.setSystemOs(queryLiveUserWatchVO.getSystemOs());
                                vo.setBrowser(queryLiveUserWatchVO.getBrowser());
                                vo.setIsNew(queryLiveUserWatchVO.getIsNew() == null ? 1 : queryLiveUserWatchVO.getIsNew());
                                vo.setMsgCount(queryLiveUserWatchVO.getSpeakCount());
                                vo.setAccount(queryLiveUserWatchVO.getUserId() + "");
                                vo.setChannelName(queryLiveUserWatchVO.getChannelName());
                                vo.setRoomId(watchQuery.getLiveId());
                                vo.setRoomName(watchQuery.getRoomName());
                                vo.setNickname(queryLiveUserWatchVO.getNickName());
                                vo.setHeadUrl(queryLiveUserWatchVO.getHeadImgUrl());
                                vo.setChannelNo(queryLiveUserWatchVO.getChannelId() + "");
//                                vo.setUserEmail("");
                                UserDat user = usetMap.get(queryLiveUserWatchVO.getUserId());
                                if (user != null) {
                                    vo.setUpdateTime(user.getUpdateAt().getTime());
                                    vo.setOpenId(user.getWxOpenid());
                                    vo.setUnionId(user.getUnionid());
                                    if (StringUtils.isNotEmpty(user.getWxOpenid())) {
                                        vo.setAccountWatchType(1); //用户观看类型 1微信 2小程序 3qq 4其他
                                    } else {
                                        vo.setAccountWatchType(4);
                                    }
                                }
                                usetMap.get(queryLiveUserWatchVO.getUserId());
                                watchTimeAllVos.add(vo);
                            }

                            PushWatchTimeAllDto pushWatchTimeAllDto = new PushWatchTimeAllDto();
                            pushWatchTimeAllDto.setType(PushInterfaceType.WATCH_TIME_All.getValue());
                            pushWatchTimeAllDto.setList(watchTimeAllVos);
                            pushWatchTimeAllDto.setDate(statisticalDate);
                            pushWatchTimeAllDto.setPushTime(pushTime);
                            pushWatchTimeAllDto.setRoomName(roomName);
                            pushWatchTimeAllDto.setRoomId(roomId);
                            //数据推送
                            log.info("全部用户观看时长数据推送，推送时间：" + System.currentTimeMillis() + "直播间id:" + roomId);
                            log.info("全部用户观看时长数据推送，推送参数长度：" + watchTimeAllVos.size());
                            log.info("全部用户观看时长数据推送，推送路径：" + com.getPath());
                            //签名
                            String sign = this.getPushSign(roomId, roomName, pushTime, PushInterfaceType.WATCH_TIME_All.getValue(), comId.toString());
                            String result = HttpRequest.post(com.getPath()).header("sign", sign).body(JSON.toJSONString(pushWatchTimeAllDto)).timeout(3000).execute().body();
                            log.info("全部用户观看时长数据推送，推送结束时间：" + System.currentTimeMillis() + "直播间id:" + roomId);
                            log.info("全部用户观看时长数据推送，" + "返回结果：" + result);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void pushMsgJob() {
        Long pushTime = System.currentTimeMillis();
        List<TrainingCom> trainingCom = trainingComService.list(new LambdaQueryWrapper<>());
        for (TrainingCom com : trainingCom) {
            if (com != null && StringUtils.isNotBlank(com.getPath()) && StringUtils.isNotBlank(com.getComKey())) {
                Long comId = com.getComId();
                //获取昨天时间
                LocalDate localDate = LocalDate.now().minusDays(1);
                String statisticalDate = String.valueOf(localDate);
                String msgConstant = MongoConstant.LIVE_MESSAGE + "_" + statisticalDate;
                Criteria criteria = Criteria.where("comId").is(comId);
                List<Long> roomIdlist = mongoTemplate.findDistinct(new Query(criteria), "roomId", msgConstant, LiveMessage.class, Long.class);
                if (!roomIdlist.isEmpty()) {
                    long count = 1000;
                    for (Long roomId : roomIdlist) {
                        //获取房间名称
                        LiveDat room = liveDatMapper.selectById(roomId);
                        String roomName = room != null ? room.getName() : "";
                        // 查询当天数据
                        Criteria criteriaUser = Criteria.where("comId").is(comId).and("roomId").is(roomId);
                        Query queryUser = new Query(criteriaUser);
                        long total = mongoTemplate.count(queryUser, LiveMessage.class, msgConstant);
                        long pageSize = total % count == 0 ? total / count : (total / count) + 1;
                        for (int i = 1; i <= pageSize; i++) {
                            Query query = new Query(criteria);
                            Sort sort = Sort.by(Sort.Direction.DESC, "msgTime");
                            query.with(sort);
                            query.skip((long) (i - 1) * count).limit((int) count);
                            List<LiveMessage> messageList = mongoTemplate.find(query, LiveMessage.class, msgConstant);

                            List<PushRoomMsgVo> msgList = new ArrayList<>();
                            getRoomMsgInfo(messageList, roomId, room, msgList);

                            if (!msgList.isEmpty()) {
                                PushRoomMsgDto pushRoomMsgDto = new PushRoomMsgDto();
                                pushRoomMsgDto.setType(PushInterfaceType.SEND_MSG.getValue());
                                pushRoomMsgDto.setList(msgList);
                                pushRoomMsgDto.setDate(statisticalDate);
                                pushRoomMsgDto.setRoomId(roomId);
                                pushRoomMsgDto.setRoomName(roomName);
                                pushRoomMsgDto.setPushTime(pushTime);
                                //数据推送
                                log.info("直播间消息数据推送，推送时间：" + System.currentTimeMillis() + "推送参数长度：" + msgList.size() + "推送路径：" + com.getPath());
                                //签名
                                String sign = this.getPushSign(roomId, roomName, pushTime, PushInterfaceType.SEND_MSG.getValue(), comId + "");
                                String result = HttpRequest.post(com.getPath()).header("sign", sign).body(JSON.toJSONString(pushRoomMsgDto)).timeout(3000).execute().body();
                                log.info("直播间消数据推送，推送结束时间：" + System.currentTimeMillis() + "返回结果：" + result);
                            }
                        }
                    }
                }
            }
        }
    }

    private void getRoomMsgInfo(List<LiveMessage> messageList, Long roomId, LiveDat room, List<PushRoomMsgVo> msgList) {
        for (LiveMessage message : messageList) {
            if (StringUtils.isNotEmpty(message.getLiveUserRole())){
                PushRoomMsgVo vo = new PushRoomMsgVo();
                vo.setChannelName(message.getChannelName());
                vo.setRoomId(roomId);
                vo.setRoomName(room.getName());
                vo.setMsgContent(message.getMessageText());
//            vo.setLogin(login);
                vo.setNickname(message.getLiveUsername());
                vo.setSendAccount(message.getLiveUserId() + "");
                vo.setSendTime(DateFormatUtils.format(new Date(message.getMsgTime() * 1000), "yyyy/MM/dd HH:mm:ss"));
                if (message.getLiveUserRole().equals("robot")) {
                    vo.setMsgType("机器人");
                } else {
                    vo.setMsgType("文本消息");
                }
                msgList.add(vo);
            }
        }
    }

    private String getPushSign(Long roomId, String roomName, Long pushTime, Integer type, String key) {
        try {
            TreeMap<String, Object> treeMap = new TreeMap<>();
            treeMap.put("roomId", roomId);
            treeMap.put("roomName", roomName);
            treeMap.put("pushTime", pushTime);
            treeMap.put("type", type);
            StringBuilder stringBuffer = new StringBuilder();
            for (String value : treeMap.keySet()) {
                String buffer = value + "=" + treeMap.get(value);
                if (!stringBuffer.isEmpty()) {
                    buffer = "&" + value + "=" + treeMap.get(value);
                }
                stringBuffer.append(buffer);
            }
            stringBuffer.append("&secretKey=").append(key);
            return MD5.create().digestHex(stringBuffer.toString().toLowerCase());
        } catch (Exception exception) {
            log.error("getPushSign error", exception);
        }
        return "";
    }
}
