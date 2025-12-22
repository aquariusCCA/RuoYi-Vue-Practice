package com.ruoyi.framework.config;

import com.ruoyi.common.utils.Threads;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置
 *
 * @author ruoyi
 *
 * NOTE: /筆記/springboot/详解Spring Boot定时任务的几种实现方案.md
 **/
@Configuration
public class ThreadPoolConfig
{
    // 核心线程池大小
    private int corePoolSize = 50;

    // 最大可创建的线程数
    private int maxPoolSize = 200;

    // 队列最大长度
    private int queueCapacity = 1000;

    // 线程池维护线程所允许的空闲时间
    private int keepAliveSeconds = 300;

    // 目前看下來沒在使用
    @Bean(name = "threadPoolTaskExecutor")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor()
    {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(maxPoolSize);
        executor.setCorePoolSize(corePoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        // 线程池对拒绝任务(无线程可用)的处理策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    /**
     * 执行周期性或定时任务
     */
    @Bean(name = "scheduledExecutorService")
    protected ScheduledExecutorService scheduledExecutorService()
    {
        return new ScheduledThreadPoolExecutor(
                // 線程池的核心線程數
                corePoolSize,
                // 創建線程的工廠
                // 命名（namingPattern）：你在線上查 thread dump、看監控、排查 CPU 飆高時，能立刻定位「這是排程池的 thread」，不然全是 pool-1-thread-1 你會想哭。
                // daemon(true)：JVM 結束時不會被這些 thread 擋住（不會因為排程池還活著而無法退出）。
                // 代價是：JVM 關機時這些任務可能被硬切掉，所以這類池不適合放「必須確保落盤/一致性」的關鍵任務，或要搭配優雅停機（graceful shutdown）。
                new BasicThreadFactory.Builder().namingPattern("schedule-pool-%d").daemon(true).build(),
                // 任務拒絕策略：線程資源不足的時候，策略是「使用調用線程池的線程來執行任務」
                new ThreadPoolExecutor.CallerRunsPolicy())
        {
            @Override
            protected void afterExecute(Runnable r, Throwable t)
            {
                super.afterExecute(r, t);
                /**
                 * 這樣做的目的只有一個核心：不要讓排程/非同步任務的例外被「吞掉」而你完全不知道。
                 * 具體來說：
                 *      - ScheduledExecutorService 執行的任務常被包成 FutureTask（或透過 submit() 回傳 Future）。這種情況下，任務內拋出的例外不一定會直接往外拋到 log；它可能被封進 Future 裡，除非有人 get()，否則你看不到錯誤。
                 *      - 覆寫 afterExecute(...) 等於在每次任務執行完畢後做一次「統一收尾檢查」：
                 *          - 如果 t != null：代表執行時有未處理例外，直接記錄。
                 *          - 如果 t == null 但 r 是 Future：就把 Future 裡的例外取出來記錄
                 *          -（Threads.printException(r, t) 通常就是做這件事）。
                 */
                Threads.printException(r, t);
            }
        };
    }
}
