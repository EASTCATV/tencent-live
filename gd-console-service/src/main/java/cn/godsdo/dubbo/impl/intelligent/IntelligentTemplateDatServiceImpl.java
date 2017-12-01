package cn.godsdo.dubbo.impl.intelligent;

import cloud.tianai.captcha.common.util.CollectionUtils;
import cn.godsdo.constant.MongoConstant;
import cn.godsdo.constant.MsgTypeConstants;
import cn.godsdo.constant.RedisConstants;
import cn.godsdo.constant.TextType;
import cn.godsdo.dto.intelligent.GetTemplateListDto;
import cn.godsdo.dto.intelligent.SendMsgByIntelligentDto;
import cn.godsdo.dto.live.StartIntelligentDto;
import cn.godsdo.dto.msg.LiveMessage;
import cn.godsdo.dto.msg.MsgDesc;
import cn.godsdo.dto.msg.MyCustomData;
import cn.godsdo.dubbo.LiveRecordDatService;
import cn.godsdo.dubbo.LiveXxlJobService;
import cn.godsdo.dubbo.cache.ClearCache;
import cn.godsdo.dubbo.com.ComVideoDatService;
import cn.godsdo.dubbo.crossService.RedisDubboService;
import cn.godsdo.dubbo.im.ImService;
import cn.godsdo.dubbo.intelligent.IntelligentTemplateDatService;
import cn.godsdo.dubbo.live.ComGiftConfigService;
import cn.godsdo.dubbo.live.LiveDatService;
import cn.godsdo.dubbo.live.TliveService;
import cn.godsdo.entity.LiveRecordDat;
import cn.godsdo.entity.PayOrder;
import cn.godsdo.entity.com.ComCommodity;
import cn.godsdo.entity.com.ComGiftConfig;
import cn.godsdo.entity.com.ComVideoDat;
import cn.godsdo.entity.com.LiveBackVideo;
import cn.godsdo.entity.intelligent.IntelligentLiveRecord;
import cn.godsdo.entity.intelligent.IntelligentTemplateDat;
import cn.godsdo.entity.live.LiveCommodity;
import cn.godsdo.entity.live.LiveDat;
import cn.godsdo.enums.Intelligent.MsgTypeEnum;
import cn.godsdo.enums.im.TIMSendTypeEnum;
import cn.godsdo.enums.live.CommodityStatusEnum;
import cn.godsdo.enums.live.LiveRecordEnum;
import cn.godsdo.enums.live.LiveStatusEnum;
import cn.godsdo.enums.live.VideoTypeEnum;
import cn.godsdo.mapper.com.ComVideoDatMapper;
import cn.godsdo.mapper.com.LiveBackVideoMapper;
import cn.godsdo.mapper.intelligent.IntelligentLiveRecordMapper;
import cn.godsdo.mapper.intelligent.IntelligentTemplateDatMapper;
import cn.godsdo.mapper.live.ComCommodityMapper;
import cn.godsdo.mapper.live.LiveCommodityMapper;
import cn.godsdo.mapper.live.LiveDatMapper;
import cn.godsdo.query.im.ImMsgQuery;
import cn.godsdo.util.R;
import cn.godsdo.util.TimeUtil;
import cn.godsdo.vo.assistant.GetCommodityInfoVo;
import cn.godsdo.vo.intelligent.CommodityMsgVo;
import cn.godsdo.vo.intelligent.GetTemplateListVo;
import cn.godsdo.vo.intelligent.LiveInteractMsgVo;
import cn.godsdo.vo.live.GetWatchUrlByLiveVo;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.extra.emoji.EmojiUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencentcloudapi.vod.v20180717.models.DescribeTaskDetailResponse;
import com.y20y.base.DeleteLivePullStreamTaskInfo;
import com.y20y.base.DescribeTaskDetailRequestinfo;
import com.y20y.constant.Constants;
import com.y20y.interfaces.IdService;
import com.y20y.live.DeleteLivePullStreamTask;
import com.y20y.vod.DescribeTaskDetail;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static cn.godsdo.constant.RedisConstants.INTELLIGENT_LIVE_STATUS;
import static cn.godsdo.util.TimeUtil.convertSecondsToHMS;

/**
 * <p>
 * 智能模板列表 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2024-06-04
 */

@Slf4j
@DubboService
public class IntelligentTemplateDatServiceImpl extends ServiceImpl<IntelligentTemplateDatMapper, IntelligentTemplateDat> implements IntelligentTemplateDatService {

    @DubboReference
    IdService idService;
    @DubboReference(check = false)
    private TliveService tliveService;

    @Resource
    ComVideoDatMapper comVideoDatMapper;
    @Resource
    LiveDatMapper liveDatMapper;
    @Resource
    LiveBackVideoMapper liveBackVideoMapper;
    @Resource
    IntelligentTemplateDatMapper intelligentTemplateDatMapper;
    @Resource
    LiveCommodityMapper liveCommodityMapper;
    @Resource
    ComCommodityMapper comCommodityMapper;
    @DubboReference
    ComVideoDatService comVideoDatService;
    @Resource
    IntelligentLiveRecordMapper intelligentLiveRecordMapper;
    @Autowired
    private MongoTemplate mongoTemplate;
    @DubboReference(check = false)
    private LiveRecordDatService liveRecordDatService;
    @DubboReference
    RedisDubboService redisService;
    @Value("${Tencent.secretId}")
    private String secretId;

    @Value("${Tencent.secretKey}")
    private String secretKey;
    @Value("${Tencent.live.appName}")
    private String appName;
    @Value("${Tencent.live.tpush}")
    private String tpush;

    @Value("${Tencent.vod.subAppId}")
    private Long subAppId;
    @DubboReference(check = false, retries = 0)
    private ImService imService;

    @DubboReference(check = false)
    LiveDatService liveDatService;

    @DubboReference(check = false)
    ComGiftConfigService comGiftConfigService;
    @DubboReference(check = false)
    private LiveXxlJobService liveXxlJobService;
    @DubboReference(check = false)
    ClearCache clearcache;

