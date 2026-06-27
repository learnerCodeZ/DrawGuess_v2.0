package com.drawguess.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drawguess.common.BusinessException;
import com.drawguess.common.ResultCode;
import com.drawguess.mapper.RoomMemberMapper;
import com.drawguess.model.dto.GameRoomVO;
import com.drawguess.model.entity.Room;
import com.drawguess.model.entity.RoomMember;
import com.drawguess.model.entity.User;
import com.drawguess.model.enums.RoomState;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RoomServiceTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private UserService userService;

    @Autowired
    private RoomMemberMapper roomMemberMapper;

    private User creator;
    private Room createdRoom;

    @BeforeAll
    void setup() {
        // 确保测试用户存在且已审核
        User existing = userService.getUserByPhone("13800138100");
        if (existing == null) {
            userService.register("13800138100", "房主测试", "test123");
            creator = userService.getUserByPhone("13800138100");
            userService.approveUser(creator.getId());
        } else {
            creator = existing;
        }
    }

    @Test
    @Order(1)
    void testCreateRoom() {
        createdRoom = roomService.createRoom(creator.getId());
        assertNotNull(createdRoom);
        assertNotNull(createdRoom.getRoomId());
        assertEquals(6, createdRoom.getRoomId().length());
        assertEquals(creator.getId(), createdRoom.getCreatorId());
        assertEquals(RoomState.WAITING, createdRoom.getState());
    }

    @Test
    @Order(2)
    void testGetRoomInfo() {
        GameRoomVO vo = roomService.getRoomInfo(createdRoom.getRoomId());
        assertNotNull(vo);
        assertEquals(createdRoom.getRoomId(), vo.getRoomId());
        assertFalse(vo.getMembers().isEmpty());
        assertTrue(vo.getMembers().stream().anyMatch(m -> m.getUserId().equals(creator.getId())));
    }

    @Test
    @Order(3)
    void testJoinRoom() {
        // 注册另一个测试用户
        User user2 = userService.getUserByPhone("13800138200");
        if (user2 == null) {
            userService.register("13800138200", "加入测试", "test123");
            user2 = userService.getUserByPhone("13800138200");
            userService.approveUser(user2.getId());
        }

        roomService.joinRoom(createdRoom.getRoomId(), user2.getId());
        GameRoomVO vo = roomService.getRoomInfo(createdRoom.getRoomId());
        assertEquals(2, vo.getMembers().size());
    }

    @Test
    @Order(4)
    void testJoinRoom_AlreadyIn() {
        BusinessException e = assertThrows(BusinessException.class, () -> {
            roomService.joinRoom(createdRoom.getRoomId(), creator.getId());
        });
        assertEquals(ResultCode.ALREADY_IN_ROOM, e.getResultCode());
    }

    @Test
    @Order(5)
    void testLeaveAndRejoin() {
        User user2 = userService.getUserByPhone("13800138200");
        roomService.leaveRoom(createdRoom.getRoomId(), user2.getId());

        GameRoomVO vo = roomService.getRoomInfo(createdRoom.getRoomId());
        assertEquals(1, vo.getMembers().size());
    }

    @Test
    @Order(6)
    void testDissolveRoom() {
        roomService.dissolveRoom(createdRoom.getRoomId(), creator.getId());
        Room room = roomService.getRoomByRoomId(createdRoom.getRoomId());
        assertEquals(RoomState.ENDED, room.getState());
    }
}
