package cn.godsdo.controller.com;


import cn.godsdo.dubbo.live.ComMarqueeService;
import cn.godsdo.entity.com.ComMarquee;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 全局跑马灯表 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2018-04-10
 */
@RestController
@RequestMapping("/comMarquee")
@CrossOrigin
public class ComMarqueeController {

    @DubboReference
    private ComMarqueeService comMarqueeService;

    /**
     * 获取跑马灯数据
     * @return
     */
    @GetMapping("/getComMarquee")
    public R getComMarquee() {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comMarqueeService.getComMarquee(comId, accountId);
    }
    /**
     * 修改登录背景
     * @return
     */
    @PostMapping("/saveLoginImage")
    public R saveLoginImage(@RequestBody ComMarquee comMarquee) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comMarqueeService.saveLoginImage(comId, accountId,comMarquee);
    }


    /**
     * 修改跑马灯数据
     * @return
     */
    @PostMapping("/updateComMarquee")
    public R updateComMarquee(@RequestBody ComMarquee comMarquee) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comMarqueeService.updateComMarquee(comId, accountId,comMarquee);
    }

    /**
     * 修改声明
     * @return
     */
    @PostMapping("/updateComStatement")
    public R updateComStatement(@RequestBody ComMarquee comMarquee) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comMarqueeService.updateComStatement(comId, accountId,comMarquee);
    }
}
