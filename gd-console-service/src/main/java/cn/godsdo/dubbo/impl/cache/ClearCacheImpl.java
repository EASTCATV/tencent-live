package cn.godsdo.dubbo.impl.cache;

import cn.godsdo.constant.CacheConstants;
import cn.godsdo.dubbo.cache.ClearCache;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

/**
 * @Author : just do it
 * @ApiNote :
 */
@DubboService
public class ClearCacheImpl implements ClearCache {
    //删除直播缓存

    /**
     * 表 LiveTag
     * 表 LiveWatchChat
     * 表 LiveMarquee
     * 表 LiveDat
     * 表 LiveConfig
     * 表 LiveBindBack LiveBackVideo ComVideoDat
     * 表 ComDefultSetting LiveRoomCdn
     * 表 ComLiveVideo
     * @param comId
     * @param liveId
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = CacheConstants.WATCH_BASE_BACK_TAG, key = "#comId + '_' + #liveId"),
            @CacheEvict(value = CacheConstants.WATCH_BASE_BACK_CHAT, key = "#comId + '_' + #liveId"),
            @CacheEvict(value = CacheConstants.WATCH_BASE_BACK_MARQUEE, key = "#comId + '_' + #liveId"),
            @CacheEvict(value = CacheConstants.WATCH_BASE_INFO, key = "#comId + '_' + #liveId"),
            @CacheEvict(value = CacheConstants.WATCH_BASE_CONFIG, key = "#comId + '_' + #liveId"),
            @CacheEvict(value = CacheConstants.WATCH_BASE_BACK_CONFIG, key = "#comId + '_' + #liveId"),
            @CacheEvict(value = CacheConstants.WATCH_BASE_BACK_CDN, key = "#comId + '_' + #liveId"),
            @CacheEvict(value = CacheConstants.COM_LIVE_VIDEO, key = "#comId + '_' + #liveId"),
            @CacheEvict(value = CacheConstants.COM_COMMODITY, key = "#comId + '_' + #liveId")
    })
    public void delLiveCache(Long comId, Long liveId) {}
    @Override
    @CacheEvict(value = CacheConstants.COM_COMMODITY, key = "#comId + '_' + #liveId")
    public void delCommodityCache(Long comId, Long liveId){}
    @Override
    @CacheEvict(value = CacheConstants.COM_COMMODITY_INFO, key = "#comId + '_' + #liveId + '_' + #commodityId")
    public void delCommodityInfoCache(Long comId, Long liveId, String commodityId){}
    /**
     * 表 VideoShareRecord
     * @param id
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = CacheConstants.WATCH_BASE_VIDEO_SHARE, key = "#id")
    })
    public void delShareRecord(Long id){}

    /**
     * 表 ChannelShareDat
     * @param encryptCode
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = CacheConstants.GET_CHANNERL_SHARE, key = "#encryptCode")
    })
    public void delChannelShareDat(String encryptCode){}

    /**
     * 删除CDN缓存
     * 出现的情况:1,域名被封 2,公司公众号重新授权
     * 如果域名被封,先删除CDN缓存,再删除公司默认设置缓存
     * 再修改下直播间信息,保存,触发删除直播间缓存
     * @param cdnId
     */
    @Override
    @CacheEvict(value = CacheConstants.WATCH_BASE_ROOM_CDN, key = "#cdnId")
    public void delCDNCache(String cdnId) {}

    /**
     * 删除公司默认设置缓存
     * @param comId
     */

    @Override
    @CacheEvict(value = CacheConstants.WATCH_BASE_COM_DEFULT_CDN, key = "#comId")
    public void delComDefultSettingService(String comId) {}

    /**
     * 训练营直播间学员表
     */
    @Override
    @CacheEvict(value = CacheConstants.WATCH_CAMP_LIVE_USER, key = "#liveId + '_' + #unionId")
    public void delCampLiveUserByCache(Long liveId, String unionId){}

    @Override
    @CacheEvict(value = CacheConstants.WATCH_BASE_COM_COUPON_LIVE, key = "#comId + '_' + #couponId+ '_' + #commodityId")
    public void delCouponCommodity(Long comId, Long couponId,Long commodityId) {}

    @Override
    @CacheEvict(value = CacheConstants.WATCH_BASE_COUPON_LIVE_DATA, key = "#comId + '_' + #liveId+ '_' + #couponId")
    public void delCouponInfo(Long comId, Long liveId, Long couponId) {}

    @Override
    @CacheEvict(value = CacheConstants.WATCH_BASE_LIVE_RECORD, key = "#comId + '_' + #liveId")
    public void delLiveRecordInfo(Long comId, Long liveId) {}


}
