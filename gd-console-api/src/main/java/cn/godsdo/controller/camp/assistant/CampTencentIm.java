package cn.godsdo.controller.camp.assistant;

import cn.godsdo.dto.QueryLiveUserWatchDto;
import cn.godsdo.dto.assistan.MsgWithDrawDto;
import cn.godsdo.dto.im.AssIstantOperateDto;
import cn.godsdo.dto.msg.LiveAuditMessage;
import cn.godsdo.dto.msg.LiveMessage;
import cn.godsdo.dubbo.AssIstantImOperateService;
import cn.godsdo.dubbo.crossService.LiveMsgDubboService;
import cn.godsdo.dubbo.crossService.RedisDubboService;
import cn.godsdo.dubbo.live.LiveMsgService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import cn.hutool.core.lang.Assert;
import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @Author : yang
 * @Date : 2024/5/28
 * @ApiNote :
 */


@RestController
@CrossOrigin
@RequestMapping("camp/TencentIm")
public class CampTencentIm {

    @DubboReference(check = false)
    AssIstantImOperateService assIstantImOperateService;

    @DubboReference(check = false)
    LiveMsgService liveMsgService;

    @DubboReference(check = false)
    LiveMsgDubboService liveMsgDubboService;

    @DubboReference(check = false)
    RedisDubboService redisDubboService;

    /**
     * 设置管理员
     */
//    public void setAdmin() {
//
//    }

    /**
     * 1,禁言
     */
    @PostMapping("/prohibition")
    public R ban(@RequestBody AssIstantOperateDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return assIstantImOperateService.prohibition(dto, comId, accountId);

    }
//    /**
//     * 2,解禁
//     */
//    @PostMapping("/unBan")
//    public void unBan() {
//
//    }

    /**
     * 3,禁言列表
     */
    @GetMapping("/prohibitionList")
    public R banList(@RequestParam("liveId") Long liveId) {
        return assIstantImOperateService.getProhibitionList(liveId);
    }

    /**
     * 4,拉黑----发im----记录拉黑列表
     */
    @PostMapping("/black")
    public R black(@RequestBody AssIstantOperateDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return assIstantImOperateService.black(dto, comId, accountId);
    }

    /**
     * 5,拉黑列表
     */
    @GetMapping("/blackList")
    public R blackList(@RequestParam("liveId") Long liveId) {
        return assIstantImOperateService.getBlackList(liveId);
    }
//    /**
//     * 6,解除拉黑
//     */
//    @PostMapping("/unBlack")
//    public void unBlack() {
//        Long comId = ShiroUtil.getComId();
//        Long accountId = ShiroUtil.getAccountId();
//        return assIstantImOperateService.black(dto,comId,accountId);
//    }

