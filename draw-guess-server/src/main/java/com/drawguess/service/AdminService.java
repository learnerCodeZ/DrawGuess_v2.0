package com.drawguess.service;

import com.drawguess.model.dto.UserInfoVO;
import com.drawguess.model.entity.User;

import java.util.List;
import java.util.Map;

public interface AdminService {

    List<User> getAllUsers();

    List<UserInfoVO> getRejectedUsers();

    void approveUser(Long userId);

    void rejectUser(Long userId);

    void deleteUser(Long userId);

    Map<String, Object> getDashboardStats();
}
