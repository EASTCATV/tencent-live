package cn.godsdo.dubbo.impl;

import cn.godsdo.dto.GetPayOrdersDto;
import cn.godsdo.dubbo.PayProductClickService;
import cn.godsdo.entity.PayProductClick;
import cn.godsdo.enums.PayEnum;
import cn.godsdo.mapper.PayProductClickMapper;
import cn.godsdo.util.R;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.Date;
import java.util.List;

/**
 * <p>
 * 商品点击购买记录表 服务实现类
 * </p>
 *
 * @author hdk
 * @since 2024-05-28
 */
@DubboService
public class PayProductClickServiceImpl extends ServiceImpl<PayProductClickMapper, PayProductClick> implements PayProductClickService {

    @Override
    public R queryPayOrderData(GetPayOrdersDto dto) {
        // 创建一个分页对象
        Page<PayProductClick> datPage = new Page<>(dto.getPage(), dto.getPageSize());
        Page<PayProductClick> vo = this.baseMapper.selectPage(datPage, Wrappers.<PayProductClick>lambdaQuery()
                .eq(PayProductClick::getLiveId, dto.getLiveId())
                .orderByAsc(PayProductClick::getCreateAt)
        );
        // 获取查询结果的记录列表和总数
        List<PayProductClick> records = vo.getRecords();
        // 返回结果
        return R.ok(records, vo.getTotal());
    }

    @Override
    public void updateProductClick(Long comId, Long liveId, Long productId, String account) {
        this.update(new LambdaUpdateWrapper<PayProductClick>()
                .set(PayProductClick::getPayType, PayEnum.FINISH.getValue())
                .set(PayProductClick::getUpdateAt, new Date())
                .eq(PayProductClick::getComId, comId)
                .eq(PayProductClick::getLiveId, liveId)
                .eq(PayProductClick::getProductId, productId)
                .eq(PayProductClick::getAccount, account)
        );
    }

}