    /**
     * 助理端 全体禁言
     */
    @GetMapping("/bannedAll")
    public R bannedAll(@RequestParam("liveId") Long liveId, @RequestParam("imGroupId") String imGroupId, @RequestParam("bannedAll") Boolean bannedAll) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return assIstantImOperateService.bannedAll(comId, accountId, liveId, imGroupId, bannedAll);
    }

    /**
     * 助理端 消息撤回
     */
    @PostMapping("/withdraw")
    public R withdraw(@RequestBody MsgWithDrawDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return assIstantImOperateService.withdraw(comId, accountId, dto);
    }

    /**
     * 直播间消息列表
     */
    @PostMapping("/queryMessage")
    public R queryMessage(@RequestBody QueryLiveUserWatchDto query) {
        try {
            Assert.notNull(query.getQueryDate(),"查询时间不能为空");
            Assert.notNull(query.getLiveId(),"直播间id不能为空");
        } catch (IllegalArgumentException e) {
            return R.failed(e.getMessage());
        }
        return liveMsgService.queryMessage(query);
    }

    /**
     * 直播间消息下载
     */
    @GetMapping("/downMessages")
    public void downMessages(HttpServletResponse response, @RequestParam("liveId") Long liveId, @RequestParam("queryDate") String queryDate,
                             @RequestParam(required = false) String nickName) throws IOException {
        // 这里注意 有同学反应使用swagger 会导致各种问题，请直接用浏览器或者用postman
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        // 这里URLEncoder.encode可以防止中文乱码 当然和easyexcel没有关系
        String fileName = URLEncoder.encode("用户消息列表", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        QueryLiveUserWatchDto query = new QueryLiveUserWatchDto();
        query.setLiveId(liveId);
        query.setQueryDate(queryDate);
        query.setNickName(nickName);

        List<LiveMessage> liveMessageList = liveMsgService.queryMessageAll(query);

        EasyExcel.write(response.getOutputStream(), LiveMessage.class).sheet("Sheet1").doWrite(liveMessageList);
    }

    @GetMapping("/getHistoryMessage")
    public R getHistoryMessage(@RequestParam("liveId") Long liveId) {
        String historyMessage = (String) redisDubboService.get("HistoryMessage_" + liveId);
        if (historyMessage == null) {
            List<LiveMessage> liveMessageList = liveMsgDubboService.getHistoryMessage(liveId);
            redisDubboService.setex("HistoryMessage_" + liveId, JSON.toJSONString(liveMessageList), 30);
            return R.ok(liveMessageList,liveMessageList.size());
        } else {
            List<LiveMessage> recordDat = JSON.parseArray(historyMessage, LiveMessage.class);
            return R.ok(recordDat,recordDat.size());
        }
    }

    /**
     * 审核未通过消息列表
     */
    @PostMapping("/queryNotPassMessage")
    public R queryNotPassMessage(@RequestBody QueryLiveUserWatchDto query) {
        try {
            Assert.notNull(query.getQueryDate(),"查询时间不能为空");
            Assert.notNull(query.getLiveId(),"直播间id不能为空");
        } catch (IllegalArgumentException e) {
            return R.failed(e.getMessage());
        }
        List<LiveAuditMessage> liveAuditMessageList = liveMsgService.queryNotPassMessage(query);
        return R.ok(liveAuditMessageList,liveAuditMessageList.size());
    }

    /**
     * 审核未通过消息下载
     */
    @GetMapping("/downNotPassMessage")
    public void downNotPassMessage(HttpServletResponse response, @RequestParam("liveId") Long liveId,
                                   @RequestParam("queryDate") String queryDate,@RequestParam(required = false) String nickName) throws IOException {
        // 这里注意 有同学反应使用swagger 会导致各种问题，请直接用浏览器或者用postman
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        // 这里URLEncoder.encode可以防止中文乱码 当然和easyexcel没有关系
        String fileName = URLEncoder.encode("用户消息列表", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        QueryLiveUserWatchDto query = new QueryLiveUserWatchDto();
        query.setLiveId(liveId);
        query.setQueryDate(queryDate);
        query.setNickName(nickName);
        List<LiveAuditMessage> liveAuditMessageList = liveMsgService.queryNotPassMessage(query);

        EasyExcel.write(response.getOutputStream(), LiveAuditMessage.class).sheet("Sheet1").doWrite(liveAuditMessageList);
    }


//    /**
//     * json转数组
//     */
//    public void arrayObjectAndJson() {
//        String jsonStringArray = "[{\"age\":3,\"birthdate\":1496738822842,\"name\":\"校长\",\"old\":true,\"salary\":123456789.0123},{\"age\":5," +
//                "\"birthdate\":1496738822842,\"name\":\"学生\",\"old\":true,\"salary\":123456789.0123}]";
//        /*json转数组*/
//        List<User> userList = JSON.parseArray(jsonStringArray, User.class);
//        System.out.println(userList.size());    // 输出 2
//        System.out.println(userList);//[User{name='校长', age=3, salary=123456789.0123}, User{name='学生', age=5, salary=123456789.0123}]
//        //数组转json
//        String s = JSON.toJSONString(userList);
//        System.out.println("s = " + s); //[{"age":3,"name":"校长","salary":123456789.0123},{"age":5,"name":"学生","salary":123456789.0123}]
//    }

}
