package com.vyay.core.services.currency;

import com.vyay.core.entity.reference.Currency;
import com.vyay.core.repository.CurrencyRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
public class CurrencyService {

    private final CurrencyRepository currencyRepository;

    public CurrencyService(CurrencyRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }

    @Cacheable("currencies")
    public List<Currency> getAllCurrencies() {
        return currencyRepository.findAll();
    }

    @Cacheable("currency-etag")
    public String getEtag() {
        return computeEtag(currencyRepository.findAll());
    }

    @CacheEvict(value = {"currencies", "currency-etag"}, allEntries = true)
    public void evictCache() {
        // clears both caches — next request repopulates from DB
    }

    private String computeEtag(List<Currency> currencies) {
        String raw = currencies.stream()
                .map(c -> c.getCode() + c.getName() + c.getSymbol() + c.getDecimalPlaces())
                .sorted()
                .reduce("", String::concat);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return "\"" + HexFormat.of().formatHex(hash).substring(0, 16) + "\"";
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}