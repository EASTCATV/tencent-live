package cn.godsdo.controller.camp;

import cn.godsdo.dto.live.CampDto;
import cn.godsdo.dto.live.CampUserDto;
import cn.godsdo.dubbo.camp.*;
import cn.godsdo.entity.camp.TrainingComTask;
import cn.godsdo.entity.camp.TrainingComTaskErrors;
import cn.godsdo.enums.camp.ComTaskEnum;
import cn.godsdo.util.R;
import cn.hutool.core.lang.Assert;
import com.y20y.interfaces.IdService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @Author: CR7
 * @Date: 2019/5/5 18:01
 * @Description:
 */
@Slf4j
@RestController
@RequestMapping("/campService")
@CrossOrigin
public class TrainingCampServiceController {
    @DubboReference(check = false)
    IdService idService;
    @DubboReference(check = false)
    private TrainingCampUserService trainingCampUserService;
    @DubboReference(check = false)
    private TrainingComTaskService trainingComTaskService;
    @DubboReference(check = false)
    private TrainingComTaskErrorsService trainingComTaskErrorsService;
    @DubboReference(check = false)
    private TrainingCampService trainingCampService;
    @DubboReference(check = false)
    private TrainingCampPeriodService trainingCampPeriodService;

    /**
     * 训练营列表-分组
     *
     * @return R
     */
    @PostMapping("/getCampList")
    public R getCampList(@RequestBody CampDto dto) {
        try {
            Assert.notNull(dto.getComId(), "客户id不能为空");
            Assert.notNull(dto.getComKey(), "客户key不能为空");
        } catch (IllegalArgumentException e) {
            return R.failed(e.getMessage());
        }
//        System.out.println("**************** 111");

        boolean isOk = trainingCampUserService.checkComKey(dto.getComId(), dto.getComKey());
        if (isOk) {
            return trainingCampService.getCampList(dto);
        } else {
            return R.failed("验证失败");
        }
    }

    /**
     * 训练营列表-营期列表
     *
     * @return R
     */
    @PostMapping("/getPeriodList")
    public R getPeriodList(@RequestBody CampDto dto) {
        try {
            Assert.notNull(dto.getComId(), "客户id不能为空");
            Assert.notNull(dto.getComKey(), "客户key不能为空");
        } catch (IllegalArgumentException e) {
            return R.failed(e.getMessage());
        }

        boolean isOk = trainingCampUserService.checkComKey(dto.getComId(), dto.getComKey());
        if (isOk) {
            return trainingCampPeriodService.getPeriodList(dto);
        } else {
            return R.failed("验证失败");
        }
    }

    /**
     * 导入学员信息
     * @param dto 学员信息
     * @return 导入结果
     */
    @PostMapping("/importUser")
    public R getCampList(@RequestBody CampUserDto dto) {
        try {
            Assert.notNull(dto.getComId(), "客户id不能为空");
            Assert.notNull(dto.getComKey(), "客户key不能为空");
            Assert.notNull(dto.getList(), "学员列表不能为空");
            Assert.notNull(dto.getPeriodId(), "营期id不能为空");
        } catch (IllegalArgumentException e) {
            return R.failed(e.getMessage());
        }

        boolean isOk = trainingCampUserService.checkComKey(dto.getComId(), dto.getComKey());
        if (isOk) {
            Long taskId = idService.nextId();
            dto.setTaskId(taskId);
            CompletableFuture.runAsync(() -> trainingCampUserService.importUser(dto));
            return R.ok(taskId);
        } else {
            return R.failed("验证失败");
        }
    }

    /**
     * 查询导入结果
     * @param dto 任务信息
     * @return 导入结果
     */
    @PostMapping("/queryImportInfo")
    public R queryImportInfo(@RequestBody CampUserDto dto) {
        try {
            Assert.notNull(dto.getComId(), "客户id不能为空");
            Assert.notNull(dto.getComKey(), "客户key不能为空");
            Assert.notNull(dto.getTaskId(), "任务id不能为空");
        } catch (IllegalArgumentException e) {
            return R.failed(e.getMessage());
        }

        boolean isOk = trainingCampUserService.checkComKey(dto.getComId(), dto.getComKey());
        if (isOk) {
            TrainingComTask task = trainingComTaskService.getById(dto.getTaskId());
            if (task == null) {
                return R.failed("查询失败, 请稍后重试或联系管理员");
            } else if (ComTaskEnum.IMPORTING.getValue().equals(task.getState())) {
                return R.ok("正在导入中");
            } else if (ComTaskEnum.COMPLETED.getValue().equals(task.getState())){
                Map<String, Object> result = new HashMap<>();
                result.put("successNum", task.getSuccessNum());
                result.put("failNum", task.getFailNum());
                result.put("totalNum", task.getTotalNum());
                if (task.getFailNum() > 0) {
                    List<TrainingComTaskErrors> errors = trainingComTaskErrorsService.selectByTaskId(dto.getTaskId());
                    List<String> errorList = errors.stream().map(TrainingComTaskErrors::getMsg).toList();
                    result.put("errorList", errorList);
                }
                return R.ok(result);
            } else if (ComTaskEnum.FAILED.getValue().equals(task.getState())) {
                return R.ok("导入失败, 请联系管理员");
            } else {
                return R.failed("接口异常, 请联系管理员");
            }
        } else {
            return R.failed("验证失败");
        }
    }

}
