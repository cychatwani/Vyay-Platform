package com.vyay.core.services.group;

import com.vyay.core.dto.response.group.GroupDetailDTO;
import com.vyay.core.entity.User;
import com.vyay.core.entity.group.Group;
import com.vyay.core.entity.group.GroupInviteLink;
import com.vyay.core.entity.group.GroupMembership;
import com.vyay.core.enums.GroupRole;
import com.vyay.core.enums.MembershipStatus;
import com.vyay.core.exception.business.AdminCannotLeaveException;
import com.vyay.core.exception.business.AlreadyAMemberException;
import com.vyay.core.exception.business.GroupNotFoundException;
import com.vyay.core.exception.business.InviteLinkExhaustedException;
import com.vyay.core.exception.business.InviteLinkExpiredException;
import com.vyay.core.exception.business.InviteLinkInactiveException;
import com.vyay.core.exception.business.InviteLinkNotFoundException;
import com.vyay.core.exception.business.NotAMemberException;
import com.vyay.core.exception.business.UserNotInvitedException;
import com.vyay.core.repository.GroupInviteLinkRepository;
import com.vyay.core.repository.GroupMembershipRepository;
import com.vyay.core.repository.GroupRepository;
import com.vyay.core.repository.UserRepository;
import com.vyay.core.security.JwtService;
import com.vyay.core.security.TokenClaims;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class GroupMembershipService {

    private final JwtService jwtService;
    private final GroupRepository groupRepository;
    private final GroupInviteLinkRepository groupInviteLinkRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final UserRepository userRepository;
    private final GroupService groupService;

    public GroupMembershipService(JwtService jwtService,
                                  GroupRepository groupRepository,
                                  GroupInviteLinkRepository groupInviteLinkRepository,
                                  GroupMembershipRepository groupMembershipRepository,
                                  UserRepository userRepository,
                                  GroupService groupService) {
        this.jwtService = jwtService;
        this.groupRepository = groupRepository;
        this.groupInviteLinkRepository = groupInviteLinkRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.userRepository = userRepository;
        this.groupService = groupService;
    }

    @Transactional
    public GroupDetailDTO join(User principal, String token) {
        TokenClaims.GroupInvite invite = jwtService.parseGroupInvite(token);

        GroupInviteLink link = groupInviteLinkRepository.findByCode(invite.code())
                .orElseThrow(InviteLinkNotFoundException::new);

        if (!link.isActive()) {
            throw new InviteLinkInactiveException();
        }
        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(Instant.now())) {
            throw new InviteLinkExpiredException();
        }
        if (!invite.allows(principal.getId())) {
            throw new UserNotInvitedException();
        }

        Group group = link.getGroup();
        if (group.isDeleted()) {
            throw new GroupNotFoundException();
        }
        UUID groupId = group.getId();

        GroupMembership existing = groupMembershipRepository
                .findAnyByGroupIdAndUserId(groupId, principal.getId())
                .orElse(null);

        if (existing != null && existing.getStatus() == MembershipStatus.ACTIVE && existing.getDeletedAt() == null) {
            throw new AlreadyAMemberException("You are already a member of this group");
        }

        if (groupInviteLinkRepository.incrementUseCountIfAvailable(link.getId()) == 0) {
            throw new InviteLinkExhaustedException();
        }

        if (existing != null) {
            groupMembershipRepository.reactivateMembership(groupId, principal.getId());
        } else {
            User userRef = userRepository.getReferenceById(principal.getId());
            groupMembershipRepository.save(GroupMembership.builder()
                    .group(group)
                    .user(userRef)
                    .role(GroupRole.MEMBER)
                    .status(MembershipStatus.ACTIVE)
                    .build());
        }

        groupRepository.incrementMemberCount(groupId);

        return groupService.getGroupDetail(principal, groupId);
    }

    @Transactional
    public void leave(User principal, UUID groupId) {
        GroupMembership me = groupMembershipRepository
                .findByGroupIdAndUserIdAndStatus(groupId, principal.getId(), MembershipStatus.ACTIVE)
                .orElseThrow(NotAMemberException::new);

        if (me.getRole() == GroupRole.ADMIN) {
            throw new AdminCannotLeaveException();
        }

        // Single atomic update: stamps deleted_at + status=LEFT + left_at in one statement.
        // Returns 0 if the row was already left/deleted by a concurrent tx — treat as not-a-member.
        int updated = groupMembershipRepository.leaveMembership(groupId, principal.getId());
        if (updated == 0) {
            throw new NotAMemberException();
        }

        groupRepository.decrementMemberCount(groupId);
    }
}