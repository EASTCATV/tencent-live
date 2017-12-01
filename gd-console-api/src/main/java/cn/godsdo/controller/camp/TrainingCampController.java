package cn.godsdo.controller.camp;

import cn.godsdo.dto.live.AddLiveDto;
import cn.godsdo.dto.live.CampDto;
import cn.godsdo.dto.live.CampUserDto;
import cn.godsdo.dto.live.GetLiveListDto;
import cn.godsdo.dubbo.camp.TrainingCampPeriodService;
import cn.godsdo.dubbo.camp.TrainingCampService;
import cn.godsdo.dubbo.camp.TrainingCampUserService;
import cn.godsdo.dubbo.live.LiveDatService;
import cn.godsdo.entity.camp.TrainingCom;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import cn.godsdo.util.tencent.CosHelperUtil;
import cn.hutool.core.lang.Assert;
import com.y20y.interfaces.IdService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * 训练营表 前端控制器
 * </p>
 *
 * @author hdk
 * @since 2024-07-05
 */
@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/trainingCamp")
public class TrainingCampController {

    @DubboReference(check = false)
    IdService idService;

    @DubboReference(check = false)
    private LiveDatService liveDatService;

    @DubboReference(check = false)
    private TrainingCampService trainingCampService;

    @DubboReference(check = false)
    private TrainingCampPeriodService trainingCampPeriodService;

    @DubboReference(check = false)
    private TrainingCampUserService trainingCampUserService;

    @Resource
    @Qualifier("CosBean")
    private CosHelperUtil cosHelperUtil;

    /**
     * 训练营列表-分组
     *
     * @return R
     */
    @PostMapping("/getCampList")
    public R getCampList(@RequestBody CampDto dto) {
        Long comId = ShiroUtil.getComId();
        dto.setComId(comId);
        return trainingCampService.getCampList(dto);
    }
    /**
     * 训练营列表-新建训练营
     */
    @PostMapping("/addCamp")
    public R addCamp(@RequestBody CampDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        dto.setComId(comId);
        dto.setAccountId(accountId);
        trainingCampService.addCamp(dto);
        return R.ok();
    }
    /**
     * 训练营列表-编辑训练营
     */
    @PostMapping("/editCamp")
    public R editCamp(@RequestBody CampDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        dto.setComId(comId);
        dto.setAccountId(accountId);
        return trainingCampService.editCamp(dto);
    }
    /**
     * 训练营列表-删除训练营
     */
    @PostMapping("/delCamp")
    public R delCamp(@RequestBody CampDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        dto.setComId(comId);
        dto.setAccountId(accountId);
        return trainingCampService.delCamp(dto);
    }

    /**
     * 训练营列表-营期列表
     *
     * @return R
     */
    @PostMapping("/getPeriodList")
    public R getPeriodList(@RequestBody CampDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        dto.setComId(comId);
        dto.setAccountId(accountId);
        return trainingCampPeriodService.getPeriodList(dto);
    }
    /**
     * 训练营列表-新建营期
     */
    @PostMapping("/addPeriod")
    public R addPeriod(@RequestBody CampDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        dto.setComId(comId);
        dto.setAccountId(accountId);
        return trainingCampPeriodService.addPeriod(dto);
    }
    /**
     * 训练营列表-编辑营期
     */
    @PostMapping("/editPeriod")
    public R editPeriod(@RequestBody CampDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        dto.setComId(comId);
        dto.setAccountId(accountId);
        return trainingCampPeriodService.editPeriod(dto);
    }
    /**
     * 训练营列表-删除营期
     */
    @PostMapping("/delPeriod")
    public R delPeriod(@RequestBody CampDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        dto.setComId(comId);
        dto.setAccountId(accountId);
        return trainingCampPeriodService.delPeriod(dto);
    }

    /**
     * 营期相应-课程列表 (同房间列表)
     *
     * @return R
     */
    @PostMapping("/getLiveList")
    public R getLiveList(@RequestBody GetLiveListDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveDatService.getLiveCampList(comId, accountId, dto);
    }

    /**
     * 营期相应-添加课节
     *
     * @return R
     */
    @PostMapping("/addLive")
    public R addLive(@RequestBody AddLiveDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        String image = dto.getCoverImage();
        if (ObjectUtils.isNotEmpty(image) && !image.startsWith("http")) {
            image = cosHelperUtil.uploadGiftImg(image, comId);
            dto.setCoverImage(image);
        }
        return liveDatService.addRoom(comId, accountId, dto);
    }

    /**
     * 营期详情-营期学员
     *
     * @return R
     */
    @PostMapping("/getUserList")
    public R getUserList(@RequestBody CampDto dto) {
        Long comId = ShiroUtil.getComId();
        dto.setComId(comId);
        return trainingCampUserService.getLiveCampList(dto);
    }

    /**
     * 营期详情-营期学员
     *
     * @return R
     */
    @PostMapping("/addUser")
    public R addUser(@RequestBody CampUserDto dto) {
        try {
            Assert.notNull(dto.getList(), "学员列表不能为空");
            Assert.notNull(dto.getPeriodId(), "营期id不能为空");
        } catch (IllegalArgumentException e) {
            return R.failed(e.getMessage());
        }

        Long comId = ShiroUtil.getComId();
        List<TrainingCom> trainingComList = trainingCampService.getKeyByComId(comId);
        if (trainingComList.size() == 1) {
            Long taskId = idService.nextId();
            dto.setTaskId(taskId);
            dto.setComId(comId);
            CompletableFuture.runAsync(() -> trainingCampUserService.importUser(dto));
            return R.ok(taskId);
        } else {
            return R.failed("验证失败");
        }
    }

}
