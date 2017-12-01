package cn.godsdo.controller.com;


import cn.godsdo.base.BasePage;
import cn.godsdo.dto.com.AddCommodityDto;
import cn.godsdo.dto.com.GetComCommodityDto;
import cn.godsdo.dubbo.live.ComCommodityService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import cn.godsdo.util.tencent.CosHelperUtil;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 商品列表 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2018/10/13
 */
@RestController
@RequestMapping("/comCommodity")
@CrossOrigin
public class ComCommodityController {

    @DubboReference
    ComCommodityService comCommodityService;

    @Resource
    @Qualifier("CosBean")
    private CosHelperUtil cosHelperUtil;

    /**
     * 获取商品（直播间未绑定）
     *
     * @param dto
     * @return
     */
    @PostMapping("getListByLiveBind")
    public R getListByLiveBind(@RequestBody GetComCommodityDto dto) {
        Long comId = ShiroUtil.getComId();
        return comCommodityService.getListByLiveBind(comId, dto);
    }
    /**
     * 获取商品（优惠卷未绑定）
     *
     * @param dto
     * @return
     */
    @PostMapping("getListByCouponBind")
    public R getListByCouponBind(@RequestBody GetComCommodityDto dto) {
        Long comId = ShiroUtil.getComId();
        return comCommodityService.getListByCouponBind(comId, dto);
    }
    /**
     * 获取所有的商品
     *
     * @param dto
     * @return
     */
    @PostMapping("getAllList")
    public R getAllList(@RequestBody GetComCommodityDto dto) {
        Long comId = ShiroUtil.getComId();
        return comCommodityService.getAllList(comId, dto);
    }

    /**
     * 添加商品
     *
     * @param dto
     * @return
     */
    @PostMapping("addCommodity")
    public R addCommodity(@RequestBody AddCommodityDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        String image = dto.getImage();
        if (ObjectUtils.isNotEmpty(image) && !image.startsWith("http")) {
            image = cosHelperUtil.uploadGiftImg(image, comId);
            dto.setImage(image);
        }
        return comCommodityService.addCommodity(comId, accountId, dto);
    }


    /**
     * 删除商品
     *
     * @param id
     * @return
     */
    @GetMapping("delCommodity")
    public R delCommodity(@RequestParam("id")Long id) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comCommodityService.delCommodity(comId, accountId, id);
    }
}
