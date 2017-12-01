package cn.godsdo.controller.Captcha;

import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import cloud.tianai.captcha.common.response.ApiResponse;
import cloud.tianai.captcha.spring.application.ImageCaptchaApplication;
import cloud.tianai.captcha.spring.plugins.secondary.SecondaryVerificationApplication;
import cloud.tianai.captcha.spring.vo.CaptchaResponse;
import cloud.tianai.captcha.spring.vo.ImageCaptchaVO;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import cn.godsdo.dubbo.SmsSendUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

/**
 * @Author : yang
 * @Date : 2018/3/24
 * @ApiNote : 图形验证码
 */
@RestController
@Slf4j
@RequestMapping("/Captcha")
@CrossOrigin
public class YCaptcha {

    @Resource
    private ImageCaptchaApplication application;
    @Resource
    private ImageCaptchaApplication sca;

    @DubboReference(check = false)
    private SmsSendUtil smsSendUtil;
    //生成验证码
    @PostMapping("/generate")
    public CaptchaResponse<ImageCaptchaVO> generate() {
        // 1.生成滑块验证码(该数据返回给前端用于展示验证码数据)
        CaptchaResponse<ImageCaptchaVO> res1 = application.generateCaptcha(CaptchaTypeConstant.SLIDER);
        return res1;
    }

    //校验验证码
//    @PostMapping("/verify")
//    public ApiResponse<?> verify(CaptchaResponse<ImageCaptchaVO> x) {
//        // 2.校验验证码
//        ImageCaptchaTrack sliderCaptchaTrack = new ImageCaptchaTrack();
//        ApiResponse<?> match = application.matching(x.getId(), sliderCaptchaTrack);
//        return match;
////        if (match.isSuccess()) {
////            return R.ok("验证通过");
////        } else {
////            return R.failed("验证失败");
////        }
//    }

    //@AccessRequired
    @PostMapping("/verify")
    @ResponseBody
    public ApiResponse<?> checkCaptcha(@RequestBody Data x,
                                       HttpServletRequest request) {
        String id = x.getId();
        ApiResponse<?> response = application.matching(id, x.getData());
        if (response.isSuccess()) {
            smsSendUtil.setRedisValueByTime(id,"true",10L);

            return ApiResponse.ofSuccess(Collections.singletonMap("id", id));
        }
        return response;
    }

    @lombok.Data
    public static class Data {
        private String  id;
        private ImageCaptchaTrack data;
        private String phone;
    }



    /**
     * 二次验证，一般用于机器内部调用，这里为了方便测试
     *
     * @param id id
     * @return boolean
     */
    @GetMapping("/check2")
    @ResponseBody
    public boolean check2Captcha(@RequestParam("id") String id) {
        // 如果开启了二次验证
        if (sca instanceof SecondaryVerificationApplication) {
            return ((SecondaryVerificationApplication) sca).secondaryVerification(id);
        }
        return false;
    }

}
