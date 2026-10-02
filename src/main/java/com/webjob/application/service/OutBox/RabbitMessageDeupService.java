package com.webjob.application.service.OutBox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMessageDeupService {
    private final RedissonClient redissonClient;
    private static final String KEY_PREFIX = "rabbitmq:dedup:";

    /**
     * Phải lớn hơn thời gian xử lý tối đa của business operation.
     * <p>
     * Nếu job có thể chạy > 5 phút:
     * - tăng giá trị này
     * - hoặc implement heartbeat/renew TTL
     */
    private static final long PROCESSING_TTL_SECONDS = 10 * 60;
    private static final long PROCESSED_TTL_SECONDS = 10 * 60;

    private static final String PROCESSING_PREFIX = "PROCESSING:";
    private static final String PROCESSED = "PROCESSED";


    /**
     * Atomic:
     * <p>
     * SET key value NX EX ttl
     * <p>
     * Nếu key đã tồn tại:
     * - PROCESSING -> false
     * - PROCESSED  -> false
     * <p>
     * Nếu key chưa tồn tại:
     * - tạo PROCESSING:<ownerToken>
     * - trả true
     */
    public String tryStartProcessing(
            String queueName,
            String eventId
    ) {
        String key = buildKey(queueName, eventId);
        String ownerToken = UUID.randomUUID().toString();

        RBucket<String> bucket = redissonClient.getBucket(key);

        boolean acquired = bucket.setIfAbsent(
                PROCESSING_PREFIX + ownerToken,
                Duration.ofSeconds(PROCESSING_TTL_SECONDS)
        );

        if (acquired) {
            log.debug("Acquired processing lock. queue={}, eventId={}, ownerToken={}",
                    queueName,
                    eventId,
                    ownerToken
            );
            return ownerToken;
        }
        log.info(
                "Duplicate/in-flight event ignored. queue={}, eventId={}",
                queueName,
                eventId
        );
        return null;
    }

    /**
     * Chỉ owner hiện tại mới được chuyển:
     * <p>
     * PROCESSING:<ownerToken>
     * ->
     * PROCESSED
     * <p>
     * Việc check owner + set PROCESSED được thực hiện atomic
     * bằng Lua script.
     */

    public boolean markProcessed(
            String queueName,
            String eventId,
            String ownerToken
    ) {
        String key = buildKey(queueName, eventId);
        String expectedValue = PROCESSING_PREFIX + ownerToken;

        String script = """
                local current = redis.call('GET', KEYS[1])

                if current == ARGV[1] then
                    redis.call(
                        'SET',
                        KEYS[1],
                        ARGV[2],
                        'EX',
                        ARGV[3]
                    )
                    return 1
                end

                return 0
                """;

        RScript rScript = redissonClient.getScript(StringCodec.INSTANCE);
        log.debug(
                "markProcessed Redis args: key={}, expectedValue={}, newValue={}, ttl={}",
                key,
                expectedValue,
                PROCESSED,
                PROCESSED_TTL_SECONDS
        );


        Long result = rScript.eval(
                RScript.Mode.READ_WRITE,
                script,
                RScript.ReturnType.INTEGER,
                Collections.singletonList(key),
                expectedValue,
                PROCESSED,
                PROCESSED_TTL_SECONDS
        );

        boolean marked = result != null && result == 1L;

        if (marked) {
            log.debug(
                    "Marked event as PROCESSED. queue={}, eventId={}",
                    queueName,
                    eventId
            );
        } else {
            log.warn(
                    "Cannot mark event as PROCESSED because ownership was lost. " +
                            "queue={}, eventId={}",
                    queueName,
                    eventId
            );
        }

        return marked;
    }


    /**
     * Chỉ owner hiện tại mới được remove PROCESSING.
     * <p>
     * Tránh race condition:
     * <p>
     * A: PROCESSING:tokenA
     * TTL hết
     * B: PROCESSING:tokenB
     * A: removeProcessing()
     * <p>
     * A không được phép xóa lock của B.
     */

    public boolean removeProcessing(
            String queueName,
            String eventId,
            String ownerToken
    ) {
        String key = buildKey(queueName, eventId);
        String expectedValue = PROCESSING_PREFIX + ownerToken;

        String script = """
            local current = redis.call('GET', KEYS[1])

            if current == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end

            return 0
            """;

        RScript rScript = redissonClient.getScript(StringCodec.INSTANCE);


        Long result = rScript.eval(
                RScript.Mode.READ_WRITE,
                script,
                RScript.ReturnType.INTEGER,
                Collections.singletonList(key),
                expectedValue
        );

        boolean deleted = result != null && result == 1L;

        if (deleted) {
            log.debug(
                    "Removed processing lock. queue={}, eventId={}",
                    queueName,
                    eventId
            );
        } else {
            log.warn(
                    "Processing lock was not removed because ownership was lost. " +
                            "queue={}, eventId={}",
                    queueName,
                    eventId
            );
        }

        return deleted;
    }


    private String buildKey(
            String queueName,
            String eventId
    ) {
        return KEY_PREFIX + queueName + ":" + eventId;
    }


}
