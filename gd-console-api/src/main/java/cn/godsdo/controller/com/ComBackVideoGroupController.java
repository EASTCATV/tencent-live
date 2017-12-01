package cn.godsdo.controller.com;


import cn.godsdo.dubbo.com.ComBackVideoGroupService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 《客户回看视频分组表》 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2018-04-11
 */
@RestController
@RequestMapping("/comBackVideoGroup")
@CrossOrigin
public class ComBackVideoGroupController {


    @DubboReference
    private ComBackVideoGroupService comBackVideoGroupService;

    /**
     * 获取所有回看视频分组
     *
     * @return
     */
    @GetMapping("getBackVideoGroupList")
    private R getBackVideoGroupList() {
        Long comId = ShiroUtil.getComId();
        return comBackVideoGroupService.getBackVideoGroupList(comId);
    }

    /**
     * 获取所有回看视频分组(直播间页面装修使用)
     *
     * @return
     */
    @GetMapping("getBackVideoGroupListByFitment")
    private R getBackVideoGroupListByFitment() {
        Long comId = ShiroUtil.getComId();
        return comBackVideoGroupService.getBackVideoGroupListBySetting(comId);
    }

    /**
     * 新增回看视频分组
     *
     * @param title 分组名称
     * @return
     */
    @GetMapping("addBackVideoGroup")
    private R addBackVideoGroup(@RequestParam("title") String title) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comBackVideoGroupService.addBackVideoGroup(title, comId, accountId);
    }

    /**
     * 修改回看视频分组
     *
     * @param id    分组id
     * @param title 分组名称
     * @return
     */
    @GetMapping("updateBackVideoGroup")
    private R updateBackVideoGroup(@RequestParam("id") Long id, @RequestParam("title") String title) {
        Long accountId = ShiroUtil.getAccountId();
        return comBackVideoGroupService.updateBackVideoGroup(id, title, accountId);
    }

    /**
     * 删除回看视频分组
     *
     * @param id 分组id
     * @return
     */
    @GetMapping("deleteBackVideoGroup")
    private R deleteBackVideoGroup(@RequestParam("id") Long id) {
        Long accountId = ShiroUtil.getAccountId();
        return comBackVideoGroupService.deleteBackVideoGroup(id, accountId);
    }

}
