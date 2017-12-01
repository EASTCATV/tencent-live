package cn.godsdo.controller.com;


import cn.godsdo.dto.com.GetComVideoShareUrlDto;
import cn.godsdo.dubbo.com.VideoShareRecordService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import org.springframework.stereotype.Controller;

/**
 * <p>
 * 视频分享记录表 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2024-06-29
 */
@RestController
@RequestMapping("/videoShareRecord")
@CrossOrigin
public class VideoShareRecordController {
    @DubboReference
    VideoShareRecordService videoShareRecordService;

    /**
     * 分享
     * @param dto
     * @return
     */
    @PostMapping("/getComVideoShareUrl")
    public R getComVideoShareUrl(@RequestBody GetComVideoShareUrlDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return videoShareRecordService.getComVideoShareUrl(accountId, comId, dto);
    }
}
