package cn.godsdo.dubbo.impl;

import cn.godsdo.constant.MongoConstant;
import cn.godsdo.dto.LiveWatchRecordDto;
import cn.godsdo.dubbo.LiveWatchRecordService;
import cn.godsdo.enums.im.TIMC2CEnum;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Date;
import java.util.Objects;

/**
 * @Author: CR7
 * @Date: 2019/5/5 10:13
 * @Description:
 */
@Slf4j
@DubboService
public class LiveWatchRecordServiceImpl implements LiveWatchRecordService {

    @Autowired
    private MongoTemplate mongoTemplate;

    public static void main(String[] args) {
        String date = DateFormatUtils.format(new Date(), "yyyy-MM-dd");
        System.out.println(date);
    }

    @Override
    public void addWatchData(LiveWatchRecordDto liveWatchRecordDto) {
        log.info("***************** addWatchData start liveWatchRecordDto : {} " , liveWatchRecordDto);

        //创建索引
        if(!mongoTemplate.collectionExists(MongoConstant.LIVE_WATCH_RECORD_USER)){
            createInboxIndex("liveId",MongoConstant.LIVE_WATCH_RECORD_USER);
            createInboxIndex("liveRecordId",MongoConstant.LIVE_WATCH_RECORD_USER);
            createInboxIndex("userId",MongoConstant.LIVE_WATCH_RECORD_USER);
            createInboxIndex("statisticsDate",MongoConstant.LIVE_WATCH_RECORD_USER);
        }
        Query query = new Query(
                Criteria.where("statisticsDate").is(liveWatchRecordDto.getStatisticsDate())
                        .and("userId").is(liveWatchRecordDto.getUserId())
                        .and("liveId").is(liveWatchRecordDto.getLiveId())
        );
        LiveWatchRecordDto dto = mongoTemplate.findOne(query, LiveWatchRecordDto.class, MongoConstant.LIVE_WATCH_RECORD_USER);
        //处理判空
        if (null == dto) {
            mongoTemplate.insert(liveWatchRecordDto, MongoConstant.LIVE_WATCH_RECORD_USER);
        } else if (ObjectUtils.compare(dto.getChannelId(), liveWatchRecordDto.getChannelId()) != 0) {
            log.info("addWatchData - updateFirst start");
            mongoTemplate.updateFirst(
                    query,
                    new Update().set("channelId", liveWatchRecordDto.getChannelId())
                            .set("channelName", liveWatchRecordDto.getChannelName()),
                    LiveWatchRecordDto.class,
                    MongoConstant.LIVE_WATCH_RECORD_USER);
            log.info("addWatchData - updateFirst end");
        }

        mongoTemplate.insert(liveWatchRecordDto, MongoConstant.LIVE_WATCH_RECORD_DATA);
        mongoTemplate.insert(liveWatchRecordDto, MongoConstant.LIVE_WATCH_RECORD_PERSISTENCE);

        log.info("***************** addWatchData end liveWatchRecordDto : {} " , liveWatchRecordDto);
    }

    public void createInboxIndex(String index_key, String collectionName) {
        Index index = new Index();
        index.on(index_key, Sort.Direction.ASC);
        mongoTemplate.indexOps(collectionName).ensureIndex(index);
    }

    @Override
    public void updateWatchRecord(JSONObject cloudCustomData) {
        log.info("updateWatchRecord start:{}", cloudCustomData.toJSONString());
        Integer type = cloudCustomData.getInteger("type");
        Long recordId = cloudCustomData.getLong("recordId");
        if (Objects.isNull(type) || Objects.isNull(recordId)) {
            log.warn("type or recordId is null");
            return;
        }
        UpdateResult updateResult = null;
        if (TIMC2CEnum.WATCH.getValue().equals(type)) {
            // 累加上报
            Query query = new Query(Criteria.where("_id").is(recordId));
            LiveWatchRecordDto liveWatchRecordDto = mongoTemplate.findOne(query, LiveWatchRecordDto.class, MongoConstant.LIVE_WATCH_RECORD_DATA);
            //处理判空
            if (liveWatchRecordDto == null) {
                log.warn("liveWatchRecordDto Record not found for id:{}", recordId);
                return;
            }
            updateResult = mongoTemplate.updateFirst(
                    query,
                    new Update().set("latestUploadTime", new Date(liveWatchRecordDto.getLatestUploadTime().getTime() + 30 * 1000)),
                    LiveWatchRecordDto.class,
                    MongoConstant.LIVE_WATCH_RECORD_DATA
            );
            mongoTemplate.updateFirst(
                    query,
                    new Update().set("latestUploadTime", new Date(liveWatchRecordDto.getLatestUploadTime().getTime() + 30 * 1000)),
                    LiveWatchRecordDto.class,
                    MongoConstant.LIVE_WATCH_RECORD_PERSISTENCE);

            Query query2 = new Query(
                    Criteria.where("liveRecordId").is(liveWatchRecordDto.getLiveRecordId())
                            .and("userId").is(liveWatchRecordDto.getUserId())
            );
            mongoTemplate.updateFirst(
                    query2,
                    new Update().set("latestUploadTime", new Date(liveWatchRecordDto.getLatestUploadTime().getTime() + 30 * 1000)),
                    LiveWatchRecordDto.class,
                    MongoConstant.LIVE_WATCH_RECORD_USER);
        } else if (TIMC2CEnum.DISCONNECT.getValue().equals(type)) {
            // 累加上报
            Query query = new Query(Criteria.where("_id").is(recordId));
            LiveWatchRecordDto liveWatchRecordDto = mongoTemplate.findOne(query, LiveWatchRecordDto.class, MongoConstant.LIVE_WATCH_RECORD_DATA);
            //处理判空
            if (liveWatchRecordDto == null) {
                log.warn("liveWatchRecordDto Record not found for id:{}", recordId);
                return;
            }

            // 用户退出直播间
            Date outTime = new Date();
            updateResult = mongoTemplate.updateFirst(
                    query,
                    new Update().set("outTime", outTime),
                    LiveWatchRecordDto.class,
                    MongoConstant.LIVE_WATCH_RECORD_DATA
            );
            mongoTemplate.updateFirst(
                    query,
                    new Update().set("outTime", outTime),
                    LiveWatchRecordDto.class,
                    MongoConstant.LIVE_WATCH_RECORD_PERSISTENCE);

            Query query2 = new Query(
                    Criteria.where("liveRecordId").is(liveWatchRecordDto.getLiveRecordId())
                            .and("userId").is(liveWatchRecordDto.getUserId())
            );
            mongoTemplate.updateFirst(
                    query2,
                    new Update().set("outTime", outTime),
                    LiveWatchRecordDto.class,
                    MongoConstant.LIVE_WATCH_RECORD_USER);
        }
        if (Objects.isNull(updateResult)) {
            log.warn("updateResult isNull");
        }
//        boolean acknowledged = updateResult.wasAcknowledged();
        log.info("updateWatchRecord end:{}", cloudCustomData.toJSONString());
    }

}
