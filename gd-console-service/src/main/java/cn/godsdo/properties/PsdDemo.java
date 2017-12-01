package cn.godsdo.properties;

import com.y20y.constant.Constants;
import com.y20y.utils.AESUtil;

/**
 * @Author : yang
 * @Date : 2024/5/20
 * @ApiNote :
 */
public class PsdDemo {
    public static void main(String[] args) throws Exception {
        String password="sND8uPyHXKZXoHCJTgVZtg==";
        String encrypt = AESUtil.Decrypt(password, Constants.PASSWORD_KEY);
        System.out.println("==密码==:"+encrypt);
    }
}
