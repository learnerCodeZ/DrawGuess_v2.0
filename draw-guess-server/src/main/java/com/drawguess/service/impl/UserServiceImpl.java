package com.drawguess.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drawguess.common.BusinessException;
import com.drawguess.common.ResultCode;
import com.drawguess.mapper.UserMapper;
import com.drawguess.model.entity.User;
import com.drawguess.model.enums.UserRole;
import com.drawguess.model.enums.UserStatus;
import com.drawguess.security.JwtTokenProvider;
import com.drawguess.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public UserServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public User getUserByPhone(String phone) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, phone)
        );
    }

    @Override
    public User register(String phone, String nickname, String password) {
        User existing = getUserByPhone(phone);
        if (existing != null) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        User user = new User();
        user.setPhone(phone);
        user.setNickname(nickname);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.PENDING);
        user.setTotalScore(0);

        userMapper.insert(user);
        log.info("用户注册成功: phone={}, nickname={}", phone, nickname);
        return user;
    }

    @Override
    public String login(String phone, String password) {
        User user = getUserByPhone(phone);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (user.getStatus() == UserStatus.PENDING) {
            throw new BusinessException(ResultCode.USER_PENDING);
        }
        if (user.getStatus() == UserStatus.REJECTED) {
            throw new BusinessException(ResultCode.USER_REJECTED);
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        // 超级管理员仍在使用默认密码，返回临时 token 供强制改密
        if (user.getRole() == UserRole.SUPER_ADMIN
                && passwordEncoder.matches("admin123", user.getPassword())) {
            log.info("超级管理员首次登录，需要修改密码: phone={}", phone);
            throw new BusinessException(ResultCode.SUPER_ADMIN_FORCE_CHANGE,
                    jwtTokenProvider.generateTempToken(user.getId(), user.getPhone(), user.getRole().name()));
        }

        log.info("用户登录成功: phone={}", phone);
        return jwtTokenProvider.generateToken(user.getId(), user.getPhone(), user.getRole().name());
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_SAME);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        log.info("用户修改密码: userId={}", userId);
    }

    /**
     * 超级管理员首次登录强制改密（使用临时 token 中的 userId）
     */
    public void forceChangePassword(Long userId, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_SAME);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        log.info("超级管理员强制修改密码: userId={}", userId);
    }

    @Override
    public void changeNickname(Long userId, String nickname) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        user.setNickname(nickname);
        userMapper.updateById(user);
        log.info("用户修改昵称: userId={}, nickname={}", userId, nickname);
    }

    @Override
    public void deleteUser(Long userId) {
        userMapper.deleteById(userId);
        log.info("用户注销: userId={}", userId);
    }

    @Override
    public List<User> getPendingUsers() {
        return userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getStatus, UserStatus.PENDING)
        );
    }

    @Override
    public void approveUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        user.setStatus(UserStatus.APPROVED);
        userMapper.updateById(user);
        log.info("用户审核通过: userId={}", userId);
    }

    @Override
    public void rejectUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        user.setStatus(UserStatus.REJECTED);
        userMapper.updateById(user);
        log.info("用户审核拒绝: userId={}", userId);
    }

    @Override
    public List<User> getAllUsers() {
        return userMapper.selectList(null);
    }

    @Override
    public void initSuperAdmin() {
        User existing = getUserByPhone("00000000000");
        if (existing == null) {
            User admin = new User();
            admin.setPhone("00000000000");
            admin.setNickname("超级管理员");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(UserRole.SUPER_ADMIN);
            admin.setStatus(UserStatus.APPROVED);
            admin.setTotalScore(0);
            userMapper.insert(admin);
            log.info("超级管理员初始化完成");
        }
    }

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }
}
