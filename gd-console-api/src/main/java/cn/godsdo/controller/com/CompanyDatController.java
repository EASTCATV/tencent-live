package cn.godsdo.controller.com;

import cn.godsdo.dubbo.com.ComDefultSettingService;
import cn.godsdo.dubbo.com.CompanyDatService;
import cn.godsdo.dubbo.pay.AlipayService;
import cn.godsdo.entity.pay.Alipay;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author W~Y~H
 * @Date : 2018/11/18
 */
@RestController
@RequestMapping("/company")
@CrossOrigin
public class CompanyDatController {
    @DubboReference
    private CompanyDatService companyDatService;
    @DubboReference
    private ComDefultSettingService comDefultSettingService;
    @DubboReference
    private AlipayService alipayservice;


    /**
     * 获取公司信息
     *
     * @return
     */
    @GetMapping("/getCompanyInfo")
    public R getCompanyInfo() {
        Long comId = ShiroUtil.getComId();
        return companyDatService.getCompanyInfo(comId);
    }


    /**
     * 获取微信授权地址
     * 微信服务商授权地址
     *
     * @return
     */
    @GetMapping(value = "/GetWxAuthUrl")
    public R getWxAuthUrl() {
        Long comId = ShiroUtil.getComId();
        return companyDatService.getWxAuthUrl(comId);
    }


    /**
     * 获取微信服务号授权
     *
     * @return
     */
    @GetMapping("getWxInfo")
    public R getWxInfo() {
        Long comId = ShiroUtil.getComId();
        return comDefultSettingService.getWxInfo(comId);
    }


    /**
     * 获取微信服务号授权
     *
     * @return
     */
    @GetMapping("updateWxpayInfo")
    public R updateWxpayInfo(@RequestParam("wxMchId") String wxMchId, @RequestParam("wxMchKey") String wxMchKey) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return comDefultSettingService.updateWxpayInfo(wxMchId, wxMchKey, accountId, comId);
    }

    @PostMapping("/updateAliPayInfo")
    public R updateAliPayInfo(@RequestBody Alipay alipay){
        Long comId = ShiroUtil.getComId();
        alipay.setComId(comId);
        alipayservice.UpdateAlipay(alipay);
        return R.ok();
    }
    @PostMapping("/getAlipayList")
    public R getAlipayList(){
        Long comId = ShiroUtil.getComId();
        List<Alipay> alipayList = alipayservice.getAlipayList(comId);
        return R.ok(alipayList);
    }
}
