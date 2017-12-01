package cn.godsdo.dubbo.impl.stati;

import cn.godsdo.constant.MongoConstant;
import cn.godsdo.dto.stati.StatiComLiveDto;
import cn.godsdo.dubbo.stati.ComPersonTimeStatiService;
import cn.godsdo.entity.LiveRecordDat;
import cn.godsdo.entity.com.CompanyDat;
import cn.godsdo.entity.stati.ComLivePersonTimeStati;
import cn.godsdo.entity.stati.ComPersonTimeStati;
import cn.godsdo.mapper.LiveRecordDatMapper;
import cn.godsdo.mapper.com.CompanyDatMapper;
import cn.godsdo.mapper.stati.ComLivePersonTimeStatiMapper;
import cn.godsdo.mapper.stati.ComPersonTimeStatiMapper;
import cn.godsdo.vo.ComOnlineSumVo;
import cn.godsdo.vo.LiveUserStateChangeVo;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.interfaces.IdService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

/**
 * <p>
 * 最高并发/人次统计 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2024-07-25
 */
@Slf4j
@DubboService
public class ComPersonTimeStatiServiceImpl extends ServiceImpl<ComPersonTimeStatiMapper, ComPersonTimeStati> implements ComPersonTimeStatiService {
    @Resource
    CompanyDatMapper companyDatMapper;

    @Resource
    private MongoTemplate mongoTemplate;
    @Resource
    LiveRecordDatMapper liveRecordDatMapper;
    @Resource
    ComLivePersonTimeStatiMapper comLivePersonTimeStatiMapper;
    @DubboReference
    IdService idService;

    public void statiComLive() {
        List<CompanyDat> companyDats = companyDatMapper.selectList(new LambdaQueryWrapper<CompanyDat>());
        LocalDate localDate = LocalDate.now().plusDays(-1);
//        String day = DateUtil.today();
        String dateStart = localDate + " 00:00:00";
        String dateEnd = localDate + " 23:59:59";
        companyDats.forEach(companyDat -> {
            Long comId = companyDat.getId();
            try {
                // 拉取直播记录
                List<LiveRecordDat> liveRecordDats = liveRecordDatMapper.selectList(new LambdaQueryWrapper<LiveRecordDat>()
                        .eq(LiveRecordDat::getComId, comId).between(LiveRecordDat::getStartTime, dateStart, dateEnd));
                if (ObjectUtil.isNotEmpty(liveRecordDats)) {
                    ArrayList<StatiComLiveDto> statiComLiveDtos = new ArrayList<>();
                    // 获取所有的直播记录
                    liveRecordDats.forEach(liveRecordDat -> {
                        Long liveId = liveRecordDat.getLiveId();
                        Optional<StatiComLiveDto> first = statiComLiveDtos.stream().filter(item -> item.getLiveId().equals(liveId)).findFirst();
                        // 如果不存在记录
                        if (!first.isPresent()) {
                            StatiComLiveDto statiComLiveDto = new StatiComLiveDto();
                            statiComLiveDto.setLiveId(liveId);
                            ArrayList<LiveRecordDat> list = new ArrayList<>();
                            list.add(liveRecordDat);
                            statiComLiveDto.setData(list);
                            statiComLiveDtos.add(statiComLiveDto);
                        } else {
                            StatiComLiveDto statiComLiveDto = first.get();
                            int i = statiComLiveDtos.indexOf(first.get());
                            List<LiveRecordDat> data = statiComLiveDto.getData();
                            data.add(liveRecordDat);
                            statiComLiveDtos.set(i, statiComLiveDto);
                        }
                    });
                    System.out.println(statiComLiveDtos);
                    log.info("statiComLiveDtos:{}", statiComLiveDtos);
                    List<ComLivePersonTimeStati> livePersonTimeStatis = new ArrayList<>();
                    // 大账号总人数
                    int comCount = 0;
                    // 直播房间数
                    int comLiveCount = statiComLiveDtos.size();
                    for (StatiComLiveDto statiComLiveDto : statiComLiveDtos) {
                        Long liveId = statiComLiveDto.getLiveId();
                        try {
                            List<LiveRecordDat> data = statiComLiveDto.getData();
                            if (ObjectUtil.isNotEmpty(data)) {
//                         count = 0;
                                Set<Long> ids = new HashSet<>();
                                for (LiveRecordDat datum : data) {
                                    Date startTime = datum.getStartTime();
                                    Date endTime = datum.getEndTime();
                                    Criteria criteria = Criteria.where("liveId").is(liveId).and("eventType").is("Login")
                                            .and("eventTime").gte(startTime).lte(endTime);
                                    Aggregation aggregation = Aggregation.newAggregation(
                                            Aggregation.match(criteria),
//                            Aggregation.sort(Sort.Direction.DESC, "_id"),
                                            Aggregation.group("liveUserId")
                                                    .first("liveUserId").as("liveUserId"),
                                            Aggregation.project()
                                                    .and("liveUserId").previousOperation()//必须添加一个字段的 previousOperation 否则会报错
                                    );
                                    AggregationResults<LiveUserStateChangeVo> results = mongoTemplate.aggregate(aggregation, MongoConstant.LIVE_USER_STATE_CHANGE_DAY, LiveUserStateChangeVo.class);
                                    List<LiveUserStateChangeVo> mappedResults = results.getMappedResults();
                                    log.info("liveId:{},-----mappedResults:{}", liveId, JSONObject.toJSONString(mappedResults));
                                    for (LiveUserStateChangeVo mappedResult : mappedResults) {
                                        ids.add(mappedResult.getLiveUserId());
                                    }
                                }
                                int size = ids.size();
                                log.info("liveId:{},-----count:{}", liveId, size);

                                if (size > 0) {
                                    ComLivePersonTimeStati comLivePersonTimeStati = new ComLivePersonTimeStati();
                                    comLivePersonTimeStati.setId(idService.nextId());
                                    comLivePersonTimeStati.setComId(comId);
                                    comLivePersonTimeStati.setLiveId(liveId);
                                    comLivePersonTimeStati.setCount(size);
                                    comLivePersonTimeStati.setDate(localDate);
                                    livePersonTimeStatis.add(comLivePersonTimeStati);
                                    comCount = comCount + size;
                                }
                            }
                        } catch (Exception e) {
                            log.error("直播间统计出错==id=={}", liveId);
                            log.error(e.getMessage());
                        }
                    }
                    if (comCount > 0) {
                        log.info("liveId:{},-----ComPersonTimeStati:{}", comId, comCount);
                        comLivePersonTimeStatiMapper.insertBatch(livePersonTimeStatis);
                        ComPersonTimeStati cpts = new ComPersonTimeStati();
                        cpts.setId(idService.nextId());
                        cpts.setComId(comId);
                        cpts.setLiveCount(comLiveCount);
                        cpts.setViewerCount(getMaxOnline(comId));
                        cpts.setCount(comCount);
                        cpts.setDate(localDate);
                        this.baseMapper.insert(cpts);
                    }
                }
            } catch (Exception e) {
                log.error("大账号统计出错==id=={}", comId);
                log.error(e.getMessage());
            }
        });
    }

