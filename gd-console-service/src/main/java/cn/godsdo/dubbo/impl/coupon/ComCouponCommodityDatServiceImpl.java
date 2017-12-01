package cn.godsdo.dubbo.impl.coupon;

import cn.godsdo.dto.coupon.GetAllCouponDto;
import cn.godsdo.dubbo.coupon.ComCouponCommodityDatService;
import cn.godsdo.entity.coupon.ComCouponCommodityDat;
import cn.godsdo.mapper.coupon.ComCouponCommodityDatMapper;
import cn.godsdo.util.R;
import cn.godsdo.vo.coupon.GetAllCouponVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * <p>
 * 商品优惠卷绑定表 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2024-07-05
 */
@DubboService
public class ComCouponCommodityDatServiceImpl extends ServiceImpl<ComCouponCommodityDatMapper, ComCouponCommodityDat> implements ComCouponCommodityDatService {


}
