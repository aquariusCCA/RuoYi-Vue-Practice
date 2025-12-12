package com.ruoyi.practice.controller;

import com.ruoyi.practice.pojo.UserForm;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class TestController {
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        System.out.println("1111");
        // StringTrimmerEditor(true) 表示：
        // trim 後如果是空字串，轉成 null
        StringTrimmerEditor stringTrimmerEditor = new StringTrimmerEditor(true);
        binder.registerCustomEditor(String.class, stringTrimmerEditor);
    }

    @PostMapping("/api/public/test")
    @ResponseBody
    public String publicTest(UserForm userForm) {
        // 這裡拿到的 form.username / form.email 已經是 trim 過的
        // "  " -> null, " abc " -> "abc"
        System.out.println("UserForm: " + userForm);
        return "OK";
    }
}