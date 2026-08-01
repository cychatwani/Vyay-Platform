package com.vyay.events.v1;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * What one person owes towards an expense, and the input that produced it.
 *
 * {@code owedAmountMinor} is always the resolved figure, whatever the split type,
 * so a consumer that only needs the number never has to reimplement the split
 * maths. The other two are the raw inputs, kept because they are domain data a
 * projection may want to show back ("you: 30%") and are not recoverable from the
 * amount alone once the total changes.
 *
 * @param userId          whose share this is.
 * @param owedAmountMinor the resolved share in MINOR units. Always positive.
 * @param percentage      the percentage entered, for {@link SplitType#PERCENTAGE}.
 *                        Null for every other split type.
 * @param shareWeight     the weight entered, for {@link SplitType#SHARES}. Null for
 *                        every other split type.
 */
public record ShareAmount(
        UUID userId,
        long owedAmountMinor,
        BigDecimal percentage,
        Integer shareWeight) {
}
