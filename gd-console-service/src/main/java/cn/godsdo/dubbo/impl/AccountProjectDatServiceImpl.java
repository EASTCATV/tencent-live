package cn.godsdo.dubbo.impl;

import cn.godsdo.dubbo.AccountProjectDatService;
import cn.godsdo.entity.AccountProjectDat;
import cn.godsdo.mapper.AccountProjectDatMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * <p>
 * 《员工数据项目权限表》 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2024-07-09
 */
@DubboService
public class AccountProjectDatServiceImpl extends ServiceImpl<AccountProjectDatMapper, AccountProjectDat> implements AccountProjectDatService {

}
