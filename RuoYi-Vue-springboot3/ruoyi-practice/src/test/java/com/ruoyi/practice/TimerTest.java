package com.ruoyi.practice;

import lombok.SneakyThrows;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TimerTest {
    public static void main(String[] args) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);

        // 任务1：延时任务  5秒后执行，只执行1次
        scheduler.schedule(() -> System.out.println("task1 run: " + new Date() + " threadName：" + Thread.currentThread().getName()), 5, TimeUnit.SECONDS);

        // 任务2：延迟1秒后执行，每隔2秒执行一次
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("task2 run: " + new Date() + " threadName："
                    + Thread.currentThread().getName());
        }, 1, 2, TimeUnit.SECONDS);

        // 任务3：上一个任务结束后，延迟2秒执行
        scheduler.scheduleWithFixedDelay(() -> {
            System.out.println("task3 run: " + new Date() + " threadName："
                    + Thread.currentThread().getName());
        }, 1, 2, TimeUnit.SECONDS);
    }
}