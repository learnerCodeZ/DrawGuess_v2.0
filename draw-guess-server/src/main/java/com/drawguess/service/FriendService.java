package com.drawguess.service;

import com.drawguess.model.dto.FriendRequestVO;
import com.drawguess.model.dto.UserInfoVO;

import java.util.List;

public interface FriendService {

    void sendFriendRequest(Long userId, Long friendId);

    List<FriendRequestVO> getFriendRequests(Long userId);

    void acceptFriendRequest(Long requestId, Long userId);

    void rejectFriendRequest(Long requestId, Long userId);

    List<UserInfoVO> getFriendList(Long userId);

    void deleteFriend(Long userId, Long friendId);

    List<UserInfoVO> searchUsers(String keyword, Long currentUserId);
}
