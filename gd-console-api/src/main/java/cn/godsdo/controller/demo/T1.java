package cn.godsdo.controller.demo;

import cn.godsdo.demo.DemoService;
import cn.godsdo.dto.com.ComGiftConfigDto;
import cn.godsdo.dubbo.LiveJobService;
import cn.godsdo.dubbo.SmsSendUtil;
import cn.godsdo.dubbo.com.BlackService;
import cn.godsdo.dubbo.com.ComBotService;
import cn.godsdo.dubbo.crossService.RedisDubboService;
import cn.godsdo.entity.com.ComBot;
import cn.godsdo.util.R;
import cn.godsdo.util.tencent.CosHelperUtil;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author : yang
 * @Date : 2024/3/26
 * @ApiNote :
 */
@RestController
@RequestMapping("t/")
@CrossOrigin
@RefreshScope
public class T1 {
//    @Value("${aa}")
    private String aa;
    @Resource
    @Qualifier("CosBean")
    private CosHelperUtil cosHelperUtil;
    @DubboReference
    DemoService demoService;
    @DubboReference
    SmsSendUtil smsSendUtil;
    @DubboReference(check = false)
    LiveJobService liveJobService;

    @DubboReference
    private BlackService blackService;
    @DubboReference
    private RedisDubboService redisDubboService;
    @DubboReference
    private ComBotService comBotService;

    @GetMapping("t0")
    @ResponseBody
    public R t01() {

        smsSendUtil.setRedisValueByPhone("asdsad","1");
        Object redisValueByPhone = smsSendUtil.getRedisValueByPhone("asdsad");
        return R.ok(redisValueByPhone);

    }

    @GetMapping("t01")
    @ResponseBody
    public R t0() {
        List<ComBot> comBots = comBotService.getComBots(1623927763763239L, 300);

//        EmojiUtil.toAlias(byId.getNickname())

        return R.ok(comBots,comBots.size());

//        return R.ok();

    }
    @GetMapping("t1")
    @ResponseBody
    public R t1() {
        // {count=1, data=[ {id=1, name = "张三"}, {id=2, name = "李四"}]}
        String a = "{count=1, data=[ {id=1, name = \"张三\"}, {id=2, name = \"李四\"}]}";
        //Result sadasdasd = new Result("sadasdasd");
        return R.EOK(a);
    }

    @GetMapping("t2")
    @ResponseBody
    public R t2() {

        String asdsad = demoService.sayHello("asdsad");
        return R.ok(asdsad);

    }

    @GetMapping("t3")
    @ResponseBody
    public R t3() {
        return R.ok(aa);
        //return R.EOK(a);


        //return Result.SUCCESS(a);
    }

    @GetMapping("t4")
    @ResponseBody
    public R t4(@RequestParam String a) {
        demoService.t1(a);
        return R.EOK(a);
    }
    @PostMapping("t5")
    @ResponseBody
    public R t5(@RequestBody @Validated ComGiftConfigDto dto) {
        String coverUrl = cosHelperUtil.uploadCover(dto.getGiftName(), 11321321321321L);
        System.out.println(coverUrl);
        //demoService.t1(a);
        //return R.EOK("12121");
        //return R.ok(coverUrl);
        return R.ok(coverUrl);
    }
    @GetMapping("t6")
    @ResponseBody
    public R t6() {
//                JSONObject param = new JSONObject();
////        // 获取文件扩展名
//        param.put("videoId", 1L);
//        param.put("teime", new Date());
//        liveJobService.addJob(param.toString(), "AdjustLivingJob");
        return R.ok();
    }


}
