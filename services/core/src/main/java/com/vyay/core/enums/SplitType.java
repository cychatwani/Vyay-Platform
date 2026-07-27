package com.vyay.core.enums;

public enum SplitType {
    EQUAL,       // total divided evenly; leftover minor unit allocated deterministically
    EXACT,       // each share's owedAmountMinor given directly; must sum to the total
    PERCENTAGE,  // each share carries a percentage; owedAmountMinor resolved from it
    SHARES       // each share carries a weight; owedAmountMinor resolved pro-rata
}