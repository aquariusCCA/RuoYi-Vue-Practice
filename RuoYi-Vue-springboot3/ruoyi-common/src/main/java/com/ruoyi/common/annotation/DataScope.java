package com.ruoyi.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限过滤注解
 * 
 * @author ruoyi
 *
 * NOTE: [深入分析若依数据权限@datascope （注解+AOP+动态sql拼接） ](https://www.cnblogs.com/kisshappyboy/p/17980084)
 * NOTE: [若依开发平台数据权限设计与实现深度剖析](https://zhuanlan.zhihu.com/p/711964058)
 * NOTE: [若依框架中@DataScope数据权限注解的使用与自定义sql语句](https://blog.csdn.net/chinatopno1/article/details/120109098)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope
{
    /**
     * 部门表的别名
     */
    public String deptAlias() default "";

    /**
     * 用户表的别名
     */
    public String userAlias() default "";

    /**
     * 权限字符（用于多个角色匹配符合要求的权限）默认根据权限注解@ss获取，多个权限用逗号分隔开来
     */
    public String permission() default "";
}
