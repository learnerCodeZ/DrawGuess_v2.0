package com.drawguess.service;

import com.drawguess.mapper.FriendMapper;
import com.drawguess.model.dto.UserInfoVO;
import com.drawguess.service.impl.FriendServiceImpl;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FriendServiceTest {

    @Autowired
    private FriendService friendService;

    @Autowired
    private UserService userService;

    private Long user1Id;
    private Long user2Id;

    @BeforeAll
    void setup() {
        // 确保两个测试用户
        var u1 = userService.getUserByPhone("13700137100");
        var u2 = userService.getUserByPhone("13700137200");

        if (u1 == null) {
            userService.register("13700137100", "好友测试1", "test123");
            u1 = userService.getUserByPhone("13700137100");
            userService.approveUser(u1.getId());
        }
        if (u2 == null) {
            userService.register("13700137200", "好友测试2", "test123");
            u2 = userService.getUserByPhone("13700137200");
            userService.approveUser(u2.getId());
        }

        user1Id = u1.getId();
        user2Id = u2.getId();
    }

    @Test
    @Order(1)
    void testSearchUsers() {
        var results = friendService.searchUsers("好友测试", user1Id);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(u -> u.getId().equals(user2Id)));
    }

    @Test
    @Order(2)
    void testSendFriendRequest() {
        friendService.sendFriendRequest(user1Id, user2Id);

        // 检查请求列表
        var requests = friendService.getFriendRequests(user2Id);
        assertFalse(requests.isEmpty());
        assertTrue(requests.stream().anyMatch(r -> r.getFromUserId().equals(user1Id)));
    }

    @Test
    @Order(3)
    void testSendDuplicatedRequest() {
        assertThrows(Exception.class, () -> {
            friendService.sendFriendRequest(user1Id, user2Id);
        });
    }

    @Test
    @Order(4)
    void testAcceptFriendRequest() {
        var requests = friendService.getFriendRequests(user2Id);
        assertFalse(requests.isEmpty());
        var request = requests.get(0);
        friendService.acceptFriendRequest(request.getId(), user2Id);
    }

    @Test
    @Order(5)
    void testFriendList() {
        var friends = friendService.getFriendList(user1Id);
        assertFalse(friends.isEmpty());
        assertTrue(friends.stream().anyMatch(f -> f.getId().equals(user2Id)));
    }

    @Test
    @Order(6)
    void testDeleteFriend() {
        friendService.deleteFriend(user1Id, user2Id);
        var friends = friendService.getFriendList(user1Id);
        assertTrue(friends.stream().noneMatch(f -> f.getId().equals(user2Id)));
    }
}
