package com.webjob.application.config.App;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {





    //    Executor cũ - giữ nguyên cho các @Async khác.
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(500);

        executor.setThreadNamePrefix("EsSync-");

        // Khi application shutdown, cố xử lý các task đang chạy
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }


//     * Executor riêng cho CV Matching.
    @Bean(name = "matchingTaskExecutor")
    public Executor matchingTaskExecutor(
            @Value("${matching.executor.core-pool-size}") int corePoolSize,
            @Value("${matching.executor.max-pool-size}") int maxPoolSize,
            @Value("${matching.executor.queue-capacity}") int queueCapacity
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);

        executor.setThreadNamePrefix("cv-matching-");

        // Khi queue đầy, thread gọi task sẽ tự chạy task
        // thay vì reject/mất job matching.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // Graceful shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }
}



