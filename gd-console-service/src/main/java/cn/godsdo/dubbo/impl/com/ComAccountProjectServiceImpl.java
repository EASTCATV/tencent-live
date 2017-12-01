package cn.godsdo.dubbo.impl.com;


import cn.godsdo.dubbo.com.ComAccountProjectService;
import cn.godsdo.entity.AccountDat;
import cn.godsdo.entity.com.ComAccountProject;
import cn.godsdo.entity.com.CompanyDat;
import cn.godsdo.mapper.AccountDatMapper;
import cn.godsdo.mapper.com.ComAccountProjectMapper;
import cn.godsdo.mapper.com.CompanyDatMapper;
import cn.godsdo.util.R;
import cn.godsdo.vo.GetComProjectInfoVo;
import cn.godsdo.vo.GetComProjectVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.interfaces.IdService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * <p>
 * 《员工项目表》 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2018-04-18
 */
@DubboService
public class ComAccountProjectServiceImpl extends ServiceImpl<ComAccountProjectMapper, ComAccountProject> implements ComAccountProjectService {


    @Resource
    CompanyDatMapper companyDatMapper;
    @Resource
    AccountDatMapper accountDatMapper;
    @DubboReference
    IdService idService;
    @Override
    public R getComProject(Long comId) {
        // 根据公司ID查询公司账号项目列表
        List<ComAccountProject> list = this.baseMapper.selectList(new LambdaQueryWrapper<ComAccountProject>()
                .eq(ComAccountProject::getComId, comId)
                .eq(ComAccountProject::getDeleteFlg, false)
        );
        // 初始化结果列表
        List<GetComProjectVo> resultList = new ArrayList<>();
        if (list == null) {
            list = new ArrayList<>();
        }
        // 分析项目信息并计算总数
        Integer total = analysisProject(resultList, list, 0L, 1);
        // 获取默认项目数量
        Long defaultNumber = this.getComProjectCountById(-1L, comId);

        // 构建GetComProjectVo对象
        GetComProjectVo gadv = new GetComProjectVo();
        // 根据公司ID查询公司信息
        CompanyDat cm = companyDatMapper.selectOne(new LambdaQueryWrapper<CompanyDat>()
                .eq(CompanyDat::getId, comId));
        gadv.setId(0L);
        gadv.setComId(comId);
        gadv.setIndex(0);
        gadv.setTitle(cm.getCompany());
        if(ObjectUtils.isEmpty(resultList)){
            resultList = null;
        }
        gadv.setChildren(resultList);
        // 构建树形结构
        List<GetComProjectVo> tree = new ArrayList<>();
        tree.add(gadv);
        // 构建GetComProjectInfoVo对象并设置相关属性
        GetComProjectInfoVo vo = new GetComProjectInfoVo();
        vo.setTreeList(tree);
        vo.setDefaultNumber(defaultNumber);
        vo.setTotalNumber(total + defaultNumber);
        // 返回R对象
        return R.ok(vo);
    }

    @Override
    public ComAccountProject getComProjectById(Long comId, Long id) {
        ComAccountProject cap = this.baseMapper.selectOne(new LambdaQueryWrapper<ComAccountProject>()
                .eq(ComAccountProject::getComId, comId)
                .eq(ComAccountProject::getId, id)
                .eq(ComAccountProject::getDeleteFlg, false)
        );
        return cap;
    }

    @Override
    public R addComProject(Long comId, Long accountId, String title, Long parentId) {
        // 查询是否存在同名的项目
        ComAccountProject cap = this.baseMapper.selectOne(new LambdaQueryWrapper<ComAccountProject>()
                .eq(ComAccountProject::getComId, comId)
                .eq(ComAccountProject::getParentId, parentId)
                .eq(ComAccountProject::getTitle, title)
                .eq(ComAccountProject::getDeleteFlg, false)
        );
        // 若已存在同名项目，则返回失败结果
        if (ObjectUtils.isNotEmpty(cap)) {
            return R.failed("项目已存在");
        }
        // 创建新的公司项目对象，设置属性，并插入数据库
        ComAccountProject comAccountProject = new ComAccountProject();
        comAccountProject.setId(idService.nextId());
        comAccountProject.setComId(comId);
        comAccountProject.setCreateBy(accountId);
        comAccountProject.setTitle(title);
        comAccountProject.setParentId(parentId);
        this.baseMapper.insert(comAccountProject);
        // 返回成功结果
        return R.ok();
    }


