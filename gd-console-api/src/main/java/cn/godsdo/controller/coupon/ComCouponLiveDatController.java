package cn.godsdo.controller.coupon;


import cn.godsdo.dto.coupon.BindCouponLiveDto;
import cn.godsdo.dto.coupon.GetAllCouponDto;
import cn.godsdo.dubbo.coupon.ComCouponCommodityDatService;
import cn.godsdo.dubbo.coupon.ComCouponLiveDatService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import org.springframework.stereotype.Controller;

/**
 * <p>
 * 直播间优惠卷绑定表 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2024-07-05
 */
@RestController
@RequestMapping("/comCouponLiveDat")
@CrossOrigin
public class ComCouponLiveDatController {

    @DubboReference
    private ComCouponLiveDatService comCouponLiveDatService;

    /**
     * 获取直播间未绑定的优惠券
     *
     * @return
     */
    @PostMapping("/getLiveNotBindCoupon")
    public R getLiveNotBindCoupon(@RequestBody GetAllCouponDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comCouponLiveDatService.getLiveNotBindCoupon(comId, accountId, dto);
    }

    /**
     * 获取直播间绑定的优惠券
     *
     * @return
     */
    @PostMapping("/getLiveBindCoupon")
    public R getLiveBindCoupon(@RequestBody GetAllCouponDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comCouponLiveDatService.getLiveBindCoupon(comId, accountId, dto);
    }

    /**
     * 直播间绑定优惠券
     *
     * @return
     */
    @PostMapping("/bindCouponLive")
    public R bindCouponLive(@RequestBody BindCouponLiveDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comCouponLiveDatService.bindCouponLive(comId, accountId, dto);
    }


    /**
     * 直播间解除绑定优惠券
     *
     * @return
     */
    @GetMapping("/relieveBindCouponLive")
    public R relieveBindCouponLive(@RequestParam("couponId") Long couponId, @RequestParam("liveId") Long liveId) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comCouponLiveDatService.relieveBindCouponLive(comId, accountId, couponId, liveId);
    }

}
