package cn.godsdo.controller.channel;


import cn.godsdo.dto.channel.BindChannelByRoomIdDto;
import cn.godsdo.dto.channel.BindRoomByChannelIdDto;
import cn.godsdo.dto.channel.GetChannelLiveDataDto;
import cn.godsdo.dubbo.channel.ChannelLiveInfoService;
import cn.godsdo.dubbo.channel.ChannelShareDatService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 《渠道直播间绑定表》 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2024-04-24
 */
@RestController
@RequestMapping("/channelLiveInfo")
@CrossOrigin
public class ChannelLiveInfoController {

    @DubboReference
    private ChannelLiveInfoService channelLiveInfoService;
    @DubboReference
    private ChannelShareDatService channelShareDatService;


    /**
     * 获取已绑定该渠道的直播间列表
     * @param channelId
     * @return
     */
    //@Role(value = "admin")
    @GetMapping("/getBindRoomByChannelId")
    public R getRoomInfoByChannelId(@RequestParam("channelId") Long channelId) {
        Long comId = ShiroUtil.getComId();
        return channelLiveInfoService.getBindRoomByChannelId(channelId, comId);
    }

    /**
     * 获取渠道未绑定的直播间                                                                                                                                                                              定该渠道的直播间列表
     * @param channelId
     * @return
     */
    @GetMapping("/getNotBindRoomByChannelId")
    public R getNotBindRoomByChannelId(@RequestParam("channelId") Long channelId) {
        Long comId = ShiroUtil.getComId();
        return channelLiveInfoService.getNotBindRoomByChannelId(channelId, comId);
    }

    /**
     * 绑定渠道直播间
     * @param dto
     * @return
     */
    @PostMapping("/bindRoomByChannelId")
    public R bindRoomByChannelId(@RequestBody BindRoomByChannelIdDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return channelLiveInfoService.bindRoomByChannelId(dto, comId, accountId);
    }


    // 获取员工渠道列表的时候把长连接进行生成（观看链接外加channelId）
    // 重新生成短连接的时候 传入长连接，以及直播间和渠道绑定的id,
    // 重新生成的话，需要删除之前存在redis中的长连接（key为短连接）,新增删除记录
    // 重新生成短连接，存入数据库，在mongdb中存入新增记录，存入redis
//    @GetMapping("getChannelShortUrl")
//    public R getChannelShortUrl(@RequestParam("channelLiveId") Long channelLiveId,@RequestParam("url") String url) {
//        Long accountId = ShiroUtil.getAccountId();
//        Long comId = ShiroUtil.getComId();
//        return channelLiveInfoService.getChannelShortUrl(comId, accountId,channelLiveId,url);
//    }
    @GetMapping("getChannelUrl")
    public R getChannelShortUrl(@RequestParam("channelLiveId") Long channelLiveId) {
        Long accountId = ShiroUtil.getAccountId();
        Long comId = ShiroUtil.getComId();
        return channelShareDatService.getChannelUrl(comId, accountId,channelLiveId);
    }
    @GetMapping("resetChannelUrl")
    public R resetChannelShortUrl(@RequestParam("channelLiveId") Long channelLiveId) {
        Long accountId = ShiroUtil.getAccountId();
        Long comId = ShiroUtil.getComId();
        return channelShareDatService.resetChannelUrl(comId, accountId,channelLiveId);
    }

    /**
     * 获取渠道直播间学员数据
     * @return
     */
    @PostMapping("getChannelLiveData")
    public R getChannelLiveData(@RequestBody GetChannelLiveDataDto dto) {
        Long comId = ShiroUtil.getComId();
        return channelLiveInfoService.getChannelLiveData(comId, dto);
    }
    /**
     * 根据时间获取直播间记录
     */
    @GetMapping("getChannelLiveByTime")
    public R getLiveByTime(@RequestParam("time") String time) {
        Long comId = ShiroUtil.getComId();
        return channelLiveInfoService.getLiveByTime(comId, time);
    }
    /**
     * 绑定渠道直播间
     * @param dto
     * @return
     */
    @PostMapping("bindChannelByRoomId")
    public R bindChannelByRoomId(@RequestBody BindChannelByRoomIdDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return channelLiveInfoService.bindChannelByRoomId(dto, comId, accountId);
    }

    /**
     * 获取渠道分享海报
     * @return
     */
    @GetMapping("getPosterInfo")
    public R getPosterInfo(@RequestParam("channelLiveId") Long channelLiveId) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return channelShareDatService.getPosterInfo(channelLiveId, comId, accountId);
    }




}
