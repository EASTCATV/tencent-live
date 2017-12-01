package cn.godsdo.dubbo.impl.channel;

import cn.godsdo.constant.MongoConstant;
import cn.godsdo.dto.GetComChannelRankingChartDto;
import cn.godsdo.dubbo.channel.ChannelRankingStatisticsService;
import cn.godsdo.entity.channel.ChannelRankingStatistics;
import cn.godsdo.entity.com.CompanyDat;
import cn.godsdo.entity.live.LiveDat;
import cn.godsdo.mapper.channel.ChannelLiveInfoMapper;
import cn.godsdo.mapper.channel.ChannelRankingStatisticsMapper;
import cn.godsdo.mapper.com.CompanyDatMapper;
import cn.godsdo.util.R;
import cn.godsdo.util.TimeUtil;
import cn.godsdo.vo.QueryLiveUserWatchVO;
import cn.godsdo.vo.channel.ChannelRankingVo;
import cn.godsdo.vo.channel.GetComChannelRankingDataVo;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.interfaces.IdService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 渠道排行榜统计 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2024-07-12
 */
@Slf4j
@DubboService
public class ChannelRankingStatisticsServiceImpl extends ServiceImpl<ChannelRankingStatisticsMapper, ChannelRankingStatistics> implements ChannelRankingStatisticsService {
    @DubboReference
    private IdService idService;
    @Resource
    CompanyDatMapper companyDatMapper;
    @Resource
    ChannelLiveInfoMapper channelLiveInfoMapper;
    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void channelRankingStatistics() {
        String format = DateUtil.format(DateUtil.yesterday(), "yyyy-MM-dd");
//        String format = "2024-07-10";
        String startTime = format + " 00:00:01";
        String endTime = format + " 23:59:59";
        List<CompanyDat> companyDats = companyDatMapper.selectList(new LambdaQueryWrapper<CompanyDat>().eq(CompanyDat::getStatus, 1).eq(CompanyDat::getDeleteFlg, false));
        if (ObjectUtils.isNotEmpty(companyDats)) {
            ArrayList<ChannelRankingStatistics> list = new ArrayList<>();
            for (CompanyDat companyDat : companyDats) {
                Long comId = companyDat.getId();
                // 获取昨天所有直播的直播间
                List<LiveDat> lives = channelLiveInfoMapper.getLiveByTime(comId, startTime, endTime);
                if (ObjectUtils.isNotEmpty(lives)) {
                    for (LiveDat dat : lives) {
                        Long id = dat.getId();
                        try {
                            Criteria criteria = Criteria.where("comId").is(comId);
                            criteria.and("liveId").is(id);

                            // 获取所有渠道为空的数据
                            criteria.and("channelId").exists(true).ne("").ne(null).ne(0);
                            criteria.and("statisticsDate").is(format);
                            Aggregation aggregation = Aggregation.newAggregation(
                                    Aggregation.match(criteria),
                                    Aggregation.group("channelId").count().as("sum")

                                            .avg("watchDurationsSeconds").as("watchDurationsSeconds")
                                            .first("channelId").as("channelId"),
                                    Aggregation.project()
                                            .and("channelId").previousOperation()//必须添加一个字段的 previousOperation 否则会报错
                                            .and("channelId").as("channelId")
                                            .and("sum").as("sum")
                                            .and("watchDurationsSeconds").as("watchDurationsSeconds")
                            );
                            AggregationResults<ChannelRankingVo> results = mongoTemplate.aggregate(aggregation, MongoConstant.LIVE_USER_WATCH_TIME_STATISTICS, ChannelRankingVo.class);
                            List<ChannelRankingVo> resultList = results.getMappedResults();

                            if (ObjectUtils.isNotEmpty(resultList)) {
                                log.info("aaaaaaaaa--={}=====->{}", id, resultList);
                                for (ChannelRankingVo channelRankingVo : resultList) {
                                    Long channelId = channelRankingVo.getChannelId();
                                    Criteria criteria1 = Criteria.where("comId").is(comId);
                                    criteria1.and("liveId").is(id);
                                    // 获取所有渠道为空的数据
                                    criteria1.and("channelId").is(channelId);

                                    criteria1.and("statisticsDate").is(format);
                                    criteria1.and("isNew").is(1);
                                    Query query = new Query(criteria1);
                                    Long total = mongoTemplate.count(query, QueryLiveUserWatchVO.class, MongoConstant.LIVE_USER_WATCH_TIME_STATISTICS);
                                    channelRankingVo.setIsNewSum(total);
                                    // 获取总发言数
                                    Criteria criteria2 = Criteria.where("comId").is(comId);
                                    criteria2.and("liveId").is(id);
                                    criteria2.and("channelId").is(channelId);
                                    criteria2.and("statisticsDate").is(format);
                                    criteria2.and("speakCount").gt(0);
                                    Query query1 = new Query(criteria2);
                                    Long speakSum = mongoTemplate.count(query1, QueryLiveUserWatchVO.class, MongoConstant.LIVE_USER_WATCH_TIME_STATISTICS);
                                    ChannelRankingStatistics channelRankingStatistics = new ChannelRankingStatistics();
                                    channelRankingStatistics.setId(idService.nextId());
                                    channelRankingStatistics.setSpeakSum(speakSum);
                                    channelRankingStatistics.setChannelId(channelId);
                                    channelRankingStatistics.setAverageDuration(channelRankingVo.getWatchDurationsSeconds());
                                    channelRankingStatistics.setTotal(channelRankingVo.getSum());
                                    channelRankingStatistics.setNewTotal(total);
                                    channelRankingStatistics.setLiveId(id);
                                    channelRankingStatistics.setComId(comId);
                                    channelRankingStatistics.setStatisticsDate(format);
                                    list.add(channelRankingStatistics);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            if (ObjectUtils.isNotEmpty(list)) {
                log.info(JSONObject.toJSONString(list));
                this.baseMapper.insertBatch(list);
            }
        }
    }

    @Override
    public R getComChannelRankingChart(Long comId, GetComChannelRankingChartDto vo) {
        List<GetComChannelRankingDataVo> comChannelRankingChart = this.baseMapper.getComChannelRankingChart(vo.getDate(), vo.getLiveId(), comId, vo.getSize());
        return R.ok(comChannelRankingChart);
    }

    @Override
    public R getComChannelRankingData(Long comId, GetComChannelRankingChartDto dto) {

        Page<ChannelRankingStatistics> page = new Page<>(dto.getPage(), dto.getPageSize());
        // 获取账户列表数据
        IPage<GetComChannelRankingDataVo> vo = this.baseMapper.getComChannelRankingData(page, dto, comId);
        List<GetComChannelRankingDataVo> records = vo.getRecords();
        for (GetComChannelRankingDataVo record : records) {
//            record.getAverageDuration()
            // 时间日期转换
            Long averageDuration = record.getAverageDuration();
            if (averageDuration > 0) {
                String s = TimeUtil.convertSecondsToHMS(averageDuration);
                record.setTime(s);
            }

        }
        return R.ok(records, vo.getTotal());
    }

    @Override
    public R getComChannelRankingInfo(Long comId, GetComChannelRankingChartDto vo) {
        Criteria criteria1 = Criteria.where("comId").is(comId);
        criteria1.and("liveId").is(vo.getLiveId());
        // 获取所有渠道为空的数据
        criteria1.and("channelId").is(vo.getChannelId());
        criteria1.and("statisticsDate").is(vo.getDate());
        Query query = new Query(criteria1);
        Long total = mongoTemplate.count(query, QueryLiveUserWatchVO.class, MongoConstant.LIVE_USER_WATCH_TIME_STATISTICS);
        List<QueryLiveUserWatchVO> queryLiveUserWatchVOS = new ArrayList<>();
        if (total > 0) {
            // 排序
            Sort sort = Sort.by(Sort.Direction.DESC, "enterTime");
            query.with(sort);
            // 分页 (当前页-1)*每页大小，每页大小
            query.skip((long) (vo.getPage() - 1) * vo.getPageSize()).limit(vo.getPageSize().intValue());

            queryLiveUserWatchVOS = mongoTemplate.find(query, QueryLiveUserWatchVO.class, MongoConstant.LIVE_USER_WATCH_TIME_STATISTICS);
        }
        return R.ok(queryLiveUserWatchVOS, total);
    }
}
