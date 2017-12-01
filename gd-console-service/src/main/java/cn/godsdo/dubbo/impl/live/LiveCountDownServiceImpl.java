package cn.godsdo.dubbo.impl.live;

import cn.godsdo.dto.live.UpdateCountDownDto;
import cn.godsdo.dubbo.live.LiveCountDownService;
import cn.godsdo.entity.live.LiveCommodity;
import cn.godsdo.entity.live.LiveCountDown;
import cn.godsdo.mapper.live.LiveCommodityMapper;
import cn.godsdo.mapper.live.LiveCountDownMapper;
import cn.godsdo.service.cos.CosService;
import cn.godsdo.util.R;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.interfaces.IdService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * <p>
 * 房间倒计时配置表 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2018/10/19
 */
@DubboService
public class LiveCountDownServiceImpl extends ServiceImpl<LiveCountDownMapper, LiveCountDown> implements LiveCountDownService {

    @DubboReference
    IdService idService;
    @DubboReference
    CosService cosService;

    @Resource
    LiveCommodityMapper liveCommodityMapper;

    @Override
    public R getCountDown(Long liveId, Long comId, Long accountId) {
        // 查询数据库中是否存在对应的倒计时信息
        LiveCountDown lrcd = this.baseMapper.selectOne(Wrappers.<LiveCountDown>lambdaQuery().eq(LiveCountDown::getLiveId, liveId).eq(LiveCountDown::getComId, comId));
        // 如果数据库中不存在对应的倒计时信息，则创建新的倒计时信息并插入数据库
        if (ObjectUtils.isEmpty(lrcd)) {
            lrcd = new LiveCountDown(idService.nextId(), comId, liveId, 0,0, "立即下单","已售罄",  accountId);
            this.baseMapper.insert(lrcd);
        }
        // 返回操作结果和倒计时信息
        return R.ok(lrcd);
    }


    @Override
    public R updateCountDown(UpdateCountDownDto dto, Long comId, Long accountId) {
        // 查询直播倒计时信息
        LiveCountDown lrcd = this.baseMapper.selectOne(Wrappers.<LiveCountDown>lambdaQuery()
                .eq(LiveCountDown::getLiveId, dto.getLiveId()).eq(LiveCountDown::getComId, comId));
        // 如果数据库中存在对应的倒计时信息，则更新倒计时信息
        if (ObjectUtils.isNotEmpty(lrcd)) {

            // 更新直播倒计时信息
            lrcd.setCountDown(dto.getCountDown());
            lrcd.setTitle(dto.getTitle());
            lrcd.setUpdateBy(accountId);
            lrcd.setSellOut(dto.getSellOut());
            if (!lrcd.getCommodityEnable().equals(dto.getCommodityEnable())) {
                // 默认状态修改，商品的上架状态随之更改
                liveCommodityMapper.update(null, new LambdaUpdateWrapper<LiveCommodity>()
                        .set(LiveCommodity::getPutStatus, dto.getCommodityEnable())
                        .set(LiveCommodity::getUpdateBy, accountId)
                        .eq(LiveCommodity::getLiveId, dto.getLiveId()));
            }
            lrcd.setCommodityEnable(dto.getCommodityEnable());
            this.baseMapper.updateById(lrcd);
        } else {
            // 创建新的直播倒计时信息
            LiveCountDown vo = new LiveCountDown(idService.nextId(), comId, dto.getLiveId(), dto.getCountDown(),dto.getCommodityEnable(), dto.getTitle(),dto.getSellOut(),  accountId);
//            vo.setCommodityEnable(dto.getCommodityEnable());
//            vo.setSellOut(dto.getSellOut());
            this.baseMapper.insert(vo);
            // 更新商品的上架状态
            liveCommodityMapper.update(null, new LambdaUpdateWrapper<LiveCommodity>()
                    .set(LiveCommodity::getPutStatus, dto.getCommodityEnable())
                    .set(LiveCommodity::getUpdateBy, accountId)
                    .eq(LiveCommodity::getLiveId, dto.getLiveId()));
        }
        return R.ok();
    }

}
