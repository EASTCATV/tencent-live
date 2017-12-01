package cn.godsdo.controller.user;


import cn.godsdo.dto.user.GetUserListDto;
import cn.godsdo.dubbo.user.UserDatService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 《用户表》 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2024-04-25
 */
@RestController
@CrossOrigin
@RequestMapping("/userDat")
public class UserDatController {

    @DubboReference
    UserDatService userDatService;

    /**
     * 获取用户列表
     *
     * @param dto
     * @return
     */
    @PostMapping("getUserList")
    public R getUserList(@RequestBody GetUserListDto dto) {
        Long comId = ShiroUtil.getComId();
        return userDatService.getUserList(comId, dto);
    }

    /**
     * 获取黑名单
     *
     * @param dto
     * @return
     */
    @PostMapping("getBlockUserList")
    public R getBlockUserList(@RequestBody GetUserListDto dto) {
        Long comId = ShiroUtil.getComId();
        return userDatService.getBlockUserList(comId, dto);
    }

    /**
     * 拉黑学员
     *
     * @return
     */
    @GetMapping("blockUser")
    public R blockUser(@RequestParam("id") Long userId) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return userDatService.blockUser(comId, accountId, userId, true);
    }

    /**
     * 解除拉黑学员
     *
     * @return
     */
    @GetMapping("unblockUser")
    public R unblockUser(@RequestParam("id") Long userId) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return userDatService.blockUser(comId, accountId, userId, false);
    }
}
