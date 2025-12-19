package com.ruoyi.common.utils;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import com.ruoyi.common.utils.spring.SpringUtils;

/**
 * 获取i18n资源文件
 * 
 * @author ruoyi
 */
public class MessageUtils
{
    /**
     * 根据消息键和参数 获取消息 委托给spring messageSource
     *
     * @param code 消息键
     * @param args 参数
     * @return 获取国际化翻译值
     *
     * NOTE: RuoYi-Vue-springboot3/筆記/springboot/Spring Boot 国际化 i18n.md
     */
    public static String message(String code, Object... args)
    {
        // MessageSource: 提供 getMessage(String code, Object[] args, Local locale) 方法獲取多語言文本
        MessageSource messageSource = SpringUtils.getBean(MessageSource.class);
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}
