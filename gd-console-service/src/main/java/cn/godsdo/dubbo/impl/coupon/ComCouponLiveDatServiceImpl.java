package cn.godsdo.dubbo.impl.coupon;

import cn.godsdo.constant.CacheConstants;
import cn.godsdo.constant.MsgTypeConstants;
import cn.godsdo.constant.RedisConstants;
import cn.godsdo.dto.assistan.UpdatePutStatusByCouponDto;
import cn.godsdo.dto.coupon.BindCouponLiveDto;
import cn.godsdo.dto.coupon.GetAllCouponDto;
import cn.godsdo.dubbo.cache.ClearCache;
import cn.godsdo.dubbo.coupon.ComCouponLiveDatService;
import cn.godsdo.dubbo.crossService.RedisDubboService;
import cn.godsdo.dubbo.im.ImService;
import cn.godsdo.entity.coupon.ComCouponCommodityDat;
import cn.godsdo.entity.coupon.ComCouponDat;
import cn.godsdo.entity.coupon.ComCouponLiveDat;
import cn.godsdo.enums.im.TIMSendTypeEnum;
import cn.godsdo.mapper.coupon.ComCouponDatMapper;
import cn.godsdo.mapper.coupon.ComCouponLiveDatMapper;
import cn.godsdo.query.im.ImMsgQuery;
import cn.godsdo.util.R;
import cn.godsdo.vo.coupon.GetAllCouponVo;
import cn.hutool.json.JSONObject;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.cache.annotation.CacheEvict;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * 直播间优惠卷绑定表 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2024-07-05
 */
@Slf4j
@DubboService
public class ComCouponLiveDatServiceImpl extends ServiceImpl<ComCouponLiveDatMapper, ComCouponLiveDat> implements ComCouponLiveDatService {
    @DubboReference(check = false, retries = 0)
    private ImService imService;
    @Resource
    ComCouponDatMapper comCouponDatMapper;
    @DubboReference
    ClearCache clearCache;
   @DubboReference
   RedisDubboService redisDubboService;

    @Override
    public R getLiveNotBindCoupon(Long comId, Long accountId, GetAllCouponDto dto) {
        // 根据页面和页面大小创建分页对象
        Page<ComCouponCommodityDat> page = new Page<>(dto.getPage(), dto.getPageSize());
        // 调用baseMapper的getAllCoupon
        IPage<GetAllCouponVo> vo = this.baseMapper.getLiveNotBindCoupon(page, comId, dto);
        return R.ok(vo.getRecords(), vo.getTotal());
    }

    @Override
    public R getLiveBindCoupon(Long comId, Long accountId, GetAllCouponDto dto) {
        // 根据页面和页面大小创建分页对象
        Page<ComCouponCommodityDat> page = new Page<>(dto.getPage(), dto.getPageSize());
        // 调用baseMapper的getLiveBindCoupon
        IPage<GetAllCouponVo> vo = this.baseMapper.getLiveBindCoupon(page, comId, dto);
        return R.ok(vo.getRecords(), vo.getTotal());
    }

