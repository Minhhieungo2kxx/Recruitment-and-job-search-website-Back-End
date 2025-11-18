package com.webjob.application.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);      // 20 thread luôn sẵn sàng
        executor.setMaxPoolSize(50);       // tối đa 50 thread
        executor.setQueueCapacity(500);    // xếp hàng chờ tối đa 500 email
        executor.setThreadNamePrefix("AsyncThread-");
        executor.initialize();
        return executor;
    }
}

//|         Gọi `@Async` từ đâu?                | Có hiệu lực không? |
//        | ----------------------------------- | ------------------ |
//        | Gọi từ controller/service khác bean | ✅ Có hiệu lực      |
//        | Gọi từ chính bên trong cùng class   | ❌ Không hiệu lực   |

//CorePoolSize = 5 → luôn giữ 5 thread sẵn sàng xử lý task.
//
//MaxPoolSize = 10 → nếu có >5 task đang chạy, sẽ tạo thêm tối đa 5 thread nữa, tổng cộng 10 thread đồng thời.
//
// QueueCapacity = 100 → nếu tất cả 10 thread đang bận, các task tiếp theo sẽ xếp hàng chờ tối đa 100 task.
// Nếu queue đầy → task mới sẽ bị từ chối hoặc ném exception (tùy rejection policy mặc định).
//
//ThreadNamePrefix = "AsyncThread-" → mỗi thread sẽ có tên dễ nhận dạng trong log.

