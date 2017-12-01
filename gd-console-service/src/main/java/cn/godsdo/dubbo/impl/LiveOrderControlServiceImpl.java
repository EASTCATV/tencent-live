package cn.godsdo.dubbo.impl;

import cn.godsdo.constant.MsgTypeConstants;
import cn.godsdo.constant.RedisConstants;
import cn.godsdo.dto.assistan.LiveOrderControlDto;
import cn.godsdo.dubbo.LiveOrderControlRobotService;
import cn.godsdo.dubbo.crossService.RedisDubboService;
import cn.godsdo.dubbo.im.ImService;
import cn.godsdo.dubbo.live.LiveDatService;
import cn.godsdo.entity.LiveOrderControl;
import cn.godsdo.entity.LiveOrderControlRobot;
import cn.godsdo.entity.com.DefaultBot;
import cn.godsdo.entity.live.LiveDat;
import cn.godsdo.enums.im.TIMSendTypeEnum;
import cn.godsdo.enums.live.OrderControlAccountEnum;
import cn.godsdo.enums.live.OrderControlRobotJude;
import cn.godsdo.mapper.DefaultBotMapper;
import cn.godsdo.mapper.LiveOrderControlMapper;
import cn.godsdo.dubbo.LiveOrderControlService;
import cn.godsdo.mapper.LiveOrderControlRobotMapper;
import cn.godsdo.query.im.ImMsgQuery;
import cn.godsdo.util.R;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.interfaces.IdService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * <p>
 * 直播间订单场控表 服务实现类
 * </p>
 *
 * @author hdk
 * @since 2024-07-15
 */
@Slf4j
@DubboService
public class LiveOrderControlServiceImpl extends ServiceImpl<LiveOrderControlMapper, LiveOrderControl> implements LiveOrderControlService {

    @DubboReference(check = false)
    private IdService idService;

    @DubboReference(check = false, retries = 0)
    private ImService imService;

    @DubboReference(check = false)
    private LiveOrderControlRobotService liveOrderControlRobotService;

    @DubboReference(check = false)
    private RedisDubboService redisDubboService;

    @DubboReference(check = false)
    LiveDatService livedatservice;

    @Resource
    private LiveOrderControlRobotMapper liveOrderControlRobotMapper;

    @Resource
    private DefaultBotMapper defaultBotMapper;

    @Override
    public R getOrderControlInfo(Long comId, Long liveId) {
        LiveOrderControl liveOrderControl = this.baseMapper.selectOne(new LambdaUpdateWrapper<LiveOrderControl>()
                .eq(LiveOrderControl::getComId, comId)
                .eq(LiveOrderControl::getLiveId, liveId));
        if (liveOrderControl!= null) {
            Object robotValue = redisDubboService.get(1, RedisConstants.ORDER_CONTROL_ROBOT + liveId);
            liveOrderControl.setStartFlag(robotValue != null);
        }
        return R.ok(liveOrderControl);
    }

