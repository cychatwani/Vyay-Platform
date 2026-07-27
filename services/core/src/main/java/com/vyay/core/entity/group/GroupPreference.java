package com.vyay.core.entity.group;

import java.util.List;

public record GroupPreference(
        String key,
        String label,
        Object value,          // polymorphic: Boolean for simplifyDebts, String for defaultSplitType
        String type,
        List<String> editableBy
) {
    public static List<GroupPreference> defaults() {
        return List.of(
                new GroupPreference("simplifyDebts", "Simplify group debts",
                        true, "BOOLEAN", List.of("ADMIN")),
                new GroupPreference("defaultSplitType", "Default split type",
                        "EQUAL", "STRING", List.of("ADMIN", "MEMBER")),
                new GroupPreference("autoIncludeAllMembers", "Auto-include all members in new expenses",
                        true, "BOOLEAN", List.of("ADMIN")),
                new GroupPreference("autoSettle", "Auto-confirm settlements",
                        false, "BOOLEAN", List.of("ADMIN")),
                new GroupPreference("thirdPartySettlementPolicy", "Who can record settlements for others",
                        "DISABLED", "ENUM", List.of("ADMIN"))
        );
    }
}