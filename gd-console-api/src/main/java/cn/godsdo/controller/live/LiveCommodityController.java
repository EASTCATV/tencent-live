package cn.godsdo.controller.live;


import cn.godsdo.dto.live.BindRoomCommodityDto;
import cn.godsdo.dto.live.GetRoomCommodityDto;
import cn.godsdo.dubbo.live.LiveCommodityService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 直播间商品绑定表 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2018/10/19
 */
@RestController
@CrossOrigin
@RequestMapping("/liveCommodity")
public class LiveCommodityController {

    @DubboReference
    LiveCommodityService liveCommodityService;

    /**
     * 获取已绑定的直播间商品
     *
     * @param dto
     * @return
     */
    @PostMapping("getRoomCommodity")
    public R getRoomCommodity(@RequestBody GetRoomCommodityDto dto) {
        if (ObjectUtils.isEmpty(dto.getLiveId())) {
            return R.failed("直播间id不能为空");
        }
        Long comId = ShiroUtil.getComId();
        return liveCommodityService.getRoomCommodity(comId, dto);
    }

    /**
     * 绑定直播间
     *
     * @return
     */
    @PostMapping("bindRoomCommodity")
    public R bindRoomCommodity(@RequestBody BindRoomCommodityDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveCommodityService.bindRoomCommodity(comId, accountId, dto);
    }

    /**
     * 解除直播间和商品的绑定
     *
     * @return
     */
    @GetMapping("unbindRoomCommodity")
    public R unbindRoomCommodity(@RequestParam("liveId") Long liveId, @RequestParam("commodityId") Long commodityId) {
        Long accountId = ShiroUtil.getAccountId();
        return liveCommodityService.unbindRoomCommodity(accountId, liveId, commodityId);
    }

    // TODO 商品上移下移
}
