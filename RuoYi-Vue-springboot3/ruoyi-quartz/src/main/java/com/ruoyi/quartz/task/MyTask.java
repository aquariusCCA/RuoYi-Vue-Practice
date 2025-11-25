package com.ruoyi.quartz.task;

import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class MyTask {
    public void showTime() {
        System.out.println("定時任務開使執行: " + new Date());
    }
}
