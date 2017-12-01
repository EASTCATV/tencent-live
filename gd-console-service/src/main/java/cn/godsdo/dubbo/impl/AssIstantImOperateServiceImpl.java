package cn.godsdo.dubbo.impl;

import cn.godsdo.constant.MongoConstant;
import cn.godsdo.constant.RedisConstants;
import cn.godsdo.dto.assistan.MsgWithDrawDto;
import cn.godsdo.dto.im.AssIstantOperateDto;
import cn.godsdo.dubbo.AssIstantImOperateService;
import cn.godsdo.dubbo.crossService.RedisDubboService;
import cn.godsdo.dubbo.im.ImService;
import cn.godsdo.entity.live.LiveWatchChat;
import cn.godsdo.enums.im.TIMSendTypeEnum;
import cn.godsdo.mapper.live.LiveWatchChatMapper;
import cn.godsdo.query.im.ImMsgQuery;
import cn.godsdo.util.R;
import cn.hutool.core.date.DateUtil;
import cn.hutool.extra.emoji.EmojiUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author W~Y~H
 * @Date : 2018/05/18
 */
@Slf4j
@DubboService
public class AssIstantImOperateServiceImpl implements AssIstantImOperateService {
    @DubboReference
    RedisDubboService redisDubboService;
    @DubboReference(check = false, retries = 0)
    private ImService imService;

    @Autowired
    private MongoTemplate mongoTemplate;
    @Resource
    private LiveWatchChatMapper liveWatchChatMapper;


    @Override
    public R black(AssIstantOperateDto dto, Long comId, Long accountId) {
        Integer banType = dto.getType();
        Long liveId = dto.getLiveId();
        String userId = dto.getUserId().toString();
        String now = DateUtil.now();
        ImMsgQuery msgQuery = new ImMsgQuery();
        JSONObject json = new JSONObject();
        if (banType == 1) {
            String result = redisDubboService.hget(2, RedisConstants.BACK_LIST_USER_LIVE + liveId, userId);
            if (StringUtils.isBlank(result)) {
                json.put("nickName", dto.getNickname());
                if (StringUtils.isNotBlank(dto.getChannel())) {
                    json.put("channel", dto.getChannel());
                }
                json.put("time", now);
                json.put("banType", 1);
                json.put("headUrl", EmojiUtil.toUnicode(dto.getHeadUrl()));
                json.put("userId", userId);
                json.put("createBy", accountId);
                log.info("封禁用户：{}，封禁人：{}，封禁时间：{}", userId, accountId, now);
                redisDubboService.hset(2, RedisConstants.BACK_LIST_USER_LIVE + liveId, userId, json.toJSONString());
            }
        } else {
            json.put("banType", 2);
            log.info("解禁：{}，解禁人：{}，解禁时间：{}", userId, accountId, now);
            redisDubboService.hDel(2, RedisConstants.BACK_LIST_USER_LIVE + liveId, userId);
        }
        json.put("userInfo", null);
        msgQuery.setCloudCustomData(JSON.toJSONString(json));
        msgQuery.setNotifyTypeEnum(TIMSendTypeEnum.BACK_USER);
        msgQuery.setMsgContent("");
        msgQuery.setGroupId(dto.getImGroupId());
        msgQuery.setFromUserId("administrator");
        imService.sendCustomGroupMsg(msgQuery);
        return R.ok();
    }

    @Override
    public R prohibition(AssIstantOperateDto dto, Long comId, Long accountId) {
        Integer banType = dto.getType();
        Long liveId = dto.getLiveId();
        String userId = dto.getUserId().toString();
        String now = DateUtil.now();
        ImMsgQuery msgQuery = new ImMsgQuery();
        JSONObject json = new JSONObject();
        json.put("userId", userId);
        if (banType == 1) {
            String result = redisDubboService.hget(2, RedisConstants.PROHIBITION_LIVE + liveId, userId);
            if (StringUtils.isBlank(result)) {
                json.put("nickName", dto.getNickname());
                if (StringUtils.isNotBlank(dto.getChannel())) {
                    json.put("channel", dto.getChannel());
                }
                json.put("time", now);
                json.put("headUrl", EmojiUtil.toUnicode(dto.getHeadUrl()));
                json.put("banType", 1);

                json.put("createBy", accountId);
                log.info("禁言用户：{}，封禁人：{}，封禁时间：{}", userId, accountId, now);
                redisDubboService.hset(2, RedisConstants.PROHIBITION_LIVE + liveId, userId, json.toJSONString(), 24 * 60 * 60L);
            }
        } else {
            json.put("banType", 2);
//            json.put("banType", 2);
            log.info("解除禁言：{}，解禁人：{}，解禁时间：{}", userId, accountId, now);
            redisDubboService.hDel(2, RedisConstants.PROHIBITION_LIVE + liveId, userId);
        }
        json.put("userInfo", null);
        msgQuery.setCloudCustomData(JSON.toJSONString(json));
        msgQuery.setNotifyTypeEnum(TIMSendTypeEnum.PROHIBITION);
        msgQuery.setMsgContent("");
        msgQuery.setGroupId(dto.getImGroupId());
        msgQuery.setFromUserId("administrator");
        imService.sendCustomGroupMsg(msgQuery);
        return R.ok();
    }

