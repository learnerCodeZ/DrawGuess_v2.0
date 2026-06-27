package com.drawguess.service;

import com.drawguess.model.entity.User;
import com.drawguess.model.enums.UserStatus;

import java.util.List;

public interface UserService {

    User getUserById(Long id);

    User getUserByPhone(String phone);

    User register(String phone, String nickname, String password);

    String login(String phone, String password);

    void changePassword(Long userId, String oldPassword, String newPassword);

    void forceChangePassword(Long userId, String newPassword);

    void changeNickname(Long userId, String nickname);

    void deleteUser(Long userId);

    List<User> getPendingUsers();

    void approveUser(Long userId);

    void rejectUser(Long userId);

    List<User> getAllUsers();

    void initSuperAdmin();

    User getCurrentUser();
}
