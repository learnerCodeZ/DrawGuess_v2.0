package com.drawguess.controller;

import com.drawguess.common.ApiResponse;
import com.drawguess.common.BusinessException;
import com.drawguess.common.ResultCode;
import com.drawguess.model.dto.ChangeNicknameRequest;
import com.drawguess.model.dto.ChangePasswordRequest;
import com.drawguess.model.dto.LoginRequest;
import com.drawguess.model.dto.RegisterRequest;
import com.drawguess.model.dto.UserInfoVO;
import com.drawguess.model.entity.User;
import com.drawguess.security.JwtTokenProvider;
import com.drawguess.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    public UserController(UserService userService, JwtTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request.getPhone(), request.getNickname(), request.getPassword());
        return ApiResponse.success("注册成功，请等待管理员审核");
    }

    /**
     * 登录接口
     * 成功返回 { code:200, data: {token, user} }
     * 超级管理员首次登录返回 { code:1008, message:"...", data: {tempToken} }
     */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        try {
            String token = userService.login(request.getPhone(), request.getPassword());
            User user = userService.getUserByPhone(request.getPhone());
            Map<String, Object> data = Map.of(
                    "token", token,
                    "user", UserInfoVO.fromEntity(user)
            );
            return ApiResponse.success(data);
        } catch (BusinessException e) {
            if (e.getResultCode() == ResultCode.SUPER_ADMIN_FORCE_CHANGE) {
                return ApiResponse.error(
                        ResultCode.SUPER_ADMIN_FORCE_CHANGE.getCode(),
                        ResultCode.SUPER_ADMIN_FORCE_CHANGE.getMessage(),
                        Map.of("tempToken", e.getMessage())
                );
            }
            throw e;
        }
    }

    /**
     * 超级管理员首次登录强制改密
     * 请求体需携带临时 token（从登录响应中获取）和新密码
     */
    @PostMapping("/change-password")
    public ApiResponse<Map<String, Object>> forceChangePassword(@RequestBody Map<String, String> body) {
        String tempToken = body.get("tempToken");
        String newPassword = body.get("newPassword");

        if (tempToken == null || newPassword == null) {
            return ApiResponse.error(400, "参数不完整");
        }

        if (!jwtTokenProvider.validateToken(tempToken) || !jwtTokenProvider.isTempToken(tempToken)) {
            return ApiResponse.error(ResultCode.TOKEN_INVALID);
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(tempToken);
        userService.forceChangePassword(userId, newPassword);

        // 改密成功后重新生成正式 token
        User user = userService.getUserById(userId);
        String newToken = jwtTokenProvider.generateToken(user.getId(), user.getPhone(), user.getRole().name());
        return ApiResponse.success(Map.of("token", newToken, "user", UserInfoVO.fromEntity(user)));
    }

    @GetMapping("/user")
    public ApiResponse<UserInfoVO> getCurrentUser() {
        User user = userService.getCurrentUser();
        if (user == null) {
            return ApiResponse.error(ResultCode.UNAUTHORIZED);
        }
        return ApiResponse.success(UserInfoVO.fromEntity(user));
    }

    @PutMapping("/user/nickname")
    public ApiResponse<Void> changeNickname(@Valid @RequestBody ChangeNicknameRequest request) {
        User user = userService.getCurrentUser();
        userService.changeNickname(user.getId(), request.getNickname());
        return ApiResponse.success("昵称修改成功");
    }

    @PutMapping("/user/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        User user = userService.getCurrentUser();
        userService.changePassword(user.getId(), request.getOldPassword(), request.getNewPassword());
        return ApiResponse.success("密码修改成功");
    }

    @DeleteMapping("/user")
    public ApiResponse<Void> deleteUser() {
        User user = userService.getCurrentUser();
        userService.deleteUser(user.getId());
        return ApiResponse.success("账户已注销");
    }
}
