package cn.godsdo.util;

import com.y20y.utils.ToBase62;

/**
 * @Author : yang
 * @Date :
 * @ApiNote :
 */
public class DemoIdTo64 {
    public static void main(String[] args) {
        String s = ToBase62.encodeToBase62(1000000000000000000L);
        System.out.println(s); //1bS0EMtBbK8
    }
}
