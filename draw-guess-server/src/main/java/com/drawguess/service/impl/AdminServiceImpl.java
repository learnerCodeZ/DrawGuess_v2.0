package com.drawguess.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drawguess.mapper.RoomMapper;
import com.drawguess.mapper.UserMapper;
import com.drawguess.mapper.WordMapper;
import com.drawguess.model.dto.UserInfoVO;
import com.drawguess.model.entity.Room;
import com.drawguess.model.entity.User;
import com.drawguess.model.enums.RoomState;
import com.drawguess.model.enums.UserStatus;
import com.drawguess.service.AdminService;
import com.drawguess.service.GameService;
import com.drawguess.service.OnlineUserService;
import com.drawguess.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final UserService userService;
    private final RoomMapper roomMapper;
    private final WordMapper wordMapper;
    private final GameService gameService;
    private final OnlineUserService onlineUserService;

    public AdminServiceImpl(UserMapper userMapper, UserService userService,
                            RoomMapper roomMapper, WordMapper wordMapper,
                            GameService gameService, OnlineUserService onlineUserService) {
        this.userMapper = userMapper;
        this.userService = userService;
        this.roomMapper = roomMapper;
        this.wordMapper = wordMapper;
        this.gameService = gameService;
        this.onlineUserService = onlineUserService;
    }

    @Override
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @Override
    public List<UserInfoVO> getRejectedUsers() {
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getStatus, UserStatus.REJECTED)
        );
        return users.stream().map(UserInfoVO::fromEntity).collect(Collectors.toList());
    }

    @Override
    public void approveUser(Long userId) {
        userService.approveUser(userId);
    }

    @Override
    public void rejectUser(Long userId) {
        userService.rejectUser(userId);
    }

    @Override
    public void deleteUser(Long userId) {
        userService.deleteUser(userId);
    }

    @Override
    public Map<String, Object> getDashboardStats() {
        long totalUsers = userMapper.selectCount(null);
        long pendingUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getStatus, UserStatus.PENDING));
        long approvedUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getStatus, UserStatus.APPROVED));
        long activeRooms = roomMapper.selectCount(
                new LambdaQueryWrapper<Room>().ne(Room::getState, RoomState.ENDED));
        long playingGames = gameService.getActiveRoomIds().size();
        long totalWords = wordMapper.selectCount(null);
        long onlineUsers = onlineUserService.getOnlineCount();

        return Map.of(
                "totalUsers", totalUsers,
                "pendingUsers", pendingUsers,
                "approvedUsers", approvedUsers,
                "activeRooms", activeRooms,
                "playingGames", playingGames,
                "totalWords", totalWords,
                "onlineUsers", onlineUsers
        );
    }
}
