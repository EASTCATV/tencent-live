package cn.godsdo.controller.live;


import cn.godsdo.dto.live.UpdateGiftRewardDto;
import cn.godsdo.dto.live.UpdateHeatDto;
import cn.godsdo.dto.live.UpdateReplayDto;
import cn.godsdo.dubbo.live.ComGiftConfigService;
import cn.godsdo.dubbo.live.LiveConfigService;
import cn.godsdo.entity.live.LiveConfig;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 房间配置信息表 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2018/11/18
 */
@RestController
@CrossOrigin
@RequestMapping("/liveConfig")
public class LiveConfigController {


    @DubboReference
    LiveConfigService liveConfigService;
    @DubboReference
    ComGiftConfigService comGiftConfigService;

    /**
     * 修改回放设置
     *
     * @return
     */
    @PostMapping("/updateReplay")
    public R updateReplay(@RequestBody UpdateReplayDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveConfigService.updateReplay(comId, accountId, dto);
    }
    /**
     * 获取回放设置
     *
     * @return
     */
    @GetMapping("/getBackSet")
    public R getBackSet(@RequestParam("liveId") Long liveId) {
        Long comId = ShiroUtil.getComId();
        return liveConfigService.getBackSet(comId, liveId);
    }

    /**
     * 获取修改直播热度
     *
     * @return
     */
    @GetMapping("/getHeat")
    public R getHeat(@RequestParam("liveId") Long liveId) {
        Long comId = ShiroUtil.getComId();
        return liveConfigService.getHeat(comId, liveId);
    }
    /**
     * 修改直播热度
     *
     * @return
     */
    @PostMapping("/updateHeat")
    public R updateHeat(@RequestBody UpdateHeatDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveConfigService.updateHeat(comId, accountId, dto);
    }

    /**
     * 获取礼物设置
     *
     * @return
     */
    @GetMapping("/getGiftInfo")
    public R getGiftInfo(@RequestParam("liveId") Long liveId) {
        Long comId = ShiroUtil.getComId();
        return liveConfigService.getGiftInfo(comId, liveId);
    }
    /**
     * 修改礼物设置
     *
     * @return
     */
    @PostMapping("/updateGift")
    public R updateGift(@RequestBody LiveConfig liveConfig) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        if(liveConfig.getGiftGivingEnable()){
            comGiftConfigService.addDefaultGifts(comId, accountId);
        }
        return liveConfigService.updateGift(comId, accountId, liveConfig);
    }

    /**
     * 获取商品设置
     *
     * @return
     */
    @GetMapping("/getCommodityEnable")
    public R getCommodityEnable(@RequestParam("liveId") Long liveId) {
        Long comId = ShiroUtil.getComId();
        return liveConfigService.getCommodityEnable(comId, liveId);
    }
    /**
     * 修改商品开关
     *
     * @return
     */
    @GetMapping("/updateCommodityEnable")
    public R updateCommodityEnable(@RequestParam("commodityEnable") Boolean commodityEnable,@RequestParam("liveId") Long liveId)  {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveConfigService.updateCommodityEnable(comId, accountId, commodityEnable,liveId,null);
    }
    /**
     * 修改礼物打赏
     *
     * @return
     */
    @PostMapping("/updateGiftReward")
    public R updateGiftReward(@RequestBody UpdateGiftRewardDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveConfigService.updateGiftReward(comId, accountId, dto);
    }

}
