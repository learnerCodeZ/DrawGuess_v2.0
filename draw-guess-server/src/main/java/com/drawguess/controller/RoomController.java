package com.drawguess.controller;

import com.drawguess.common.ApiResponse;
import com.drawguess.model.dto.GameRoomVO;
import com.drawguess.model.entity.User;
import com.drawguess.service.RoomService;
import com.drawguess.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final UserService userService;

    public RoomController(RoomService roomService, UserService userService) {
        this.roomService = roomService;
        this.userService = userService;
    }

    @PostMapping
    public ApiResponse<GameRoomVO> createRoom() {
        User currentUser = userService.getCurrentUser();
        var room = roomService.createRoom(currentUser.getId());
        return ApiResponse.success(roomService.getRoomInfo(room.getRoomId()));
    }

    @GetMapping("/{roomId}")
    public ApiResponse<GameRoomVO> getRoomInfo(@PathVariable String roomId) {
        return ApiResponse.success(roomService.getRoomInfo(roomId));
    }

    @PostMapping("/{roomId}/join")
    public ApiResponse<Void> joinRoom(@PathVariable String roomId) {
        User currentUser = userService.getCurrentUser();
        roomService.joinRoom(roomId, currentUser.getId());
        return ApiResponse.success("已加入房间");
    }

    @PostMapping("/{roomId}/leave")
    public ApiResponse<Void> leaveRoom(@PathVariable String roomId) {
        User currentUser = userService.getCurrentUser();
        roomService.leaveRoom(roomId, currentUser.getId());
        return ApiResponse.success("已离开房间");
    }

    @DeleteMapping("/{roomId}")
    public ApiResponse<Void> dissolveRoom(@PathVariable String roomId) {
        User currentUser = userService.getCurrentUser();
        roomService.dissolveRoom(roomId, currentUser.getId());
        return ApiResponse.success("房间已解散");
    }
}