    @Override
    public R addTemplate(Long videoId, Integer type, Long accountId, Long comId) {
        IntelligentTemplateDat dat = new IntelligentTemplateDat();
        Long id = idService.nextId();
        dat.setId(id);
        dat.setComId(comId);
        dat.setVideoId(videoId);
        dat.setVideoType(type);
        dat.setCreateBy(accountId);
        if (VideoTypeEnum.BACK.getValue() == type) {
            LiveBackVideo liveBackVideo = liveBackVideoMapper.selectOne(new LambdaQueryWrapper<LiveBackVideo>().eq(LiveBackVideo::getComId, comId).eq(LiveBackVideo::getId, videoId));
            if (ObjectUtils.isEmpty(liveBackVideo)) {
                return R.failed("视频不存在");
            }
//            liveBackVideo.getOssUrl()
            dat.setVideoUrl(liveBackVideo.getM3u8Url());
            dat.setImage(liveBackVideo.getImage());
            dat.setName(liveBackVideo.getVideoName());
            dat.setStatus(1);
            dat.setVideoSize(liveBackVideo.getVideoSize());
            dat.setVideoDuration(liveBackVideo.getVideoDuration());
            insertTemplateMsg(liveBackVideo, id);
        } else {
            ComVideoDat cvd = comVideoDatMapper.selectOne(new LambdaQueryWrapper<ComVideoDat>().eq(ComVideoDat::getComId, comId).eq(ComVideoDat::getId, videoId));
            if (ObjectUtils.isEmpty(cvd)) {
                return R.failed("视频不存在");
            }
            dat.setVideoUrl(cvd.getOssResource());
            dat.setImage(cvd.getImage());
            dat.setName(cvd.getVideoName());
            dat.setVideoDuration(cvd.getTimeLength().intValue());
            dat.setVideoSize(cvd.getVolume());
            dat.setStatus(2);
            String transcoding = comVideoDatService.transcoding(cvd);
            dat.setTask(transcoding);
        }
        this.baseMapper.insert(dat);

        return R.ok();
    }


    /**
     * 拉取模版数据
     *
     * @param liveBackVideo
     * @param templateId
     */
    private void insertTemplateMsg(LiveBackVideo liveBackVideo, Long templateId) {
        ThreadUtil.execute(new Runnable() {
            @Override
            public void run() {
                Long liveId = liveBackVideo.getLiveId();
                Long comId = liveBackVideo.getComId();
                // 如果是视频回看，从消息库拉取录制时间的消息
                Date recordStartTime = liveBackVideo.getRecordStartTime();
                Date recordEndTime = liveBackVideo.getRecordEndTime();
                Date newDate = DateUtil.offset(recordStartTime, DateField.HOUR, 8);
                Date newDate1 = DateUtil.offset(recordEndTime, DateField.HOUR, 8);

                Query query = null;
                Criteria textType = Criteria.where("liveId").is(liveId).and("comId").is(comId).and("msgType").is(MsgTypeConstants.TIM_TEXT_ELEM).and("createAt").gte(newDate).lte(newDate1);
                query = new Query(textType);
                // 排序
                Sort sort = Sort.by(Sort.Direction.ASC, "createAt");
                query.with(sort);
                val messageList = mongoTemplate.find(query, LiveMessage.class, MongoConstant.LIVE_MESSAGE);
                if (!CollectionUtils.isEmpty(messageList)) {
                    val listMap = messageList.stream().map(message -> {
                        val jsonObject = JSON.parseObject(message.getMessage());
                        // 获取消息发出时长
                        long sendTime = DateUtil.between(recordStartTime, new Date(message.getMsgTime() * 1000), DateUnit.SECOND);
                        return LiveInteractMsgVo.builder().sendTime(sendTime).liveUserRole(message.getLiveUserRole()).userId(message.getLiveUserId()).username(message.getLiveUsername()).messageText(jsonObject.getString("Text")).userAvatar(message.getLiveUserAvatar()).message(message.getMessage()).cloudCustomData(message.getCloudCustomData()).comId(message.getComId()).templateId(templateId).textType(message.getTextType()).build();
                    }).toList();
                    mongoTemplate.insert(listMap, MongoConstant.LIVE_TEMPLATE_MESSAGE + "_" + templateId);
                }
                // 查询商品消息
                Query query1 = null;
                Criteria textType1 = Criteria.where("liveId").is(liveId).and("comId").is(comId).and("textType").is(TextType.TEXT_PRODUCT).and("msgType").is(MsgTypeConstants.TIM_CUSTOM_ELEM).and("createAt").gte(newDate).lte(newDate1);
                query1 = new Query(textType1);
                // 排序
                query1.with(sort);
                val messageListByCommodity = mongoTemplate.find(query1, LiveMessage.class, MongoConstant.LIVE_MESSAGE);
                if (!CollectionUtils.isEmpty(messageListByCommodity)) {
                    val listMap = messageListByCommodity.stream().map(message -> {
                        val jsonObject = JSON.parseObject(message.getMessage());
//                                String cloudCustomData1 = message.getCloudCustomData();
                        JSONObject cloudCustomData = JSON.parseObject(message.getCloudCustomData());
//                        {"commodityInfo":"{\"commodity\":\"大力丸\",\"commodityLineationPrice\":1.0,\"commodityPrice\":0.01,\"countDown\":0,\"id\":1620078005059616,\"image\":\"https://t.dodoit.live/files/1620077080215584/gift/fdFMZm54KgsFLtcqfidxaxwwI3WhhLgM.jpg\",\"jumpAddress\":\"https://app.apifox.com/\",\"payType\":0,\"putStatus\":0,\"recommendStatus\":0,\"styleType\":\"0\"}","commodityId":1620078005059616}
//                        cloudCustomData
//                                {"Ext":"url","Data":"{\"commodityId\":1620078005059616,\"commodityLineationPrice\":\"1.00\",\"commodityName\":\"大力丸\",\"commodityPrice\":\"0.01\",\"image\":\"https://t.dodoit.live/files/1620077080215584/gift/fdFMZm54KgsFLtcqfidxaxwwI3WhhLgM.jpg\",\"jumpAddress\":\"https://app.apifox.com/\",\"notifyType\":6,\"payType\":0}","Sound":"dingdong.aiff"}
                        Long commodityId = cloudCustomData.getLong("commodityId");
                        ComCommodity comCommodity = comCommodityMapper.selectOne(new LambdaQueryWrapper<ComCommodity>().eq(ComCommodity::getId, commodityId).eq(ComCommodity::getDeleteFlag, false));
                        if (ObjectUtils.isNotEmpty(comCommodity)) {
//                                    JSONObject messageInfo = jsonObject.getJSONObject("message");
                            // 商品操作类型
                            String desc = jsonObject.getString("Desc");

                            // 获取消息发出时长
                            long sendTime = DateUtil.between(recordStartTime, new Date(message.getMsgTime() * 1000), DateUnit.SECOND);
                            CommodityMsgVo vo = new CommodityMsgVo();
                            vo.setComId(comId);
                            vo.setCommodityId(commodityId);
                            vo.setCommodityName(comCommodity.getCommodity());
                            vo.setImage(comCommodity.getImage());
                            BigDecimal commodityLineationPrice = comCommodity.getCommodityLineationPrice();
                            if(ObjectUtils.isNotEmpty(commodityLineationPrice)){
                                vo.setCommodityLineationPrice(commodityLineationPrice.toString());
                            }
                            String jumpAddress = comCommodity.getJumpAddress();
                            if(StringUtils.isNotEmpty(jumpAddress)){
                                vo.setJumpAddress(jumpAddress);
                            }
                            vo.setPayType(comCommodity.getPayType());

                            vo.setSendTime(sendTime);
                            vo.setTextType(TextType.TEXT_PRODUCT);
                            vo.setTemplateId(templateId);
                            switch (desc) {
                                case MsgDesc.ROOM_PRODUCT_ON_SALE:
                                    vo.setNotifyType(TIMSendTypeEnum.PRODUCT_UP.getValue());
                                    break;
                                case MsgDesc.ROOM_PRODUCT_OFF_SALE:
                                    vo.setNotifyType(TIMSendTypeEnum.PRODUCT_DOWN.getValue());
                                    break;
                                case MsgDesc.ROOM_PRODUCT_RECOMMEND:
                                    vo.setNotifyType(TIMSendTypeEnum.PRODUCT_RECOMMENDED.getValue());
                                    break;
                                case MsgDesc.ROOM_PRODUCT_UN_RECOMMEND:
                                    vo.setNotifyType(TIMSendTypeEnum.PRODUCT_UN_RECOMMENDED.getValue());
                                    break;
                                case MsgDesc.ROOM_PRODUCT_SOLD_OUT:
                                    vo.setNotifyType(TIMSendTypeEnum.PRODUCT_SOLD_OUT.getValue());
                                    break;
                                case MsgDesc.ROOM_PRODUCT_NOT_SOLD_OUT:
                                    vo.setNotifyType(TIMSendTypeEnum.PRODUCT_UN_SOLD_OUT.getValue());
                                    break;
                            }
                            JSONObject jsonObject1 = new JSONObject();
                            jsonObject1.put("Data", JSONObject.toJSONString(vo));
                            vo.setMessage(JSONObject.toJSONString(jsonObject1));
                            return vo;
                        }
                        return null;
                    }).toList();
                    mongoTemplate.insert(listMap, MongoConstant.LIVE_TEMPLATE_MESSAGE + "_" + templateId);
                }
                IntelligentTemplateDat intelligentTemplateDat = intelligentTemplateDatMapper.selectById(templateId);
                intelligentTemplateDat.setStatus(1);
                intelligentTemplateDatMapper.updateById(intelligentTemplateDat);
            }
        });

    }