    @Override
    public R getBlackList(Long liveId) {
        List<JSONObject> list = new ArrayList<>();
        Map<String, String> stringStringMap = redisDubboService.hgetAll(2, RedisConstants.BACK_LIST_USER_LIVE + liveId);
        for (String key : stringStringMap.keySet()) {
            JSONObject json = JSON.parseObject(stringStringMap.get(key));
            list.add(json);
        }
        return R.ok(list);
    }

    @Override
    public R getProhibitionList(Long liveId) {
        List<JSONObject> list = new ArrayList<>();
        Map<String, String> stringStringMap = redisDubboService.hgetAll(2, RedisConstants.PROHIBITION_LIVE + liveId);
        for (String key : stringStringMap.keySet()) {
            JSONObject json = JSON.parseObject(stringStringMap.get(key));
            list.add(json);
        }
        return R.ok(list);
    }

    @Override
    public R bannedAll(Long comId, Long accountId, Long liveId, String imGroupId, Boolean bannedAll) {
        LiveWatchChat liveWatchChat = liveWatchChatMapper.selectOne(new LambdaQueryWrapper<LiveWatchChat>()
                .eq(LiveWatchChat::getComId, comId)
                .eq(LiveWatchChat::getLiveId, liveId)
                .eq(LiveWatchChat::getDeleteFlg, false));
        if (ObjectUtils.isEmpty(liveWatchChat)) {
            return R.failed("直播间信息不存在");
        }
        liveWatchChat.setBannedAll(bannedAll);
        liveWatchChat.setUpdateBy(accountId);
        liveWatchChatMapper.updateById(liveWatchChat);
        if (bannedAll.equals(liveWatchChat.getBannedAll())) {
            ImMsgQuery msgQuery = new ImMsgQuery();
            cn.hutool.json.JSONObject json = new cn.hutool.json.JSONObject();
            json.set("liveWatchChatInfo", liveWatchChat);
            json.set("userInfo", null);
            msgQuery.setCloudCustomData(JSON.toJSONString(json));
            msgQuery.setNotifyTypeEnum(TIMSendTypeEnum.BANNED_All);
            msgQuery.setMsgContent("");
            msgQuery.setGroupId(imGroupId);
            msgQuery.setFromUserId("administrator");
            imService.sendCustomGroupMsg(msgQuery);

        }
        return R.ok();
    }

    @Override
    public R withdraw(Long comId, Long accountId, MsgWithDrawDto dto) {
        Long sequence = dto.getSequence();
        Long liveId = dto.getLiveId();
        String imGroupId = dto.getImGroupId();
        Long userId = dto.getUserId();
        Query query = null;
        // 查找id，及文本消息是否存在
        query = new Query(Criteria.where("msgSeq").is(sequence).and("liveId").is(liveId)
                .and("comId").is(comId));

        // 修改id
        mongoTemplate.remove(query, MongoConstant.LIVE_MESSAGE);
        String statisticalDate = DateFormatUtils.format(new Date(), "yyyy-MM-dd");
        mongoTemplate.remove(query, MongoConstant.LIVE_MESSAGE + "_" + statisticalDate);
        // 清除历史消息缓存
        redisDubboService.del("HistoryMessage_" + liveId);
        ImMsgQuery msgQuery = new ImMsgQuery();
        cn.hutool.json.JSONObject json = new cn.hutool.json.JSONObject();
        cn.hutool.json.JSONObject userInfo = new cn.hutool.json.JSONObject();
        userInfo.set("userId", userId);
        json.set("sequence", sequence);
        json.set("userInfo", userInfo);
        msgQuery.setCloudCustomData(JSON.toJSONString(json));
        msgQuery.setNotifyTypeEnum(TIMSendTypeEnum.WITHDRAW);
        msgQuery.setMsgContent("");
        msgQuery.setGroupId(imGroupId);
        msgQuery.setFromUserId("administrator");
        imService.sendCustomGroupMsg(msgQuery);
        return R.ok();
    }


}
