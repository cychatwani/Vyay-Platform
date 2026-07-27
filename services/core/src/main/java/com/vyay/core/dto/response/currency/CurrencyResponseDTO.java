package com.vyay.core.dto.response.currency;

import com.vyay.core.entity.reference.Currency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CurrencyResponseDTO {

    private String code;
    private String name;
    private String symbol;
    private int decimalPlaces;

    public static CurrencyResponseDTO from(Currency currency) {
        return CurrencyResponseDTO.builder()
                .code(currency.getCode())
                .name(currency.getName())
                .symbol(currency.getSymbol())
                .decimalPlaces(currency.getDecimalPlaces())
                .build();
    }
}