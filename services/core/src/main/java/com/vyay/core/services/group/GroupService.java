package com.vyay.core.services.group;

import com.vyay.core.common.utils.InviteCodeGenerator;
import com.vyay.core.dto.requests.group.CreateGroupRequestDTO;
import com.vyay.core.dto.response.group.GroupDetailDTO;
import com.vyay.core.entity.User;
import com.vyay.core.entity.balance.Balance;
import com.vyay.core.entity.group.Group;
import com.vyay.core.entity.group.GroupInviteLink;
import com.vyay.core.entity.group.GroupMembership;
import com.vyay.core.entity.group.GroupPreference;
import com.vyay.core.entity.reference.Currency;
import com.vyay.core.enums.GroupRole;
import com.vyay.core.enums.GroupType;
import com.vyay.core.enums.InviteLinkType;
import com.vyay.core.enums.MembershipStatus;
import com.vyay.core.exception.business.GroupNotFoundException;
import com.vyay.core.exception.business.InvalidCurrencyException;
import com.vyay.core.exception.business.NotAMemberException;
import com.vyay.core.repository.BalanceRepository;
import com.vyay.core.repository.CurrencyRepository;
import com.vyay.core.repository.GroupInviteLinkRepository;
import com.vyay.core.repository.GroupMembershipRepository;
import com.vyay.core.repository.GroupRepository;
import com.vyay.core.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final GroupInviteLinkRepository groupInviteLinkRepository;
    private final CurrencyRepository currencyRepository;
    private final UserRepository userRepository;
    private final BalanceRepository balanceRepository;
    private final String frontendBaseUrl;

    public GroupService(GroupRepository groupRepository,
                        GroupMembershipRepository groupMembershipRepository,
                        GroupInviteLinkRepository groupInviteLinkRepository,
                        CurrencyRepository currencyRepository,
                        UserRepository userRepository,
                        BalanceRepository balanceRepository,
                        @Value("${app.frontend.base-url}") String frontendBaseUrl) {
        this.groupRepository = groupRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.groupInviteLinkRepository = groupInviteLinkRepository;
        this.currencyRepository = currencyRepository;
        this.userRepository = userRepository;
        this.balanceRepository = balanceRepository;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Transactional
    public Group createGroup(User principal, CreateGroupRequestDTO request) {

        Currency currency = currencyRepository.findByCode(request.getDefaultCurrencyCode())
                .orElseThrow(() -> new InvalidCurrencyException(request.getDefaultCurrencyCode()));

        User creatorRef = userRepository.getReferenceById(principal.getId());

        Group group = Group.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .defaultCurrency(currency)
                .createdBy(creatorRef)
                .memberCount(1)
                .preferences(GroupPreference.defaults())
                .build();

        Group saved = groupRepository.save(group);

        GroupMembership creatorMembership = GroupMembership.builder()
                .group(saved)
                .user(creatorRef)
                .role(GroupRole.ADMIN)
                .status(MembershipStatus.ACTIVE)
                .build();
        groupMembershipRepository.save(creatorMembership);

        GroupInviteLink primaryLink = GroupInviteLink.builder()
                .group(saved)
                .code(InviteCodeGenerator.generate())
                .type(InviteLinkType.PRIMARY)
                .createdBy(creatorRef)
                .build();
        groupInviteLinkRepository.save(primaryLink);

        return saved;
    }

    /**
     * Branches on whether a type filter was supplied. See
     * GroupMembershipRepository#findGroupsByUserIdAndStatusAndType for why this is
     * two repository methods rather than one query with an inline null-check.
     */
    @Transactional(readOnly = true)
    public Page<Group> listMyGroups(User principal, GroupType type, Pageable pageable) {
        return type == null
                ? groupMembershipRepository.findGroupsByUserIdAndStatus(
                        principal.getId(), MembershipStatus.ACTIVE, pageable)
                : groupMembershipRepository.findGroupsByUserIdAndStatusAndType(
                        principal.getId(), MembershipStatus.ACTIVE, type, pageable);
    }

    @Transactional(readOnly = true)
    public GroupDetailDTO getGroupDetail(User principal, UUID groupId) {
        Group group = groupRepository.findById(groupId)
                .filter(g -> !g.isDeleted())
                .orElseThrow(GroupNotFoundException::new);

        GroupMembership me = groupMembershipRepository
                .findByGroupIdAndUserIdAndStatus(groupId, principal.getId(), MembershipStatus.ACTIVE)
                .orElseThrow(NotAMemberException::new);

        List<GroupMembership> members =
                groupMembershipRepository.findMembersByGroupIdAndStatus(groupId, MembershipStatus.ACTIVE);
        List<GroupInviteLink> invites =
                groupInviteLinkRepository.findByGroupIdAndIsActiveTrue(groupId);
        List<Balance> balances =
                balanceRepository.findByGroupIdWithCurrency(groupId);

        return GroupDetailDTO.from(group, me.getRole(), members, invites, balances, principal.getId(), frontendBaseUrl);
    }
}