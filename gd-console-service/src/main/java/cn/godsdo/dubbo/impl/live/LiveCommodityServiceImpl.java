package cn.godsdo.dubbo.impl.live;

import cn.godsdo.constant.CacheConstants;
import cn.godsdo.constant.MsgTypeConstants;
import cn.godsdo.dto.assistan.UpdateCommodityByAssistantDto;
import cn.godsdo.dto.live.BindRoomCommodityDto;
import cn.godsdo.dto.live.GetRoomCommodityDto;
import cn.godsdo.dubbo.cache.ClearCache;
import cn.godsdo.dubbo.im.ImService;
import cn.godsdo.dubbo.live.LiveCommodityService;
import cn.godsdo.entity.LiveRecordDat;
import cn.godsdo.entity.com.ComCommodityGroup;
import cn.godsdo.entity.live.LiveCommodity;
import cn.godsdo.entity.live.LiveCountDown;
import cn.godsdo.entity.live.LiveDat;
import cn.godsdo.enums.im.TIMSendTypeEnum;
import cn.godsdo.enums.live.CommodityStatusEnum;
import cn.godsdo.enums.live.LiveStatusEnum;
import cn.godsdo.mapper.live.ComCommodityGroupMapper;
import cn.godsdo.mapper.live.LiveCommodityMapper;
import cn.godsdo.mapper.live.LiveCountDownMapper;
import cn.godsdo.mapper.live.LiveDatMapper;
import cn.godsdo.query.im.ImMsgQuery;
import cn.godsdo.util.R;
import cn.godsdo.vo.assistant.GetCommodityInfoVo;
import cn.godsdo.vo.assistant.GetCommodityListByAssistantVo;
import cn.godsdo.vo.live.getLiveCommodityVo;
import cn.hutool.json.JSONObject;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.constant.Constants;
import com.y20y.interfaces.IdService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * 直播间商品绑定表 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2018/10/19
 */
@Slf4j
@DubboService
public class LiveCommodityServiceImpl extends ServiceImpl<LiveCommodityMapper, LiveCommodity> implements LiveCommodityService {
    @DubboReference(check = false, retries = 0)
    private ImService imService;
    @Resource
    ComCommodityGroupMapper comCommodityGroupMapper;
    @Resource
    LiveDatMapper liveDatMapper;

    @DubboReference
    IdService idService;

    @Resource
    LiveCountDownMapper liveCountDownMapper;
    @DubboReference(check = false)
    ClearCache clearcache;

    @Override
    public R getRoomCommodity(Long comId, GetRoomCommodityDto dto) {
        // 创建分页对象
        Page<LiveCommodity> page = new Page<>(dto.getPage(), dto.getPageSize());
        // 调用基类的方法获取直播间商品列表
        IPage<getLiveCommodityVo> roomCommodityList = this.baseMapper.getRoomCommodityList(page, comId, dto.getLiveId());
        // 对直播间商品列表进行处理
        List<getLiveCommodityVo> collect = roomCommodityList.getRecords().stream().map(e -> {
            Long groupId = e.getGroupId();
            // 默认分组
            if (groupId == 0) {
                e.setGroupName(Constants.DEFAULT_GROUP);
            } else {
                // 查询商品分组信息
                ComCommodityGroup comCommodityGroup = comCommodityGroupMapper.selectOne(new LambdaQueryWrapper<ComCommodityGroup>()
                        .eq(ComCommodityGroup::getComId, comId).eq(ComCommodityGroup::getId, groupId));
                // 若未找到对应的商品分组信息，则使用默认分组
                if (ObjectUtils.isEmpty(comCommodityGroup)) {
                    e.setGroupName(Constants.DEFAULT_GROUP);
                } else {
                    e.setGroupName(comCommodityGroup.getGroupName());
                }
            }
            return e;
        }).collect(Collectors.toList());
        // 返回处理后的直播间商品列表及总数
        return R.ok(collect, roomCommodityList.getTotal());
    }


