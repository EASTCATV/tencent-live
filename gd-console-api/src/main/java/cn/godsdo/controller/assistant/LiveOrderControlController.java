package cn.godsdo.controller.assistant;


import cn.godsdo.dto.GetBaseDto;
import cn.godsdo.dto.assistan.LiveOrderControlDto;
import cn.godsdo.dubbo.LiveOrderControlRobotService;
import cn.godsdo.dubbo.LiveOrderControlService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import cn.hutool.core.lang.Assert;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 直播间订单场控表 前端控制器
 * </p>
 *
 * @author hdk
 * @since 2024-07-15
 */
@RestController
@RequestMapping("assistant")
@CrossOrigin
public class LiveOrderControlController {

    @DubboReference
    LiveOrderControlService liveOrderControlService;

    @DubboReference
    LiveOrderControlRobotService liveOrderControlRobotService;

    /**
     * 保存/修改订单场控信息
     * @param dto 订单场控信息
     * @return R
     */
    @RequestMapping("saveOrderControl")
    public R saveOrderControl(@RequestBody LiveOrderControlDto dto) {
        try {
            Assert.notNull(dto.getLiveId(), "直播间id不能为空");
        } catch (IllegalArgumentException e) {
            return R.failed(e.getMessage());
        }
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        dto.setComId(comId);
        return liveOrderControlService.saveOrderControl(dto, accountId);
    }

    /**
     * 获取订单场控信息
     * @param liveId 直播间id
     * @return R
     */
    @RequestMapping("getOrderControl")
    public R getOrderControl(@RequestParam("liveId") Long liveId) {
        Long comId = ShiroUtil.getComId();
        return liveOrderControlService.getOrderControlInfo(comId, liveId);
    }

    /**
     * 停止机器人消息
     * @param dto 订单场控信息
     * @return R
     */
    @RequestMapping("updateRobotMessage")
    public R updateRobotMessage(@RequestBody LiveOrderControlDto dto) {
        try {
            Assert.notNull(dto.getLiveId(), "直播间id不能为空");
        } catch (IllegalArgumentException e) {
            return R.failed(e.getMessage());
        }
        return liveOrderControlService.updateRobotMessage(dto);
    }

    /**
     * 添加机器人
     * @param dto 订单场控信息
     * @return R
     */
    @RequestMapping("addOrderControlRobot")
    public R addOrderControlRobot(@RequestBody LiveOrderControlDto dto) {
        try {
            Assert.notNull(dto.getLiveId(), "直播间id不能为空");
        } catch (IllegalArgumentException e) {
            return R.failed(e.getMessage());
        }
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        dto.setComId(comId);
        dto.setUpdateBy(accountId);
        return liveOrderControlService.addRobot(dto);
    }

    /**
     * 查看机器人
     * @param dto 订单场控信息
     * @return R
     */
    @RequestMapping("getOrderControlRobotList")
    public R getOrderControlRobotList(@RequestBody GetBaseDto dto) {
        try {
            Assert.notNull(dto.getLiveId(), "直播间id不能为空");
        } catch (IllegalArgumentException e) {
            return R.failed(e.getMessage());
        }
        Long comId = ShiroUtil.getComId();
        dto.setComId(comId);
        return liveOrderControlRobotService.getOrderControlRobotList(dto);
    }

    /**
     * 清除机器人
     * @param dto 订单场控信息
     * @return R
     */
    @RequestMapping("clearOrderControlRobot")
    public R clearOrderControlRobot(@RequestBody GetBaseDto dto) {
        try {
            Assert.notNull(dto.getLiveId(), "直播间id不能为空");
        } catch (IllegalArgumentException e) {
            return R.failed(e.getMessage());
        }
        Long comId = ShiroUtil.getComId();
        dto.setComId(comId);
        return liveOrderControlRobotService.clearByRoomId(dto);
    }

    /**
     * 清除机器人
     * @param dto 订单场控信息
     * @return R
     */
    @RequestMapping("updateOrderControlRobot")
    public R updateOrderControlRobot(@RequestBody GetBaseDto dto) {
        try {
            Assert.notNull(dto.getId(), "id不能为空");
            Assert.notNull(dto.getName(), "机器人昵称不能为空");
            Assert.notNull(dto.getLiveId(), "直播间id不能为空");
        } catch (IllegalArgumentException e) {
            return R.failed(e.getMessage());
        }
        Long comId = ShiroUtil.getComId();
        dto.setComId(comId);
        Long accountId = ShiroUtil.getAccountId();
        return liveOrderControlRobotService.updateOrderControlRobot(dto, accountId);
    }

    /**
     * 清除机器人
     * @param dto 订单场控信息
     * @return R
     */
    @RequestMapping("delOrderControlRobotOne")
    public R delOrderControlRobotOne(@RequestBody GetBaseDto dto) {
        try {
            Assert.notNull(dto.getId(), "id不能为空");
            Assert.notNull(dto.getLiveId(), "直播间id不能为空");
        } catch (IllegalArgumentException e) {
            return R.failed(e.getMessage());
        }
        Long comId = ShiroUtil.getComId();
        dto.setComId(comId);
        return liveOrderControlRobotService.delOrderControlRobotOne(dto);
    }

}
