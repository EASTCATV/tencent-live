package cn.godsdo.controller.assistant;

import cn.godsdo.dto.assistan.UpdateCommodityByAssistantDto;
import cn.godsdo.dubbo.live.LiveCommodityService;
import cn.godsdo.dubbo.live.LiveConfigService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * 助教端场控设置
 *
 * @author W~Y~H
 * @Date : 2018/05/18
 */
@RestController
@RequestMapping("assistantCommodity")
@CrossOrigin
public class AssistantCommodityController {
    @DubboReference
    LiveConfigService liveConfigService;
    @DubboReference
    LiveCommodityService liveCommodityService;



    /**
     * 获取商品列表
     * @param liveId
     * @return
     */
    @GetMapping("getCommodityListByAssistant")
    public R getCommodityListByAssistant(@RequestParam("liveId") Long liveId) {
        Long comId = ShiroUtil.getComId();
        return liveCommodityService.getCommodityListByAssistant(liveId,comId);
    }

    /**
     * 获取商品开关
     *
     * @param liveId
     * @return
     */
    @GetMapping("getCommodityEnable")
    public R getCommodityEnable(@RequestParam("liveId") Long liveId) {
        Long comId = ShiroUtil.getComId();
        return liveConfigService.getCommodityEnable(comId, liveId);
    }

    /**
     * 修改商品开关（助理后台）
     *
     * @param commodityEnable
     * @param liveId
     * @return
     */
    @GetMapping("updateCommodityEnable")
    public R updateCommodityEnable(@RequestParam("commodityEnable") Boolean commodityEnable, @RequestParam("liveId") Long liveId, @RequestParam("imGroupId") String imGroupId) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveConfigService.updateCommodityEnable(comId, accountId, commodityEnable, liveId,imGroupId);
    }

    /**
     * 助理端  商品上下架
     * @return
     */
    @PostMapping("updatePutStatusByAssistant")
    public R updatePutStatusByAssistant(@RequestBody UpdateCommodityByAssistantDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveCommodityService.updatePutStatusByAssistant(dto,comId,accountId);
    }
    /**
     * 助理端  商品推荐
     * @return
     */
    @PostMapping("updateRecommendByAssistant")
    public R updateRecommendByAssistant(@RequestBody UpdateCommodityByAssistantDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveCommodityService.updateRecommendByAssistant(dto,comId,accountId);
    }
    /**
     * 助理端  商品售罄
     * @return
     */
    @PostMapping("updateSellOutByAssistant")
    public R updateSellOutByAssistant(@RequestBody UpdateCommodityByAssistantDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveCommodityService.updateSellOutByAssistant(dto,comId,accountId);
    }



}
