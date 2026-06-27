package com.drawguess.controller;

import com.drawguess.common.ApiResponse;
import com.drawguess.model.dto.FriendRequestVO;
import com.drawguess.model.dto.UserInfoVO;
import com.drawguess.model.entity.User;
import com.drawguess.service.FriendService;
import com.drawguess.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;
    private final UserService userService;

    public FriendController(FriendService friendService, UserService userService) {
        this.friendService = friendService;
        this.userService = userService;
    }

    @GetMapping("/search")
    public ApiResponse<List<UserInfoVO>> searchUsers(@RequestParam String q) {
        User currentUser = userService.getCurrentUser();
        return ApiResponse.success(friendService.searchUsers(q, currentUser.getId()));
    }

    @PostMapping("/request")
    public ApiResponse<Void> sendFriendRequest(@RequestBody Map<String, Long> body) {
        User currentUser = userService.getCurrentUser();
        friendService.sendFriendRequest(currentUser.getId(), body.get("friendId"));
        return ApiResponse.success("好友请求已发送");
    }

    @GetMapping("/requests")
    public ApiResponse<List<FriendRequestVO>> getFriendRequests() {
        User currentUser = userService.getCurrentUser();
        return ApiResponse.success(friendService.getFriendRequests(currentUser.getId()));
    }

    @PostMapping("/accept")
    public ApiResponse<Void> acceptFriendRequest(@RequestBody Map<String, Long> body) {
        User currentUser = userService.getCurrentUser();
        friendService.acceptFriendRequest(body.get("requestId"), currentUser.getId());
        return ApiResponse.success("已同意好友请求");
    }

    @PostMapping("/reject")
    public ApiResponse<Void> rejectFriendRequest(@RequestBody Map<String, Long> body) {
        User currentUser = userService.getCurrentUser();
        friendService.rejectFriendRequest(body.get("requestId"), currentUser.getId());
        return ApiResponse.success("已拒绝好友请求");
    }

    @GetMapping
    public ApiResponse<List<UserInfoVO>> getFriendList() {
        User currentUser = userService.getCurrentUser();
        return ApiResponse.success(friendService.getFriendList(currentUser.getId()));
    }

    @DeleteMapping("/{friendId}")
    public ApiResponse<Void> deleteFriend(@PathVariable Long friendId) {
        User currentUser = userService.getCurrentUser();
        friendService.deleteFriend(currentUser.getId(), friendId);
        return ApiResponse.success("已删除好友");
    }
}
