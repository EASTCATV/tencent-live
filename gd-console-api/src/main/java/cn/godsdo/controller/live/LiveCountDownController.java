package cn.godsdo.controller.live;


import cn.godsdo.dto.live.UpdateCountDownDto;
import cn.godsdo.dubbo.live.LiveCountDownService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import cn.godsdo.util.tencent.CosHelperUtil;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 房间倒计时配置表 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2018/10/19
 */
@RestController
@CrossOrigin
@RequestMapping("/liveCountDown")
public class LiveCountDownController {
    @DubboReference
    LiveCountDownService liveCountDownService;

    @Resource
    @Qualifier("CosBean")
    private CosHelperUtil cosHelperUtil;

    /**
     * 获取推荐商品倒计时
     *
     * @return
     */
    @GetMapping("/getCountDown")
    public R getCountDown(@RequestParam("liveId") Long liveId) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveCountDownService.getCountDown(liveId, comId, accountId);
    }

    /**
     * 修改推荐商品倒计时
     *
     * @return
     */
    @PostMapping("/updateCountDown")
    public R updateCountDown(@RequestBody UpdateCountDownDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveCountDownService.updateCountDown(dto, comId, accountId);
    }

}
