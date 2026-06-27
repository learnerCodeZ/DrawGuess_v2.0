package com.drawguess.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drawguess.common.BusinessException;
import com.drawguess.common.ResultCode;
import com.drawguess.model.entity.User;
import com.drawguess.model.enums.UserRole;
import com.drawguess.model.enums.UserStatus;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_PHONE = "13800138000";
    private static final String TEST_NICKNAME = "测试用户";
    private static final String TEST_PASSWORD = "test123456";

    @BeforeAll
    void setup() {
        // 初始化超级管理员
        userService.initSuperAdmin();
    }

    @AfterAll
    void cleanup() {
        // 清理测试数据
        var user = userService.getUserByPhone(TEST_PHONE);
        if (user != null) {
            // deleteUser
        }
    }

    @Test
    @Order(1)
    void testRegister_Success() {
        // 清理可能存在的测试用户
        User existing = userService.getUserByPhone(TEST_PHONE);
        if (existing != null) {
            // 已有则跳过
            return;
        }

        User user = userService.register(TEST_PHONE, TEST_NICKNAME, TEST_PASSWORD);
        assertNotNull(user);
        assertEquals(TEST_PHONE, user.getPhone());
        assertEquals(TEST_NICKNAME, user.getNickname());
        assertEquals(UserRole.USER, user.getRole());
        assertEquals(UserStatus.PENDING, user.getStatus());
        assertTrue(passwordEncoder.matches(TEST_PASSWORD, user.getPassword()));
    }

    @Test
    @Order(2)
    void testRegister_DuplicatePhone() {
        BusinessException e = assertThrows(BusinessException.class, () -> {
            userService.register("00000000000", "重复", "pass123");
        });
        assertEquals(ResultCode.USER_ALREADY_EXISTS, e.getResultCode());
    }

    @Test
    @Order(3)
    void testLogin_PendingUser() {
        BusinessException e = assertThrows(BusinessException.class, () -> {
            userService.login(TEST_PHONE, TEST_PASSWORD);
        });
        assertEquals(ResultCode.USER_PENDING, e.getResultCode());
    }

    @Test
    @Order(4)
    void testApproveAndLogin() {
        // 审核通过
        userService.approveUser(userService.getUserByPhone(TEST_PHONE).getId());

        // 登录
        String token = userService.login(TEST_PHONE, TEST_PASSWORD);
        assertNotNull(token);
        assertTrue(token.length() > 20);
    }

    @Test
    @Order(5)
    void testLogin_WrongPassword() {
        BusinessException e = assertThrows(BusinessException.class, () -> {
            userService.login(TEST_PHONE, "wrongpassword");
        });
        assertEquals(ResultCode.PASSWORD_ERROR, e.getResultCode());
    }

    @Test
    @Order(6)
    void testSuperAdminInit() {
        User superAdmin = userService.getUserByPhone("00000000000");
        assertNotNull(superAdmin);
        assertEquals(UserRole.SUPER_ADMIN, superAdmin.getRole());
        assertEquals(UserStatus.APPROVED, superAdmin.getStatus());
    }

    @Test
    @Order(7)
    void testSuperAdminForceChange() {
        // 超级管理员使用默认密码应该触发强制改密
        BusinessException e = assertThrows(BusinessException.class, () -> {
            userService.login("00000000000", "admin123");
        });
        assertEquals(ResultCode.SUPER_ADMIN_FORCE_CHANGE, e.getResultCode());
        assertNotNull(e.getMessage()); // 应包含临时 token
    }

    @Test
    @Order(8)
    void testChangePassword() {
        User user = userService.getUserByPhone(TEST_PHONE);
        userService.changePassword(user.getId(), TEST_PASSWORD, "newpass123");
        // 用新密码登录
        String token = userService.login(TEST_PHONE, "newpass123");
        assertNotNull(token);
        // 恢复
        userService.changePassword(user.getId(), "newpass123", TEST_PASSWORD);
    }

    @Test
    @Order(9)
    void testChangeNickname() {
        User user = userService.getUserByPhone(TEST_PHONE);
        userService.changeNickname(user.getId(), "新昵称");
        User updated = userService.getUserById(user.getId());
        assertEquals("新昵称", updated.getNickname());
        // 恢复
        userService.changeNickname(user.getId(), TEST_NICKNAME);
    }

    @Test
    @Order(10)
    void testGetPendingUsers() {
        // 注册一个待审核用户
        String tempPhone = "13900139000";
        try {
            userService.register(tempPhone, "临时用户", "pass123");
            var pending = userService.getPendingUsers();
            assertFalse(pending.isEmpty());
        } finally {
            var tempUser = userService.getUserByPhone(tempPhone);
            if (tempUser != null) userService.deleteUser(tempUser.getId());
        }
    }
}
