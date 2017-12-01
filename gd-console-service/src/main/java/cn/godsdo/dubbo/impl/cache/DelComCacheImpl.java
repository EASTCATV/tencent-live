package cn.godsdo.dubbo.impl.cache;

import cn.godsdo.dubbo.cache.ClearCache;
import cn.godsdo.dubbo.cache.DelComCache;
import cn.godsdo.dubbo.com.ComDefultSettingService;
import cn.godsdo.dubbo.live.LiveRoomCdnService;
import cn.godsdo.entity.com.ComDefultSetting;
import cn.godsdo.entity.live.LiveRoomCdn;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * @Author : just do it
 * @ApiNote :
 */
@DubboService
public class DelComCacheImpl implements DelComCache {
    @DubboReference(check = false)
    ClearCache clearcache;
    @DubboReference
    ComDefultSettingService comDefultSettingService;
    @DubboReference(check = false)
    LiveRoomCdnService liveRoomCdnService;

    @Override
    public void del(Long comId) {
        //查询账户下的ComDefultSettingWatchService
        ComDefultSetting cdnInfoByComId = comDefultSettingService.getCdnInfoByComId(comId);
        if (cdnInfoByComId != null) {
            //删除缓存
            clearcache.delComDefultSettingService(cdnInfoByComId.getPushCdnId());
        }
        //删除liveRoom缓存
        LiveRoomCdn LiveRoomCdn = liveRoomCdnService.getByCdnId(cdnInfoByComId.getPushCdnId());
        if (LiveRoomCdn != null) {
            String cdnId = LiveRoomCdn.getCdnId();
            clearcache.delCDNCache(cdnId);
        }


    }
}
