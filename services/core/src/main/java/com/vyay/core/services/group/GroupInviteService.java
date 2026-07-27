package com.vyay.core.services.group;

import com.vyay.core.common.utils.InviteCodeGenerator;
import com.vyay.core.dto.requests.group.CreateInviteRequestDTO;
import com.vyay.core.dto.response.group.InviteResponseDTO;
import com.vyay.core.entity.User;
import com.vyay.core.entity.group.Group;
import com.vyay.core.entity.group.GroupInviteLink;
import com.vyay.core.entity.group.GroupMembership;
import com.vyay.core.enums.GroupRole;
import com.vyay.core.enums.InviteLinkType;
import com.vyay.core.enums.MembershipStatus;
import com.vyay.core.exception.business.AdminOnlyException;
import com.vyay.core.exception.business.GroupNotFoundException;
import com.vyay.core.exception.business.NotAMemberException;
import com.vyay.core.repository.GroupInviteLinkRepository;
import com.vyay.core.repository.GroupMembershipRepository;
import com.vyay.core.repository.GroupRepository;
import com.vyay.core.repository.UserRepository;
import com.vyay.core.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class GroupInviteService {

    private static final Duration PRIMARY_TOKEN_TTL =
            Duration.ofDays(3650L * 20); // ~200 years

    private final GroupRepository groupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final GroupInviteLinkRepository groupInviteLinkRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public GroupInviteService(GroupRepository groupRepository,
                              GroupMembershipRepository groupMembershipRepository,
                              GroupInviteLinkRepository groupInviteLinkRepository,
                              UserRepository userRepository,
                              JwtService jwtService) {
        this.groupRepository = groupRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.groupInviteLinkRepository = groupInviteLinkRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public InviteResponseDTO createInvite(User principal, UUID groupId, CreateInviteRequestDTO request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(GroupNotFoundException::new);

        GroupMembership me = groupMembershipRepository
                .findByGroupIdAndUserIdAndStatus(groupId, principal.getId(), MembershipStatus.ACTIVE)
                .orElseThrow(NotAMemberException::new);

        if (request.getType() == InviteLinkType.PRIMARY && me.getRole() != GroupRole.ADMIN) {
            throw new AdminOnlyException();
        }

        List<UUID> invited = request.getInvitedUserIds();
        if (invited != null) {
            invited = invited.stream()
                    .distinct()
                    .toList();
        }

        Instant expiresAt = request.getExpiresAt();
        Integer maxUses = request.getMaxUses();

        if (request.getType() == InviteLinkType.PRIMARY) {
            groupInviteLinkRepository.deactivateActiveLinksOfType(groupId, InviteLinkType.PRIMARY);
        }

        User creatorRef = userRepository.getReferenceById(principal.getId());

        GroupInviteLink link = groupInviteLinkRepository.save(GroupInviteLink.builder()
                .group(group)
                .code(InviteCodeGenerator.generate())
                .type(request.getType())
                .createdBy(creatorRef)
                .expiresAt(expiresAt)
                .maxUses(maxUses)
                .build());

        long ttl = (expiresAt != null)
                ? Math.max(0, expiresAt.toEpochMilli() - System.currentTimeMillis())
                : PRIMARY_TOKEN_TTL.toMillis();
        String token = jwtService.generateGroupInviteToken(link.getCode(), invited, ttl);

        return InviteResponseDTO.builder()
                .token(token)
                .type(link.getType())
                .expiresAt(link.getExpiresAt())
                .maxUses(link.getMaxUses())
                .useCount(link.getUseCount())
                .createdAt(link.getCreatedAt())
                .build();
    }
}
