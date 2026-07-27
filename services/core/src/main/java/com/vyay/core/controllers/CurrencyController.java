package com.vyay.core.controllers;

import com.vyay.core.dto.response.currency.CurrencyResponseDTO;
import com.vyay.core.dto.wrapper.ApiResponse;
import com.vyay.core.services.currency.CurrencyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/currencies")
public class CurrencyController {

    private final CurrencyService currencyService;

    public CurrencyController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @GetMapping
    public ResponseEntity<?> getAllCurrencies(
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {

        String etag = currencyService.getEtag();

        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(304).build();
        }

        List<CurrencyResponseDTO> currencies = currencyService.getAllCurrencies().stream()
                .map(CurrencyResponseDTO::from)
                .toList();

        return ResponseEntity.ok()
                .eTag(etag)
                .body(ApiResponse.success(currencies));
    }

    @PostMapping("/evict-cache")
    public ResponseEntity<ApiResponse<Void>> evictCache() {
        currencyService.evictCache();
        return ResponseEntity.ok(ApiResponse.success(null, "Currency cache cleared."));
    }
}