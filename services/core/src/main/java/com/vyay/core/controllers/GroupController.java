package com.vyay.core.controllers;

import com.vyay.core.dto.requests.group.CreateGroupRequestDTO;
import com.vyay.core.dto.requests.group.CreateInviteRequestDTO;
import com.vyay.core.dto.response.group.GroupDetailDTO;
import com.vyay.core.dto.response.group.GroupResponseDTO;
import com.vyay.core.dto.response.group.GroupSummaryDTO;
import com.vyay.core.dto.response.group.InviteResponseDTO;
import com.vyay.core.dto.wrapper.ApiResponse;
import com.vyay.core.dto.wrapper.PagedResponse;
import com.vyay.core.entity.User;
import com.vyay.core.entity.group.Group;
import com.vyay.core.enums.GroupType;
import com.vyay.core.services.group.GroupInviteService;
import com.vyay.core.services.group.GroupMembershipService;
import com.vyay.core.services.group.GroupService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/groups")
public class GroupController {

    private final GroupService groupService;
    private final GroupInviteService groupInviteService;
    private final GroupMembershipService groupMembershipService;

    public GroupController(GroupService groupService,
                           GroupInviteService groupInviteService,
                           GroupMembershipService groupMembershipService) {
        this.groupService = groupService;
        this.groupInviteService = groupInviteService;
        this.groupMembershipService = groupMembershipService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GroupResponseDTO>> createGroup(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateGroupRequestDTO request) {
        Group group = groupService.createGroup(user, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(GroupResponseDTO.from(group, user), "Group created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<GroupSummaryDTO>>> listMyGroups(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) GroupType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Group> groups = groupService.listMyGroups(user, type, pageable);
        return ResponseEntity.ok(
                ApiResponse.success(PagedResponse.from(groups, GroupSummaryDTO::from)));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<GroupDetailDTO>> getGroup(
            @AuthenticationPrincipal User user,
            @PathVariable UUID groupId) {
        return ResponseEntity.ok(ApiResponse.success(groupService.getGroupDetail(user, groupId)));
    }

    @PostMapping("/{groupId}/invites")
    public ResponseEntity<ApiResponse<InviteResponseDTO>> createInvite(
            @AuthenticationPrincipal User user,
            @PathVariable UUID groupId,
            @Valid @RequestBody CreateInviteRequestDTO request) {
        InviteResponseDTO invite = groupInviteService.createInvite(user, groupId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(invite, "Invite created"));
    }

    @PostMapping("/{groupId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveGroup(
            @AuthenticationPrincipal User user,
            @PathVariable UUID groupId) {
        groupMembershipService.leave(user, groupId);
        return ResponseEntity.ok(ApiResponse.success(null, "You have left the group"));
    }
}
