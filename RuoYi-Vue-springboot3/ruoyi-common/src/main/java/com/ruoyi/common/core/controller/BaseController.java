package com.ruoyi.common.core.controller;

import java.beans.PropertyEditorSupport;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.page.PageDomain;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.page.TableSupport;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.PageUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.sql.SqlUtil;

/**
 * web层通用数据处理
 * 
 * @author ruoyi
 *
 * 为什么若依的controller类要继承BaseController类?
 *
 * 我们在写项目的时候，会把一些很多相同的代码或使用相同的功能抽取出来，例如一些工具类。
 * 也会为了提现代码的高可用性，我们常用的是的把dao层进行抽取，在若依里面，抽了一些controller层常用的方法，然后进行继承。
 * 这样，在后面使用的时候，可以直接调用他，这样可以简化开发。他是controller层的一个通用数据处理。
 */
public class BaseController
{
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    // 将前台传递过来的日期格式的字符串，自动转化为Date类型
    // NOTE: /筆記/springmvc/@InitBinder 使用教學.md
    @InitBinder
    public void initBinder(WebDataBinder binder)
    {

        /**
         * 1) HTTP 請求參數在 Servlet 層就是 String（或 String[]）
         *      - request.getParameter(name) → String
         *      - request.getParameterValues(name) → String[]
         *      也就是說，Spring WebDataBinder 拿到的「原始值」在大多數情況就是字串，然後才嘗試把它轉成目標型別（例如 Date）。
         *
         * 2) Spring 只有在「需要把文字轉成目標型別」時才會呼叫 setAsText(String text)
         *      PropertyEditorSupport 的典型轉換流程就是：文字（text） → 目標型別（Date / Integer / Enum / …）
         *      所以 Spring 在綁定 Date 欄位時，會把那個原始字串丟給 setAsText(String text)，這就是參數型別是 String 的原因。
         *
         * 3) 但要小心：不是所有情境都走 setAsText
         *      這裡很多人會誤判：
         *      - 若你的資料是 JSON（@RequestBody），通常走的是 HttpMessageConverter（例如 Jackson）直接反序列化，不一定會用到 PropertyEditorSupport#setAsText。
         *      - 若原始值不是文字（例如某些框架內部直接給物件），可能會走 setValue(Object value) 或其他型別轉換機制（Converter/Formatter），而不是 setAsText。
         */
        binder.registerCustomEditor(Date.class, new PropertyEditorSupport()
        {
            @Override
            public void setAsText(String text)
            {
                // Date 类型转换
                setValue(DateUtils.parseDate(text));
            }
        });
    }

    /**
     * 设置请求分页数据
     */
    protected void startPage()
    {
        PageUtils.startPage();
    }

    /**
     * 设置请求排序数据
     */
    protected void startOrderBy()
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        if (StringUtils.isNotEmpty(pageDomain.getOrderBy()))
        {
            String orderBy = SqlUtil.escapeOrderBySql(pageDomain.getOrderBy());
            PageHelper.orderBy(orderBy);
        }
    }

    /**
     * 清理分页的线程变量
     */
    protected void clearPage()
    {
        PageUtils.clearPage();
    }

    /**
     * 响应请求分页数据
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected TableDataInfo getDataTable(List<?> list)
    {
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(HttpStatus.SUCCESS);
        rspData.setMsg("查询成功");
        rspData.setRows(list);
        rspData.setTotal(new PageInfo(list).getTotal());
        return rspData;
    }

    /**
     * 返回成功
     */
    public AjaxResult success()
    {
        return AjaxResult.success();
    }

    /**
     * 返回失败消息
     */
    public AjaxResult error()
    {
        return AjaxResult.error();
    }

    /**
     * 返回成功消息
     */
    public AjaxResult success(String message)
    {
        return AjaxResult.success(message);
    }
    
    /**
     * 返回成功消息
     */
    public AjaxResult success(Object data)
    {
        return AjaxResult.success(data);
    }

    /**
     * 返回失败消息
     */
    public AjaxResult error(String message)
    {
        return AjaxResult.error(message);
    }

    /**
     * 返回警告消息
     */
    public AjaxResult warn(String message)
    {
        return AjaxResult.warn(message);
    }

    /**
     * 响应返回结果
     * 
     * @param rows 影响行数
     * @return 操作结果
     */
    protected AjaxResult toAjax(int rows)
    {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 响应返回结果
     * 
     * @param result 结果
     * @return 操作结果
     */
    protected AjaxResult toAjax(boolean result)
    {
        return result ? success() : error();
    }

    /**
     * 页面跳转
     */
    public String redirect(String url)
    {
        return StringUtils.format("redirect:{}", url);
    }

    /**
     * 获取用户缓存信息
     */
    public LoginUser getLoginUser()
    {
        return SecurityUtils.getLoginUser();
    }

    /**
     * 获取登录用户id
     */
    public Long getUserId()
    {
        return getLoginUser().getUserId();
    }

    /**
     * 获取登录部门id
     */
    public Long getDeptId()
    {
        return getLoginUser().getDeptId();
    }

    /**
     * 获取登录用户名
     */
    public String getUsername()
    {
        return getLoginUser().getUsername();
    }
}
