package cn.godsdo.dubbo.impl.live;

import cn.godsdo.dto.live.LiveWatchChatDto;
import cn.godsdo.dubbo.cache.ClearCache;
import cn.godsdo.dubbo.im.ImService;
import cn.godsdo.dubbo.live.LiveWatchChatService;
import cn.godsdo.entity.live.LiveDat;
import cn.godsdo.entity.live.LiveWatchChat;
import cn.godsdo.enums.im.TIMSendTypeEnum;
import cn.godsdo.enums.live.LiveStatusEnum;
import cn.godsdo.mapper.live.LiveDatMapper;
import cn.godsdo.mapper.live.LiveWatchChatMapper;
import cn.godsdo.query.im.ImMsgQuery;
import cn.godsdo.util.R;
import cn.hutool.json.JSONObject;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.interfaces.IdService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * <p>
 * 房间聊天设置 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2018/10/18
 */
@DubboService
public class LiveWatchChatServiceImpl extends ServiceImpl<LiveWatchChatMapper, LiveWatchChat> implements LiveWatchChatService {

    @Resource
    LiveDatMapper liveDatMapper;

    @DubboReference
    IdService idService;
    @DubboReference(check = false, retries = 0)
    private ImService imService;
    @DubboReference(check = false)
    ClearCache clearcache;

    @Override
    public R  updateLiveChat(LiveWatchChatDto dto, Long comId, Long accountId) {
        // 根据直播间信息查询直播数据
        Long liveId = dto.getLiveId();
        //删除缓存
        clearcache.delLiveCache(comId, liveId);

        LiveDat liveDat = liveDatMapper.selectOne(new LambdaQueryWrapper<LiveDat>().eq(LiveDat::getComId, comId)
                .eq(LiveDat::getId, liveId).eq(LiveDat::getDeleteFlg, false));
        // 如果直播数据为空，返回直播间不存在的错误信息
        if (ObjectUtils.isEmpty(liveDat)) {
            return R.failed("直播间不存在");
        }
        // 根据直播间信息查询观看聊天室信息
        LiveWatchChat liveWatchChat = this.baseMapper.selectOne(new LambdaQueryWrapper<LiveWatchChat>().eq(LiveWatchChat::getComId, comId)
                .eq(LiveWatchChat::getLiveId, liveId).eq(LiveWatchChat::getDeleteFlg, false));
        // 如果观看聊天室信息不为空，更新观看聊天室信息，否则插入新的观看聊天室信息
        if (ObjectUtils.isNotEmpty(liveWatchChat)) {
            liveWatchChat.setLoginPrompt(dto.getLoginPrompt());
            liveWatchChat.setBannedAll(dto.getBannedAll());
            liveWatchChat.setHistory(dto.getHistory());
            liveWatchChat.setSensitiveOn(dto.getSensitiveOn());
            liveWatchChat.setFree(dto.getFree());
            liveWatchChat.setMessage(dto.getMessage());
            liveWatchChat.setSensitiveWord(dto.getSensitiveWord());
            liveWatchChat.setFreeWord(dto.getFreeWord());
            liveWatchChat.setUpdateBy(accountId);
            this.baseMapper.updateById(liveWatchChat);
        } else {
            liveWatchChat = new LiveWatchChat(idService.nextId(), comId, liveId, dto.getLoginPrompt(), dto.getBannedAll(),
                    dto.getHistory(), dto.getSensitiveOn(), dto.getFree(), dto.getMessage(), dto.getSensitiveWord(), dto.getFreeWord(), accountId);
            this.baseMapper.insert(liveWatchChat);
        }

        // 判断直播间是否直播中

        if (liveDat.getLiveStatus().equals(LiveStatusEnum.LIVING.getValue())) {
            // 如果在直播中，推送免审词与敏感词到直播间中
            // 全体禁言
            //  消息审核
            //        if (e.getBannedAll() == 0) {
//            unforbidden(ocClientId, ocRoomId);
//        } else {
//            forbidden(ocClientId, ocRoomId);
//        }
//        if (e.getMessage() == 0) {
//            unsetImRoomMsg(ocClientId, ocRoomId);
//        } else {
//            setImRoomMsg(ocClientId, ocRoomId);
//        }


        }

        return R.ok();
    }


    @Override
    public R getLiveChatInfo(Long liveId, Long comId, Long accountId) {
        // 根据商品ID、房间ID和删除标识查询直播聊天信息
        LiveWatchChat liveWatchChat = this.baseMapper.selectOne(new LambdaQueryWrapper<LiveWatchChat>()
                .eq(LiveWatchChat::getComId, comId)
                .eq(LiveWatchChat::getLiveId, liveId)
                .eq(LiveWatchChat::getDeleteFlg, false));
        // 如果直播聊天信息为空，则创建新的直播聊天信息并插入数据库
        if (ObjectUtils.isEmpty(liveWatchChat)) {
            liveWatchChat = new LiveWatchChat(idService.nextId(), comId, liveId, false, false,
                    false, false, false, false, "", "", accountId);
            this.baseMapper.insert(liveWatchChat);
        }
        // 返回包含直播聊天信息的成功响应对象
        return R.ok(liveWatchChat);
    }

    @Override
    public R updateWatchChatFree(Boolean free,String imGroupId, Long liveId, Long comId, Long accountId) {
        //删除缓存
        clearcache.delLiveCache(comId, liveId);
        LiveWatchChat liveWatchChat = this.baseMapper.selectOne(new LambdaQueryWrapper<LiveWatchChat>()
                .eq(LiveWatchChat::getComId, comId)
                .eq(LiveWatchChat::getLiveId, liveId)
                .eq(LiveWatchChat::getDeleteFlg, false));
        if (ObjectUtils.isEmpty(liveWatchChat)) {
            return R.failed("直播间信息不存在");
        }
        liveWatchChat.setFree(free);
        liveWatchChat.setUpdateBy(accountId);
        this.baseMapper.updateById(liveWatchChat);
        if(free.equals(liveWatchChat.getFree())){
            ImMsgQuery msgQuery = new ImMsgQuery();
            JSONObject json = new JSONObject();
            json.set("liveWatchChatInfo", liveWatchChat);
            json.set("userInfo", null);
            msgQuery.setCloudCustomData(JSON.toJSONString(json));
            msgQuery.setNotifyTypeEnum(TIMSendTypeEnum.FREE_UPDATE);
            msgQuery.setMsgContent("");
            msgQuery.setGroupId(imGroupId);
            msgQuery.setFromUserId("administrator");
            imService.sendCustomGroupMsg(msgQuery);
        }
        return R.ok();
    }



}
