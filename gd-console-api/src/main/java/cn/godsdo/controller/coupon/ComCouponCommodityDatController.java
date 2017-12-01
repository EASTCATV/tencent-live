package cn.godsdo.controller.coupon;


import cn.godsdo.dto.com.GetComCommodityDto;
import cn.godsdo.dto.com.UpdateWatchChatDto;
import cn.godsdo.dto.coupon.GetAllCouponDto;
import cn.godsdo.dubbo.coupon.ComCouponCommodityDatService;
import cn.godsdo.dubbo.coupon.ComCouponDatService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import org.springframework.stereotype.Controller;

/**
 * <p>
 * 商品优惠卷绑定表 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2024-07-05
 */
@RestController
@RequestMapping("/comCouponCommodityDat")
@CrossOrigin
public class ComCouponCommodityDatController {

}
