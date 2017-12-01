package cn.godsdo.controller.com;


import cn.godsdo.base.BasePage;
import cn.godsdo.dto.com.UpdateBotListDto;
import cn.godsdo.dubbo.com.ComBotService;
import cn.godsdo.entity.com.ComBot;
import cn.godsdo.util.ShiroUtil;
import cn.godsdo.util.R;
import cn.godsdo.util.tencent.CosHelperUtil;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 用户机器人列表 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2018-04-11
 */
@RestController
@RequestMapping("/comBot")
@CrossOrigin
public class ComBotController {
    @DubboReference
    private ComBotService comBotService;

    @Resource
    @Qualifier("CosBean")
    private CosHelperUtil cosHelperUtil;


    /**
     * 获取所有的机器人
     *
     * @param dto
     * @return
     */
    @PostMapping("/getAllBot")
    public R getAllBot(@RequestBody BasePage dto) {
        Long accountId = ShiroUtil.getAccountId();
        Long comId = ShiroUtil.getComId();
        return comBotService.getAllBot(dto, accountId, comId);
    }

    /**
     * 获取随机机器人
     *
     * @return
     */
    @GetMapping("/getDefaultBot")
    public R getDefaultBot(@RequestParam("limit") int limit) {
        Long comId = ShiroUtil.getComId();
        return comBotService.getDefaultBot(limit, comId);
    }


    /**
     * 修改新增机器人
     *
     * @return
     */
    @PostMapping("/saveBot")
    public R saveBot(@RequestBody ComBot vo) {
        Long accountId = ShiroUtil.getAccountId();
        Long comId = ShiroUtil.getComId();
        String image = vo.getHeadUrl();
        if (ObjectUtils.isNotEmpty(image) && !image.startsWith("http")) {
            image = cosHelperUtil.uploadCover(image, comId);
            vo.setHeadUrl(image);
        }
        return comBotService.saveBot(vo, accountId, comId);
    }

    /**
     * 修改新增机器人
     *
     * @return
     */
    @PostMapping("/updateBotList")
    public R updateBotList(@RequestBody UpdateBotListDto vo) {
        Long accountId = ShiroUtil.getAccountId();
        Long comId = ShiroUtil.getComId();
        return comBotService.updateBotList(vo, accountId, comId);
    }
}
