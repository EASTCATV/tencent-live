package cn.godsdo.controller.com;


import cn.godsdo.base.BasePage;
import cn.godsdo.dto.com.ComGiftConfigDto;
import cn.godsdo.dubbo.live.ComGiftConfigService;
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
 * 用户礼物配置表 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2018/10/13
 */
@RestController
@CrossOrigin
@RequestMapping("/comGiftConfigController")
public class ComGiftConfigController {

    @DubboReference
    ComGiftConfigService comGiftConfigService;
    @Resource
    @Qualifier("CosBean")
    private CosHelperUtil cosHelperUtil;

    /**
     * 获取礼物列表
     *
     * @param basePage 分页信息
     * @return R对象
     */
    @PostMapping("/getGiftList")
    public R getGiftList(@RequestBody BasePage basePage) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comGiftConfigService.getGiftList(comId, accountId, basePage);
    }

    /**
     * 添加礼物
     *
     * @param dto 礼物配置数据传输对象
     * @return 返回处理结果
     */
    @PostMapping("/addGift")
    public R addGift(@RequestBody ComGiftConfigDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        Long giftSum = comGiftConfigService.getGiftSum(comId);
        if (giftSum >= 20) {
            return R.failed("最多可以添加20个礼物！！");
        }
        String image = dto.getGiftPicUrl();
        if (ObjectUtils.isNotEmpty(image) && !image.startsWith("http")) {
            image = cosHelperUtil.uploadGiftImg(image, comId);
            dto.setGiftPicUrl(image);
        }

        return comGiftConfigService.addGift(comId, accountId, dto);
    }

    /**
     * 删除礼物
     */
    @GetMapping("/delGift")
    public R delGift(@RequestParam("id") Long id) {
        // 获取公司ID
        Long comId = ShiroUtil.getComId();
        // 获取账户ID
        Long accountId = ShiroUtil.getAccountId();
        // 调用服务层方法删除礼品
        return comGiftConfigService.delGift(comId, accountId, id);
    }

    /**
     * 修改礼物
     */
    @PostMapping("/updateGift")
    public R updateGift(@RequestBody ComGiftConfigDto dto) {
        // 获取公司ID
        Long comId = ShiroUtil.getComId();
        // 获取账户ID
        Long accountId = ShiroUtil.getAccountId();
        String image = dto.getGiftPicUrl();
        if (ObjectUtils.isNotEmpty(image) && !image.startsWith("http")) {
            image = cosHelperUtil.uploadGiftImg(image, comId);
            dto.setGiftPicUrl(image);
        }
        // 调用服务层方法删除礼品
        return comGiftConfigService.updateGift(comId, accountId, dto);
    }

}
