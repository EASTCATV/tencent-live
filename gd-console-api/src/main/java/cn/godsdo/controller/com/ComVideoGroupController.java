package cn.godsdo.controller.com;


import cn.godsdo.dubbo.com.ComVideoGroupService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 《客户视频素材分组表》 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2018-04-11
 */
@RestController
@RequestMapping("/comVideoGroup")
@CrossOrigin
public class ComVideoGroupController {
    @DubboReference
    private ComVideoGroupService comVideoGroupService;

    /**
     * 获取媒体库所有视频分组
     *
     * @return
     */
    @GetMapping("getComVideoGroupList")
    private R getComVideoGroupList() {
        Long comId = ShiroUtil.getComId();
        return comVideoGroupService.getComVideoGroupList(comId);
    }


    /**
     * 获取媒体库所有视频分组(直播间页面装修使用)
     *
     * @return
     */
    @GetMapping("getComVideoGroupListByFitment")
    private R getComVideoGroupListByFitment() {
        Long comId = ShiroUtil.getComId();
        return comVideoGroupService.getComVideoGroupListBySetting(comId);
    }

    /**
     * 新增媒体库视频分组
     *
     * @param title 分组名称
     * @return
     */
    @GetMapping("addComVideoGroup")
    private R addComVideoGroup(@RequestParam("title") String title) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comVideoGroupService.addComVideoGroup(title, comId, accountId);
    }

    /**
     * 修改媒体库视频分组
     *
     * @param id    分组id
     * @param title 分组名称
     * @return
     */
    @GetMapping("updateComVideoGroup")
    private R updateComVideoGroup(@RequestParam("id") Long id, @RequestParam("title") String title) {
        Long accountId = ShiroUtil.getAccountId();
        return comVideoGroupService.updateComVideoGroup(id, title, accountId);
    }

    /**
     * 删除媒体库视频分组
     *
     * @param id 分组id
     * @return
     */
    @GetMapping("deleteComVideoGroup")
    private R deleteComVideoGroup(@RequestParam("id") Long id) {
        Long accountId = ShiroUtil.getAccountId();
        return comVideoGroupService.deleteComVideoGroup(id, accountId);
    }
}
