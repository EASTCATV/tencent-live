package cn.godsdo.controller.channel;


import cn.godsdo.dto.channel.AddGroupAdminDto;
import cn.godsdo.dto.channel.GetChannelOnLineDto;
import cn.godsdo.dubbo.channel.ChannelGroupAdminService;
import cn.godsdo.dubbo.channel.ComChannelGroupService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 《渠道分组表》 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2019-04-22
 */
@RestController
@RequestMapping("/comChannelGroup")
@CrossOrigin
public class ComChannelGroupController {
    @DubboReference
    ComChannelGroupService comChannelGroupService;
    @DubboReference
    ChannelGroupAdminService channelGroupAdminService;


    /**
     * 获取所有渠道分组
     *
     * @return
     */
    @GetMapping("getComChannelGroup")
    public R getComChannelGroup() {
        Long comId = ShiroUtil.getComId();
        Boolean isAdmin = ShiroUtil.getIsAdmin();
        Long accountId = ShiroUtil.getAccountId();
        return comChannelGroupService.getAllGroups(comId, accountId, isAdmin);
    }
    /**
     * 获取所有渠道分组
     *
     * @return
     */
    @GetMapping("getComChannelGroupByRoomDistribution")
    public R getComChannelGroupByRoomDistribution() {
        Long comId = ShiroUtil.getComId();
        return comChannelGroupService.getComChannelGroupByRoomDistribution(comId);
    }
    /**
     * 获取所有渠道分组
     *
     * @return
     */
    @GetMapping("getComChannelGroupByAddAccount")
    public R getComChannelGroupByAddAccount() {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comChannelGroupService.getAllGroups(comId, accountId, true);
    }
    /**
     * 获取所有渠道分组
     *
     * @return
     */
    @GetMapping("getComChannelGroupAndChannel")
    public R getComChannelGroupAndChannel(@RequestParam("liveId") Long liveId,@RequestParam("date") String date) {
        Long comId = ShiroUtil.getComId();
        Boolean isAdmin = ShiroUtil.getIsAdmin();
        Long accountId = ShiroUtil.getAccountId();
        return comChannelGroupService.getComChannelGroupAndChannel(liveId,date,comId, accountId, isAdmin);
    }
    /**
     * 新增渠道分组
     *
     * @param name
     * @param parentId
     * @return
     */
    @GetMapping("addGroup")
    public R addGroup(@RequestParam("name") String name, @RequestParam("parentId") Long parentId) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comChannelGroupService.addGroup(comId, accountId, name, parentId);
    }

    /**
     * 修改渠道分组
     *
     * @param name
     * @return
     */
    @GetMapping("updateGroup")
    public R updateGroup(@RequestParam("name") String name, @RequestParam("id") Long id) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comChannelGroupService.updateGroup(id, name, accountId, comId);
    }


    /**
     * 删除渠道分组
     */
    @GetMapping("delGroup")
    public R delGroup(@RequestParam("id") Long id) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comChannelGroupService.deleteGroup(id, accountId, comId);
    }


    /**
     * 获取渠道分组的管理员列表
     *
     * @return
     */
    @GetMapping("/getGroupAdminList")
    public R getGroupAdminList(@RequestParam("groupId") Long groupId) {
        Long comId = ShiroUtil.getComId();
        return channelGroupAdminService.getGroupAdminList(groupId, comId);
    }

    /**
     * 新增渠道分组的管理员
     *
     * @return
     */
    @PostMapping("/addGroupAdmin")
    public R addGroupAdmin(@RequestBody AddGroupAdminDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return channelGroupAdminService.addGroupAdmin(dto, comId, accountId);
    }

    /**
     * 删除渠道分组的管理员
     *
     * @return
     */
    @GetMapping("/delGroupAdmin")
    public R delGroupAdmin(@RequestParam("id") Long id) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return channelGroupAdminService.delGroupAdmin(id, comId, accountId);
    }


    /**
     * 获取渠道实时在线人数
     * @return
     */
    @PostMapping("/getChannelOnLine")
    public R getChannelOnLine(@RequestBody GetChannelOnLineDto dto) {
        Long comId = ShiroUtil.getComId();
        return comChannelGroupService.getChannelOnLine(comId,dto);
    }

}
