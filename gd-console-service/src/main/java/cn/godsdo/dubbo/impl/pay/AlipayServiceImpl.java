package cn.godsdo.dubbo.impl.pay;

import cn.godsdo.dubbo.pay.AlipayService;
import cn.godsdo.entity.pay.Alipay;
import cn.godsdo.mapper.pay.AlipayMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.interfaces.IdService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;

import java.util.List;

/**
 * <p>
 *  支付宝服务实现类
 * </p>
 *
 * @author yang
 * @since 2024-07-01
 */
@Slf4j
@DubboService
public class AlipayServiceImpl extends ServiceImpl<AlipayMapper, Alipay> implements AlipayService {

    @DubboReference(check = false)
    private IdService idService;


    @Override
    public List<Alipay> getAlipayList(Long comId) {
        List<Alipay> alipayList = this.baseMapper.getAlipayList(comId);
        //判空
        if (alipayList == null || alipayList.size() == 0) {
            return null;
        }else {
            return alipayList;
        }
    }

    @Override
    public Alipay getAlipayByAppId(Long comId, String appId) {
        Alipay alipay = this.baseMapper.getAlipayByAppId(comId, appId);
        return alipay;
    }

    @Override
    public void UpdateAlipay(Alipay alipay) {
        //判断是不是新增
        if (alipay.getId() == null) {
            long id = idService.nextId();
            //Alipay alipay1 = new Alipay();
            //BeanUtils.copyProperties(alipay, alipay1);
            alipay.setId(id);
            this.save(alipay);
        } else {
            this.updateById(alipay);
        }
    }

    @Override
    public Alipay selectById(Long payId) {
        return this.baseMapper.selectById(payId);
    }

}