    @Override
    public R updatePutStatusByCoupon(Long comId, Long accountId, UpdatePutStatusByCouponDto dto) {
        Long couponId = dto.getCouponId();
        String imGroupId = dto.getImGroupId();
        Long liveId = dto.getLiveId();
        Integer putStatus = dto.getPutStatus();
        ComCouponLiveDat comCouponLiveDat = this.baseMapper.selectOne(new LambdaQueryWrapper<ComCouponLiveDat>().eq(ComCouponLiveDat::getComId, comId)
                .eq(ComCouponLiveDat::getLiveId, liveId).eq(ComCouponLiveDat::getCouponId, couponId));
        if (ObjectUtils.isEmpty(comCouponLiveDat)) {
            return R.failed("请核对优惠券信息");
        }
        ImMsgQuery msgQuery = new ImMsgQuery();
        JSONObject json = new JSONObject();
        json.set("userInfo", null);
        json.set("couponId", couponId);
        TIMSendTypeEnum typeEnum = TIMSendTypeEnum.PRODUCT_DOWN;
        ComCouponDat comCouponDat = comCouponDatMapper.selectOne(new LambdaQueryWrapper<ComCouponDat>().eq(ComCouponDat::getId, couponId).eq(ComCouponDat::getComId, comId).eq(ComCouponDat::getDeleteFlag, false));
        String type = "";
        // 上架
        if (1 == putStatus) {
            type = "上架";
            typeEnum = TIMSendTypeEnum.COUPON_UP;


        } else {
            // 下架
            type = "下架";
            typeEnum = TIMSendTypeEnum.COUPON_DOWN;
            Integer recommend = comCouponLiveDat.getRecommend();
            if(1 == recommend){
                ImMsgQuery msgQuery1 = new ImMsgQuery();
                json.set("comCouponDat", JSON.toJSONString(comCouponDat));
                comCouponLiveDat.setRecommend(0);
                msgQuery1.setCloudCustomData(JSON.toJSONString(json));
                msgQuery1.setNotifyTypeEnum(TIMSendTypeEnum.COUPON_UN_RECOMMENDED);
                msgQuery1.setMsgContent("");
                msgQuery1.setGroupId(imGroupId);
                msgQuery1.setFromUserId("administrator");
                //imService.sendGroupMsg(msgQuery1);
                imService.sendCustomGroupMsg(msgQuery1);
            }
        }
        json.set("comCouponDat", JSON.toJSONString(comCouponDat));
        // 发送请求
        msgQuery.setCloudCustomData(JSON.toJSONString(json));
        msgQuery.setMsgContent("");
        msgQuery.setNotifyTypeEnum(typeEnum);
        msgQuery.setGroupId(imGroupId);
        msgQuery.setMsgType(MsgTypeConstants.TIM_CUSTOM_ELEM);
        msgQuery.setFromUserId("administrator");
        imService.sendCustomGroupMsg(msgQuery);
        comCouponLiveDat.setPutStatus(putStatus);
        comCouponLiveDat.setUpdateBy(accountId);
        this.baseMapper.updateById(comCouponLiveDat);
        String couponUserList = RedisConstants.COUPON_USER_LIST + liveId;
        redisDubboService.del(6,couponUserList);
        clearCache.delCouponInfo(comId,liveId,couponId);
        String desc = "房间号:" + liveId + ",操作者:" + accountId + ",优惠券id：" + couponId + "操作内容" + type + ",时间:" + DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss") + "\n";
        log.info(desc);
        return R.ok();
    }

