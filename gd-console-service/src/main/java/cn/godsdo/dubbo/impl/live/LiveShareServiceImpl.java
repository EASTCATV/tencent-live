package cn.godsdo.dubbo.impl.live;

import cn.godsdo.dto.live.UpdateLiveShareDto;
import cn.godsdo.dubbo.live.LiveShareService;
import cn.godsdo.entity.live.LiveShare;
import cn.godsdo.mapper.live.LiveShareMapper;
import cn.godsdo.util.R;
import cn.godsdo.util.tencent.CosHelperUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.interfaces.IdService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * <p>
 * 直播间分享配置表 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2024-07-21
 */
@DubboService
public class LiveShareServiceImpl extends ServiceImpl<LiveShareMapper, LiveShare> implements LiveShareService {

    @DubboReference
    IdService idService;
    @Resource
    @Qualifier("CosBean")
    private CosHelperUtil cosHelperUtil;
    @Override
    public R getWxShareInfo(Long comId, Long liveId) {
        LiveShare liveShare = this.baseMapper.selectOne(new LambdaQueryWrapper<LiveShare>()
                .select(LiveShare::getId, LiveShare::getWxShareDescription, LiveShare::getWxShareImage, LiveShare::getLiveId)
                .eq(LiveShare::getLiveId, liveId).eq(LiveShare::getComId, comId));
        if(ObjectUtils.isEmpty(liveShare)){
            // 插入分享设置
            liveShare = new LiveShare(idService.nextId(), comId, liveId, "", "", "", "", comId);
            this.baseMapper.insert(liveShare);
        }
        return R.ok(liveShare);
    }

    @Override
    public R updateWxShareInfo(Long comId, Long accountId, UpdateLiveShareDto dto) {
        Long liveId = dto.getLiveId();
        LiveShare liveShare = this.baseMapper.selectOne(new LambdaQueryWrapper<LiveShare>()
                .eq(LiveShare::getLiveId, liveId).eq(LiveShare::getComId, comId));
        if(ObjectUtils.isEmpty(liveShare)){
            return R.failed("修改的信息不存在");

        }
        String image = dto.getWxShareImage();
        if (ObjectUtils.isNotEmpty(image) && !image.startsWith("http")) {
            image = cosHelperUtil.uploadCover(image, comId);
            liveShare.setWxShareImage(image);
        }
        liveShare.setWxShareDescription(dto.getWxShareDescription());
        this.baseMapper.updateById(liveShare);
        return R.ok();
    }

    @Override
    public R getChannelShareInfo(Long comId, Long liveId) {
        LiveShare liveShare = this.baseMapper.selectOne(new LambdaQueryWrapper<LiveShare>()
                .select(LiveShare::getId, LiveShare::getChannelShareDescription, LiveShare::getChannelShareImage, LiveShare::getLiveId)
                .eq(LiveShare::getLiveId, liveId).eq(LiveShare::getComId, comId));
        if(ObjectUtils.isEmpty(liveShare)){
            // 插入分享设置
            liveShare = new LiveShare(idService.nextId(), comId, liveId, "", "", "", "", comId);
            this.baseMapper.insert(liveShare);
        }
        return R.ok(liveShare);
    }

    @Override
    public R updateChannelShareInfo(Long comId, Long accountId, UpdateLiveShareDto dto) {
        Long liveId = dto.getLiveId();
        LiveShare liveShare = this.baseMapper.selectOne(new LambdaQueryWrapper<LiveShare>()
                .eq(LiveShare::getLiveId, liveId).eq(LiveShare::getComId, comId));
        if(ObjectUtils.isEmpty(liveShare)){
            return R.failed("修改的信息不存在");

        }
        String image = dto.getChannelShareImage();
        if (ObjectUtils.isNotEmpty(image) && !image.startsWith("http")) {
            image = cosHelperUtil.uploadCover(image, comId);
            liveShare.setChannelShareImage(image);
        }
        liveShare.setChannelShareDescription(dto.getChannelShareDescription());
        this.baseMapper.updateById(liveShare);
        return R.ok();
    }
}
