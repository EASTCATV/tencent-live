package cn.godsdo.controller.com;


import cn.godsdo.base.BasePage;
import cn.godsdo.dubbo.live.ComCommodityGroupService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 商品分组表（默认分组为0） 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2018/10/13
 */
@RestController
@CrossOrigin
@RequestMapping("/comCommodityGroup")
public class ComCommodityGroupController {

    @DubboReference
    ComCommodityGroupService comCommodityGroupService;

    @GetMapping("getAllGroupByCommodity")
    private R getAllGroupByCommodity() {
        Long comId = ShiroUtil.getComId();
        return comCommodityGroupService.getAllGroupByCommodity(comId);
    }

    /**
     * 获取所有商品分组
     *
     * @return
     */
    @PostMapping("getAllGroup")
    private R getAllGroup(@RequestBody BasePage dto) {
        Long comId = ShiroUtil.getComId();
        return comCommodityGroupService.getAllGroup(comId, dto);
    }
    /**
     * 新建商品分组
     *
     * @return
     */
    @GetMapping("addGroup")
    private R addGroup(@RequestParam("groupName") String groupName) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comCommodityGroupService.addGroup(comId,accountId, groupName);
    }

    /**
     * 修改商品分组名称
     *
     * @return
     */
    @GetMapping("updateGroup")
    private R updateGroup(@RequestParam("groupId") Long groupId, @RequestParam("groupName") String groupName) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comCommodityGroupService.updateGroup(comId, accountId, groupId, groupName);
    }

    /**
     * 删除商品分组
     *
     * @return
     */
    @GetMapping("delGroup")
    private R delGroup(@RequestParam("groupId") Long groupId) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comCommodityGroupService.delGroup(comId, accountId, groupId);
    }
}