    @Override
    public R delComProject(Long comId, Long accountId, Long id) {
        ComAccountProject cap = this.baseMapper.selectOne(new LambdaQueryWrapper<ComAccountProject>()
                .eq(ComAccountProject::getComId, comId)
                .eq(ComAccountProject::getId, id)
                .eq(ComAccountProject::getDeleteFlg, false)
        );
        if (ObjectUtils.isEmpty(cap)) {
            return R.failed("项目不存在");
        }
        // 判断是否绑定的员工
        Long comProjectCountById = getComProjectCountById(id, comId);
        if (comProjectCountById > 0) {
            return R.failed("项目已绑定员工，请解绑后再删除");
        }
        //判断是否有子集
        Long count = this.baseMapper.selectCount(new LambdaQueryWrapper<ComAccountProject>().eq(ComAccountProject::getComId, comId)
                .eq(ComAccountProject::getParentId, id).eq(ComAccountProject::getDeleteFlg, false));
        if (count > 0) {
            return R.failed("请先删除子项目");
        }
        cap.setDeleteFlg(1);
        this.baseMapper.updateById(cap);
        return R.ok();
    }


    @Override
    public R saveComProject(Long comId, Long accountId, String title, Long id) {
        // 根据公司ID和项目ID查询公司项目信息
        ComAccountProject cap = this.baseMapper.selectOne(new LambdaQueryWrapper<ComAccountProject>()
                .eq(ComAccountProject::getComId, comId)
                .eq(ComAccountProject::getId, id)
                .eq(ComAccountProject::getDeleteFlg, false)
        );
        // 如果公司项目信息不存在，则返回操作失败结果
        if (ObjectUtils.isEmpty(cap)) {
            return R.failed("项目不存在");
        }
        // 更新公司项目标题和更新人信息
        cap.setTitle(title);
//        cap.setType(type);
        cap.setUpdateBy(accountId);
        this.baseMapper.updateById(cap);
        // 返回操作成功结果
        return R.ok();
    }



    private Integer analysisProject(List<GetComProjectVo> resultList, List<ComAccountProject> groupList, Long parentId, Integer index) {
        int total = 0;
        Integer index1 = index;
        for (ComAccountProject group : groupList) {
            if (group.getParentId().equals(parentId)) {
                int nowTotal = 0;
                GetComProjectVo getAllDepartmentVo = new GetComProjectVo();
                BeanUtils.copyProperties(group, getAllDepartmentVo);
                getAllDepartmentVo.setChildren(new ArrayList<>());
                getAllDepartmentVo.setIndex(index1);
                //获取子数组和数量
                Integer childrenTotal = this.analysisProject(getAllDepartmentVo.getChildren(), groupList, getAllDepartmentVo.getId(), index + 1);
                //计算当前分组的总数
                nowTotal = getAllDepartmentVo.getTotalItem() + childrenTotal;
                //计算所有子集的总数
                total += nowTotal;
                getAllDepartmentVo.setTotalItem(nowTotal);
                if (ObjectUtils.isEmpty(getAllDepartmentVo.getChildren())) {
                    getAllDepartmentVo.setChildren(null);
                }
                resultList.add(getAllDepartmentVo);
            }
        }
        //先按照order
        resultList.sort(Comparator.comparing(GetComProjectVo::getOrderNumber).thenComparing(GetComProjectVo::getId));
        return total;
    }


    public Long getComProjectCountById(Long projectId, Long clientId) {
        Long count = accountDatMapper.selectCount(new LambdaQueryWrapper<AccountDat>().eq(AccountDat::getComId, clientId).eq(AccountDat::getProject, projectId));
        return count;
    }
}
