package com.drawguess.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drawguess.common.BusinessException;
import com.drawguess.common.ResultCode;
import com.drawguess.mapper.FriendMapper;
import com.drawguess.mapper.UserMapper;
import com.drawguess.model.dto.FriendRequestVO;
import com.drawguess.model.dto.UserInfoVO;
import com.drawguess.model.entity.Friend;
import com.drawguess.model.entity.User;
import com.drawguess.model.enums.FriendStatus;
import com.drawguess.model.enums.UserStatus;
import com.drawguess.service.FriendService;
import com.drawguess.service.OnlineUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class FriendServiceImpl implements FriendService {

    private static final Logger log = LoggerFactory.getLogger(FriendServiceImpl.class);

    private final FriendMapper friendMapper;
    private final UserMapper userMapper;
    private final OnlineUserService onlineUserService;

    public FriendServiceImpl(FriendMapper friendMapper, UserMapper userMapper, OnlineUserService onlineUserService) {
        this.friendMapper = friendMapper;
        this.userMapper = userMapper;
        this.onlineUserService = onlineUserService;
    }

    @Override
    public void sendFriendRequest(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new BusinessException(ResultCode.CANNOT_ADD_SELF);
        }

        User friend = userMapper.selectById(friendId);
        if (friend == null || friend.getStatus() != UserStatus.APPROVED) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 检查是否已有好友关系（双向）
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<Friend>()
                .and(w -> w
                        .eq(Friend::getUserId, userId).eq(Friend::getFriendId, friendId)
                        .or()
                        .eq(Friend::getUserId, friendId).eq(Friend::getFriendId, userId)
                );
        Friend existing = friendMapper.selectOne(wrapper);
        if (existing != null) {
            if (existing.getStatus() == FriendStatus.ACCEPTED) {
                throw new BusinessException(ResultCode.ALREADY_FRIENDS);
            }
            throw new BusinessException(ResultCode.FRIEND_REQUEST_EXISTS);
        }

        Friend request = new Friend();
        request.setUserId(userId);
        request.setFriendId(friendId);
        request.setStatus(FriendStatus.PENDING);
        friendMapper.insert(request);
        log.info("好友请求发送: userId={}, friendId={}", userId, friendId);
    }

    @Override
    public List<FriendRequestVO> getFriendRequests(Long userId) {
        List<Friend> requests = friendMapper.selectList(
                new LambdaQueryWrapper<Friend>()
                        .eq(Friend::getFriendId, userId)
                        .eq(Friend::getStatus, FriendStatus.PENDING)
        );
        return requests.stream().map(f -> {
            User fromUser = userMapper.selectById(f.getUserId());
            return fromUser != null ? FriendRequestVO.fromEntity(f, fromUser) : null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public void acceptFriendRequest(Long requestId, Long userId) {
        Friend request = friendMapper.selectById(requestId);
        if (request == null || !request.getFriendId().equals(userId)) {
            throw new BusinessException(ResultCode.FRIEND_REQUEST_NOT_FOUND);
        }
        if (request.getStatus() != FriendStatus.PENDING) {
            throw new BusinessException(ResultCode.FRIEND_REQUEST_NOT_FOUND);
        }
        request.setStatus(FriendStatus.ACCEPTED);
        friendMapper.updateById(request);
        log.info("好友请求通过: requestId={}", requestId);
    }

    @Override
    public void rejectFriendRequest(Long requestId, Long userId) {
        Friend request = friendMapper.selectById(requestId);
        if (request == null || !request.getFriendId().equals(userId)) {
            throw new BusinessException(ResultCode.FRIEND_REQUEST_NOT_FOUND);
        }
        if (request.getStatus() != FriendStatus.PENDING) {
            throw new BusinessException(ResultCode.FRIEND_REQUEST_NOT_FOUND);
        }
        request.setStatus(FriendStatus.REJECTED);
        friendMapper.updateById(request);
        log.info("好友请求拒绝: requestId={}", requestId);
    }

    @Override
    public List<UserInfoVO> getFriendList(Long userId) {
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<Friend>()
                .eq(Friend::getStatus, FriendStatus.ACCEPTED)
                .and(w -> w.eq(Friend::getUserId, userId).or().eq(Friend::getFriendId, userId));

        List<Friend> friends = friendMapper.selectList(wrapper);
        return friends.stream().map(f -> {
            Long friendUserId = f.getUserId().equals(userId) ? f.getFriendId() : f.getUserId();
            User friendUser = userMapper.selectById(friendUserId);
            if (friendUser == null) return null;
            UserInfoVO vo = UserInfoVO.fromEntity(friendUser);
            vo.setOnlineStatus(onlineUserService.isOnline(friendUserId));
            return vo;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public void deleteFriend(Long userId, Long friendId) {
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<Friend>()
                .eq(Friend::getStatus, FriendStatus.ACCEPTED)
                .and(w -> w
                        .eq(Friend::getUserId, userId).eq(Friend::getFriendId, friendId)
                        .or()
                        .eq(Friend::getUserId, friendId).eq(Friend::getFriendId, userId)
                );
        friendMapper.delete(wrapper);
        log.info("好友删除: userId={}, friendId={}", userId, friendId);
    }

    @Override
    public List<UserInfoVO> searchUsers(String keyword, Long currentUserId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getStatus, UserStatus.APPROVED)
                .ne(User::getId, currentUserId)
                .and(w -> w.like(User::getNickname, keyword).or().like(User::getPhone, keyword));
        List<User> users = userMapper.selectList(wrapper);
        return users.stream().map(UserInfoVO::fromEntity).collect(Collectors.toList());
    }
}