    public static void main(String[] args) {
        System.out.println(LocalDate.now());
    }
    /**
     * 获取最高在线人数
     * @param comId
     * @return
     */
    public int getMaxOnline(Long comId) {
        LocalDate localDate = LocalDate.now().plusDays(-1);
        String dateSs = localDate + " 00:00:00";
        String dateEnd = localDate + " 23:59:59";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Criteria criteria = Criteria.where("comId").is(comId);
        try {
            Date start = formatter.parse(dateSs);
            criteria.and("createAt").gte(start).lte(formatter.parse(dateEnd));
            Aggregation aggregation = Aggregation.newAggregation(
                    Aggregation.match(criteria),
                    Aggregation.group("statisticsDate").sum("sumOnLineUser").as("sum")
                            .first("statisticsDate").as("statisticsDate"),
                    Aggregation.project()
                            .and("statisticsDate").previousOperation()//必须添加一个字段的 previousOperation 否则会报错
                            .and("statisticsDate").as("statisticsDate")
                            .and("sum").as("sum"),
                    // 根据在线人数排序
                    Aggregation.sort(Sort.Direction.DESC, "sum")
            );
            AggregationResults<ComOnlineSumVo> results = mongoTemplate.aggregate(aggregation, MongoConstant.STATIC_ONLINE_USER_STATUE, ComOnlineSumVo.class);
            List<ComOnlineSumVo> resultList = results.getMappedResults();
            if (ObjectUtil.isEmpty(resultList)) {
                return 0;
            }
            log.info("196信息===={}", resultList);
            return resultList.get(0).getSum();
        } catch (ParseException e) {

            throw new RuntimeException(e);

        }
    }


}
