package cn.godsdo.dubbo.impl.user;

import cn.godsdo.constant.RedisConstants;
import cn.godsdo.dto.user.GetUserListDto;
import cn.godsdo.dubbo.crossService.RedisDubboService;
import cn.godsdo.dubbo.user.UserDatService;
import cn.godsdo.entity.user.UserDat;
import cn.godsdo.mapper.user.UserDatMapper;
import cn.godsdo.util.R;
import cn.godsdo.vo.user.GetUserListvo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * <p>
 * 《用户表》 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2024-04-25
 */
@DubboService
public class UserDatServiceImpl extends ServiceImpl<UserDatMapper, UserDat> implements UserDatService {
    @DubboReference
    RedisDubboService redisService;
    @Override
    public R getUserList(Long comId, GetUserListDto dto) {
        // 获取分页对象
        Page<UserDat> page = new Page<>(dto.getPage(), dto.getPageSize());
        // 查询信息
        IPage<GetUserListvo> vo = this.baseMapper.getUserList(page, dto, comId);
        return R.ok(vo.getRecords(), vo.getTotal());
    }

    @Override
    public R getBlockUserList(Long comId, GetUserListDto dto) {
        // 获取分页对象
        Page<UserDat> page = new Page<>(dto.getPage(), dto.getPageSize());
        // 查询信息
        IPage<GetUserListvo> vo = this.baseMapper.getBlockUserList(page, dto, comId);
        return R.ok(vo.getRecords(), vo.getTotal());
    }

    @Override
    public R blockUser(Long comId, Long accountId, Long userId,Boolean block) {
        String userIdStr = userId.toString();

        UserDat userDat = this.baseMapper.selectOne(new LambdaQueryWrapper<UserDat>().eq(UserDat::getComId, comId).eq(UserDat::getId, userId));
        if(ObjectUtils.isEmpty(userDat)){
            return R.failed("用户信息不存在");
        }
        if(block){
            redisService.hset(2, RedisConstants.COM_BACK_LIST + comId, userIdStr, userIdStr);
        }else{
            redisService.hDel(2, RedisConstants.COM_BACK_LIST + comId, userIdStr);
        }
        userDat.setBlock(block);
        userDat.setUpdateBy(accountId);
        this.baseMapper.updateById(userDat);
        return R.ok();
    }


}
