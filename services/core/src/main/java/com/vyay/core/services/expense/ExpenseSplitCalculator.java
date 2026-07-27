package com.vyay.core.services.expense;

import com.vyay.core.exception.business.InvalidExpenseException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class ExpenseSplitCalculator {

    public long[] equally(long total, int participants) {
        if (participants <= 0) {
            throw new InvalidExpenseException("A split needs at least one participant");
        }
        long base = total / participants;
        long remainder = total - base * participants;        // 0 .. participants-1
        long[] owed = new long[participants];
        for (int i = 0; i < participants; i++) {
            owed[i] = base + (i < remainder ? 1 : 0);          // leftover paise to the first participants
        }
        return owed;
    }

    public long[] exact(long total, long[] amountsMinor) {
        long sum = 0;
        for (long a : amountsMinor) {
            if (a < 0) {
                throw new InvalidExpenseException("EXACT split amounts cannot be negative");
            }
            sum += a;
        }
        if (sum != total) {
            throw new InvalidExpenseException("Exact shares (" + sum + ") must sum to the total (" + total + ")");
        }
        return amountsMinor.clone();
    }

    public long[] byPercentage(long total, List<BigDecimal> percentages) {
        BigDecimal pctSum = BigDecimal.ZERO;
        long[] owed = new long[percentages.size()];
        long allocated = 0;
        for (int i = 0; i < percentages.size(); i++) {
            BigDecimal pct = percentages.get(i);
            if (pct == null || pct.signum() <= 0) {
                throw new InvalidExpenseException("PERCENTAGE split needs a positive percentage per participant");
            }
            pctSum = pctSum.add(pct);
            owed[i] = BigDecimal.valueOf(total).multiply(pct)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR).longValue();
            allocated += owed[i];
        }
        if (pctSum.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new InvalidExpenseException("Percentages must sum to 100 (got " + pctSum.toPlainString() + ")");
        }
        distributeRemainder(owed, total - allocated);
        return owed;
    }

    public long[] byWeight(long total, List<Integer> weights) {
        long totalWeight = 0;
        for (Integer w : weights) {
            if (w == null || w <= 0) {
                throw new InvalidExpenseException("SHARES split needs a positive weight per participant");
            }
            totalWeight += w;
        }
        long[] owed = new long[weights.size()];
        long allocated = 0;
        for (int i = 0; i < weights.size(); i++) {
            owed[i] = total * weights.get(i) / totalWeight;    // floor
            allocated += owed[i];
        }
        distributeRemainder(owed, total - allocated);
        return owed;
    }

    // Leftover minor units handed one-at-a-time to the first participants, in order.
    private void distributeRemainder(long[] owed, long remainder) {
        for (int i = 0; i < remainder && i < owed.length; i++) {
            owed[i] += 1;
        }
    }
}