    @Override
    @CacheEvict(key = "#comId + '_' + #dto.getLiveId()",value = CacheConstants.COM_COMMODITY)
    public R bindRoomCommodity(Long comId, Long accountId, BindRoomCommodityDto dto) {
        // 清除缓存
        // 获取房间和商品信息
        Long liveId = dto.getLiveId();
        List<Long> ids = dto.getIds();
        if (ObjectUtils.isEmpty(ids)) {
            return R.failed("商品id不能为空");
        }
        LiveDat liveDat = liveDatMapper.selectOne(new LambdaQueryWrapper<LiveDat>().eq(LiveDat::getId, liveId).eq(LiveDat::getDeleteFlg, false));
        if (ObjectUtils.isEmpty(liveDat)) {
            return R.failed("直播间不存在");
        }

        // 获取最大排序号和商品可投放状态
        QueryWrapper<LiveCommodity> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("MAX(order_number) as orderNumber");
        queryWrapper.eq("com_id", comId);
        queryWrapper.eq("live_id", liveId);
        queryWrapper.eq("delete_flag", false);
        LiveCommodity cgcMax = this.baseMapper.selectOne(queryWrapper);
        int index = 0;
        if (ObjectUtils.isNotEmpty(cgcMax)) {
            index = cgcMax.getOrderNumber() + 1;
        }
        LiveCountDown lrcd = liveCountDownMapper.selectOne(Wrappers.<LiveCountDown>lambdaQuery().eq(LiveCountDown::getLiveId, liveId).eq(LiveCountDown::getComId, comId));
        int putStatus = 0;
        if (ObjectUtils.isNotEmpty(lrcd)) {
            putStatus = lrcd.getCommodityEnable();
        }
        List<Long> bindLiveCommodity = this.baseMapper.getBindLiveCommodity(liveId, comId);

        // 绑定商品并插入数据库
        List<LiveCommodity> list = new ArrayList<>();
        for (Long id : ids) {
            // 筛选是否存在已绑定数据
            Optional<Long> first = bindLiveCommodity.stream().filter(item -> item.equals(id)).findFirst();
            if (!first.isPresent()) {
                LiveCommodity lrc = new LiveCommodity();
                lrc.setId(idService.nextId());
                lrc.setCommodityId(id);
                lrc.setComId(comId);
                lrc.setOrderNumber(index);
                lrc.setLiveId(liveId);
                lrc.setCreateBy(accountId);
                lrc.setPutStatus(putStatus);
                list.add(lrc);
                index++;
            }
        }
        this.baseMapper.insertBatch(list);
        return R.ok();
    }

    //    public static void main(String[] args) {
//        List<Integer> list = Arrays.asList(1, 2, 3);
//        List<Integer> list1 = Arrays.asList(1, 2, 3, 4);
//        for (Integer id : list1) {
//            Optional<Integer> first = list.stream().filter(item -> item.equals(id)).findFirst();
//            if (!first.isPresent()) {
//                System.out.println(id);
//            }
//        }
//    }
    @Override
    @CacheEvict(key = "#comId + '_' + #liveId",value = CacheConstants.COM_COMMODITY)
    public R unbindRoomCommodity(Long accountId, Long liveId, Long commodityId) {
        // 查找指定房间和商品的关联记录
        LiveCommodity liveCommodity = this.baseMapper.selectOne(new LambdaQueryWrapper<LiveCommodity>()
                .eq(LiveCommodity::getCommodityId, commodityId).eq(LiveCommodity::getLiveId, liveId).eq(LiveCommodity::getDeleteFlag, false));
        // 如果关联记录存在则返回操作成功
        if (ObjectUtils.isEmpty(liveCommodity)) {
            return R.ok();
        }
        // 更新关联记录的删除标志和操作人
        liveCommodity.setUpdateBy(accountId);
        liveCommodity.setDeleteFlag(true);
        this.baseMapper.updateById(liveCommodity);
        return R.ok();
    }

