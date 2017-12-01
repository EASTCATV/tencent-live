package cn.godsdo.controller;


import cn.godsdo.dubbo.PermissionDatService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 后台权限表 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2024-05-26
 */
@RestController
@RequestMapping("/permissionDat")
@CrossOrigin
public class PermissionDatController {
    @DubboReference
    private PermissionDatService permissionDatService;

    @GetMapping("/getPermissionList")
    public R getPermissionList() {
        Long comId = ShiroUtil.getComId();
        return permissionDatService.getPermissionList(comId);
    }

}