    @Override
    public R saveOrderControl(LiveOrderControlDto liveOrderControl, Long accountId) {
        String message = "保存成功";
        try {
            liveOrderControl.setAutomaticStartTimeNew(StringUtils.isNotBlank(liveOrderControl.getStartTime())?
                    DateUtils.parseDate(liveOrderControl.getStartTime(),"HH:mm:ss") : null );
            liveOrderControl.setAutomaticEndTimeNew(StringUtils.isNotBlank(liveOrderControl.getEndTime())?
                    DateUtils.parseDate(liveOrderControl.getEndTime(),"HH:mm:ss") : null );
        } catch (Exception e) {
            log.error("liveOrderControl.getStartTime() : {}", liveOrderControl.getStartTime());
            log.error("liveOrderControl.getEndTime() : {}", liveOrderControl.getEndTime());
            log.error("日期转换异常 : {}", e.getMessage());
            return R.failed("日期转换异常");
        }

        //修改
        if (liveOrderControl.getId() != null) {
            liveOrderControl.setUpdateBy(accountId);
            this.baseMapper.updateInfo(liveOrderControl);
            //不展示真实用户
            if(!liveOrderControl.getIsShowAccount()){
                //删除redis
                String accountRedisKey = RedisConstants.ORDER_CONTROL_ACCOUNT + liveOrderControl.getLiveId();
                redisDubboService.del(1,accountRedisKey);
            }
        } else {
            System.out.println("*********************asdf");
            liveOrderControl.setId(idService.nextId());
            liveOrderControl.setCreateBy(accountId);
            this.save(liveOrderControl);
//            this.baseMapper.addInfo(liveOrderControl);
        }
        //启动机器人
        if(liveOrderControl.getIsShowRobot()){
            //是否启动机器人
            if(liveOrderControl.getIsSendRobot()){
                int second = 60 * 60 * 3;
                //自动计算缓存时间
                if(liveOrderControl.getRobotTriggerType() == 2){
                    LocalTime now =LocalTime.now();
                    ZoneId zoneId=ZoneId.systemDefault();
                    LocalTime endTime= LocalDateTime.ofInstant(liveOrderControl.getAutomaticEndTimeNew().toInstant(), zoneId).toLocalTime();
                    second=(int) ChronoUnit.SECONDS.between(now,endTime);
                }

                if(second>0){
                    //多缓存一个小时
                    second = second+(60*60);
                    this.saveRobotRedis(liveOrderControl,second);
                }
            }
            message = "发送成功";
        } else {
            //删除redis
            String robotKey = RedisConstants.ORDER_CONTROL_ROBOT + liveOrderControl.getLiveId();
            redisDubboService.del(1,robotKey);
        }
        return R.ok(message);
    }