    @Override
    public R getCommodityListByAssistant(Long liveId, Long comId) {
        List<GetCommodityListByAssistantVo> vo = this.baseMapper.getCommodityListByAssistant(comId, liveId);
        return R.ok(vo);
    }

    @Override
    public R updatePutStatusByAssistant(UpdateCommodityByAssistantDto dto, Long comId, Long accountId) {

        Long commodityId = dto.getCommodityId();
        String imGroupId = dto.getImGroupId();
        Integer recommendStatus = dto.getRecommendStatus();
        Long liveId = dto.getLiveId();
        //删除缓存
        clearcache.delCommodityCache(comId, liveId);
        clearcache.delCommodityInfoCache(comId, liveId, commodityId.toString());



        Integer putType = dto.getPutType();
        GetCommodityInfoVo commodityInfo = this.baseMapper.getCommodityInfo(commodityId, liveId, comId);
        String type = "";
        if (ObjectUtils.isEmpty(commodityInfo)) {
            return R.failed("请核对商品信息");
        }
        ImMsgQuery msgQuery = new ImMsgQuery();
        JSONObject json = new JSONObject();
        json.set("userInfo", null);
        json.set("commodityId", commodityId);
        TIMSendTypeEnum typeEnum = TIMSendTypeEnum.PRODUCT_DOWN;
        LiveCommodity liveCommodity = this.baseMapper.selectOne(new LambdaQueryWrapper<LiveCommodity>().eq(LiveCommodity::getCommodityId, commodityId).eq(LiveCommodity::getLiveId, liveId).eq(LiveCommodity::getDeleteFlag, false));
        // 上架
        if (CommodityStatusEnum.PUT_STATUS_LISTING.getValue() == putType) {
            type = "上架";
            typeEnum = TIMSendTypeEnum.PRODUCT_UP;

//            json.set("notifyType", TIMSendTypeEnum.PRODUCT_UP.getValue());

        } else {
            // 下架   下架时判断是否推荐中
            type = "下架";
            typeEnum = TIMSendTypeEnum.PRODUCT_DOWN;
//            json.set("notifyType", typeEnum.getValue());
            // 下架时商品存在推荐信息
            if (CommodityStatusEnum.RECOMMEND.getValue() == recommendStatus) {
                ImMsgQuery msgQuery1 = new ImMsgQuery();
//                JSONObject json1 = new JSONObject();
////                json1.set("notifyType", TIMSendTypeEnum.PRODUCT_UN_RECOMMENDED.getValue());
//                json1.set("commodityInfo", commodityInfo);
//                json1.set("commodityId", commodityId);
//                json1.set("userInfo", null);
                json.set("commodityInfo", JSON.toJSONString(commodityInfo));
                int putStatus = CommodityStatusEnum.NOT_RECOMMEND.getValue();
                commodityInfo.setPutStatus(putStatus);
                msgQuery1.setCloudCustomData(JSON.toJSONString(json));
                msgQuery1.setNotifyTypeEnum(TIMSendTypeEnum.PRODUCT_UN_RECOMMENDED);
                msgQuery1.setMsgContent("");
                msgQuery1.setGroupId(imGroupId);
                msgQuery1.setFromUserId("administrator");
                //imService.sendGroupMsg(msgQuery1);
                imService.sendCustomGroupMsg(msgQuery1);
                liveCommodity.setRecommendStatus(putStatus);

            }
        }
        json.set("commodityInfo", JSON.toJSONString(commodityInfo));
        // 发送请求
        msgQuery.setCloudCustomData(JSON.toJSONString(json));
        msgQuery.setMsgContent("");
        msgQuery.setNotifyTypeEnum(typeEnum);
        msgQuery.setGroupId(imGroupId);
        msgQuery.setMsgType(MsgTypeConstants.TIM_CUSTOM_ELEM);
        msgQuery.setFromUserId("administrator");
        imService.sendCustomGroupMsg(msgQuery);
//        LiveCommodity liveCommodity = this.baseMapper.selectOne(new LambdaQueryWrapper<LiveCommodity>().eq(LiveCommodity::getCommodityId, commodityId).eq(LiveCommodity::getLiveId, liveId).eq(LiveCommodity::getDeleteFlag, false));
        liveCommodity.setPutStatus(putType);
        liveCommodity.setUpdateBy(accountId);
        this.baseMapper.updateById(liveCommodity);
        String desc = "房间号:" + liveId + ",操作者:" + accountId + ",商品id：" + commodityId + "操作内容" + type + ",时间:" + DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss") + "\n";
        log.info(desc);
        return R.ok();
    }

