package com.vyay.core.dto.response.group;

import com.vyay.core.dto.base.VersionedResponse;
import com.vyay.core.dto.response.User.UserSummaryDTO;
import com.vyay.core.entity.balance.Balance;
import com.vyay.core.entity.group.Group;
import com.vyay.core.entity.group.GroupInviteLink;
import com.vyay.core.entity.group.GroupMembership;
import com.vyay.core.entity.reference.Currency;
import com.vyay.core.enums.GroupRole;
import com.vyay.core.enums.GroupType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@SuperBuilder
public class GroupDetailDTO extends VersionedResponse {
    private UUID groupId;
    private String name;
    private String description;
    private GroupType type;
    private String defaultCurrencyCode;
    private UserSummaryDTO createdBy;
    private Integer memberCount;
    private GroupRole myRole;
    private List<MemberDTO> members;
    private List<InviteSummaryDTO> invites;
    private Instant createdAt;
    private Instant updatedAt;

    // Admins first, then members (any future role falls through after).
    // Within each role: case-insensitive alphabetical by displayName.
    private static final Comparator<MemberDTO> MEMBER_ORDER =
            Comparator.comparingInt((MemberDTO m) -> m.getRole() == GroupRole.ADMIN ? 0 : 1)
                    .thenComparing(MemberDTO::getDisplayName, String.CASE_INSENSITIVE_ORDER);

    public static GroupDetailDTO from(Group g, GroupRole myRole,
                                      List<GroupMembership> members,
                                      List<GroupInviteLink> invites,
                                      List<Balance> balances,
                                      UUID currentUserId,
                                      String frontendBaseUrl) {
        Map<UUID, List<CurrencyBalanceDTO>> balancesByUser =
                balancesByUser(members, balances, g.getDefaultCurrency());

        return GroupDetailDTO.builder()
                .version(g.getVersion())
                .groupId(g.getId())
                .name(g.getName())
                .description(g.getDescription())
                .type(g.getType())
                .defaultCurrencyCode(g.getDefaultCurrency().getCode())
                .createdBy(UserSummaryDTO.from(g.getCreatedBy()))
                .memberCount(g.getMemberCount())
                .myRole(myRole)
                .members(members.stream()
                        .map(m -> MemberDTO.from(m, currentUserId,
                                balancesByUser.getOrDefault(m.getUser().getId(), List.of())))
                        .sorted(MEMBER_ORDER)
                        .toList())
                .invites(invites.stream().map(i -> InviteSummaryDTO.from(i, frontendBaseUrl)).toList())
                .createdAt(g.getCreatedAt())
                .updatedAt(g.getUpdatedAt())
                .build();
    }

    /**
     * Per-member balances, zero-filled across the group's currency set.
     * "Group currencies" = the default currency plus any currency present on at
     * least one member's balance row, ordered default-first then alphabetically by
     * code. Every member reports the same currency set (0 where they have no row),
     * so the client never has to reconcile differing currency lists across members.
     */
    private static Map<UUID, List<CurrencyBalanceDTO>> balancesByUser(
            List<GroupMembership> members, List<Balance> balances, Currency defaultCurrency) {

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

        Map<UUID, List<CurrencyBalanceDTO>> result = new HashMap<>();
        for (GroupMembership m : members) {
            UUID userId = m.getUser().getId();
            Map<UUID, Long> userMinors = minorByUserThenCurrency.getOrDefault(userId, Map.of());
            List<CurrencyBalanceDTO> list = groupCurrencies.stream()
                    .map(c -> {
                        long minor = userMinors.getOrDefault(c.getId(), 0L);
                        return CurrencyBalanceDTO.builder()
                                .currencyCode(c.getCode())
                                .netAmountMinor(minor)
                                .netAmount(BigDecimal.valueOf(minor).movePointLeft(c.getDecimalPlaces()))
                                .build();
                    })
                    .toList();
            result.put(userId, list);
        }
        return result;
    }
}