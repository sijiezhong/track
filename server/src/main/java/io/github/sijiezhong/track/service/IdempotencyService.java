package io.github.sijiezhong.track.service;

import io.github.sijiezhong.track.dto.IdempotentSummary;
import java.util.Optional;

/**
 * 幂等服务接口：用于根据 Idempotency-Key 判定请求是否已处理，并可回显摘要
 */
public interface IdempotencyService {
    /**
     * 检查 key 是否首次、存入响应摘要
     * @return true-首处理，false-已存在
     */
    boolean checkAndSet(String key, IdempotentSummary summary);
    /**
     * 查询 key 对应的响应摘要，未命中返回 empty
     */
    Optional<IdempotentSummary> findSummary(String key);
}
