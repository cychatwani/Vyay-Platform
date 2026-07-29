package com.vyay.core.services.balance;

import com.vyay.core.dto.requests.expense.CreateExpenseRequestDTO;
import com.vyay.core.dto.requests.expense.ExpensePayerInputDTO;
import com.vyay.core.dto.requests.expense.ExpenseShareInputDTO;
import com.vyay.core.entity.User;
import com.vyay.core.entity.group.Group;
import com.vyay.core.entity.group.GroupMembership;
import com.vyay.core.entity.reference.Currency;
import com.vyay.core.enums.AuthProvider;
import com.vyay.core.enums.GroupRole;
import com.vyay.core.enums.GroupType;
import com.vyay.core.enums.MembershipStatus;
import com.vyay.core.enums.SplitType;
import com.vyay.core.repository.CurrencyRepository;
import com.vyay.core.repository.GroupMembershipRepository;
import com.vyay.core.repository.GroupRepository;
import com.vyay.core.repository.UserRepository;
import com.vyay.core.services.expense.ExpenseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Hammers a SINGLE user with concurrent expenses across TWO different groups and
 * asserts the user_balance_totals rollup stays exactly in step with SUM(balances).
 *
 * This is the race the chokepoint has to survive: two transactions touching the
 * same user in different groups don't conflict on the balances rows (different
 * rows, different optimistic-lock versions), so without serialisation both would
 * recompute the same user's totals off a READ COMMITTED snapshot that misses the
 * other's not-yet-committed balance write, and the later committer would clobber
 * the total with a stale sum. The ensure/lock/recompute sequence in
 * BalanceUpdateService must prevent that.
 *
 * Runs against the configured Postgres (there is no test profile / in-memory DB);
 * the FOR UPDATE + READ COMMITTED behaviour under test only exists on a real
 * database. Seeds via committed repository saves so the worker threads (separate
 * transactions/connections) can see the fixtures, and cleans up by id afterwards.
 */
@SpringBootTest
class UserBalanceTotalsConcurrencyTest {

    private static final int EXPENSES_PER_GROUP = 40;
    private static final long AMOUNT_MAJOR = 10L; // 10.00 per expense, EQUAL split

    @Autowired private ExpenseService expenseService;
    @Autowired private UserRepository userRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private GroupMembershipRepository membershipRepository;
    @Autowired private CurrencyRepository currencyRepository;
    @Autowired private JdbcTemplate jdbc;

    private Currency currency;
    private User userA; // the hammered user — member of both groups
    private User userB; // group 1 counterparty
    private User userC; // group 2 counterparty
    private Group group1;
    private Group group2;

    @BeforeEach
    void seed() {
        currency = currencyRepository.findByCode("INR")
                .orElseThrow(() -> new IllegalStateException("INR currency seed missing (V2)"));

        userA = saveUser("bal-conc-a");
        userB = saveUser("bal-conc-b");
        userC = saveUser("bal-conc-c");

        group1 = saveGroup("bal-conc-group-1", userA);
        group2 = saveGroup("bal-conc-group-2", userA);

        addMember(group1, userA, GroupRole.ADMIN);
        addMember(group1, userB, GroupRole.MEMBER);
        addMember(group2, userA, GroupRole.ADMIN);
        addMember(group2, userC, GroupRole.MEMBER);
    }

