package cn.godsdo.dubbo.impl.channel;

import cn.godsdo.constant.RedisConstants;
import cn.godsdo.dubbo.cache.ClearCache;
import cn.godsdo.dubbo.channel.ChannelShareDatService;
import cn.godsdo.dubbo.com.BlackService;
import cn.godsdo.dubbo.live.LiveDatService;
import cn.godsdo.entity.channel.ChannelLiveInfo;
import cn.godsdo.entity.channel.ChannelShareDat;
import cn.godsdo.entity.channel.ComChannelDat;
import cn.godsdo.entity.live.LiveDat;
import cn.godsdo.entity.live.LiveShare;
import cn.godsdo.enums.live.LiveRoleEnum;
import cn.godsdo.mapper.channel.ChannelLiveInfoMapper;
import cn.godsdo.mapper.channel.ChannelShareDatMapper;
import cn.godsdo.mapper.channel.ComChannelDatMapper;
import cn.godsdo.mapper.live.LiveDatMapper;
import cn.godsdo.mapper.live.LiveShareMapper;
import cn.godsdo.util.R;
import cn.godsdo.util.RandomStringGenerator;
import cn.godsdo.vo.channel.GetPosterInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.constant.Constants;
import com.y20y.interfaces.IdService;
import com.y20y.utils.DesHelper;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;

import java.util.Random;

/**
 * <p>
 * 渠道关联分享表 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2024-06-01
 */
@DubboService
public class ChannelShareDatServiceImpl extends ServiceImpl<ChannelShareDatMapper, ChannelShareDat> implements ChannelShareDatService {
    @Resource
    ChannelLiveInfoMapper channelLiveInfoMapper;
    @Resource
    ComChannelDatMapper comChannelDatMapper;
    @Resource
    LiveShareMapper liveShareMapper;
    @Resource
    LiveDatMapper liveDatMapper;
    @Resource
    IdService idService;
    @DubboReference
    LiveDatService liveDatService;
    @Value(value = "${CHANNEL_SHARE_URL}")
    public String channelShareUrl;
    @Value(value = "${WATCH_ROOM_KEY}")
    public String watchRoomKey;
    @Value(value = "${shortDomain}")
    public String shortDomain;
    @DubboReference
    BlackService blackService;
    @DubboReference(check = false)
    ClearCache clearcache;
    @Override
    public R getChannelUrl(Long comId, Long accountId, Long channelLiveId) {
        ChannelLiveInfo channelLiveInfo = channelLiveInfoMapper.selectById(channelLiveId);

        if (ObjectUtils.isEmpty(channelLiveInfo)) {
            return R.failed("渠道信息不存在");
        }
        if (StringUtils.isNotEmpty(channelLiveInfo.getShortUrl())) {
            return R.ok(channelLiveInfo);
        }
        channelLiveInfo = insertShare(comId, accountId, channelLiveInfo);
        return R.ok(channelLiveInfo);
    }

    private ChannelLiveInfo insertShare(Long comId, Long accountId, ChannelLiveInfo channelLiveInfo) {
        String[] split = shortDomain.split(",");
        String header = split[new Random().nextInt(split.length)];
        Long liveId = channelLiveInfo.getLiveId();
        Long channelId = channelLiveInfo.getChannelId();
        ChannelShareDat channelShareDat = this.baseMapper.selectOne(new LambdaQueryWrapper<ChannelShareDat>().eq(ChannelShareDat::getLiveId, liveId).eq(ChannelShareDat::getChannelId, channelId));
        String domain = liveDatService.getShareComDomainUrl(comId, accountId);
        if (ObjectUtils.isEmpty(channelShareDat)) {
            channelShareDat = new ChannelShareDat();
            Long id = idService.nextId();
            channelShareDat.setId(id);
            channelShareDat.setComId(comId);

            channelShareDat.setChannelId(channelId);
            channelShareDat.setLiveId(liveId);
            ComChannelDat comChannelDat = comChannelDatMapper.selectById(channelId);
            if (ObjectUtils.isNotEmpty(comChannelDat)) {
                channelShareDat.setChannelName(comChannelDat.getName());
            }

            // 加密码
            String randomCode = RandomStringGenerator.getRandomCode(id, 1L);
            channelShareDat.setEncryptCode(randomCode);
            // 获取直播间观看链接
            String key = getLiveWatchUrl(liveId);
            channelShareDat.setEnterpriseCode(key);
            this.baseMapper.insert(channelShareDat);
            // 存入Redis的长链
            String url = domain + Constants.SLASH + randomCode;
            // 短连接
            String shortUrl = header + randomCode;
//            String url = shortDomain + Constants.SLASH + randomCode;
//            String s = RedisConstants.CHANNEL_WATCH_SHORT_URL + key;
            blackService.setShare(randomCode,url);
            channelLiveInfo.setShortUrl(shortUrl);
        } else {
            String encryptCode = channelShareDat.getEncryptCode();
            // 短连接
            String shortUrl = header + encryptCode;
            // 存入Redis的长链
            String url = domain + Constants.SLASH + encryptCode;
//            String s = RedisConstants.CHANNEL_WATCH_SHORT_URL + encryptCode;
            blackService.setShare(encryptCode,url);
            channelLiveInfo.setShortUrl(shortUrl);
        }
        channelLiveInfoMapper.updateById(channelLiveInfo);
        return channelLiveInfo;
    }