    @Override
    public R updateRobotMessage(LiveOrderControlDto dto) {
        String redisKey = RedisConstants.ORDER_CONTROL_ROBOT + dto.getLiveId();
        //是否启动
        if(dto.getStartFlag()){
            Object value = redisDubboService.get(1,redisKey);
            //已存在且不发送，返回前端提示
            if(null != value && !dto.getIsSendRobot()){
                return R.ok("已存在");
            }
            this.saveRobotRedis(dto,3*60*60);
        } else {
            //删除redisKey
            redisDubboService.del(1,redisKey);
        }
        return R.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R addRobot(LiveOrderControlDto dto) {
        List<LiveOrderControlRobot> addList = new ArrayList<>();
        //获取总数
        Long count = liveOrderControlRobotMapper.selectCount(new LambdaUpdateWrapper<LiveOrderControlRobot>().eq(LiveOrderControlRobot::getLiveId, dto.getLiveId()));
        if(count >= 100){
            return R.failed("只能添加100个机器人");
        }
        long number = 100 - count;
        number = number > 20 ? 20 : number;
        //获取随机机器人
        Long defaultId = defaultBotMapper.getDefaultBotId();
        if (defaultId == null) {
            return R.failed("机器人不存在");
        }
        List<DefaultBot> botList = defaultBotMapper.getRandDefaultBot(number);
        if(CollectionUtils.isEmpty(botList)){
            return R.failed("机器人不存在");
        }
        for (DefaultBot defaultBot : botList) {
            LiveOrderControlRobot bot =new LiveOrderControlRobot();
            bot.setId(idService.nextId());
            bot.setLiveId(dto.getLiveId());
            bot.setNickName(defaultBot.getNickname());
            bot.setComId(dto.getComId());
            bot.setCreateBy(dto.getUpdateBy());
            addList.add(bot);
        }
        liveOrderControlRobotService.saveBatch(addList);
//        liveOrderControlRobotMapper.insertBatch(addList);
        return R.ok();
    }

    @Override
    public void sendOrderController(LiveOrderControlDto dto) {
        //获取订单场控信息
        LiveOrderControl liveOrderControl = this.baseMapper.selectOne(new LambdaUpdateWrapper<LiveOrderControl>()
                .eq(LiveOrderControl::getComId, dto.getComId())
                .eq(LiveOrderControl::getLiveId, dto.getLiveId()));
        if (null != liveOrderControl && liveOrderControl.getIsShowAccount()) {
            //获取队列数据
            String redisKey = RedisConstants.ORDER_CONTROL_ACCOUNT + dto.getLiveId();
            long listCount = redisDubboService.lLenByIndex(1, redisKey);
            //超过最大数量
            if (listCount < 100L) {
                //复制数据库中的值
                BeanUtils.copyProperties(liveOrderControl, dto);

                LiveDat liveDat = livedatservice.selectOne(dto.getComId(), dto.getLiveId());
                dto.setImGroupId(liveDat.getImGroupId());

                //添加redis--缓存3个小时
                redisDubboService.lPushByIndexEx(1, redisKey, JSON.toJSONString(dto), 60 * 60 * 3);
            }
        }
    }

    @Override
    public void sendMessage() {
        //获取真实用户的队列
        Set<String> keys = redisDubboService.keys(1, RedisConstants.ORDER_CONTROL_ACCOUNT + "*");
        //已经发送过的直播间
        Map<Long, Long> exists = new HashMap<>();
        for (String key : keys) {
            //获取长度
            long listLength = redisDubboService.lLenByIndex(1, key);
            if (listLength > 0) {
                //获取拼接的语句
                OrderControlAccountEnum orderControlAccountJudge = OrderControlAccountEnum.getInfoByJudge((int) listLength);
                if (orderControlAccountJudge != null) {
                    //获取第一个元素
                    String account = redisDubboService.lIndexByIndex(1, key, 0L);
                    //删除队列里面的指定数据
                    redisDubboService.lTrimByIndex(1, key, orderControlAccountJudge.getResultValue(), -1);
                    LiveOrderControlDto liveOrderControlDto = JSONObject.parseObject(account, LiveOrderControlDto.class);
                    //发送系统消息消息
                    this.setMessageByInfo(liveOrderControlDto, orderControlAccountJudge.getResultKey());
                    exists.put(liveOrderControlDto.getLiveId(), liveOrderControlDto.getLiveId());
                }

            }
        }
        /*
         *发送机器人消息
         */
        Set<String> robotKeys = redisDubboService.keys(1, RedisConstants.ORDER_CONTROL_ROBOT + "*");
        for (String robotKey : robotKeys) {
            String robotValueStr = (String) redisDubboService.get(1, robotKey);
            if (StringUtils.isNotBlank(robotValueStr)) {
                LiveOrderControlDto liveOrderControlDto = JSONObject.parseObject(robotValueStr, LiveOrderControlDto.class);
                //已经发送过了
                if (exists.containsKey(liveOrderControlDto.getLiveId())) {
                    continue;
                }
                //没有机器人
                if (CollectionUtils.isEmpty(liveOrderControlDto.getRobotList())) {
                    //删除redis
                    redisDubboService.del(1,robotKey);
                    continue;
                }
                int sendIndex = liveOrderControlDto.getSendRobotIndex() != null ? liveOrderControlDto.getSendRobotIndex() : 0;
                //超过索引
                if (sendIndex > liveOrderControlDto.getRobotList().size() - 1) {
                    //删除redis
                    redisDubboService.del(1,robotKey);
                    continue;
                }
                //自动
                if (liveOrderControlDto.getRobotTriggerType().equals(2)) {
                    LocalTime  now =LocalTime.now();
                    ZoneId zoneId = ZoneId.systemDefault();
                    LocalTime automaticStartTime=LocalDateTime.ofInstant(liveOrderControlDto.getAutomaticStartTimeNew().toInstant(), zoneId).toLocalTime();
                    LocalTime automaticEndTime=LocalDateTime.ofInstant(liveOrderControlDto.getAutomaticEndTimeNew().toInstant(), zoneId).toLocalTime();
                    //不在时间区间内
                    if (now.isBefore(automaticStartTime) ) {
                        continue;
                    }
                    //如果超过自动发送的时间
                    if(now.isAfter(automaticEndTime)){
                        //删除redis
                        redisDubboService.del(1,robotKey);
                        continue;
                    }
                }
                //随机数
                int number = (int) (Math.random() * 3 + 1);
                liveOrderControlDto.setNickName(liveOrderControlDto.getRobotList().get(sendIndex).getNickName());
                //发送消息
                this.setMessageByInfo(liveOrderControlDto, OrderControlRobotJude.getResultKeyByValue(number));
                Integer automaticCirculationNum = liveOrderControlDto.getAutomaticCirculationNum() != null
                        ? liveOrderControlDto.getAutomaticCirculationNum() : 0;
                int alreadyCirculationNum = liveOrderControlDto.getAlreadyCirculationNum() != null ? liveOrderControlDto.getAlreadyCirculationNum() : 0;
                //判断是否发送到最后一个机器人
                boolean lastFlag = sendIndex == liveOrderControlDto.getRobotList().size()-1;
                boolean circulationFlag = automaticCirculationNum.equals(alreadyCirculationNum);
                //最后一次循环
                if (lastFlag && circulationFlag) {
                    //删除redis
                    redisDubboService.del(1,robotKey);
                    continue;
                }
                //到最后重置数据
                sendIndex = lastFlag ? 0 : ++sendIndex;
                liveOrderControlDto.setSendRobotIndex(sendIndex);
                liveOrderControlDto.setAlreadyCirculationNum(lastFlag ? alreadyCirculationNum + 1 : alreadyCirculationNum);
                //重新筛入redis--缓存3个小时
                redisDubboService.setex(1,robotKey, JSONObject.toJSONString(liveOrderControlDto), 60*60*3);
            }
        }
    }

    private void saveRobotRedis(LiveOrderControlDto liveOrderControl, Integer second){
        List<LiveOrderControlRobot> robotList = liveOrderControlRobotMapper.selectList(new LambdaUpdateWrapper<LiveOrderControlRobot>()
               .eq(LiveOrderControlRobot::getLiveId, liveOrderControl.getLiveId()));
        if(!CollectionUtils.isEmpty(robotList)){
            LiveOrderControlDto robotDto = new LiveOrderControlDto();
            BeanUtils.copyProperties(liveOrderControl,robotDto);
            robotDto.setRobotList(robotList);
            robotDto.setSendRobotIndex(0);
            robotDto.setAlreadyCirculationNum(0);
            redisDubboService.setex(1, RedisConstants.ORDER_CONTROL_ROBOT + liveOrderControl.getLiveId(), JSON.toJSONString(robotDto),second);
        }
    }

    private void setMessageByInfo(LiveOrderControlDto dto, String judgeStr) {
        //发送系统消息
        String nickName = dto.getNickName()!=null?dto.getNickName():"";
        if(nickName.length()>1){
            nickName = nickName.length()>2?nickName.substring(0,1)+"*"+nickName.substring(nickName.length()-1):nickName.substring(0,1)+"*" ;
        }
        ImMsgQuery msgQuery = new ImMsgQuery();
        JSONObject text = new JSONObject();
        text.put("userInfo", null);
        text.put("nickname",nickName);
        text.put("judgeStr",judgeStr);
        text.put("prompt",dto.getPrompt());
        text.put("isShowButton", dto.getIsShowButton());
        text.put("buttonTitle", dto.getButtonTitle());
        text.put("clickType", dto.getClickType());
        text.put("showText", dto.getShowText());
        text.put("showImg", dto.getShowImg());
        text.put("showLink", dto.getShowLink());

        // 发送请求
        msgQuery.setCloudCustomData(JSON.toJSONString(text));
        msgQuery.setMsgContent("");
        msgQuery.setNotifyTypeEnum(TIMSendTypeEnum.ORDER_CONTROL);
        msgQuery.setGroupId(dto.getImGroupId());
        msgQuery.setMsgType(MsgTypeConstants.TIM_CUSTOM_ELEM);
        msgQuery.setFromUserId("administrator");
        imService.sendCustomGroupMsg(msgQuery);
    }

}
