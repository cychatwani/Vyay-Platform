package com.vyay.core.repository;

import com.vyay.core.entity.settlement.Settlement;
import com.vyay.core.enums.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface SettlementRepository extends JpaRepository<Settlement, UUID> {

    /**
     * Group-scoped single fetch. currency is eagerly loaded because the response
     * DTO exposes its ISO code; fromUser/toUser stay lazy — only their ids are
     * read downstream, which resolve off the FK without initialising the proxy.
     */
    @EntityGraph(attributePaths = {"currency"})
    Optional<Settlement> findByIdAndGroupId(UUID id, UUID groupId);

    /**
     * Paginated group history, newest first. currency is fetched via @EntityGraph
     * so the controller can map to the response DTO outside the service tx. The
     * sort is baked into the query (not left to the caller's Pageable) to guarantee
     * newest-first regardless of the request. currency is a to-ONE association, so
     * pagination + fetch does not trigger in-memory paging.
     */
    @EntityGraph(attributePaths = {"currency"})
    @Query("select s from Settlement s where s.group.id = :groupId order by s.createdAt desc")
    Page<Settlement> findByGroupId(UUID groupId, Pageable pageable);

    @EntityGraph(attributePaths = {"currency"})
    @Query("select s from Settlement s where s.group.id = :groupId and s.status = :status order by s.createdAt desc")
    Page<Settlement> findByGroupIdAndStatus(UUID groupId, SettlementStatus status, Pageable pageable);
}
