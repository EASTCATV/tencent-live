package cn.godsdo.controller.live;


import cn.godsdo.base.BasePage;
import cn.godsdo.dto.live.AddLiveDto;
import cn.godsdo.dto.live.GetLiveListDto;
import cn.godsdo.dto.live.StartIntelligentDto;
import cn.godsdo.dubbo.LiveJobService;
import cn.godsdo.dubbo.LiveXxlJobService;
import cn.godsdo.dubbo.intelligent.IntelligentLiveRecordService;
import cn.godsdo.dubbo.intelligent.IntelligentTemplateDatService;
import cn.godsdo.dubbo.live.LiveDatService;
import cn.godsdo.enums.live.LiveStatusEnum;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import cn.godsdo.util.tencent.CosHelperUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.mortbay.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * <p>
 * 房间基础信息表 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2018/11/18
 */
@RestController
@CrossOrigin
@RequestMapping("/liveDat")
public class LiveDatController {
    private static final Logger log = LoggerFactory.getLogger(LiveDatController.class);
    @DubboReference
    private LiveDatService liveDatService;

    @Resource
    @Qualifier("CosBean")
    private CosHelperUtil cosHelperUtil;
    @DubboReference
    LiveJobService liveJobService;
    @DubboReference
    IntelligentTemplateDatService intelligentTemplateDatService;

    @DubboReference(check = false)
    private LiveXxlJobService liveXxlJobService;

    @DubboReference(check = false)
    private IntelligentLiveRecordService intelligentLiveRecordService;

    /**
     * 获取房间列表
     *
     * @return
     */
    @PostMapping("/getLiveListByHome")
    public R getLiveListByHome(@RequestBody GetLiveListDto dto) {
        Long comId = ShiroUtil.getComId();
        // 只要直播中的数据
        dto.setLiveStatus(LiveStatusEnum.LIVING.getValue());
        return liveDatService.getLiveListByHome(comId, dto);
    }

    /**
     * 获取房间列表
     *
     * @return
     */
    @PostMapping("/getLiveList")
    public R getLiveList(@RequestBody GetLiveListDto dto) {
        Long comId = ShiroUtil.getComId();
        // id
        Long accountId = ShiroUtil.getAccountId();
        // 判断是否为管理员
        Boolean isAdmin = ShiroUtil.getIsAdmin();
        return liveDatService.getLiveList(comId, accountId, isAdmin, dto);
    }
    /**
     * 获取OBS开播方式
     *
     * @return
     */
    @PostMapping("/getObsUrl")
    public R getObsUrl(@RequestBody GetLiveListDto dto) {
        Long comId = ShiroUtil.getComId();
        // id
        Long accountId = ShiroUtil.getAccountId();
        // 判断是否为管理员
        Boolean isAdmin = ShiroUtil.getIsAdmin();
        return liveDatService.getObsUrl(comId, accountId, isAdmin, dto);
    }
    /**
     * 修改OBS开播状态
     * 点击开播
     * @return
     */
    @PostMapping("/setObsRecord")
    public R setObsRecord(@RequestBody GetLiveListDto dto) {
        Long comId = ShiroUtil.getComId();
        // id
        Long accountId = ShiroUtil.getAccountId();
        // 判断是否为管理员
        Boolean isAdmin = ShiroUtil.getIsAdmin();
        return liveDatService.setObsRecord(comId, accountId, isAdmin, dto);
    }
    /**
     * 获取OBS直播录制状态
     *
     * @return
     */
    @PostMapping("/getObsRecordStatus")
    public R getObsRecordStatus(@RequestBody GetLiveListDto dto) {
        Long comId = ShiroUtil.getComId();
        // id
        Long accountId = ShiroUtil.getAccountId();
        // 判断是否为管理员
        Boolean isAdmin = ShiroUtil.getIsAdmin();
        return liveDatService.getObsRecordStatus(comId, accountId, isAdmin, dto);
    }
    /**
     * 获取直播间详细信息
     *
     * @return
     */
    @GetMapping("/getLiveDat")
    public R getLiveDat(@RequestParam("liveId") Long liveId) {
        Long comId = ShiroUtil.getComId();
        return liveDatService.getLiveDat(comId, liveId);
    }

    /**
     * 获取直播间详细信息
     *
     * @return
     */
    @GetMapping("/getLiveDetail")
    public R getLiveDetail(@RequestParam("liveId") Long liveId) {
        Long comId = ShiroUtil.getComId();
        return liveDatService.getLiveDetail(comId, liveId);
    }

    /**
     * 新建直播间
     *
     * @return
     */
    @PostMapping("/add")
    public R add(@RequestBody AddLiveDto dto) {
        Long comId = ShiroUtil.getComId();
        // id
        Long accountId = ShiroUtil.getAccountId();
        String image = dto.getCoverImage();
        if (ObjectUtils.isNotEmpty(image) && !image.startsWith("http")) {
            image = cosHelperUtil.uploadGiftImg(image, comId);
            dto.setCoverImage(image);
        }
        return liveDatService.addRoom(comId, accountId, dto);
    }



    /**
     * 删除直播间
     *
     * @return
     */
    @GetMapping("/del")
    public R delRoom(@RequestParam("liveId") Long liveId) {
        Long comId = ShiroUtil.getComId();
        // id
        Long accountId = ShiroUtil.getAccountId();
        return liveDatService.delRoom(comId, accountId, liveId);
    }