    /**
     * 获取直播间观看链接
     *
     * @param liveId
     * @return
     */
    private @NotNull String getLiveWatchUrl(Long liveId) {
        DesHelper.getDesHelper(watchRoomKey);
        String key = DesHelper.getEncString(LiveRoleEnum.visitor.getValue() + Constants.UNDERLINE + liveId);
        return key;
    }

    @Override
    public R resetChannelUrl(Long comId, Long accountId, Long channelLiveId) {
        ChannelLiveInfo channelLiveInfo = channelLiveInfoMapper.selectById(channelLiveId);
        if (ObjectUtils.isEmpty(channelLiveInfo)) {
            return R.failed("渠道信息不存在");
        }
        Long liveId = channelLiveInfo.getLiveId();
        Long channelId = channelLiveInfo.getChannelId();
        ChannelShareDat channelShareDat = this.baseMapper.selectOne(new LambdaQueryWrapper<ChannelShareDat>().eq(ChannelShareDat::getLiveId, liveId).eq(ChannelShareDat::getChannelId, channelId));
        //删除缓存
        clearcache.delChannelShareDat(channelShareDat.getEncryptCode());
        String domain = liveDatService.getShareComDomainUrl(comId, accountId);
        Long id = idService.nextId();
        //加密码
        String randomCode = RandomStringGenerator.getRandomCode(id, 1L);
        if (ObjectUtils.isEmpty(channelShareDat)) {
            channelShareDat = new ChannelShareDat();
            channelShareDat.setId(id);
            channelShareDat.setComId(comId);

            channelShareDat.setChannelId(channelId);
            channelShareDat.setLiveId(liveId);

            channelShareDat.setEncryptCode(randomCode);

            ComChannelDat comChannelDat = comChannelDatMapper.selectById(channelId);
            if (ObjectUtils.isNotEmpty(comChannelDat)) {
                channelShareDat.setChannelName(comChannelDat.getName());
            }
            this.baseMapper.insert(channelShareDat);

        } else {
            blackService.delShare(channelShareDat.getEncryptCode());
            channelShareDat.setEncryptCode(randomCode);
            this.baseMapper.updateById(channelShareDat);
        }
        String[] split = shortDomain.split(",");
        String header = split[new Random().nextInt(split.length)];
        // 短连接
        String shortUrl = header + randomCode;
        // 存入Redis的长链
        String url = domain + Constants.SLASH + randomCode;
        blackService.setShare(randomCode,url);
        channelLiveInfo.setShortUrl(shortUrl);
        channelLiveInfoMapper.updateById(channelLiveInfo);
        return R.ok(channelLiveInfo);
    }

    @Override
    public R getPosterInfo(Long channelLiveId, Long comId, Long accountId) {
        ChannelLiveInfo channelLiveInfo = channelLiveInfoMapper.selectById(channelLiveId);
        if (ObjectUtils.isEmpty(channelLiveInfo)) {
            return R.failed("渠道信息不存在");
        }
        if (StringUtils.isEmpty(channelLiveInfo.getShortUrl())) {
            channelLiveInfo = insertShare(comId, accountId, channelLiveInfo);
        }
        GetPosterInfoVo getPosterInfoVo = new GetPosterInfoVo();
        getPosterInfoVo.setShortUrl(channelLiveInfo.getShortUrl());
        Long liveId = channelLiveInfo.getLiveId();
        LiveShare liveShare = liveShareMapper.selectOne(new LambdaQueryWrapper<LiveShare>()
                .eq(LiveShare::getLiveId, liveId).eq(LiveShare::getComId, comId));
        if(ObjectUtils.isNotEmpty(liveShare)){
            getPosterInfoVo.setChannelShareImage(liveShare.getChannelShareImage());
            getPosterInfoVo.setChannelShareDescription(liveShare.getChannelShareDescription());
            LiveDat liveDat = liveDatMapper.selectById(liveId);
            if(ObjectUtils.isNotEmpty(liveDat)){
                getPosterInfoVo.setLiveName(liveDat.getName());
                getPosterInfoVo.setStartPlayTime(liveDat.getStartPlayTime());
            }
        }
        return R.ok(getPosterInfoVo);
    }


}