    @Override
    public R updateRecommendByCoupon(Long comId, Long accountId, UpdatePutStatusByCouponDto dto) {
        Long couponId = dto.getCouponId();
        String imGroupId = dto.getImGroupId();
        Long liveId = dto.getLiveId();
        Integer recommend = dto.getRecommend();
        ComCouponLiveDat comCouponLiveDat = this.baseMapper.selectOne(new LambdaQueryWrapper<ComCouponLiveDat>().eq(ComCouponLiveDat::getComId, comId)
                .eq(ComCouponLiveDat::getLiveId, liveId).eq(ComCouponLiveDat::getCouponId, couponId));
        if (ObjectUtils.isEmpty(comCouponLiveDat)) {
            return R.failed("请核对优惠券信息");
        }
        Integer putStatus = comCouponLiveDat.getPutStatus();
        ImMsgQuery msgQuery = new ImMsgQuery();
        JSONObject json = new JSONObject();
        json.set("userInfo", null);
        json.set("couponId", couponId);
        TIMSendTypeEnum typeEnum = TIMSendTypeEnum.PRODUCT_DOWN;
        ComCouponDat comCouponDat = comCouponDatMapper.selectOne(new LambdaQueryWrapper<ComCouponDat>().eq(ComCouponDat::getId, couponId).eq(ComCouponDat::getComId, comId).eq(ComCouponDat::getDeleteFlag, false));
        String type = "";
        // 上架
        if (1 == recommend) {
            this.baseMapper.selectOne(new LambdaQueryWrapper<ComCouponLiveDat>().eq(ComCouponLiveDat::getComId, comId)
                    .eq(ComCouponLiveDat::getLiveId, liveId).eq(ComCouponLiveDat::getCouponId, couponId));
            this.baseMapper.update(null, new LambdaUpdateWrapper<ComCouponLiveDat>()
                    .set(ComCouponLiveDat::getRecommend, 0)
                    .set(ComCouponLiveDat::getUpdateBy, accountId)
                    .eq(ComCouponLiveDat::getLiveId, dto.getLiveId())
                    .eq(ComCouponLiveDat::getRecommend, 1)
            );
            type = "推荐";
            typeEnum = TIMSendTypeEnum.COUPON_RECOMMENDED;
            if(0 == putStatus){
                // 推荐时遇见未上架时，将其上架
                ImMsgQuery msgQuery1 = new ImMsgQuery();
//                int value = CommodityStatusEnum.PUT_STATUS_LISTING.getValue();
                comCouponLiveDat.setPutStatus(1);
                json.set("comCouponDat", JSON.toJSONString(comCouponDat));
                msgQuery1.setCloudCustomData(JSON.toJSONString(json));
                msgQuery1.setNotifyTypeEnum(TIMSendTypeEnum.COUPON_UP);
                msgQuery1.setMsgContent("");
                msgQuery1.setGroupId(imGroupId);
                msgQuery1.setFromUserId("administrator");
                //imService.sendGroupMsg(msgQuery1);
                imService.sendCustomGroupMsg(msgQuery1);
            }
        } else {
            // 下架
            type = "取消推荐";
            typeEnum = TIMSendTypeEnum.COUPON_UN_RECOMMENDED;
        }
        json.set("comCouponDat", JSON.toJSONString(comCouponDat));
        // 发送请求
        msgQuery.setCloudCustomData(JSON.toJSONString(json));
        msgQuery.setMsgContent("");
        msgQuery.setNotifyTypeEnum(typeEnum);
        msgQuery.setGroupId(imGroupId);
        msgQuery.setMsgType(MsgTypeConstants.TIM_CUSTOM_ELEM);
        msgQuery.setFromUserId("administrator");
        imService.sendCustomGroupMsg(msgQuery);
        comCouponLiveDat.setRecommend(recommend);
        comCouponLiveDat.setUpdateBy(accountId);
        this.baseMapper.updateById(comCouponLiveDat);
        String couponUserList = RedisConstants.COUPON_USER_LIST + liveId;
        redisDubboService.del(6,couponUserList);
        clearCache.delCouponInfo(comId,liveId,couponId);
        String desc = "房间号:" + liveId + ",操作者:" + accountId + ",优惠券id：" + couponId + "操作内容" + type + ",时间:" + DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss") + "\n";
        log.info(desc);
        return R.ok();
    }

    @Override
    public R bindCouponLive(Long comId, Long accountId, BindCouponLiveDto dto) {
        List<Long> couponIds = dto.getCouponIds();
        if (ObjectUtils.isEmpty(couponIds)) {
            return R.ok();
        }
        Long liveId = dto.getLiveId();
        ArrayList<ComCouponLiveDat> comCouponLiveDats = new ArrayList<>();
        for (Long couponId : couponIds) {
            ComCouponLiveDat comCouponLiveDat = new ComCouponLiveDat();
            comCouponLiveDat.setCouponId(couponId);
            comCouponLiveDat.setLiveId(liveId);
            comCouponLiveDat.setComId(comId);
            comCouponLiveDat.setUpdateBy(accountId);
            comCouponLiveDats.add(comCouponLiveDat);
        }
        this.baseMapper.insertBatch(comCouponLiveDats);
        return R.ok();
    }

    @Override
    @CacheEvict(value = CacheConstants.WATCH_BASE_COUPON_LIVE_DATA, key = "#comId + '_' + #liveId+ '_' + #couponId")
    public R relieveBindCouponLive(Long comId, Long accountId, Long couponId, Long liveId) {
        ComCouponLiveDat comCouponLiveDat = this.baseMapper.selectOne(new LambdaQueryWrapper<ComCouponLiveDat>().eq(ComCouponLiveDat::getCouponId, couponId)
                .eq(ComCouponLiveDat::getComId, comId).eq(ComCouponLiveDat::getLiveId, liveId).eq(ComCouponLiveDat::getDeleteFlag, false));
        if (ObjectUtils.isEmpty(comCouponLiveDat)) {
            return R.ok();
        }
        if (comCouponLiveDat.getPutStatus() == 1) {
            return R.failed("上架中，请取消后再绑定");
        }
        comCouponLiveDat.setDeleteFlag(true);
        comCouponLiveDat.setUpdateBy(accountId);
        this.baseMapper.updateById(comCouponLiveDat);

        return R.ok();
    }


}
