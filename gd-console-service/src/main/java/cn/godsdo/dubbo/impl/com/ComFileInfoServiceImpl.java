package cn.godsdo.dubbo.impl.com;

import cn.godsdo.dubbo.com.ComFileInfoService;
import cn.godsdo.entity.com.ComFileInfo;
import cn.godsdo.mapper.com.ComFileInfoMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

/**
 * <p>
 * 《客户视频合并数据临时表》 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2018-04-13
 */
@DubboService
public class ComFileInfoServiceImpl extends ServiceImpl<ComFileInfoMapper, ComFileInfo> implements ComFileInfoService {

    @Override
    public List<ComFileInfo> selectFileByParams(ComFileInfo fileInfo) {
        return this.baseMapper.selectList(Wrappers.<ComFileInfo>lambdaQuery().eq(ComFileInfo::getComId,fileInfo.getComId()).eq(ComFileInfo::getIdentifier,fileInfo.getIdentifier()));

    }

    @Override
    public int addFileInfo(ComFileInfo fileInfo) {
        return this.baseMapper.insert(fileInfo);
    }
}