    /**
     * 修改房间基本信息
     *
     * @return
     */
    @PostMapping("/updateBasicInfo")
    public R updateBasicInfo(@RequestBody AddLiveDto dto) {
        Long comId = ShiroUtil.getComId();
        // id
        Long accountId = ShiroUtil.getAccountId();
        String image = dto.getCoverImage();
        if (ObjectUtils.isNotEmpty(image) && !image.startsWith("http")) {
            image = cosHelperUtil.uploadGiftImg(image, comId);
            dto.setCoverImage(image);
        }
        return liveDatService.updateBasicInfo(comId, accountId, dto);
    }

    /**
     * 获取正在直播房间列表
     *
     * @return
     */
    @PostMapping("/getLivingRoom")
    public R getLivingRoom(@RequestBody BasePage page) {
        Long comId = ShiroUtil.getComId();
        return liveDatService.getLivingRoom(page, comId);
    }

    /**
     * 复制直播间
     *
     * @return
     */
    @GetMapping("/copyLive")
    public R copyLive(@RequestParam("id") Long id, @RequestParam("name") String name) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveDatService.copyLive(id, name, comId, accountId);
    }


    /**
     * 获取直播间观看链接
     *
     * @param liveId
     * @return
     */
    @GetMapping("/getRoomViewLink")
    public R getRoomViewLink(@RequestParam("liveId") Long liveId) {
        if (ObjectUtils.isEmpty(liveId)) {
            return R.failed("直播间id不能为空");
        }
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveDatService.getRoomViewLink(liveId, comId, accountId);
    }

    /**
     * 获取直播间观看链接
     *
     * @param liveId
     * @return
     */
    @GetMapping("/getWatchUrlByRoomId")
    public R getWatchUrlByRoomId(@RequestParam("liveId") Long liveId) {
        if (ObjectUtils.isEmpty(liveId)) {
            return R.failed("直播间id不能为空");
        }
        return liveDatService.getWatchUrlByRoomId(liveId);
    }

    /**
     * 获取首页信息
     *
     * @return
     */
    @GetMapping("/getHomeInfo")
    public R getHomeInfo() {
        Long comId = ShiroUtil.getComId();
        return liveDatService.getHomeInfo(comId);
    }

    /**
     * 获取首页实时在线人数
     * @return
     */
    @GetMapping("/getHomeOnLine")
    public R getHomeOnLine() {
        Long comId = ShiroUtil.getComId();
        return liveDatService.getHomeOnLine(comId);
    }

    /**
     * 获取首页本月累计在线人数
     * @return
     */
    @GetMapping("/getHomeMonth")
    public R getHomeMonth(@RequestParam(required = false) String month) {
        Long comId = ShiroUtil.getComId();
        return liveDatService.getHomeMonth(comId, month);
    }
    /**
     * @return
     */
    @GetMapping("test")
    public R test() {
        Long comId = ShiroUtil.getComId();
        Date newDate = DateUtil.offset(new Date(), DateField.SECOND, 20);
        liveXxlJobService.addJob("134241", "test", newDate);
//        liveXxlJobService.addJob("134241", "IntelligentLiveJob", newDate);
        log.info("qinqinqinqinqinqinqinqinqinq");
        return R.ok();
    }
    /**
     * 开始智能直播
     *
     * @return
     */
    @PostMapping("/startIntelligent")
    public R startIntelligent(@Valid  @RequestBody StartIntelligentDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        Integer liveType = dto.getLiveType();
        dto.setComId(comId);
        dto.setAccountId(accountId);
//        intelligentLiveRecordService.addIntelligentLiveRecord(dto);
        //现在开播
        if (ObjectUtils.isNotEmpty(liveType) && liveType == 0) {
            intelligentLiveRecordService.addIntelligentLiveRecord(dto);
            intelligentTemplateDatService.startIntelligent(dto);
//            liveJobService.addJob(JSONObject.toJSONString(dto), "IntelligentLiveJob");
        } else {
//            dto.setStartTime(dto.getTime());
            Log.info("=========开始智能直播=======dto:{}", dto);
            Date date = DateUtil.parse(dto.getTime());

            Log.info("=========date=======:{}", date);
//            liveJobService.addJob(JSONObject.toJSONString(dto), "IntelligentLiveJob", date);
            Object o = liveXxlJobService.addJob(JSONObject.toJSONString(dto), "IntelligentLiveJob", date);
            if(ObjectUtils.isNotEmpty(o)) {
                dto.setJobId(o.toString());
                intelligentLiveRecordService.addIntelligentLiveRecord(dto);
            }else{
                return R.failed("开播失败");
            }

        }
        return R.ok();
    }

    /**
     * 获取智能直播状态
     *
     * @return
     */
    @GetMapping("/getIntelligentStatus")
    public R getIntelligentStatus(@RequestParam("liveId") Long liveId) {
        Long comId = ShiroUtil.getComId();
        return  intelligentTemplateDatService.getIntelligentStatus(comId,liveId);
    }


    /**
     * 结束智能直播推流
     *
     * @return
     */
    @GetMapping("/closeIntelligent")
    public R closeIntelligent(@RequestParam("liveId") Long liveId,@RequestParam("templateId") Long templateId) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return  intelligentTemplateDatService.closeIntelligent(comId,accountId,liveId,templateId);
    }
}
