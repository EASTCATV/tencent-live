package cn.godsdo.dubbo.impl;

import cn.godsdo.dubbo.PermissionDatService;
import cn.godsdo.dubbo.com.ComDefultSettingService;
import cn.godsdo.entity.PermissionDat;
import cn.godsdo.entity.com.ComDefultSetting;
import cn.godsdo.enums.PermissionsTypeEnum;
import cn.godsdo.mapper.PermissionDatMapper;
import cn.godsdo.util.R;
import cn.godsdo.vo.GetPermissionDataVo;
import cn.godsdo.vo.GetPermissionListVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 后台权限表 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2024-05-26
 */
@DubboService
public class PermissionDatServiceImpl extends ServiceImpl<PermissionDatMapper, PermissionDat> implements PermissionDatService {
    @DubboReference(check = false)
    private ComDefultSettingService comDefultSettingService;

    @Override
    public R getPermissionList(Long comId) {
        GetPermissionListVo getPermissionListVo = new GetPermissionListVo();
        ComDefultSetting info = comDefultSettingService.getInfo(comId);
        // 智能直播
//            20,21,22,23
        // 场控 16 助理端 92、93、94
        ArrayList<Long> ids = new ArrayList<>();
        if (info.getIntelligentStatus() == 0) {
            ids.add(20L);
            ids.add(21L);
            ids.add(22L);
            ids.add(23L);
        }
        if (info.getBotStatus() == 0) {
            ids.add(16L);
            ids.add(92L);
            ids.add(93L);
            ids.add(94L);
        }
        // 控制台
        int controlType = PermissionsTypeEnum.CONTROL.getValue();
        List<GetPermissionDataVo> control = this.baseMapper.getPermissionList(0L, controlType, ids);
        getPermissionListVo.setConsole(getChildrenList(control, controlType, ids));

        // 获取助理菜单分组
        int assistantType = PermissionsTypeEnum.assistant.getValue();
        List<GetPermissionDataVo> assistant = this.baseMapper.getPermissionList(0L, assistantType, ids);
        getPermissionListVo.setAssistant(getChildrenList(assistant, assistantType, ids));
        return R.ok(getPermissionListVo);
    }

    private List<GetPermissionDataVo> getChildrenList(List<GetPermissionDataVo> list, Integer type, ArrayList<Long> ids) {
        return list.stream().map(e -> {
            List<GetPermissionDataVo> childrenList = this.baseMapper.getPermissionList(e.getId(), type, ids);
            childrenList = childrenList.stream().map(ee -> {
                List<GetPermissionDataVo> childrenList1 = this.baseMapper.getPermissionList(ee.getId(), type, ids);
                ee.setChildren(childrenList1);
                return ee;
            }).collect(Collectors.toList());
            e.setChildren(childrenList);
            return e;
        }).collect(Collectors.toList());
    }

}
