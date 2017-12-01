package cn.godsdo.dubbo.impl.channel;

import cn.godsdo.constant.MongoConstant;
import cn.godsdo.constant.RedisConstants;
import cn.godsdo.dto.LiveWatchRecordDto;
import cn.godsdo.dto.channel.BindChannelByRoomIdDto;
import cn.godsdo.dto.channel.BindRoomByChannelIdDto;
import cn.godsdo.dto.channel.GetChannelLiveDataDto;
import cn.godsdo.dubbo.channel.ChannelLiveInfoService;
import cn.godsdo.dubbo.com.BlackService;
import cn.godsdo.dubbo.crossService.RedisDubboService;
import cn.godsdo.entity.channel.ChannelLiveInfo;
import cn.godsdo.entity.live.LiveDat;
import cn.godsdo.mapper.channel.ChannelLiveInfoMapper;
import cn.godsdo.mapper.com.CompanyDatMapper;
import cn.godsdo.util.R;
import cn.godsdo.util.ShortUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.constant.Constants;
import com.y20y.interfaces.IdService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * <p>
 * 《渠道直播间绑定表》 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2024-04-24
 */
@Slf4j
@DubboService
public class ChannelLiveInfoServiceImpl extends ServiceImpl<ChannelLiveInfoMapper, ChannelLiveInfo> implements ChannelLiveInfoService {

    @DubboReference
    private IdService idService;
    @DubboReference
    RedisDubboService redisService;
    @Resource
    CompanyDatMapper companyDatMapper;
    @Value(value = "${shortDomain}")
    public String shortDomain;
    @Autowired
    private MongoTemplate mongoTemplate;
    @DubboReference
    BlackService blackService;

    @Override
    public R getBindRoomByChannelId(Long channelId, Long comId) {
        List<LiveDat> bindRoomByChannelId = this.baseMapper.getBindRoomByChannelId(channelId, comId);
        return R.ok(bindRoomByChannelId);
    }

    @Override
    public R getNotBindRoomByChannelId(Long channelId, Long comId) {
        List<LiveDat> bindRoomByChannelId = this.baseMapper.getNotBindRoomByChannelId(channelId, comId);
        return R.ok(bindRoomByChannelId);
    }

    @Override
    public R bindRoomByChannelId(BindRoomByChannelIdDto dto, Long comId, Long accountId) {
        // 从dto中获取频道ID和直播间ID列表
        Long channelId = dto.getChannelId();
        List<Long> liveIds = dto.getLiveIds();
        // 如果传回来的房间集合为空，
        if (ObjectUtils.isEmpty(liveIds)) {
            this.baseMapper.update(null, new LambdaUpdateWrapper<ChannelLiveInfo>()
                    .set(ChannelLiveInfo::getDeleteFlag, true).set(ChannelLiveInfo::getUpdateBy, accountId)
                    .eq(ChannelLiveInfo::getChannelId, channelId));
            return R.ok();
        }
        // 去除解除绑定的直播间
//        if(ObjectUtils.isNotEmpty())
        this.baseMapper.update(null, new LambdaUpdateWrapper<ChannelLiveInfo>()
                .set(ChannelLiveInfo::getDeleteFlag, true).set(ChannelLiveInfo::getUpdateBy, accountId)
                .eq(ChannelLiveInfo::getChannelId, channelId).notIn(ChannelLiveInfo::getLiveId, liveIds));

        // 查询未删除的指定频道和公司的直播间信息
        List<ChannelLiveInfo> list = this.baseMapper.selectList(new LambdaQueryWrapper<ChannelLiveInfo>().eq(ChannelLiveInfo::getDeleteFlag, false)
                .eq(ChannelLiveInfo::getChannelId, channelId).eq(ChannelLiveInfo::getComId, comId));
        ArrayList<ChannelLiveInfo> result = new ArrayList<>();
        // 遍历直播间ID列表
        for (Long id : liveIds) {
            // 查找当前直播间是否已存在于list中
            Optional<ChannelLiveInfo> first = list.stream().filter(item -> item.getLiveId().equals(id)).findFirst();
            // 若list中不存在当前直播间，则创建新的ChannelLiveInfo对象，并添加至结果列表
            if (!first.isPresent()) {
                ChannelLiveInfo cga = new ChannelLiveInfo();
                cga.setId(idService.nextId());
                cga.setChannelId(channelId);
                cga.setComId(comId);
                cga.setLiveId(id);
                cga.setCreateBy(accountId);
                result.add(cga);
            }
        }
        // 若结果列表不为空，则批量插入新的直播间信息
        if (result.size() > 0) {
            this.baseMapper.insertBatch(result);
        }
        return R.ok();
    }

