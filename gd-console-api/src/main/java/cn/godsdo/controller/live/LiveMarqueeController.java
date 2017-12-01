package cn.godsdo.controller.live;


import cn.godsdo.dto.live.UpdateRoomMarqueeDto;
import cn.godsdo.dubbo.live.LiveMarqueeService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import cn.godsdo.util.validator.ValidatorUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 房间跑马灯表 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2018/10/12
 */
@RestController
@CrossOrigin
@RequestMapping("/liveMarquee")
public class LiveMarqueeController {

    @DubboReference
    LiveMarqueeService liveMarqueeService;

    /**
     * 获取跑马灯信息
     *
     * @param liveId
     * @return
     */
    @GetMapping("getInfo")
    public R getInfo(@RequestParam("liveId") Long liveId) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveMarqueeService.getInfo(comId,accountId, liveId);
    }

    /**
     * 修改跑马灯信息
     *
     * @return
     */
    @PostMapping("updateMarquee")
    public R updateMarquee(@RequestBody UpdateRoomMarqueeDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        //非空判断
        String message = ValidatorUtils.validateEntity(dto);
        if (StringUtils.isNotBlank(message)) {
            return R.failed(message);
        }
        return liveMarqueeService.updateMarquee(comId, accountId, dto);
    }

}