    @Override
    public R getTemplateList(Long comId, GetTemplateListDto dto) {
        Page<IntelligentTemplateDat> ipage = new Page<>(dto.getPage(), dto.getPageSize());
        IPage<IntelligentTemplateDat> result = this.baseMapper.selectPage(ipage, Wrappers.<IntelligentTemplateDat>lambdaQuery()
                .eq(IntelligentTemplateDat::getComId, comId)
                .eq(IntelligentTemplateDat::getDeleteFlag, false)
                .like(StringUtils.isNotBlank(dto.getName()), IntelligentTemplateDat::getName, dto.getName())
                .orderByDesc(IntelligentTemplateDat::getCreateAt)
        );
        List<GetTemplateListVo> list = result.getRecords().stream().map(e -> {
            GetTemplateListVo vo = new GetTemplateListVo();
            BeanUtils.copyProperties(e, vo);
            Integer videoDuration = e.getVideoDuration();
            if (ObjectUtils.isNotEmpty(videoDuration)) {
                String s = convertSecondsToHMS(videoDuration.longValue());
                vo.setTime(s);
            }
            return vo;
        }).toList();
        return R.ok(list, result.getTotal());
    }

    @Override
    public R getTemplateListByOpenLive(Long comId, GetTemplateListDto dto) {
        Page<IntelligentTemplateDat> ipage = new Page<>(dto.getPage(), dto.getPageSize());
        IPage<IntelligentTemplateDat> result = this.baseMapper.selectPage(ipage, Wrappers.<IntelligentTemplateDat>lambdaQuery().eq(IntelligentTemplateDat::getComId, comId).eq(IntelligentTemplateDat::getDeleteFlag, false).ne(IntelligentTemplateDat::getStatus, 2).like(StringUtils.isNotBlank(dto.getName()), IntelligentTemplateDat::getName, dto.getName()));
        List<GetTemplateListVo> list = result.getRecords().stream().map(e -> {
            GetTemplateListVo vo = new GetTemplateListVo();
            BeanUtils.copyProperties(e, vo);
            Integer videoDuration = e.getVideoDuration();
            if (ObjectUtils.isNotEmpty(videoDuration)) {
                String s = convertSecondsToHMS(videoDuration.longValue());
                vo.setTime(s);
            }
            return vo;
        }).toList();
        return R.ok(list, result.getTotal());
    }

    @Override
    public R delete(Long comId, Long accountId, Long id) {
        IntelligentTemplateDat intelligentTemplateDat = this.baseMapper.selectById(id);
        if (ObjectUtils.isEmpty(intelligentTemplateDat)) {
            return R.ok();
        }
        intelligentTemplateDat.setUpdateBy(accountId);
        intelligentTemplateDat.setDeleteFlag(true);
        this.baseMapper.updateById(intelligentTemplateDat);
        // 如果模版存在消息库
        if (mongoTemplate.collectionExists(MongoConstant.LIVE_TEMPLATE_MESSAGE + "_" + id)) {
            mongoTemplate.dropCollection(MongoConstant.LIVE_TEMPLATE_MESSAGE + "_" + id);
        }
        return R.ok();
    }

    @Override
    public R getTemplateInfo(Long comId, Long id) {
        IntelligentTemplateDat dat = this.baseMapper.selectById(id);
        GetTemplateListVo vo = new GetTemplateListVo();
        BeanUtils.copyProperties(dat, vo);
        String time = convertSecondsToHMS(dat.getVideoDuration().longValue());
        vo.setTime(time);
        return R.ok(vo);
    }

    @Override
    public R updateTemplateName(Long accountId, Long id, String name) {
        IntelligentTemplateDat intelligentTemplateDat = this.baseMapper.selectOne(new LambdaQueryWrapper<IntelligentTemplateDat>().eq(IntelligentTemplateDat::getId, id).eq(IntelligentTemplateDat::getDeleteFlag, false));
        if (ObjectUtils.isEmpty(intelligentTemplateDat)) {
            return R.failed("模版不存在");
        }
        intelligentTemplateDat.setUpdateBy(accountId);
        intelligentTemplateDat.setName(name);
        this.baseMapper.updateById(intelligentTemplateDat);
        return R.ok();
    }

