package com.gamenight.controller;

import com.gamenight.dto.GroupDtos.CreateGroupRequest;
import com.gamenight.dto.GroupDtos.GroupDetails;
import com.gamenight.dto.GroupDtos.GroupSummary;
import com.gamenight.dto.GroupDtos.JoinGroupRequest;
import com.gamenight.dto.LibraryDtos.GameResponse;
import com.gamenight.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {
    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public List<GroupSummary> getGroups() {
        return groupService.getMyGroups();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupSummary create(@Valid @RequestBody CreateGroupRequest request) {
        return groupService.create(request.name());
    }

    @PostMapping("/join")
    public GroupSummary join(@Valid @RequestBody JoinGroupRequest request) {
        return groupService.join(request.joinCode());
    }

    @GetMapping("/{groupId}")
    public GroupDetails getDetails(@PathVariable Long groupId) {
        return groupService.getDetails(groupId);
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long groupId, @PathVariable Long userId) {
        groupService.removeMember(groupId, userId);
    }

    @PostMapping("/{groupId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable Long groupId) {
        groupService.leave(groupId);
    }

    @GetMapping("/{groupId}/shared-games")
    public List<GameResponse> getSharedGames(
            @PathVariable Long groupId,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Boolean multiplayerSupport,
            @RequestParam(required = false) Integer minPlayers) {
        return groupService.getSharedGames(groupId, genre, multiplayerSupport, minPlayers);
    }
}
