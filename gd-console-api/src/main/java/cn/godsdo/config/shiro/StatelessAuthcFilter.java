package cn.godsdo.config.shiro;

import cn.godsdo.config.jwt.JWTToken;
import com.alibaba.fastjson.JSONObject;
import com.y20y.constant.CodeDefs;
import com.y20y.constant.Constants;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.UserFilter;
import org.apache.shiro.web.util.WebUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMethod;

//import javax.servlet.ServletRequest;
//import javax.servlet.ServletResponse;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;

//  //解决跨域:https://blog.csdn.net/qq_32618611/article/details/105622681
public class StatelessAuthcFilter extends UserFilter {

    @Override
    protected boolean preHandle(ServletRequest request, ServletResponse response) throws Exception {
        HttpServletRequest httpRequest = WebUtils.toHttp(request);
        HttpServletResponse httpResponse = WebUtils.toHttp(response);
        if (httpRequest.getMethod().equals(RequestMethod.OPTIONS.name())) {
            httpResponse.setHeader("Access-control-Allow-Origin", httpRequest.getHeader("Origin"));
            httpResponse.setHeader("Access-Control-Allow-Methods", httpRequest.getMethod());
            httpResponse.setHeader("Access-Control-Allow-Headers", httpRequest.getHeader("Access-Control-Request-Headers"));
            httpResponse.setStatus(HttpStatus.OK.value());
            return false;
        }
        return super.preHandle(request, response);
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        HttpServletResponse httpResp = WebUtils.toHttp(response);
        HttpServletRequest httpReq = WebUtils.toHttp(request);

        /*系统重定向会默认把请求头清空，这里通过拦截器重新设置请求头，解决跨域问题*/
        httpResp.addHeader("Access-Control-Allow-Origin", httpReq.getHeader("Origin"));
        httpResp.addHeader("Access-Control-Allow-Headers", "*");
        httpResp.addHeader("Access-Control-Allow-Methods", "*");
        httpResp.addHeader("Access-Control-Allow-Credentials", "true");

        this.saveRequestAndRedirectToLogin(request, response);
        return false;
    }

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) throws UnauthorizedException {
        //判断请求的请求头是否带上 "Token"
        if (isLoginAttempt(request, response)) {
            try {
//                System.out.println("aa");
//                return true;
                return executeLogin(request, response);
            } catch (Exception e) {
                //token 错误
                return checkToken(request, response);
            }
        }else{
            return checkToken(request, response);
        }
        //如果请求头不存在 Token，则可能是执行登陆操作或者是游客状态访问，无需检查 token，直接返回 true
    }
    protected boolean isLoginAttempt(ServletRequest request, ServletResponse response) {
        HttpServletRequest req = (HttpServletRequest) request;
        String token = req.getHeader("token");






        return token != null;
    }
    private boolean checkToken(ServletRequest request, ServletResponse response) {
        // 没有携带Token
        HttpServletRequest httpRequest = (HttpServletRequest)request;
        String httpMethod = httpRequest.getMethod();
        String requestURI = httpRequest.getRequestURI();
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        httpServletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
        httpServletResponse.setCharacterEncoding("UTF-8");
        httpServletResponse.setContentType("application/json; charset=utf-8");
        PrintWriter out = null;
        try  {
            out = httpServletResponse.getWriter();
            JSONObject object = new JSONObject();
            object.put("code", CodeDefs.ERROR_REQUEST);
            object.put("message","您无权限");
            out.print(object.toJSONString());
            out.flush();;
        } catch (IOException e) {
            //logger.error("直接返回Response信息出现IOException异常", e);
        }finally {
            if(out!=null){
                out.close();
            }
        }
        return false;
    }

    /**
     * 核心验证权限,只走这个方法
     * @param request
     * @param response
     * @return
     * @throws Exception
     */

    protected boolean executeLogin(ServletRequest request, ServletResponse response) throws Exception {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String token = httpServletRequest.getHeader("token");
        Subject subject = SecurityUtils.getSubject();
        Session session = subject.getSession();
        Serializable id = session.getId();
        System.out.println("id:"+id);
        System.out.println("token:"+token);

        if(id.toString().equals(token)){
            System.out.println("验证权限通过");
        }
        JWTToken jwtToken = new JWTToken(token);
        // 提交给realm进行登入，如果错误他会抛出异常并被捕获
        try {
            if(!(id.toString().equals(token))){
                throw new AuthenticationException();
            }
        } catch (AuthenticationException e) {
            System.out.println("验证权限不通过");
//            e.printStackTrace();
            HttpServletResponse httpServletResponse = (HttpServletResponse) response;
            httpServletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
            httpServletResponse.setCharacterEncoding("UTF-8");
            httpServletResponse.setContentType("application/json; charset=utf-8");
            PrintWriter out = null;
            try {
                out = httpServletResponse.getWriter();
                JSONObject object = new JSONObject();
                object.put("code", CodeDefs.ERROR_REQUEST);
                object.put("message","您无权限");
                out.print(object.toJSONString());
                out.flush();
            } catch (IOException ex) {
                //logger.error("直接返回Response信息出现IOException异常", e);
            }finally {
                if(out!=null){
                    out.close();
                }
            }
            return false;
        }
        // 如果没有抛出异常则代表登入成功，返回true
        return true;
    }

}
