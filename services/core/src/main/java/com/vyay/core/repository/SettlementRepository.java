package com.vyay.core.repository;

import com.vyay.core.entity.settlement.Settlement;
import com.vyay.core.enums.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SettlementRepository extends JpaRepository<Settlement, UUID> {

  /**
   * Group-scoped single fetch. currency is eagerly loaded because the response
   * DTO exposes its ISO code; fromUser/toUser stay lazy — only their ids are
   * read downstream, which resolve off the FK without initialising the proxy.
   */
  @EntityGraph(attributePaths = { "currency" })
  Optional<Settlement> findByIdAndGroupId(UUID id, UUID groupId);

  /**
   * Paginated group history, newest first. currency is fetched via @EntityGraph
   * so the controller can map to the response DTO outside the service tx. The
   * sort is baked into the query (not left to the caller's Pageable) to guarantee
   * newest-first regardless of the request. currency is a to-ONE association, so
   * pagination + fetch does not trigger in-memory paging.
   */
  @EntityGraph(attributePaths = { "currency" })
  @Query("select s from Settlement s where s.group.id = :groupId order by s.createdAt desc")
  Page<Settlement> findByGroupId(UUID groupId, Pageable pageable);

  @EntityGraph(attributePaths = { "currency" })
  @Query("select s from Settlement s where s.group.id = :groupId and s.status = :status order by s.createdAt desc")
  Page<Settlement> findByGroupIdAndStatus(UUID groupId, SettlementStatus status, Pageable pageable);

  /**
   * Settlements in a (group, currency) with the given status, oldest-first by id
   * (UUIDv7 is time-ordered).
   * <p>
   * The status arrives as a BOUND PARAMETER, deliberately — see
   * {@link #findOutstandingProposed}.
   */
  @Query("""
      select s from Settlement s
      where s.group.id = :groupId
        and s.currency.id = :currencyId
        and s.status = :status
      """)
  List<Settlement> findByStatus(@Param("groupId") UUID groupId,
      @Param("currencyId") UUID currencyId,
      @Param("status") SettlementStatus status);

  /**
   * Every settlement still in flight for a (group, currency) — PROPOSED, so the
   * money has been promised but has NOT moved any balance yet (balances shift
   * only at confirmation). Settlement-plan generation attaches these to the lines
   * they are paying off; without them the plan would ask for debt somebody has
   * already committed to paying.
   * <p>
   * Soft-deleted rows are excluded by the entity's @SQLRestriction. fromUser /
   * toUser stay lazy — generation reads only their ids, which resolve off the FK
   * without initialising the proxy, so this stays a single query.
   * <p>
   * Ordered by id (UUIDv7, time-ordered) so the lines a plan fills from this list
   * are filled in a stable, oldest-first order.
   *
   * <h2>Why this delegates instead of inlining the constant</h2>
   * DO NOT write
   * {@code and s.status = com.vyay.core.enums.SettlementStatus.PROPOSED}
   * here. {@code Settlement.status} is mapped with
   * {@code @JdbcType(PostgreSQLEnumJdbcType.class)} and
   * {@code columnDefinition = "settlement_status"}, and Hibernate renders a
   * hardcoded JPQL enum literal by casting to the JAVA type's simple name:
   * {@code 'PROPOSED'::SettlementStatus}. No such Postgres type exists, so the
   * query dies at execution with {@code type "settlementstatus" does not exist}.
   * <p>
   * A bound parameter goes through the JDBC type descriptor instead and resolves
   * to the real {@code settlement_status} type, which is why every other query on
   * this column works. The failure is invisible until the statement first runs —
   * it compiles, and ddl-auto validate passes at startup — so it surfaced only
   * when settlement-plan generation first called this method.
   */
  default List<Settlement> findOutstandingProposed(UUID groupId, UUID currencyId) {
    return findByStatus(groupId, currencyId, SettlementStatus.PROPOSED);
  }
}
