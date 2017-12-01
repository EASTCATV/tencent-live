package cn.godsdo.controller.coupon;


import cn.godsdo.dto.com.UpdateWatchChatDto;
import cn.godsdo.dto.coupon.AddCouponDatDto;
import cn.godsdo.dto.coupon.GetAllCouponDto;
import cn.godsdo.dubbo.coupon.ComCouponDatService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import org.springframework.stereotype.Controller;

/**
 * <p>
 * 优惠卷列表 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2024-07-05
 */
@RestController
@RequestMapping("/comCouponDat")
@CrossOrigin
public class ComCouponDatController {
    @DubboReference
    ComCouponDatService comCouponDatService;
    /**
     * 获取优惠券列表
     *
     * @return
     */
    @PostMapping("/getAllCoupon")
    public R getAllCoupon(@RequestBody GetAllCouponDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comCouponDatService.getAllCoupon(comId, accountId, dto);
    }
    /**
     * 新增优惠卷
     *
     * @return
     */
    @PostMapping("/addCoupon")
    public R addCoupon(@RequestBody AddCouponDatDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comCouponDatService.addCoupon(comId, accountId, dto);
    }
    /**
     * 获取优惠券信息
     *
     * @return
     */
    @GetMapping("/getCouponInfo")
    public R getCouponInfo(@RequestParam("id") Long id) {
        Long comId = ShiroUtil.getComId();
        return comCouponDatService.getCouponInfo(comId,  id);
    }
    /**
     * 修改优惠卷
     *
     * @return
     */
    @PostMapping("/updateCoupon")
    public R updateCoupon(@RequestBody AddCouponDatDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comCouponDatService.updateCoupon(comId, accountId, dto);
    }

    /**
     * 删除优惠券
     *
     * @return
     */
    @GetMapping("/delCoupon")
    public R delCoupon(@RequestParam("id") Long id) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comCouponDatService.delCoupon(comId,accountId,  id);
    }
}
