package cn.godsdo.controller.channel;


import cn.godsdo.dto.channel.AddGroupAdminDto;
import cn.godsdo.dto.channel.GetAccountChannelListDto;
import cn.godsdo.dto.channel.GetChannelListDto;
import cn.godsdo.dto.channel.UpdateChannelDto;
import cn.godsdo.dubbo.channel.ComChannelDatService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 《渠道信息表》 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2019-04-22
 */
@RestController
@RequestMapping("/comChannelDat")
@CrossOrigin
public class ComChannelDatController {
    @DubboReference
    private ComChannelDatService comChannelDatService;

    /**
     * 添加渠道信息
     *
     * @param name
     * @param groupId
     * @param userId
     * @return
     */
    @GetMapping("/addChannel")
    public R addChannel(@RequestParam("name") String name, @RequestParam("groupId") Long groupId, @RequestParam(value="userId",required=false) Long userId) {
        Long accountId = ShiroUtil.getAccountId();
        Long comId = ShiroUtil.getComId();
        return comChannelDatService.addChannel(comId, accountId, name, groupId, userId);
    }

    /**
     * 删除渠道信息
     *
     * @param id
     * @return
     */
    @GetMapping("/deleteChannel")
    public R deleteChannel(@RequestParam("id") Long id) {
        Long accountId = ShiroUtil.getAccountId();
        Long comId = ShiroUtil.getComId();
        return comChannelDatService.deleteChannel(comId, accountId, id);
    }

    /**
     * 修改渠道信息
     *
     * @return
     */
    @PostMapping("/updateChannel")
    public R updateChannel(@RequestBody UpdateChannelDto dto) {
        Long accountId = ShiroUtil.getAccountId();
        Long comId = ShiroUtil.getComId();
        return comChannelDatService.updateChannel(comId, accountId, dto);
    }

    /**
     * 获取渠道列表
     *
     * @return
     */
    @PostMapping("/getChannelList")
    public R getChannelList(@RequestBody GetChannelListDto dto) {
        Long accountId = ShiroUtil.getAccountId();
        Long comId = ShiroUtil.getComId();
        Boolean isAdmin = ShiroUtil.getIsAdmin();
        return comChannelDatService.getChannelList(comId, accountId,isAdmin, dto);
    }

    /**
     * 多选移动渠道分组
     *
     * @return
     */
    @PostMapping("moveComChannelGroupByList")
    public R moveComChannelGroupByList(@RequestBody AddGroupAdminDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comChannelDatService.moveComChannelGroupByList(dto, comId, accountId);
    }

    /**
     * 我的渠道 -列表
     *
     * @return
     */
    @PostMapping("getAccountChannelList")
    public R getAccountChannelList(@RequestBody GetAccountChannelListDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comChannelDatService.getAccountChannelList(dto, comId, accountId);
    }

    /**
     * 我的渠道--获取渠道名称列表
     *
     * @return
     */
    @GetMapping("/getChannelListByAccount")
    public R getChannelListByAccount() {
        Long accountId = ShiroUtil.getAccountId();
        Long comId = ShiroUtil.getComId();
        return comChannelDatService.getChannelListByAccount(comId, accountId);
    }
    /**
     * 获取直播间所有未绑定的渠道
     *
     * @param groupId
     * @param liveId
     * @return
     */
    @GetMapping("/getNotBindChannel")
    public R getNotBindChannel(@RequestParam("groupId") Long groupId, @RequestParam("liveId") Long liveId) {
        Long comId = ShiroUtil.getComId();
        return comChannelDatService.getNotBindChannel(comId, groupId, liveId);
    }

    /**
     * 获取直播间所有绑定的渠道
     *
     * @param groupId
     * @param liveId
     * @return
     */
    @GetMapping("/getBindChannelByLiveId")
    public R getBindChannelByLiveId(@RequestParam("groupId") Long groupId, @RequestParam("liveId") Long liveId) {
        Long comId = ShiroUtil.getComId();
        return comChannelDatService.getBindChannelByLiveId(comId, groupId, liveId);
    }
}
