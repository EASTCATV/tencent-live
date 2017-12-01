package cn.godsdo.controller.assistant;

import cn.godsdo.dto.assistan.UpdatePutStatusByCouponDto;
import cn.godsdo.dto.coupon.GetAllCouponDto;
import cn.godsdo.dubbo.coupon.ComCouponLiveDatService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * @author W~Y~H
 * @Date : 2018/05/18
 */
@RestController
@RequestMapping("assistantCoupon")
@CrossOrigin
public class AssistantCouponController {
    @DubboReference
    private ComCouponLiveDatService comCouponLiveDatService;

    /**
     * 获取直播间绑定的优惠券
     * @return
     */
    @PostMapping("/getLiveBindCoupon")
    public R getLiveBindCoupon(@RequestBody GetAllCouponDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comCouponLiveDatService.getLiveBindCoupon(comId, accountId, dto);
    }


    /**
     * 助理端  优惠券上下架
     * @return
     */
    @PostMapping("updatePutStatusByCoupon")
    public R updatePutStatusByCoupon(@RequestBody UpdatePutStatusByCouponDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comCouponLiveDatService.updatePutStatusByCoupon(comId,accountId,dto);
    }

    /**
     * 助理端  优惠券推荐
     * @return
     */
    @PostMapping("updateRecommendByCoupon")
    public R updateRecommendByCoupon(@RequestBody UpdatePutStatusByCouponDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comCouponLiveDatService.updateRecommendByCoupon(comId,accountId,dto);
    }
}