    @Override
    public void startIntelligent(StartIntelligentDto dto) {

        Long templateId = dto.getTemplateId();
        Long liveId = dto.getLiveId();
        Long accountId = dto.getAccountId();
        Long comId = dto.getComId();
        //删除缓存
        clearcache.delLiveCache(comId, liveId);
        String endTime = dto.getEndTime();
        String startTime = dto.getStartTime();
        IntelligentTemplateDat intelligentTemplateDat = this.baseMapper.selectById(templateId);
        // 修改直播间状态
        LiveDat liveDat = liveDatMapper.selectById(liveId);
        liveDat.setLiveStatus(LiveStatusEnum.LIVING.getValue());
        liveDat.setStartPlayTime(new Date());
        liveDatMapper.updateById(liveDat);
        if (ObjectUtils.isNotEmpty(intelligentTemplateDat)) {
            // 视频id
            Long videoId = intelligentTemplateDat.getVideoId();
            // 模版消息
//            val messageList = mongoTemplate.find(null, LiveInteractMsgVo.class, MongoConstant.LIVE_TEMPLATE_MESSAGE + "_" + templateId);
            // 视频时长
            Integer videoDuration = intelligentTemplateDat.getVideoDuration();
//            StartIntelligentDto dto = new StartIntelligentDto();
            dto.setLiveId(liveId);
            Integer trtcId = liveDat.getTrtcId();
            dto.setTrtcId(trtcId.longValue());
            if (StringUtils.isNotBlank(endTime)) {
                int i = TimeUtil.returnSeconde(endTime);
                int y = TimeUtil.returnSeconde(startTime);
                dto.setOffsetTime(Long.valueOf(y));
                Date newDate = DateUtil.offset(new Date(), DateField.SECOND, i - y + 10);
                dto.setEndTime(DateUtil.formatDateTime(newDate));
            }

//            dto.setStreamName(liveDat.getTrtcId().toString());
            dto.setAccountId(accountId);
            dto.setComId(comId);
            // todo 开播10秒延迟，如果需要修改，记得修改发消息的开始延迟
            DateTime offset = DateUtil.offset(new Date(), DateField.SECOND, 10);
            dto.setStartTime(DateUtil.formatDateTime(offset));
//            DateUtil.offset(offset, DateField.SECOND, videoDuration);
//            dto.setStartTime(DateUtil.formatDateTime(offset));
//            DateTime offset1 = DateUtil.offset(offset, DateField.SECOND, videoDuration);
//            dto.setEndTime(DateUtil.formatDateTime(offset1));
            dto.setLiveId(liveId);
            dto.setSourceUrl(intelligentTemplateDat.getVideoUrl());

            try {
                log.info("准备开始智能直播了========{}",JSON.toJSON(dto));
                //开始智能直播
                String taskId = tliveService.startIntelligent(dto);
//                log.info("taskId======{}",taskId);

                if (StringUtils.isNotBlank(taskId)) {
                    LiveRecordDat dat = new LiveRecordDat();
                    dat.setStartTime(new Date());
                    dat.setVideoType(LiveRecordEnum.INTELLIGENT.getValue());
                    dat.setLiveId(liveId);
                    dat.setReqTaskId(taskId);
                    dat.setComId(comId);

                    dat.setId(idService.nextId());
                    dat.setState(LiveStatusEnum.LIVING.getValue());

                    dat.setCreateBy(accountId);
                    clearcache.delLiveRecordInfo(comId,liveId);
                    liveRecordDatService.save(dat);
                    redisService.setex("LIVE_RECORD_ID_" + liveId, dat.getId() + "", 60 * 60 * 24);
                    // 智能直播记录
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("taskId", taskId);
                    jsonObject.put("trtcId", trtcId);

                    jsonObject.put("liveStartTime", offset);
                    jsonObject.put("templateId", templateId);
                    jsonObject.put("videoDuration", videoDuration);
                    jsonObject.put("type", dto.getLiveType());
                    jsonObject.put("videoId", videoId);
                    jsonObject.put("name", intelligentTemplateDat.getName());
                    jsonObject.put("startTime", startTime);
                    jsonObject.put("endTime", endTime);
                    jsonObject.put("recordId", taskId);
                    redisService.setex(2, INTELLIGENT_LIVE_STATUS + liveId, JSONObject.toJSONString(jsonObject), videoDuration);
                    redisService.setex(2, RedisConstants.LIVE_STATUS + comId + Constants.UNDERLINE + liveId, liveId.toString(), videoDuration);
                    // 修改智能直播记录状态
                    intelligentLiveRecordMapper.update(null, new LambdaUpdateWrapper<IntelligentLiveRecord>().set(IntelligentLiveRecord::getStatus, 1).eq(IntelligentLiveRecord::getTemplateId, templateId).eq(IntelligentLiveRecord::getLiveId, liveId).eq(IntelligentLiveRecord::getStatus, 0).eq(IntelligentLiveRecord::getDeleteFlg, false));
                    ImMsgQuery msgQuery = new ImMsgQuery();
                    cn.hutool.json.JSONObject json = new cn.hutool.json.JSONObject();
                    json.set("liveStatus", 1);
                    Boolean showTiw = liveDatService.getThreeScreen(liveId);
                    //插入新的直播记录
                    GetWatchUrlByLiveVo cdnData = liveDatService.getCdnData(liveId, liveDat, showTiw);
                    json.set("cdnData", cdnData);
                    json.set("userInfo", null);
                    msgQuery.setCloudCustomData(JSON.toJSONString(json));
                    msgQuery.setNotifyTypeEnum(TIMSendTypeEnum.LIVE_STATUS_UPDATE);
                    msgQuery.setMsgContent("");
                    msgQuery.setGroupId(liveDat.getImGroupId());
                    msgQuery.setFromUserId("administrator");
                    imService.sendCustomGroupMsg(msgQuery);
                    SendMsgByIntelligentDto sm = new SendMsgByIntelligentDto();
                    BeanUtils.copyProperties(intelligentTemplateDat, sm);
                    sm.setStartTime(offset);
                    sm.setOffsetTime(dto.getOffsetTime());
//                    sm.setStartTime(offset);
                    sm.setLiveId(liveId);
                    sm.setTaskId(taskId);
                    ThreadUtil.execute(new Runnable() {
                        @Override
                        public void run() {
                            // todo 智能直播开播十秒延迟，发消息也添加延迟
                            ThreadUtil.sleep(10000);
                            sendMsgByIntelligent(sm);
                        }
                    });

                }

            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }

    @Override
    public R getIntelligentStatus(Long comId, Long liveId) {
        // 从Redis中获取直播状态
        Object o = redisService.get(2, INTELLIGENT_LIVE_STATUS + liveId);
        if (ObjectUtils.isEmpty(o)) {
            // 获取所有的未开播的提交过定时任务的智能直播
            List<IntelligentLiveRecord> intelligentLiveRecords = intelligentLiveRecordMapper.selectList(new LambdaQueryWrapper<IntelligentLiveRecord>().eq(IntelligentLiveRecord::getComId, comId).eq(IntelligentLiveRecord::getLiveId, liveId).eq(IntelligentLiveRecord::getDeleteFlg, false).eq(IntelligentLiveRecord::getType, 1).eq(IntelligentLiveRecord::getStatus, 0));
            if (ObjectUtils.isNotEmpty(intelligentLiveRecords)) {
                IntelligentLiveRecord record = intelligentLiveRecords.get(0);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", record.getName());
                jsonObject.put("templateId", record.getTemplateId());
                jsonObject.put("liveId", record.getLiveId());
                jsonObject.put("videoDuration", record.getVideoDuration());
                jsonObject.put("startTime", record.getStartTime());
//                Date time  = DateUtil.parse(record.getTime());
//                Long startSecond  = DateUtil.between(time, new Date(), DateUnit.SECOND);
//                jsonObject.put("startSecond", startSecond);
                jsonObject.put("endTime", record.getEndTime());
                jsonObject.put("time", record.getTime());
                jsonObject.put("liveType", record.getType());
                jsonObject.put("type", LiveStatusEnum.LIVING.getValue());
                // 如果Redis中直播状态为空，返回未开始状态
                return R.ok(jsonObject);
            } else {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type", LiveStatusEnum.NOT_STARTED.getValue());
                // 如果Redis中直播状态为空，返回未开始状态
                return R.ok(jsonObject);
            }

        } else {
            JSONObject jsonObject = JSONObject.parseObject(o.toString());

            Integer videoDuration = jsonObject.getInteger("videoDuration");
            String s = convertSecondsToHMS(videoDuration.longValue());
            Date startTime = jsonObject.getDate("liveStartTime");
            Integer type = jsonObject.getInteger("type");
            Long startSecond = DateUtil.between(startTime, new Date(), DateUnit.SECOND);
            log.info("startTime====={},{}", startTime, startSecond);
            jsonObject.put("startSecond", startSecond);
            jsonObject.put("time", s);
            jsonObject.put("liveType", type);
            jsonObject.put("type", LiveStatusEnum.LIVING.getValue());
            // 如果Redis中直播状态不为空，返回直播中状态
            return R.ok(jsonObject);
        }
    }

    @Override
    public R closeIntelligent(Long comId, Long accountId, Long liveId, Long templateId) {

        // 从Redis中获取直播状态
        Object o = redisService.get(2, INTELLIGENT_LIVE_STATUS + liveId);
        if (ObjectUtils.isNotEmpty(o)) {
            JSONObject jsonObject = JSONObject.parseObject(o.toString());
            Integer trtcId = jsonObject.getInteger("trtcId");
//            Long templateId = jsonObject.getLong("templateId");
            String taskId = jsonObject.getString("taskId");
            // 根据直播ID获取直播记录
            LiveRecordDat byLiveId = liveRecordDatService.getByLiveId(liveId, taskId);
            if (ObjectUtils.isEmpty(byLiveId)) {
                // 如果直播记录为空，返回未开始状态
                return R.failed();
            }
            //DropLiveStreamInfo dropLiveStreamInfo = new DropLiveStreamInfo();
            //dropLiveStreamInfo.setSecretId(secretId);
            //dropLiveStreamInfo.setSecretKey(secretKey);
            //dropLiveStreamInfo.setStreamName(trtcId.toString());
            //dropLiveStreamInfo.setAppName(appName);
            //dropLiveStreamInfo.setDomainName(tpush);
            //// 正在直播中，进行断流
            //DropLiveStream.dropLiveStream(dropLiveStreamInfo);
            DeleteLivePullStreamTaskInfo deletelivepullstreamtaskinfo = DeleteLivePullStreamTaskInfo.builder().taskId(taskId).secretId(secretId).secretKey(secretKey).operator("admin").build();
            DeleteLivePullStreamTask.del(deletelivepullstreamtaskinfo);

            redisService.del(2, INTELLIGENT_LIVE_STATUS + liveId);
            Integer state = byLiveId.getState();
            if (LiveStatusEnum.LIVING.getValue() == state) {
                byLiveId.setState(LiveStatusEnum.NOT_STARTED.getValue());
                byLiveId.setEndTime(new Date());
                byLiveId.setUpdateBy(accountId);
                liveRecordDatService.updateDat(byLiveId);
            }
            IntelligentLiveRecord intelligentLiveRecord = intelligentLiveRecordMapper.selectOne(new LambdaQueryWrapper<IntelligentLiveRecord>().eq(IntelligentLiveRecord::getTemplateId, templateId).eq(IntelligentLiveRecord::getLiveId, liveId).eq(IntelligentLiveRecord::getStatus, 0).eq(IntelligentLiveRecord::getDeleteFlg, false));
            if (ObjectUtils.isNotEmpty(intelligentLiveRecord)) {
                // 获取未开播的定时智能
                if (intelligentLiveRecord.getType() == 1) {
                    String jobId = intelligentLiveRecord.getJobId();
                    liveXxlJobService.stopJob(jobId);
                    intelligentLiveRecord.setStatus(2);
                    intelligentLiveRecordMapper.updateById(intelligentLiveRecord);
                }
            } else {
                intelligentLiveRecordMapper.update(null, new LambdaUpdateWrapper<IntelligentLiveRecord>().set(IntelligentLiveRecord::getStatus, 2).eq(IntelligentLiveRecord::getTemplateId, templateId).eq(IntelligentLiveRecord::getLiveId, liveId).eq(IntelligentLiveRecord::getDeleteFlg, false));
            }

        } else {
            IntelligentLiveRecord intelligentLiveRecord = intelligentLiveRecordMapper.selectOne(new LambdaQueryWrapper<IntelligentLiveRecord>().eq(IntelligentLiveRecord::getTemplateId, templateId).eq(IntelligentLiveRecord::getLiveId, liveId).eq(IntelligentLiveRecord::getStatus, 0).eq(IntelligentLiveRecord::getType, 1).eq(IntelligentLiveRecord::getDeleteFlg, false));
            if (ObjectUtils.isNotEmpty(intelligentLiveRecord)) {
                String jobId = intelligentLiveRecord.getJobId();
                liveXxlJobService.stopJob(jobId);
                intelligentLiveRecord.setStatus(2);
                intelligentLiveRecord.setDeleteFlg(1);
                intelligentLiveRecordMapper.updateById(intelligentLiveRecord);
            }
        }
        // 删除缓存
        //删除缓存
        clearcache.delLiveCache(comId, liveId);
        redisService.del("LIVE_RECORD_ID_" + liveId);
        // 更新直播状态
        LiveDat liveDat = liveDatMapper.selectById(liveId);
        liveDat.setLiveStatus(LiveStatusEnum.NOT_STARTED.getValue());
        liveDat.setEndPlayTime(new Date());
        liveDatMapper.updateById(liveDat);
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
        return R.ok();
    }

    @Override
    public void sendGiftMessage(PayOrder updatedOrder) {
        log.info("**************************** sendGiftMessage start ****************************");
        LiveDat liveDat = liveDatService.selectOne(updatedOrder.getComId(), updatedOrder.getLiveId());
        ImMsgQuery msgQuery = new ImMsgQuery();
        Long giftId = updatedOrder.getProductId();
        cn.hutool.json.JSONObject json = new cn.hutool.json.JSONObject();
        MyCustomData.UserInfo userInfo = new MyCustomData.UserInfo();
        // 设置公司ID
        userInfo.setComId(updatedOrder.getComId());
        userInfo.setSendUserAvatar(EmojiUtil.toAlias(updatedOrder.getHeadUrl()));
        // 设置发送用户昵称
        userInfo.setSendNickName(EmojiUtil.toHtml(updatedOrder.getNickname()));
        userInfo.setSendUserId(Long.parseLong(updatedOrder.getAccount()));
        // 设置操作用户昵称为空
        userInfo.setOperateNickName(EmojiUtil.toHtml(updatedOrder.getNickname()));

        ComGiftConfig comGiftConfig = comGiftConfigService.getById(giftId);
        LiveInteractMsgVo message = new LiveInteractMsgVo();
        message.setGiftId(giftId);
        message.setGiftName(updatedOrder.getProductName());
        message.setGiftImgUrl(comGiftConfig.getGiftPicUrl());
        message.setTextType(MsgTypeEnum.GIFT.getCode());
        message.setLiveId(updatedOrder.getLiveId());

        json.set("giftInfo", JSON.toJSONString(message));
        json.set("userInfo", userInfo);
        json.set("giftId", giftId);
        msgQuery.setCloudCustomData(JSON.toJSONString(json));
        msgQuery.setMsgContent("");
        msgQuery.setNotifyTypeEnum(TIMSendTypeEnum.GIFT);
        msgQuery.setGroupId(liveDat.getImGroupId());
        msgQuery.setMsgType(MsgTypeConstants.TIM_CUSTOM_ELEM);
        msgQuery.setFromUserId("administrator");
        imService.sendCustomGroupMsg(msgQuery);

        log.info("**************************** sendGiftMessage end ****************************");
    }

    @Override
    public R sendMsgByIntelligent(SendMsgByIntelligentDto sm) {
        log.info("开始发送智能模版消息===={}", JSONObject.toJSONString(sm));
        Long templateId = sm.getId();
        Long liveId = sm.getLiveId();
        String taskId = sm.getTaskId();
        Long comId = sm.getComId();

//        LiveRecordDat byLiveId = liveRecordDatService.getByLiveId(liveId, taskId);
        LiveDat liveDat = liveDatMapper.selectById(liveId);
        String imGroupId = liveDat.getImGroupId();
        // 获取智能模版记录
        // 获取直播开播记录

        // 获取智能模版消息
        Query query = new Query();
        query.with(Sort.by(Sort.Order.asc("sendTime")));
        val messageList = mongoTemplate.find(query, LiveInteractMsgVo.class, MongoConstant.LIVE_TEMPLATE_MESSAGE + "_" + templateId);
        if (CollectionUtils.isEmpty(messageList)) {
            log.info("智能模板不存在互动消息, templateId={}", templateId);
            return R.ok();
        }
        log.info("智能模版消息不为空");
        //按秒分组消息
        val messageMap = messageList.stream().collect(Collectors.groupingBy(LiveInteractMsgVo::getSendTime));
//        Date startTime = byLiveId.getStartTime();
        Date startTime = sm.getStartTime();
        // 获取发送消息开始时间
        Long sendTime = DateUtil.between(startTime, new Date(), DateUnit.SECOND);
        Long offsetTime = sm.getOffsetTime();
        if (ObjectUtils.isNotEmpty(offsetTime)) {
            sendTime = sendTime + offsetTime;
        }
        Integer videoDuration = sm.getVideoDuration();
        log.info("发送开始---》", messageMap);
        //退出条件
        while (sendTime < videoDuration) {
            Object o = redisService.get(2, INTELLIGENT_LIVE_STATUS + liveId);
            //直播结束，key被删除，发送消息结束
            if (ObjectUtils.isEmpty(o)) {
                break;
            }
//            log.info("3======sendTime：{}", sendTime);
            if (messageMap.containsKey(sendTime)) {
                log.info("3======sendTime：{}", sendTime);
                //模拟登录，批量加入群组
//                val accountList = messageMap.get(sendTime).stream()
//                        .map(message -> String.valueOf(message.getLiveUserId())).distinct().toList();
                try {
                    messageMap.get(sendTime).forEach(message -> {
//                        log.info("5-----》{}", JSON.toJSONString(message));
                        ThreadUtil.execAsync(() -> {
                            sendIntelligentMsg(message, comId, imGroupId, liveId);
                        });

                    });
                } catch (Exception e) {
                    //不影响后续消息发送
                    log.error("发送消息失败: ", e);
                    continue;
                }

            }
            ThreadUtil.sleep(1000);
            sendTime++; //自增1秒
        }
        return R.ok();
    }

    /**
     * 发送智能模版消息
     * @param message
     * @param comId
     * @param imGroupId
     * @param liveId
     */
    private void sendIntelligentMsg(LiveInteractMsgVo message, Long comId, String imGroupId, Long liveId) {
        Integer textType = message.getTextType();
        // MsgTypeEnum.FLOWER.getCode() == textType ||
        if (MsgTypeEnum.TEXT.getCode() == textType) {
            MyCustomData.UserInfo userInfo = new MyCustomData.UserInfo();
            // 设置公司ID
            userInfo.setComId(comId);
            String userAvatar = message.getUserAvatar();
            if (StringUtils.isNotBlank(userAvatar)) {
                userInfo.setSendUserAvatar(EmojiUtil.toHtml(userAvatar));
            }
            // 设置发送用户昵称
            userInfo.setSendNickName(EmojiUtil.toHtml(message.getUsername()));
            userInfo.setSendUserId(message.getUserId());
            // 设置发送用户角色为观众
            userInfo.setSendUserRole(message.getLiveUserRole());
            // 设置操作用户昵称为空
            userInfo.setOperateNickName(EmojiUtil.toHtml(message.getUsername()));
//            userInfo.setOperateNickName(EmojiUtil.toHtml(message.getUsername()));
            // 创建群组消息查询对象
            ImMsgQuery msgQuery = new ImMsgQuery();

            // 创建自定义数据对象
            MyCustomData text = new MyCustomData();
            // 创建消息信息对象
            MyCustomData.MessageInfo messageInfo = new MyCustomData.MessageInfo();
            // 设置通知类型为字段控制类型
            messageInfo.setNoticeType(TIMSendTypeEnum.FIELD_CONTROL.getValue());  // 设置用户信息
            text.setUserInfo(userInfo);
            // 设置消息信息
            text.setMessageInfo(messageInfo);
            // 将自定义数据对象转换为JSON字符串
            String json = JSON.toJSONString(text);
            // 设置云自定义数据
            msgQuery.setCloudCustomData(json);
            // 设置消息内容
            msgQuery.setMsgContent(message.getMessageText());
            // 设置消息类型为文本消息
            msgQuery.setMsgType(MsgTypeConstants.TIM_TEXT_ELEM);
            // 设置群组ID
            msgQuery.setGroupId(imGroupId);
            // 设置发送者ID为管理员
            msgQuery.setFromUserId("administrator");
            // 调用消息服务发送群组消息
            imService.sendGroupMsg(msgQuery);
        } else if (MsgTypeEnum.GIFT.getCode() == textType) {
            // 发送礼物信息
            ImMsgQuery msgQuery = new ImMsgQuery();
            Long giftId = message.getGiftId();
            cn.hutool.json.JSONObject json = new cn.hutool.json.JSONObject();
            MyCustomData.UserInfo userInfo = new MyCustomData.UserInfo();
            // 设置公司ID
            userInfo.setComId(comId);
            userInfo.setSendUserAvatar(EmojiUtil.toHtml(message.getUserAvatar()));
            // 设置发送用户昵称
            userInfo.setSendNickName(EmojiUtil.toHtml(message.getUsername()));
            userInfo.setSendUserId(message.getUserId());
            // 设置操作用户昵称为空
            userInfo.setOperateNickName(EmojiUtil.toHtml(message.getUsername()));
            json.set("giftPicUrl", message.getGiftImgUrl());
            json.set("giftName", message.getGiftName());
            json.set("userInfo", userInfo);
            json.set("id", giftId);
            msgQuery.setCloudCustomData(JSON.toJSONString(json));
            msgQuery.setMsgContent("");
            msgQuery.setNotifyTypeEnum(TIMSendTypeEnum.GIFT);
            msgQuery.setGroupId(imGroupId);
            msgQuery.setMsgType(MsgTypeConstants.TIM_CUSTOM_ELEM);
            msgQuery.setFromUserId("administrator");
            imService.sendCustomGroupMsg(msgQuery);
        } else if (MsgTypeEnum.COMMODITY.getCode() == textType) {
            // 商品信息
            Long commodityId = message.getCommodityId();
            log.info("commodityId====={}", commodityId);
            GetCommodityInfoVo commodityInfo = liveCommodityMapper.getCommodityInfo(commodityId, liveId, comId);
            Boolean flag = false;
            if (ObjectUtils.isEmpty(commodityInfo)) {
                ComCommodity comCommodity = comCommodityMapper.selectById(commodityId);
                if (ObjectUtils.isNotEmpty(comCommodity)) {
                    flag = true;
                    addLiveCommodity(liveId, comId, commodityId);
                    commodityInfo = liveCommodityMapper.getCommodityInfo(commodityId, liveId, comId);
                }
            } else {
                flag = true;
            }
            log.info("commodityId===flag=={}", flag);
            if (flag) {
                // 更新库里的数据
                LiveCommodity liveCommodity = liveCommodityMapper.selectOne(new LambdaQueryWrapper<LiveCommodity>().eq(LiveCommodity::getCommodityId, commodityId).eq(LiveCommodity::getLiveId, liveId).eq(LiveCommodity::getDeleteFlag, false));
                TIMSendTypeEnum typeEnum = TIMSendTypeEnum.getByValue(message.getNotifyType());
                if (typeEnum.equals(TIMSendTypeEnum.PRODUCT_UP)) {
                    liveCommodity.setPutStatus(CommodityStatusEnum.PUT_STATUS_LISTING.getValue());
                } else if (typeEnum.equals(TIMSendTypeEnum.PRODUCT_DOWN)) {
                    liveCommodity.setPutStatus(CommodityStatusEnum.PUT_STATUS_UNDERCARRIAGE.getValue());
                } else if (typeEnum.equals(TIMSendTypeEnum.PRODUCT_RECOMMENDED)) {
                    // 推荐时判断该商品是否上架
                    if (commodityInfo.getPutStatus() == 0) {
                        liveCommodity.setPutStatus(CommodityStatusEnum.PUT_STATUS_LISTING.getValue());
                        sendPutStatus(commodityInfo, commodityId, imGroupId);
                    }
                    liveCommodity.setRecommendStatus(CommodityStatusEnum.RECOMMEND.getValue());
                } else if (typeEnum.equals(TIMSendTypeEnum.PRODUCT_UN_RECOMMENDED)) {
                    liveCommodity.setRecommendStatus(CommodityStatusEnum.NOT_RECOMMEND.getValue());
                } else if (typeEnum.equals(TIMSendTypeEnum.PRODUCT_SOLD_OUT)) {
                    // 售罄时判断该商品是否上架
                    // 推荐时判断该商品是否上架
                    if (commodityInfo.getPutStatus() == 0) {
                        liveCommodity.setPutStatus(CommodityStatusEnum.PUT_STATUS_LISTING.getValue());
                        sendPutStatus(commodityInfo, commodityId, imGroupId);
                    }
                    liveCommodity.setSellOut(CommodityStatusEnum.SELL_OUT.getValue());
                } else if (typeEnum.equals(TIMSendTypeEnum.PRODUCT_UN_SOLD_OUT)) {
                    liveCommodity.setSellOut(CommodityStatusEnum.NOT_SELL_OUT.getValue());
                }
                liveCommodityMapper.updateById(liveCommodity);
                log.info("数据修改完毕======{}", liveCommodity);
                ImMsgQuery msgQuery = new ImMsgQuery();
                cn.hutool.json.JSONObject json = new cn.hutool.json.JSONObject();
                json.set("commodityInfo", JSON.toJSONString(commodityInfo));
                json.set("userInfo", null);
                json.set("commodityId", commodityId);

                msgQuery.setCloudCustomData(JSON.toJSONString(json));
                msgQuery.setMsgContent("");
                msgQuery.setNotifyTypeEnum(typeEnum);
                msgQuery.setGroupId(imGroupId);
                msgQuery.setMsgType(MsgTypeConstants.TIM_CUSTOM_ELEM);
                msgQuery.setFromUserId("administrator");
                log.info("商品消息发送成功:{},------发送时间为：{} ", commodityInfo.getCommodity(), message.getSendTime());
                imService.sendCustomGroupMsg(msgQuery);
            }
        }
    }

    /**
     * 发送上架消息
     *
     * @param commodityInfo
     * @param commodityId
     * @param imGroupId
     */
    private void sendPutStatus(GetCommodityInfoVo commodityInfo, Long commodityId, String imGroupId) {
        ImMsgQuery msgQuery = new ImMsgQuery();
        int value = CommodityStatusEnum.PUT_STATUS_LISTING.getValue();
        commodityInfo.setPutStatus(value);
        cn.hutool.json.JSONObject json = new cn.hutool.json.JSONObject();
        json.set("commodityInfo", JSON.toJSONString(commodityInfo));
        json.set("userInfo", null);
        json.set("commodityId", commodityId);
        msgQuery.setCloudCustomData(JSON.toJSONString(json));
        msgQuery.setNotifyTypeEnum(TIMSendTypeEnum.PRODUCT_UP);
        msgQuery.setMsgContent("");
        msgQuery.setGroupId(imGroupId);
        msgQuery.setFromUserId("administrator");
        imService.sendCustomGroupMsg(msgQuery);
    }



    @Override
    public void refreshTemolateByTrancoding() {
        List<IntelligentTemplateDat> intelligentTemplateDats = this.baseMapper.selectList(new LambdaQueryWrapper<IntelligentTemplateDat>().eq(IntelligentTemplateDat::getStatus, 2).isNotNull(IntelligentTemplateDat::getTask).eq(IntelligentTemplateDat::getVideoType, VideoTypeEnum.MEDIA.getValue()).eq(IntelligentTemplateDat::getDeleteFlag, false));
        if (ObjectUtils.isNotEmpty(intelligentTemplateDats)) {
            for (IntelligentTemplateDat intelligentTemplateDat : intelligentTemplateDats) {
                DescribeTaskDetailRequestinfo build = DescribeTaskDetailRequestinfo.builder().secretId(secretId).secretKey(secretKey).taskId(intelligentTemplateDat.getTask()).subAppId(subAppId).build();

                DescribeTaskDetailResponse describeTaskDetailResponse = DescribeTaskDetail.describeTaskDetail(build);
                log.info("describeTaskDetailResponse==={}", describeTaskDetailResponse);
                if (ObjectUtils.isNotEmpty(describeTaskDetailResponse)) {
                    String resultJsonStr = DescribeTaskDetailResponse.toJsonString(describeTaskDetailResponse);
                    log.info("resultJsonStr==={}", resultJsonStr);
                    JSONObject jsonObject = JSONObject.parseObject(resultJsonStr);
                    if (ObjectUtils.isNotEmpty(jsonObject)) {
//                        JSONObject response = jsonObject.getJSONObject("Response");
//                        if (ObjectUtils.isNotEmpty(response)) {
                        JSONObject procedureTask = jsonObject.getJSONObject("ProcedureTask");
                        if (ObjectUtils.isNotEmpty(procedureTask)) {
                            JSONArray mediaProcessResultSet = procedureTask.getJSONArray("MediaProcessResultSet");
                            if (ObjectUtils.isNotEmpty(mediaProcessResultSet)) {
                                JSONObject o = mediaProcessResultSet.getJSONObject(0);
                                if (ObjectUtils.isNotEmpty(o)) {
                                    JSONObject transcodeTask = o.getJSONObject("TranscodeTask");
                                    if (ObjectUtils.isNotEmpty(transcodeTask)) {
                                        JSONObject output = transcodeTask.getJSONObject("Output");
                                        if (ObjectUtils.isNotEmpty(output)) {
                                            String string = output.getString("Url");
                                            intelligentTemplateDat.setStatus(1);
                                            intelligentTemplateDat.setVideoUrl(string);
                                            this.baseMapper.updateById(intelligentTemplateDat);
                                        }

                                    }

                                }

                            }
                        }

//                        }

                    }

                }

            }
        }
    }

    private void addLiveCommodity(Long liveId, Long comId, Long commodityId) {
        // 获取最大排序号和商品可投放状态
        QueryWrapper<LiveCommodity> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("MAX(order_number) as orderNumber");
        queryWrapper.eq("com_id", comId);
        queryWrapper.eq("live_id", liveId);
        queryWrapper.eq("delete_flag", false);
        LiveCommodity cgcMax = liveCommodityMapper.selectOne(queryWrapper);
        int index = 0;
        if (org.apache.commons.lang3.ObjectUtils.isNotEmpty(cgcMax)) {
            index = cgcMax.getOrderNumber() + 1;
        }
        LiveCommodity lrc = new LiveCommodity();
        lrc.setId(idService.nextId());
        lrc.setCommodityId(commodityId);
        lrc.setComId(comId);
        lrc.setOrderNumber(index);
        lrc.setLiveId(liveId);
        lrc.setPutStatus(0);
        liveCommodityMapper.insert(lrc);
    }

    /**
     * 查询创建进度
     */
    public String gettaskStatus() {


        DescribeTaskDetailRequestinfo build = DescribeTaskDetailRequestinfo.builder().secretId(secretId).secretKey(secretKey).taskId("1253642699093897234").subAppId(subAppId).build();

        DescribeTaskDetailResponse describeTaskDetailResponse = DescribeTaskDetail.describeTaskDetail(build);
        String resultJsonStr = DescribeTaskDetailResponse.toJsonString(describeTaskDetailResponse);
        JSONObject jsonObject = JSONObject.parseObject(resultJsonStr);


        return "";
    }

}