    @Test
    void totalsStayConsistentUnderConcurrentExpensesAcrossTwoGroups() throws Exception {
        // Two pools so both groups are written truly concurrently, maximising the
        // window where both transactions want userA's totals rows at once.
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> firstFailure = new AtomicReference<>();

        try {
            java.util.List<Future<?>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < EXPENSES_PER_GROUP; i++) {
                futures.add(submit(pool, start, firstFailure, group1, userA, userB));
                futures.add(submit(pool, start, firstFailure, group2, userA, userC));
            }

            start.countDown(); // release everyone at once
            for (Future<?> f : futures) {
                f.get(60, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        if (firstFailure.get() != null) {
            fail("A concurrent createExpense failed", firstFailure.get());
        }

        // Invariant, read straight from the DB (no JPA cache): for every user the
        // stored rollup must equal a fresh aggregate over balances.
        assertTotalsMatchBalances(userA.getId());
        assertTotalsMatchBalances(userB.getId());
        assertTotalsMatchBalances(userC.getId());

        // And the concrete expected numbers for userA: payer of every expense in
        // both groups, EQUAL split, so userA is always a net creditor.
        // Per expense userA is owed AMOUNT_MAJOR - (AMOUNT_MAJOR / 2) = half.
        long minorPerExpense = AMOUNT_MAJOR * 100L;              // INR, 2 dp
        long userAOwedPerExpense = minorPerExpense - (minorPerExpense / 2);
        long expectedOwed = userAOwedPerExpense * (2L * EXPENSES_PER_GROUP);

        Long owed = jdbc.queryForObject(
                "SELECT COALESCE(SUM(total_owed_minor), 0) FROM user_balance_totals WHERE user_id = ?",
                Long.class, userA.getId());
        Long owing = jdbc.queryForObject(
                "SELECT COALESCE(SUM(total_owing_minor), 0) FROM user_balance_totals WHERE user_id = ?",
                Long.class, userA.getId());

        assertThat(owed).isEqualTo(expectedOwed);
        assertThat(owing).isEqualTo(0L);
    }

    private Future<?> submit(ExecutorService pool,
                             CountDownLatch start,
                             AtomicReference<Throwable> firstFailure,
                             Group group,
                             User payer,
                             User other) {
        return pool.submit(() -> {
            try {
                start.await();
                expenseService.createExpense(payer, equalSplitExpense(group, payer, other));
            } catch (Throwable t) {
                firstFailure.compareAndSet(null, t);
            }
            return null;
        });
    }

    private CreateExpenseRequestDTO equalSplitExpense(Group group, User payer, User other) {
        CreateExpenseRequestDTO dto = new CreateExpenseRequestDTO();
        dto.setDescription("concurrency probe");
        dto.setTotalAmount(BigDecimal.valueOf(AMOUNT_MAJOR));
        dto.setCurrencyCode(currency.getCode());
        dto.setGroupId(group.getId());
        dto.setSplitType(SplitType.EQUAL);
        dto.setPayers(List.of(new ExpensePayerInputDTO(payer.getId(), BigDecimal.valueOf(AMOUNT_MAJOR))));
        dto.setParticipants(List.of(
                new ExpenseShareInputDTO(payer.getId(), null, null, null),
                new ExpenseShareInputDTO(other.getId(), null, null, null)));
        return dto;
    }

    /** Fresh aggregate over balances vs the stored rollup, per (user, currency). */
    private void assertTotalsMatchBalances(UUID userId) {
        Long expectedOwed = jdbc.queryForObject(
                "SELECT COALESCE(SUM(net_amount_minor) FILTER (WHERE net_amount_minor > 0), 0) "
                        + "FROM balances WHERE user_id = ?", Long.class, userId);
        Long expectedOwing = jdbc.queryForObject(
                "SELECT COALESCE(SUM(-net_amount_minor) FILTER (WHERE net_amount_minor < 0), 0) "
                        + "FROM balances WHERE user_id = ?", Long.class, userId);

        Long actualOwed = jdbc.queryForObject(
                "SELECT COALESCE(SUM(total_owed_minor), 0) FROM user_balance_totals WHERE user_id = ?",
                Long.class, userId);
        Long actualOwing = jdbc.queryForObject(
                "SELECT COALESCE(SUM(total_owing_minor), 0) FROM user_balance_totals WHERE user_id = ?",
                Long.class, userId);

        assertThat(actualOwed)
                .as("owed rollup must equal SUM(positive balances) for user %s", userId)
                .isEqualTo(expectedOwed);
        assertThat(actualOwing)
                .as("owing rollup must equal SUM(|negative balances|) for user %s", userId)
                .isEqualTo(expectedOwing);
    }

    // ------------------------------------------------------------------
    // Fixtures (committed so worker transactions can see them)
    // ------------------------------------------------------------------

    private User saveUser(String tag) {
        String email = tag + "-" + UUID.randomUUID() + "@test.local";
        return userRepository.save(User.builder()
                .firstName(tag)
                .lastName("test")
                .fullName(tag + " test")
                .email(email)
                .authProvider(AuthProvider.PASSWORD)
                .passwordHash("x")
                .emailVerified(true)
                .build());
    }

    private Group saveGroup(String name, User creator) {
        return groupRepository.save(Group.builder()
                .name(name)
                .defaultCurrency(currency)
                .type(GroupType.OTHER)
                .createdBy(creator)
                .memberCount(0)
                .build());
    }

    private void addMember(Group group, User user, GroupRole role) {
        membershipRepository.save(GroupMembership.builder()
                .group(group)
                .user(user)
                .role(role)
                .status(MembershipStatus.ACTIVE)
                .build());
    }

    @AfterEach
    void cleanup() {
        if (group1 == null || group2 == null) return;
        UUID g1 = group1.getId();
        UUID g2 = group2.getId();
        List<UUID> userIds = List.of(userA.getId(), userB.getId(), userC.getId());

        // Children first, then parents. Raw deletes bypass @SQLDelete soft-delete
        // on purpose — this is test teardown, we want the rows gone.
        jdbc.update("DELETE FROM expense_payers WHERE expense_id IN "
                + "(SELECT id FROM expenses WHERE group_id IN (?, ?))", g1, g2);
        jdbc.update("DELETE FROM expense_shares WHERE expense_id IN "
                + "(SELECT id FROM expenses WHERE group_id IN (?, ?))", g1, g2);
        jdbc.update("DELETE FROM balance_ledger WHERE group_id IN (?, ?)", g1, g2);
        jdbc.update("DELETE FROM balances WHERE group_id IN (?, ?)", g1, g2);
        deleteByUserIds("DELETE FROM user_balance_totals WHERE user_id IN (%s)", userIds);
        jdbc.update("DELETE FROM expenses WHERE group_id IN (?, ?)", g1, g2);
        jdbc.update("DELETE FROM group_memberships WHERE group_id IN (?, ?)", g1, g2);
        jdbc.update("DELETE FROM groups WHERE id IN (?, ?)", g1, g2);
        deleteByUserIds("DELETE FROM users WHERE id IN (%s)", userIds);
    }

    private void deleteByUserIds(String template, List<UUID> ids) {
        String placeholders = String.join(", ", java.util.Collections.nCopies(ids.size(), "?"));
        jdbc.update(String.format(template, placeholders), ids.toArray());
    }
}
