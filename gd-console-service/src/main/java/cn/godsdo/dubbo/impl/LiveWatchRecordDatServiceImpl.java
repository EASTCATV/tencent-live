package cn.godsdo.dubbo.impl;

import cn.godsdo.constant.MongoConstant;
import cn.godsdo.dto.LiveWatchRecordDto;
import cn.godsdo.entity.LiveWatchRecordDat;
import cn.godsdo.mapper.LiveWatchRecordDatMapper;
import cn.godsdo.dubbo.LiveWatchRecordDatService;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongodb.client.result.DeleteResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 直播观看记录表 服务实现类
 * </p>
 *
 * @author hdk
 * @since 2024-06-05
 */
@Slf4j
@DubboService
public class LiveWatchRecordDatServiceImpl extends ServiceImpl<LiveWatchRecordDatMapper, LiveWatchRecordDat> implements LiveWatchRecordDatService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void watchDataJob() {
        try {
            log.info("开始进行分批次从mongo中获取直播观看记录表数据");
            //获取mongo中的数据量
            long dataCount = mongoTemplate.count(new Query(), MongoConstant.LIVE_WATCH_RECORD_DATA);
            log.info("mongo中的数据量：" + dataCount);
            //如果为0则直接进行返回
            if (dataCount == 0) {
                log.info("数据为0，同步结束");
                return;
            }
            //先判断长度是否大于1000
            int page;
            Integer isFlag = 0;
            int dataSize = (int) dataCount;

            if (dataSize % 1000 == 0) {
                // 如果数据数量为1000的整数倍，页数为对应的倍数
                page = dataSize / 1000;
            } else {
                // 如果有余数，则页数为对应的倍数加1
                page = dataSize / 1000 + 1;
            }
            //判断条件
            Query query = new Query();
            //循环插入
            for (int i = 0; i < page; i++) {
                //先进行数据的排序拉取，拉取进入时间最远的
                query.with(Sort.by(Sort.Direction.ASC, "enterTime")).limit(1000);
                //进行查询
                List<LiveWatchRecordDto> liveWatchRecordDtos = mongoTemplate.find(query, LiveWatchRecordDto.class, MongoConstant.LIVE_WATCH_RECORD_DATA);
                //判断是否是新增操作 ，数据库没有则进行新增，有则进行数据更新
                log.info("进行第" + page + "页插入" + "liveWatchRecordDtos-------" + JSON.toJSON(liveWatchRecordDtos));

                if (CollectionUtils.isNotEmpty(liveWatchRecordDtos)) {
                    List<LiveWatchRecordDat> liveWatchRecordDats = new ArrayList<>();
                    LiveWatchRecordDat liveWatchRecordDat;
                    for (LiveWatchRecordDto liveWatchRecordDto : liveWatchRecordDtos) {
                        liveWatchRecordDat = new LiveWatchRecordDat();
                        BeanUtils.copyProperties(liveWatchRecordDto,liveWatchRecordDat);
                        liveWatchRecordDats.add(liveWatchRecordDat);
                    }

                    isFlag = this.baseMapper.synchronizationData(liveWatchRecordDats);

                    if (isFlag > 0) {
                        log.info("进行第" + page + "页同步成功，开始删除mongo中的数据");
                        log.info("进行第" + page + "页删除" + "liveWatchRecordDtos-------" + JSON.toJSON(liveWatchRecordDtos));
                        //删除这1000个里面获取最近的一个,然后从最近时间开始往前,删除有结束时间的。
                        Query lastEnter = new Query(
                                Criteria.where("enterTime").lte(liveWatchRecordDtos.get(dataSize - 1).getEnterTime())
                                        .and("outTime").exists(true)
                        );
                        //执行删除操作
                        DeleteResult liveWatchRecordDto = mongoTemplate.remove(lastEnter, LiveWatchRecordDto.class, MongoConstant.LIVE_WATCH_RECORD_DATA);
                        if (liveWatchRecordDto.wasAcknowledged()) {
                            log.info("进行第" + page + "页删除成功，同步结束");
                        } else {
                            log.info("进行第" + page + "页删除失败");
                        }
                    } else if (isFlag == 0) {
                        log.info("进行第" + page + "页同步成功");
                    }
                }
            }
        } catch (Exception e) {
            log.error("同步失败" + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

}
