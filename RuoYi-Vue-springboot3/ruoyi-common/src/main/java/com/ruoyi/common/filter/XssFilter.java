package com.ruoyi.common.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.enums.HttpMethod;

/**
 * 防止XSS攻击的过滤器
 * 
 * @author ruoyi
 *
 * NOTE: /筆記/Web Security/XSS 攻擊.md
 * NOTE: /筆記/Web Security/防 Xss 代码攻击.md
 *
 * 「XssFilter 先執行」 ≠ 「XssFilter 已經把 body 讀掉了」
 * Filter 先後順序只決定「包 wrapper 的順序」，真正把 body 讀光的是第一次有人呼叫 getInputStream() / getReader() 的那一刻，而不是 new wrapper 的那一刻。
 * 這裡只做了一件事：👉 用 XssHttpServletRequestWrapper 把原始 request 包起來，還沒有讀 body。
 * 第一次真正讀取 body 的地方在 HttpHelper.getBodyString(request) 裡面呼叫 request.getInputStream() 。
 */
public class XssFilter implements Filter
{
    /**
     * 排除链接
     */
    public List<String> excludes = new ArrayList<>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        String tempExcludes = filterConfig.getInitParameter("excludes");
        if (StringUtils.isNotEmpty(tempExcludes))
        {
            String[] urls = tempExcludes.split(",");
            for (String url : urls)
            {
                excludes.add(url);
            }
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException
    {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        if (handleExcludeURL(req, resp))
        {
            chain.doFilter(request, response);
            return;
        }
        XssHttpServletRequestWrapper xssRequest = new XssHttpServletRequestWrapper((HttpServletRequest) request);
        chain.doFilter(xssRequest, response);
    }

    private boolean handleExcludeURL(HttpServletRequest request, HttpServletResponse response)
    {
        String url = request.getServletPath();
        String method = request.getMethod();
        // GET DELETE 不过滤
        if (method == null || HttpMethod.GET.matches(method) || HttpMethod.DELETE.matches(method))
        {
            return true;
        }
        return StringUtils.matches(url, excludes);
    }

    @Override
    public void destroy()
    {

    }
}