    @Override
    public R updateRecommendByAssistant(UpdateCommodityByAssistantDto dto, Long comId, Long accountId) {
        Long commodityId = dto.getCommodityId();
        String imGroupId = dto.getImGroupId();

        Long liveId = dto.getLiveId();
        //删除缓存
        //clearcache.delLiveCache(comId, liveId);
        //删除缓存
        clearcache.delCommodityCache(comId, liveId);
        clearcache.delCommodityInfoCache(comId, liveId, commodityId.toString());

        Integer recommendStatus = dto.getRecommendStatus();
        GetCommodityInfoVo commodityInfo = this.baseMapper.getCommodityInfo(commodityId, liveId, comId);
        String type = "";
        if (ObjectUtils.isEmpty(commodityInfo)) {
            return R.failed("请核对商品信息");
        }
        LiveCommodity liveCommodity = this.baseMapper.selectOne(new LambdaQueryWrapper<LiveCommodity>().eq(LiveCommodity::getCommodityId, commodityId).eq(LiveCommodity::getLiveId, liveId).eq(LiveCommodity::getDeleteFlag, false));
        liveCommodity.setRecommendStatus(recommendStatus);
        Integer styleType = dto.getStyleType();
        liveCommodity.setStyleType(styleType);
        liveCommodity.setUpdateBy(accountId);
        ImMsgQuery msgQuery = new ImMsgQuery();
        TIMSendTypeEnum typeEnum = null;
        JSONObject json = new JSONObject();
        json.set("userInfo", null);
        json.set("commodityId", commodityId);
        // 推荐
        if (CommodityStatusEnum.NOT_RECOMMEND.getValue() != recommendStatus) {
            this.baseMapper.update(null, new LambdaUpdateWrapper<LiveCommodity>()
                    .set(LiveCommodity::getRecommendStatus, CommodityStatusEnum.NOT_RECOMMEND.getValue())
                    .set(LiveCommodity::getUpdateBy, accountId)
                    .eq(LiveCommodity::getComId, comId)
                    .eq(LiveCommodity::getLiveId, liveId)
            );
            type = "推荐";
            typeEnum = TIMSendTypeEnum.PRODUCT_RECOMMENDED;
            commodityInfo.setRecommendStatus(CommodityStatusEnum.RECOMMEND.getValue() );
            Integer putType = dto.getPutType();
            if (CommodityStatusEnum.PUT_STATUS_UNDERCARRIAGE.getValue() == putType){
                // 推荐时遇见未上架时，将其上架
                ImMsgQuery msgQuery1 = new ImMsgQuery();
                int value = CommodityStatusEnum.PUT_STATUS_LISTING.getValue();
                commodityInfo.setPutStatus(value);
                json.set("commodityInfo", JSON.toJSONString(commodityInfo));
                msgQuery1.setCloudCustomData(JSON.toJSONString(json));
                msgQuery1.setNotifyTypeEnum(TIMSendTypeEnum.PRODUCT_UP);
                msgQuery1.setMsgContent("");
                msgQuery1.setGroupId(imGroupId);
                msgQuery1.setFromUserId("administrator");
                //imService.sendGroupMsg(msgQuery1);
                imService.sendCustomGroupMsg(msgQuery1);
                liveCommodity.setPutStatus(value);
            }
//            json.set("notifyType", TIMSendTypeEnum.PRODUCT_RECOMMENDED.getValue());
//            msgQuery.setNotifyTypeEnum(TIMSendTypeEnum.PRODUCT_RECOMMENDED);

        } else {
            commodityInfo.setRecommendStatus(CommodityStatusEnum.NOT_RECOMMEND.getValue() );
            // 取消推荐
            type = "取消推荐";
//            json.set("notifyType", TIMSendTypeEnum.PRODUCT_UN_RECOMMENDED.getValue());
//            msgQuery.setNotifyTypeEnum(TIMSendTypeEnum.PRODUCT_UN_RECOMMENDED);
            typeEnum = TIMSendTypeEnum.PRODUCT_UN_RECOMMENDED;
        }
        commodityInfo.setStyleType(styleType.toString());
        json.set("commodityInfo", JSON.toJSONString(commodityInfo));
        this.baseMapper.updateById(liveCommodity);
        msgQuery.setNotifyTypeEnum(typeEnum);
        msgQuery.setCloudCustomData(JSON.toJSONString(json));
        msgQuery.setMsgContent("");
        msgQuery.setGroupId(imGroupId);
        msgQuery.setMsgType(MsgTypeConstants.TIM_CUSTOM_ELEM);
        msgQuery.setFromUserId("administrator");

        imService.sendCustomGroupMsg(msgQuery);

        String desc = "房间号:" + liveId + ",操作者:" + accountId + ",商品id：" + commodityId + "操作内容" + type + ",时间:" + DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss") + "\n";
        log.info(desc);
        return R.ok();
    }

