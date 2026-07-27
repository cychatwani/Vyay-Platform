package com.vyay.core.services.balance;

import com.github.f4b6a3.uuid.UuidCreator;
import com.vyay.core.common.security.HmacSigner;
import com.vyay.core.entity.balance.BalanceLedgerEntry;
import com.vyay.core.repository.BalanceLedgerRepository;
import com.vyay.core.repository.BalanceRepository;
import com.vyay.core.repository.UserRepository;
import com.vyay.core.services.balance.commands.BalanceUpdateCommand;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BalanceUpdateService {

    private final BalanceRepository balanceRepository;
    private final BalanceLedgerRepository ledgerRepository;
    private final UserRepository userRepository;
    private final HmacSigner hmacSigner;

    public BalanceUpdateService(BalanceRepository balanceRepository,
                                BalanceLedgerRepository ledgerRepository,
                                UserRepository userRepository,
                                HmacSigner hmacSigner) {        
        this.balanceRepository = balanceRepository;
        this.ledgerRepository = ledgerRepository;
        this.userRepository = userRepository;
        this.hmacSigner = hmacSigner;
    }

    @Transactional
    public void applyDeltas(BalanceUpdateCommand command) {
        var entries = command.getUserDeltas().entrySet().stream().toList();
        if (entries.isEmpty()) return;

        UUID groupId = command.getGroup().getId();
        UUID currencyId = command.getCurrency().getId();

        int n = entries.size();
        UUID[] ids = new UUID[n], groupIds = new UUID[n], userIds = new UUID[n], currencyIds = new UUID[n];
        Long[] deltas = new Long[n];
        List<BalanceLedgerEntry> ledgerEntries = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            var e = entries.get(i);
            ids[i] = UuidCreator.getTimeOrderedEpoch();
            groupIds[i] = groupId;
            userIds[i] = e.getKey();
            currencyIds[i] = currencyId;
            deltas[i] = e.getValue();

            ledgerEntries.add(hmacSigner.signedEntry(
                    command.getGroup(),
                    userRepository.getReferenceById(e.getKey()),   // proxy — .getId() won't trigger a load
                    command.getCurrency(),
                    e.getValue(),
                    command.getSourceType(),
                    command.getSourceId()));
        }

        balanceRepository.applyDeltasBatch(ids, groupIds, userIds, currencyIds, deltas);
        ledgerRepository.saveAll(ledgerEntries);
    }
}