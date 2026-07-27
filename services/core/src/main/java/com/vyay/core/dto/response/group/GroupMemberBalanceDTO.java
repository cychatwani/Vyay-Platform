package com.vyay.core.dto.response.group;

import com.vyay.core.entity.balance.Balance;
import com.vyay.core.entity.group.GroupMembership;
import com.vyay.core.entity.reference.Currency;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Value
@Builder
public class GroupMemberBalanceDTO {

    UUID userId;
    List<CurrencyBalanceDTO> balances;

    /**
     * One entry per member, zero-filled across every currency the group touches.
     * "Group currencies" = the default currency plus any currency present on at
     * least one member's balance row, ordered default-first then alphabetically by
     * code. A member with no row in a given currency reports 0 for it, so all
     * members expose the same currency set — the client joins these against
     * members[] by userId (identity fields intentionally live only there).
     */
    public static List<GroupMemberBalanceDTO> buildList(List<GroupMembership> members,
                                                        List<Balance> balances,
                                                        Currency defaultCurrency) {
        // Ordered group-currency set. Dedup by id, not object identity: the group's
        // defaultCurrency and the balances' currencies can be different instances.
        Map<UUID, Currency> currencyById = new LinkedHashMap<>();
        currencyById.put(defaultCurrency.getId(), defaultCurrency);
        balances.stream()
                .map(Balance::getCurrency)
                .sorted(Comparator.comparing(Currency::getCode))
                .forEach(c -> currencyById.putIfAbsent(c.getId(), c));
        List<Currency> groupCurrencies = new ArrayList<>(currencyById.values());

        // userId -> (currencyId -> netAmountMinor)
        Map<UUID, Map<UUID, Long>> minorByUserThenCurrency = new HashMap<>();
        for (Balance b : balances) {
            minorByUserThenCurrency
                    .computeIfAbsent(b.getUser().getId(), k -> new HashMap<>())
                    .put(b.getCurrency().getId(), b.getNetAmountMinor());
        }

        return members.stream()
                .map(m -> {
                    UUID userId = m.getUser().getId();
                    Map<UUID, Long> userMinors =
                            minorByUserThenCurrency.getOrDefault(userId, Map.of());
                    List<CurrencyBalanceDTO> balanceList = groupCurrencies.stream()
                            .map(c -> {
                                long minor = userMinors.getOrDefault(c.getId(), 0L);
                                return CurrencyBalanceDTO.builder()
                                        .currencyCode(c.getCode())
                                        .netAmountMinor(minor)
                                        .netAmount(BigDecimal.valueOf(minor)
                                                .movePointLeft(c.getDecimalPlaces()))
                                        .build();
                            })
                            .toList();
                    return GroupMemberBalanceDTO.builder()
                            .userId(userId)
                            .balances(balanceList)
                            .build();
                })
                .toList();
    }
} 