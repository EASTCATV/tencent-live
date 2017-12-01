package cn.godsdo.controller.com;


import cn.godsdo.dubbo.com.ComAccountProjectService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 《员工项目表》 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2018-04-18
 */
@RestController
@RequestMapping("/comAccountProject")
@CrossOrigin
public class ComAccountProjectController {
    @DubboReference
    ComAccountProjectService comAccountProjectService;



    /**
     * 获取项目列表
     *
     * @return
     */
    @GetMapping("/getComProject")
    public R getComProject() {
        Long comId = ShiroUtil.getComId();
        return comAccountProjectService.getComProject(comId);
    }


    /**
     * 新建项目
     *
     * @return
     */
    @GetMapping("/addComProject")
    public R addComProject(@RequestParam("title") String title, @RequestParam("parentId") Long parentId) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comAccountProjectService.addComProject(comId,accountId,title,parentId);
    }



    /**
     * 删除项目
     *
     * @return
     */
    @GetMapping("/delComProject")
    public R delComProject(@RequestParam("id") Long id) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comAccountProjectService.delComProject(comId,accountId,id);
    }

    /**
     * 修改项目
     *
     * @return
     */
    @GetMapping("/saveComProject")
    public R saveComProject(@RequestParam("title") String title,@RequestParam("id") Long id) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil .getAccountId();
        return comAccountProjectService.saveComProject(comId,accountId,title,id);
    }

}
