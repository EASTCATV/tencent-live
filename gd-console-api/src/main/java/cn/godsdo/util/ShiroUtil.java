package cn.godsdo.util;

import cn.godsdo.entity.AccountDat;
import com.alibaba.fastjson.JSONObject;
import com.y20y.constant.Constants;
import org.apache.shiro.SecurityUtils;

/**
 * @author W~Y~H
 * @Date : 2018/11/18
 */
public class ShiroUtil {

    /**
     * 获取comId
     *
     * @return
     */
    public static Long getComId() {
        JSONObject accountJson = (JSONObject) SecurityUtils.getSubject().getSession().getAttribute(Constants.SESSION_ACCOUNT_INFO);
        return accountJson.getLong("comId");
    }

    /**
     * 获取comId
     *
     * @return
     */
    public static Long getAccountId() {
        JSONObject accountJson = (JSONObject) SecurityUtils.getSubject().getSession().getAttribute(Constants.SESSION_ACCOUNT_INFO);
        return accountJson.getLong("id");
    }
    /**
     * 获取项目ID
     *
     * @return
     */
    public static Long getProject() {
        JSONObject accountJson = (JSONObject) SecurityUtils.getSubject().getSession().getAttribute(Constants.SESSION_ACCOUNT_INFO);
        return accountJson.getLong("project");
    }
    /**
     * 获取accountDat
     *
     * @return
     */
    public static AccountDat getAccountDat() {
        AccountDat accountDat = JSONObject.parseObject(SecurityUtils.getSubject().getSession().getAttribute(Constants.SESSION_ACCOUNT_INFO).toString(), AccountDat.class);
        return accountDat;
    }

    /**
     * 获取RoleId
     *
     * @return
     */
    public static Boolean getIsAdmin() {
        JSONObject accountJson = (JSONObject) SecurityUtils.getSubject().getSession().getAttribute(Constants.SESSION_ACCOUNT_INFO);
        return accountJson.getBoolean("isAdmin");
    }

}
