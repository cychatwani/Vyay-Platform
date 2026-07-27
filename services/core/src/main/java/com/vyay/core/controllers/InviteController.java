package com.vyay.core.controllers;

import com.vyay.core.dto.requests.group.JoinGroupRequestDTO;
import com.vyay.core.dto.response.group.GroupDetailDTO;
import com.vyay.core.dto.wrapper.ApiResponse;
import com.vyay.core.entity.User;
import com.vyay.core.services.group.GroupMembershipService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/invites")
public class InviteController {

    private final GroupMembershipService groupMembershipService;

    public InviteController(GroupMembershipService groupMembershipService) {
        this.groupMembershipService = groupMembershipService;
    }

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<GroupDetailDTO>> join(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody JoinGroupRequestDTO request) {
        GroupDetailDTO detail = groupMembershipService.join(user, request.getToken());
        return ResponseEntity.ok(ApiResponse.success(detail, "Joined group successfully"));
    }
}