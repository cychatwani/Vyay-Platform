package com.vyay.core.services.expense;

import com.github.f4b6a3.uuid.UuidCreator;
import com.vyay.core.common.security.HmacSigner;
import com.vyay.core.common.utils.MoneyUtils;
import com.vyay.core.dto.requests.expense.CreateExpenseRequestDTO;
import com.vyay.core.dto.requests.expense.ExpensePayerInputDTO;
import com.vyay.core.dto.requests.expense.ExpenseShareInputDTO;
import com.vyay.core.dto.response.expense.ExpenseResponseDTO;
import com.vyay.core.entity.User;
import com.vyay.core.entity.balance.BalanceLedgerEntry;
import com.vyay.core.entity.expense.Expense;
import com.vyay.core.entity.expense.ExpensePayer;
import com.vyay.core.entity.expense.ExpenseShare;
import com.vyay.core.entity.group.Group;
import com.vyay.core.entity.reference.Currency;
import com.vyay.core.enums.MembershipStatus;
import com.vyay.core.enums.SplitType;
import com.vyay.core.exception.business.GroupNotFoundException;
import com.vyay.core.exception.business.InvalidCurrencyException;
import com.vyay.core.exception.business.InvalidExpenseException;
import com.vyay.core.exception.business.NotAMemberException;
import com.vyay.core.repository.*;
import com.vyay.core.services.balance.BalanceUpdateService;
import com.vyay.core.services.balance.commands.BalanceUpdateCommandFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CurrencyRepository currencyRepository;
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final UserRepository userRepository;
    private final ExpenseSplitCalculator splitCalculator;
    private final ExpenseLineFactory lineFactory;
    private final BalanceRepository balanceRepository;
    private final BalanceLedgerRepository ledgerRepository;
    private final HmacSigner hmacSigner;
    private final BalanceUpdateService balanceUpdateService;

    public ExpenseService(ExpenseRepository expenseRepository,
                          CurrencyRepository currencyRepository,
                          GroupRepository groupRepository,
                          GroupMembershipRepository groupMembershipRepository,
                          UserRepository userRepository,
                          ExpenseSplitCalculator splitCalculator,
                          ExpenseLineFactory lineFactory,
                          BalanceRepository balanceRepository,
                          BalanceLedgerRepository ledgerRepository,
                          HmacSigner hmacSigner,
                          BalanceUpdateService balanceUpdateService ) {
        this.expenseRepository = expenseRepository;
        this.balanceUpdateService = balanceUpdateService;
        this.currencyRepository = currencyRepository;
        this.groupRepository = groupRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.userRepository = userRepository;
        this.splitCalculator = splitCalculator;
        this.lineFactory = lineFactory;
        this.balanceRepository = balanceRepository;
        this.ledgerRepository = ledgerRepository;
        this.hmacSigner = hmacSigner;
    }

    @Transactional
    public ExpenseResponseDTO createExpense(User principal, CreateExpenseRequestDTO request) {
        Currency currency = currencyRepository.findByCode(request.getCurrencyCode())
                .orElseThrow(() -> new InvalidCurrencyException(request.getCurrencyCode()));

        long total = MoneyUtils.toMinor(request.getTotalAmount(), currency);
        if (total <= 0) {
            throw new InvalidExpenseException("Total amount must be positive");
        }

        Group group = null;
        if (request.getGroupId() != null) {
            group = groupRepository.findById(request.getGroupId())
                    .filter(g -> !g.isDeleted())
                    .orElseThrow(GroupNotFoundException::new);
            requireActiveMember(group.getId(), principal.getId());
        }

        Expense expense = Expense.builder()
                .description(request.getDescription())
                .totalAmountMinor(total)
                .currency(currency)
                .group(group)
                .createdBy(userRepository.getReferenceById(principal.getId()))
                .splitType(group == null ? null : requireSplitType(request.getSplitType()))
                .expenseDate(request.getExpenseDate())
                .notes(request.getNotes())
                .build();

        if (group == null) {
            expense.getPayers().add(lineFactory.payer(expense, principal, total));
            expense.getShares().add(lineFactory.share(expense, principal, total, null, null));
        } else {
            if (request.getPayers() == null || request.getPayers().isEmpty()) {
                throw new InvalidExpenseException("A group expense needs at least one payer");
            }
            if (request.getParticipants() == null || request.getParticipants().isEmpty()) {
                throw new InvalidExpenseException("A split needs at least one participant");
            }

            Set<UUID> involvedUserIds = Stream.concat(
                    request.getPayers().stream().map(ExpensePayerInputDTO::getUserId),
                    request.getParticipants().stream().map(ExpenseShareInputDTO::getUserId)
            ).collect(Collectors.toUnmodifiableSet());

            List<UUID> existingMemberUserIds = groupMembershipRepository.findExistingMemberUserIds(group.getId(), involvedUserIds, MembershipStatus.ACTIVE);
            if (existingMemberUserIds.size() != involvedUserIds.size()) {
                throw new InvalidExpenseException("Some participants are not active members of the group");
            }

            buildPayers(expense, request.getPayers(), total, currency);
            buildShares(expense, request.getSplitType(), request.getParticipants(), total, currency);
        }

        Expense saved = expenseRepository.save(expense);

        if (saved.getGroup() != null) {
            balanceUpdateService.applyDeltas(BalanceUpdateCommandFactory.from(saved));
        }

        return ExpenseResponseDTO.from(saved);
    }

    private void buildPayers(Expense expense,
                             List<ExpensePayerInputDTO> inputs,
                             long total,
                             Currency currency) {

        if (inputs == null || inputs.isEmpty()) {
            throw new InvalidExpenseException("A group expense needs at least one payer");
        }

        Set<UUID> seen = new HashSet<>();
        long sum = 0;

        for (ExpensePayerInputDTO in : inputs) {

            if (!seen.add(in.getUserId())) {
                throw new InvalidExpenseException("Duplicate payer: " + in.getUserId());
            }

            long paid = MoneyUtils.toMinor(in.getAmountPaid(), currency);
            if (paid <= 0) {
                throw new InvalidExpenseException("Each payer must have a positive amount");
            }

            expense.getPayers().add(
                    ExpensePayer.builder()
                            .expense(expense)
                            .user(userRepository.getReferenceById(in.getUserId()))
                            .amountPaidMinor(paid)
                            .build()
            );

            sum += paid;
        }

        if (sum != total) {
            throw new InvalidExpenseException(
                    "Payer amounts (" + sum + ") must sum to the total (" + total + ")"
            );
        }
    }

    private void buildShares(Expense expense,
                             SplitType type,
                             List<ExpenseShareInputDTO> inputs,
                             long total,
                             Currency currency) {
        if (inputs == null || inputs.isEmpty()) {
            throw new InvalidExpenseException("A split needs at least one participant");
        }
        Set<UUID> seen = new HashSet<>();
        for (ExpenseShareInputDTO in : inputs) {
            if (!seen.add(in.getUserId())) {
                throw new InvalidExpenseException("Duplicate participant: " + in.getUserId());
            }
        }

        long[] owed = switch (type) {
            case EQUAL -> splitCalculator.equally(total, inputs.size());
            case EXACT -> splitCalculator.exact(
                    total,
                    exactMinors(inputs, currency)
            );
            case PERCENTAGE -> splitCalculator.byPercentage(
                    total,
                    inputs.stream()
                            .map(ExpenseShareInputDTO::getPercentage)
                            .toList()
            );
            case SHARES -> splitCalculator.byWeight(
                    total,
                    inputs.stream()
                            .map(ExpenseShareInputDTO::getShareWeight)
                            .toList()
            );
        };

        for (int i = 0; i < inputs.size(); i++) {
            ExpenseShareInputDTO input = inputs.get(i);
            BigDecimal percentage =
                    type == SplitType.PERCENTAGE ? input.getPercentage() : null;
            Integer shareWeight =
                    type == SplitType.SHARES ? input.getShareWeight() : null;
            expense.getShares().add(
                    ExpenseShare.builder()
                            .expense(expense)
                            .user(userRepository.getReferenceById(input.getUserId()))
                            .owedAmountMinor(owed[i])
                            .percentage(percentage)
                            .shareWeight(shareWeight)
                            .build()
            );
        }
    }

    private long[] exactMinors(List<ExpenseShareInputDTO> inputs, Currency currency) {
        long[] out = new long[inputs.size()];
        for (int i = 0; i < inputs.size(); i++) {
            BigDecimal amt = inputs.get(i).getExactAmount();
            if (amt == null) {
                throw new InvalidExpenseException("EXACT split needs an amount per participant");
            }
            out[i] = MoneyUtils.toMinor(amt, currency);
        }
        return out;
    }

    private SplitType requireSplitType(SplitType type) {
        if (type == null) {
            throw new InvalidExpenseException("splitType is required for a group expense");
        }
        return type;
    }

    private void requireActiveMember(UUID groupId, UUID userId) {
        if (!groupMembershipRepository.existsByGroupIdAndUserIdAndStatus(
                groupId,
                userId,
                MembershipStatus.ACTIVE)) {
            throw new NotAMemberException();
        }
    }
}