    @Override
    public R getChannelShortUrl(Long comId, Long accountId, Long channelLiveId, String url) {
        ChannelLiveInfo channelLiveInfo = this.baseMapper.selectById(channelLiveId);
        if (ObjectUtils.isEmpty(channelLiveInfo)) {
            return R.failed("渠道信息不存在");
        }
        String shortUrl = channelLiveInfo.getShortUrl();
        // 删除之前的短连接
        String key = RedisConstants.LIVE_WATCH_SHORT_URL + shortUrl.substring(shortUrl.lastIndexOf(Constants.SLASH) + 1);
        redisService.del(key);
        // 存入mongdb中操作记录
//        ActionLog log=new ActionLog();
//        log.setClientId(resourceDat.getClientId());
//        log.setAccountId(accountId);
//        log.setLoginName(loginName);
//        log.setRemark("删除短链接:"+shortUrl+",渠道Id:"+resourceDat.getChannelId()+"，资源Id:"+resourceDat.getResourceId());
//        log.setCreateTime(System.currentTimeMillis());
//        log.setType(ActionLogEnum.DEL.getKey());
//        mongoService.saveLog(log);
//        ActionLog log=new ActionLog();
        String[] uslList = shortDomain.split(",");
        // 重新生成短链接
        String tmpShortUrl = ShortUtil.create(blackService, url, uslList[new Random().nextInt(uslList.length)]);
        // 查看生成的短连接是否存在
        ChannelLiveInfo cri = this.baseMapper.selectOne(new LambdaQueryWrapper<ChannelLiveInfo>().eq(ChannelLiveInfo::getComId, comId).eq(ChannelLiveInfo::getShortUrl, tmpShortUrl));
        if (ObjectUtils.isEmpty(cri)) {
            channelLiveInfo.setShortUrl(tmpShortUrl);
            channelLiveInfo.setUpdateBy(accountId);
            this.baseMapper.updateById(channelLiveInfo);
            // 插入操作记录
//            ActionLog log=new ActionLog();
//            log.setClientId(resourceDat.getClientId());
//            log.setAccountId(accountId);
//            log.setLoginName(loginName);
//            log.setRemark("新增短链接:"+tmpShortUrl+",渠道Id:"+resourceDat.getChannelId()+"，资源Id:"+resourceDat.getResourceId());
//            log.setCreateTime(System.currentTimeMillis());
//            log.setType(ActionLogEnum.ADD.getKey());
//            mongoService.saveLog(log);
            String newkey = RedisConstants.LIVE_WATCH_SHORT_URL + tmpShortUrl.substring(tmpShortUrl.lastIndexOf(Constants.SLASH) + 1);
            Object value = blackService.get(newkey);
            if (ObjectUtils.isEmpty(value)) {
                blackService.setShare(key, url);
            }
        } else {
            return R.failed("重新生成失败");

        }
        return R.ok(tmpShortUrl);
    }

    @Override
    public R getChannelLiveData(Long comId, GetChannelLiveDataDto dto) {
        Criteria criteria = Criteria.where("liveId").is(dto.getLiveId()).and("comId").is(comId)
                .and("channelId").is(dto.getChannelId());
        Query query = new Query(criteria);
        List<LiveWatchRecordDto> queryLiveUserWatchVOS = new ArrayList<>();
        Long total = mongoTemplate.count(query, LiveWatchRecordDto.class, MongoConstant.LIVE_WATCH_RECORD_USER);
        if (total > 0) {
            // 排序
//            Sort sort = Sort.by(Sort.Direction.DESC, "enterTime");
//            query.with(sort);
            // 分页 (当前页-1)*每页大小，每页大小
            query.skip((dto.getPage() - 1) * dto.getPageSize()).limit(dto.getPageSize().intValue());
            queryLiveUserWatchVOS = mongoTemplate.find(query, LiveWatchRecordDto.class, MongoConstant.LIVE_WATCH_RECORD_USER);
        }
        return R.ok(queryLiveUserWatchVOS, total);
    }

    @Override
    public R getLiveByTime(Long comId, String time) {
        String startTime = time + " 00:00:01";
        String endTime = time + " 23:59:59";
        return R.ok(this.baseMapper.getLiveByTime(comId, startTime, endTime));
    }

    @Override
    public R bindChannelByRoomId(BindChannelByRoomIdDto dto, Long comId, Long accountId) {
        // 从dto中获取频道ID和直播间ID列表
        List<Long> channelIds = dto.getChannelIds();
        Long liveId = dto.getLiveId();
        // 如果传回来的房间集合为空，
        if (ObjectUtils.isEmpty(channelIds)) {
            this.baseMapper.update(null, new LambdaUpdateWrapper<ChannelLiveInfo>()
                    .set(ChannelLiveInfo::getDeleteFlag, true).set(ChannelLiveInfo::getUpdateBy, accountId)
                    .eq(ChannelLiveInfo::getLiveId, liveId));
            return R.ok();
        }
        // 去除解除绑定的直播间
//        if(ObjectUtils.isNotEmpty())
        this.baseMapper.update(null, new LambdaUpdateWrapper<ChannelLiveInfo>()
                .set(ChannelLiveInfo::getDeleteFlag, true).set(ChannelLiveInfo::getUpdateBy, accountId)
                .eq(ChannelLiveInfo::getLiveId, liveId).notIn(ChannelLiveInfo::getChannelId, channelIds));

        // 查询未删除的指定频道和公司的直播间信息
        List<ChannelLiveInfo> list = this.baseMapper.selectList(new LambdaQueryWrapper<ChannelLiveInfo>().eq(ChannelLiveInfo::getDeleteFlag, false)
                .eq(ChannelLiveInfo::getLiveId, liveId).eq(ChannelLiveInfo::getComId, comId));
        ArrayList<ChannelLiveInfo> result = new ArrayList<>();
        // 遍历直播间ID列表
        for (Long id : channelIds) {
            // 查找当前直播间是否已存在于list中
            Optional<ChannelLiveInfo> first = list.stream().filter(item -> item.getChannelId().equals(id)).findFirst();
            // 若list中不存在当前直播间，则创建新的ChannelLiveInfo对象，并添加至结果列表
            if (!first.isPresent()) {
                ChannelLiveInfo cga = new ChannelLiveInfo();
                cga.setId(idService.nextId());
                cga.setChannelId(id);
                cga.setComId(comId);
                cga.setLiveId(liveId);
                cga.setCreateBy(accountId);
                result.add(cga);
            }
        }
        // 若结果列表不为空，则批量插入新的直播间信息
        if (result.size() > 0) {
            this.baseMapper.insertBatch(result);
        }
        return R.ok();
    }

    public static void main(String[] args) {
        System.out.println(DateUtil.yesterday());
    }



}