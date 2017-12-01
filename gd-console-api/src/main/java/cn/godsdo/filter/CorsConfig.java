package cn.godsdo.filter;

//import jakarta.servlet.annotation.WebFilter;
import org.springframework.context.annotation.Configuration;
//import org.springframework.web.server.WebFilter;
//import org.springframework.web.server.WebFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

//import javax.servlet.FilterChain;
//import javax.servlet.ServletException;
//import java.io.IOException;

/**
 * @Author : yang
 * @Date : 2024/4/30
 * @ApiNote :
 */
//@Configuration
////@WebFilter(urlPatterns = "/*")
//public class CorsConfig implements WebFilter {
//
//    @Override
//    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
//            throws IOException, ServletException {
//        final HttpServletResponse responseHeader = (HttpServletResponse) response;
//        responseHeader.setHeader("Access-Control-Allow-Origin", "*");
//        responseHeader.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
//        responseHeader.setHeader("Access-Control-Max-Age", "3600");
//        responseHeader.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, Origin, X-Requested-With, Accept, Access-Control-Request-Method, Access-Control-Request-Headers");
//        chain.doFilter(request, response);
//    }
//}