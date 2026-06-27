package com.drawguess.controller;

import com.drawguess.common.ApiResponse;
import com.drawguess.model.dto.GameRoomVO;
import com.drawguess.model.dto.UserInfoVO;
import com.drawguess.model.dto.WordVO;
import com.drawguess.model.entity.User;
import com.drawguess.service.AdminService;
import com.drawguess.service.OnlineUserService;
import com.drawguess.service.RoomService;
import com.drawguess.service.UserService;
import com.drawguess.service.WordService;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RolesAllowed({"ADMIN", "SUPER_ADMIN"})
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;
    private final WordService wordService;
    private final RoomService roomService;
    private final OnlineUserService onlineUserService;

    public AdminController(AdminService adminService, UserService userService,
                           WordService wordService, RoomService roomService,
                           OnlineUserService onlineUserService) {
        this.adminService = adminService;
        this.userService = userService;
        this.wordService = wordService;
        this.roomService = roomService;
        this.onlineUserService = onlineUserService;
    }

    // ---- 用户管理 ----

    @GetMapping("/users")
    public ApiResponse<List<UserInfoVO>> getAllUsers() {
        List<User> users = adminService.getAllUsers();
        List<UserInfoVO> vos = users.stream().map(UserInfoVO::fromEntity).toList();
        return ApiResponse.success(vos);
    }

    @GetMapping("/users/pending")
    public ApiResponse<List<UserInfoVO>> getPendingUsers() {
        List<User> users = userService.getPendingUsers();
        List<UserInfoVO> vos = users.stream().map(UserInfoVO::fromEntity).toList();
        return ApiResponse.success(vos);
    }

    @GetMapping("/users/rejected")
    public ApiResponse<List<UserInfoVO>> getRejectedUsers() {
        List<UserInfoVO> vos = adminService.getRejectedUsers();
        return ApiResponse.success(vos);
    }

    @PostMapping("/approve")
    public ApiResponse<Void> approveUser(@RequestBody Map<String, Long> body) {
        adminService.approveUser(body.get("userId"));
        return ApiResponse.success("审核通过");
    }

    @PostMapping("/reject")
    public ApiResponse<Void> rejectUser(@RequestBody Map<String, Long> body) {
        adminService.rejectUser(body.get("userId"));
        return ApiResponse.success("审核拒绝");
    }

    @DeleteMapping("/users/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ApiResponse.success("用户已注销");
    }

    // ---- 词库管理 ----

    @GetMapping("/words")
    public ApiResponse<List<WordVO>> getWords() {
        return ApiResponse.success(wordService.getAllWords());
    }

    @GetMapping("/words/count")
    public ApiResponse<Map<String, Object>> getWordCount() {
        return ApiResponse.success(Map.of("count", wordService.getWordCount()));
    }

    @PostMapping("/words")
    public ApiResponse<Void> addWord(@RequestBody Map<String, String> body) {
        wordService.addWord(body.get("word"));
        return ApiResponse.success("词语添加成功");
    }

    @PostMapping("/words/batch")
    public ApiResponse<Void> batchAddWords(@RequestBody Map<String, List<String>> body) {
        List<String> words = body.get("words");
        if (words == null || words.isEmpty()) {
            return ApiResponse.error(400, "词语列表不能为空");
        }
        wordService.batchAddWords(words);
        return ApiResponse.success("批量添加成功");
    }

    @DeleteMapping("/words/{wordId}")
    public ApiResponse<Void> deleteWord(@PathVariable Long wordId) {
        User currentUser = userService.getCurrentUser();
        wordService.deleteWord(wordId, currentUser.getId());
        return ApiResponse.success("词语删除成功");
    }

    @DeleteMapping("/words/batch")
    public ApiResponse<Void> batchDeleteWords(@RequestBody Map<String, List<?>> body) {
        List<?> rawIds = body.get("wordIds");
        if (rawIds == null || rawIds.isEmpty()) {
            return ApiResponse.error(400, "请选择要删除的词语");
        }
        List<Long> wordIds = rawIds.stream().map(id -> ((Number) id).longValue()).toList();
        User currentUser = userService.getCurrentUser();
        wordService.batchDeleteWords(wordIds, currentUser.getId());
        return ApiResponse.success("批量删除成功");
    }

    // ---- 房间管理 ----

    @GetMapping("/rooms")
    public ApiResponse<List<GameRoomVO>> getOnlineRooms() {
        return ApiResponse.success(roomService.getOnlineRooms());
    }

    @PostMapping("/rooms/{roomId}/pause")
    public ApiResponse<Void> pauseRoom(@PathVariable String roomId) {
        roomService.pauseRoom(roomId);
        return ApiResponse.success("房间已暂停");
    }

    @PostMapping("/rooms/{roomId}/resume")
    public ApiResponse<Void> resumeRoom(@PathVariable String roomId) {
        roomService.resumeRoom(roomId);
        return ApiResponse.success("房间已恢复");
    }

    @PostMapping("/rooms/{roomId}/dismiss")
    public ApiResponse<Void> dismissRoom(@PathVariable String roomId) {
        roomService.dismissRoom(roomId);
        return ApiResponse.success("房间已解散");
    }

    // ---- 统计信息 ----

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        return ApiResponse.success(adminService.getDashboardStats());
    }

    @GetMapping("/stats/online")
    public ApiResponse<Map<String, Object>> getOnlineStats() {
        long onlineCount = onlineUserService.getOnlineCount();
        return ApiResponse.success(Map.of(
                "onlineCount", onlineCount,
                "onlineUserIds", onlineUserService.getOnlineUserIds()
        ));
    }
}