    @Override
    public R updateSellOutByAssistant(UpdateCommodityByAssistantDto dto, Long comId, Long accountId) {
        Long commodityId = dto.getCommodityId();
        String imGroupId = dto.getImGroupId();
        Long liveId = dto.getLiveId();
        //删除缓存
        clearcache.delCommodityCache(comId, liveId);
        clearcache.delCommodityInfoCache(comId, liveId, commodityId.toString());
        Integer sellOut = dto.getSellOut();
        LiveCommodity liveCommodity = this.baseMapper.selectOne(new LambdaQueryWrapper<LiveCommodity>().eq(LiveCommodity::getCommodityId, commodityId).eq(LiveCommodity::getLiveId, liveId).eq(LiveCommodity::getDeleteFlag, false));
        liveCommodity.setSellOut(sellOut);
        liveCommodity.setStyleType(dto.getStyleType());
        liveCommodity.setUpdateBy(accountId);
        this.baseMapper.updateById(liveCommodity);
        GetCommodityInfoVo commodityInfo = this.baseMapper.getCommodityInfo(commodityId, liveId, comId);
        String type = "";
        if (ObjectUtils.isEmpty(commodityInfo)) {
            return R.failed("请核对商品信息");
        }
        ImMsgQuery msgQuery = new ImMsgQuery();
        JSONObject json = new JSONObject();
        json.set("commodityInfo", JSON.toJSONString(commodityInfo));
        json.set("userInfo", null);
        json.set("commodityId", commodityId);
        TIMSendTypeEnum typeEnum = null;
        // 售罄
        if (CommodityStatusEnum.SELL_OUT.getValue() == sellOut) {
            type = "售罄";
            typeEnum = TIMSendTypeEnum.PRODUCT_SOLD_OUT;
        } else {
            // 取消售罄
            type = "取消售罄";
            typeEnum = TIMSendTypeEnum.PRODUCT_UN_SOLD_OUT;
        }
        msgQuery.setNotifyTypeEnum(typeEnum);
        msgQuery.setCloudCustomData(JSON.toJSONString(json));
        msgQuery.setMsgContent("");
        msgQuery.setGroupId(imGroupId);
        msgQuery.setMsgType(MsgTypeConstants.TIM_CUSTOM_ELEM);
        msgQuery.setFromUserId("administrator");

        imService.sendCustomGroupMsg(msgQuery);

        String desc = "房间号:" + liveId + ",操作者:" + accountId + ",商品id：" + commodityId + "操作内容" + type + ",时间:" + DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss") + "\n";
        log.info(desc);
        return R.ok();
    }
}
