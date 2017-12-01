package cn.godsdo.controller.camp.assistant;

import cn.godsdo.dto.assistan.GetCampUsersDto;
import cn.godsdo.dubbo.camp.TrainingCampLiveUserService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import cn.hutool.core.lang.Assert;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * @Author: CR7
 * @Date: 2019/5/5 16:54
 * @Description:
 */
@RestController
@RequestMapping("camp/assistantCamp")
@CrossOrigin
public class CampAssistantCampController {

    @DubboReference(check = false)
    private TrainingCampLiveUserService campLiveUserService;

    /**
     * 获取协助渠道id
     */
    @PostMapping("/getChannelIds")
    public R getChannelIds(@RequestBody GetCampUsersDto dto) {
        try {
            Assert.notNull(dto.getLiveId(),"直播间id不能为空");
            Assert.notNull(dto.getChannelId(),"渠道id不能为空");
        } catch (IllegalArgumentException e) {
            return R.failed(e.getMessage());
        }
        Long comId = ShiroUtil.getComId();
        dto.setComId(comId);
        return campLiveUserService.getChannelIds(dto);
    }

    /**
     * 获取签到
     */
    @PostMapping("/getSignIn")
    public R getSignIn(@RequestBody GetCampUsersDto dto) {
        try {
            Assert.notNull(dto.getLiveId(),"直播间id不能为空");
        } catch (IllegalArgumentException e) {
            return R.failed(e.getMessage());
        }
        Long comId = ShiroUtil.getComId();
        dto.setComId(comId);
        return campLiveUserService.getSignIn(dto);
    }

}
