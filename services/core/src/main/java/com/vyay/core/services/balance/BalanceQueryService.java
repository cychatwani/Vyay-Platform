package com.vyay.core.services.balance;

import com.vyay.core.dto.response.balance.UserBalanceResponseDTO;
import com.vyay.core.repository.UserBalanceTotalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Read side for user balances. Serves /me/balances straight off the
 * user_balance_totals rollup — one indexed read per user, no aggregation at
 * query time.
 */
@Service
public class BalanceQueryService {

    private final UserBalanceTotalRepository userBalanceTotalRepository;

    public BalanceQueryService(UserBalanceTotalRepository userBalanceTotalRepository) {
        this.userBalanceTotalRepository = userBalanceTotalRepository;
    }

    @Transactional(readOnly = true)
    public List<UserBalanceResponseDTO> getMyBalances(UUID userId) {
        return userBalanceTotalRepository.findByUserIdWithCurrency(userId).stream()
                .map(UserBalanceResponseDTO::from)
                .toList();
    }
}
