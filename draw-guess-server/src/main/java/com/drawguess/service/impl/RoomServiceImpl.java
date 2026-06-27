package com.drawguess.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drawguess.common.BusinessException;
import com.drawguess.common.ResultCode;
import com.drawguess.mapper.RoomMapper;
import com.drawguess.mapper.RoomMemberMapper;
import com.drawguess.mapper.UserMapper;
import com.drawguess.model.dto.GameRoomVO;
import com.drawguess.model.entity.Room;
import com.drawguess.model.entity.RoomMember;
import com.drawguess.model.entity.User;
import com.drawguess.model.enums.RoomState;
import com.drawguess.service.RoomService;
import com.drawguess.util.RoomCodeGenerator;
import com.drawguess.websocket.WsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RoomServiceImpl implements RoomService {

    private static final Logger log = LoggerFactory.getLogger(RoomServiceImpl.class);
    private static final int MAX_MEMBERS = 8;

    private final RoomMapper roomMapper;
    private final RoomMemberMapper roomMemberMapper;
    private final UserMapper userMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public RoomServiceImpl(RoomMapper roomMapper, RoomMemberMapper roomMemberMapper,
                           UserMapper userMapper, SimpMessagingTemplate messagingTemplate) {
        this.roomMapper = roomMapper;
        this.roomMemberMapper = roomMemberMapper;
        this.userMapper = userMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public Room createRoom(Long creatorId) {
        String roomId;
        do {
            roomId = RoomCodeGenerator.generate();
        } while (getRoomByRoomId(roomId) != null);

        Room room = new Room();
        room.setRoomId(roomId);
        room.setCreatorId(creatorId);
        room.setState(RoomState.WAITING);
        roomMapper.insert(room);

        RoomMember member = new RoomMember();
        member.setRoomId(room.getId());
        member.setUserId(creatorId);
        member.setScore(0);
        member.setPainterOrder(0);
        roomMemberMapper.insert(member);

        log.info("房间创建成功: roomId={}, creatorId={}", roomId, creatorId);
        broadcastRoomEvent(roomId, "room_created",
                Map.of("roomId", roomId, "creatorId", creatorId));
        return room;
    }

    @Override
    public GameRoomVO getRoomInfo(String roomId) {
        Room room = getRoomByRoomId(roomId);
        if (room == null) {
            throw new BusinessException(ResultCode.ROOM_NOT_FOUND);
        }
        return buildRoomVO(room);
    }

    @Override
    public void joinRoom(String roomId, Long userId) {
        Room room = getRoomByRoomId(roomId);
        if (room == null) {
            throw new BusinessException(ResultCode.ROOM_NOT_FOUND);
        }
        if (room.getState() == RoomState.PLAYING) {
            throw new BusinessException(ResultCode.ROOM_ALREADY_PLAYING);
        }

        List<RoomMember> members = getRoomMembers(room.getId());
        if (members.size() >= MAX_MEMBERS) {
            throw new BusinessException(ResultCode.ROOM_FULL);
        }

        boolean alreadyIn = members.stream().anyMatch(m -> m.getUserId().equals(userId));
        if (alreadyIn) {
            throw new BusinessException(ResultCode.ALREADY_IN_ROOM);
        }

        RoomMember member = new RoomMember();
        member.setRoomId(room.getId());
        member.setUserId(userId);
        member.setScore(0);
        member.setPainterOrder(members.size());
        roomMemberMapper.insert(member);

        User user = userMapper.selectById(userId);
        String nickname = user != null ? user.getNickname() : "";
        broadcastRoomEvent(roomId, "user_joined",
                Map.of("userId", userId, "nickname", nickname));
        broadcastRoomState(roomId);
        log.info("用户加入房间: roomId={}, userId={}", roomId, userId);
    }

    @Override
    public void leaveRoom(String roomId, Long userId) {
        Room room = getRoomByRoomId(roomId);
        if (room == null) {
            throw new BusinessException(ResultCode.ROOM_NOT_FOUND);
        }

        List<RoomMember> members = getRoomMembers(room.getId());
        boolean isInRoom = members.stream().anyMatch(m -> m.getUserId().equals(userId));
        if (!isInRoom) {
            throw new BusinessException(ResultCode.NOT_IN_ROOM);
        }

        // 房主离开：解散房间
        if (room.getCreatorId().equals(userId)) {
            dissolveRoom(roomId, userId);
            return;
        }

        // 普通成员离开
        roomMemberMapper.delete(
                new LambdaQueryWrapper<RoomMember>()
                        .eq(RoomMember::getRoomId, room.getId())
                        .eq(RoomMember::getUserId, userId)
        );

        User user = userMapper.selectById(userId);
        String nickname = user != null ? user.getNickname() : "";
        broadcastRoomEvent(roomId, "user_left",
                Map.of("userId", userId, "nickname", nickname));
        broadcastRoomState(roomId);
        log.info("用户离开房间: roomId={}, userId={}", roomId, userId);
    }

    @Override
    public void dissolveRoom(String roomId, Long userId) {
        Room room = getRoomByRoomId(roomId);
        if (room == null) {
            throw new BusinessException(ResultCode.ROOM_NOT_FOUND);
        }
        if (!room.getCreatorId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_ROOM_CREATOR);
        }

        room.setState(RoomState.ENDED);
        roomMapper.updateById(room);

        broadcastRoomEvent(roomId, "room_dismissed",
                Map.of("roomId", roomId, "reason", "房主解散"));
        roomMemberMapper.delete(
                new LambdaQueryWrapper<RoomMember>().eq(RoomMember::getRoomId, room.getId())
        );
        log.info("房间解散: roomId={}", roomId);
    }

    @Override
    public void pauseRoom(String roomId) {
        Room room = getRoomByRoomId(roomId);
        if (room == null) {
            throw new BusinessException(ResultCode.ROOM_NOT_FOUND);
        }
        room.setState(RoomState.PAUSED);
        roomMapper.updateById(room);

        broadcastRoomEvent(roomId, "room_paused", Map.of("roomId", roomId));
        broadcastRoomState(roomId);
        log.info("房间暂停: roomId={}", roomId);
    }

    @Override
    public void resumeRoom(String roomId) {
        Room room = getRoomByRoomId(roomId);
        if (room == null) {
            throw new BusinessException(ResultCode.ROOM_NOT_FOUND);
        }
        if (room.getState() != RoomState.PAUSED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "房间未处于暂停状态");
        }
        room.setState(RoomState.WAITING);
        roomMapper.updateById(room);

        broadcastRoomEvent(roomId, "room_resumed", Map.of("roomId", roomId));
        broadcastRoomState(roomId);
        log.info("房间恢复: roomId={}", roomId);
    }

    @Override
    public void dismissRoom(String roomId) {
        Room room = getRoomByRoomId(roomId);
        if (room == null) {
            throw new BusinessException(ResultCode.ROOM_NOT_FOUND);
        }
        room.setState(RoomState.ENDED);
        roomMapper.updateById(room);

        broadcastRoomEvent(roomId, "room_dismissed",
                Map.of("roomId", roomId, "reason", "管理员解散"));
        roomMemberMapper.delete(
                new LambdaQueryWrapper<RoomMember>().eq(RoomMember::getRoomId, room.getId())
        );
        log.info("房间被管理员解散: roomId={}", roomId);
    }

    @Override
    public List<GameRoomVO> getOnlineRooms() {
        List<Room> rooms = roomMapper.selectList(
                new LambdaQueryWrapper<Room>().ne(Room::getState, RoomState.ENDED)
        );
        List<GameRoomVO> result = new ArrayList<>();
        for (Room room : rooms) {
            result.add(buildRoomVO(room));
        }
        return result;
    }

    @Override
    public Room getRoomByRoomId(String roomId) {
        return roomMapper.selectOne(
                new LambdaQueryWrapper<Room>().eq(Room::getRoomId, roomId)
        );
    }

    @Override
    public void updateRoom(Room room) {
        roomMapper.updateById(room);
    }

    @Override
    public List<Long> getMemberUserIds(String roomId) {
        Room room = getRoomByRoomId(roomId);
        if (room == null) return List.of();
        List<RoomMember> members = getRoomMembers(room.getId());
        return members.stream().map(RoomMember::getUserId).toList();
    }

    // ---- 内部辅助方法 ----

    private List<RoomMember> getRoomMembers(Long roomDbId) {
        return roomMemberMapper.selectList(
                new LambdaQueryWrapper<RoomMember>().eq(RoomMember::getRoomId, roomDbId)
        );
    }

    private void broadcastRoomEvent(String roomId, String event, Map<String, Object> data) {
        try {
            messagingTemplate.convertAndSend("/topic/room/" + roomId,
                    WsMessage.event(event, data));
        } catch (Exception e) {
            log.warn("WebSocket广播失败: roomId={}, event={}, error={}", roomId, event, e.getMessage());
        }
    }

    private void broadcastRoomState(String roomId) {
        try {
            GameRoomVO roomInfo = getRoomInfo(roomId);
            messagingTemplate.convertAndSend("/topic/room/" + roomId,
                    WsMessage.event("room_state", roomInfo));
        } catch (Exception e) {
            log.warn("广播房间状态失败: roomId={}, error={}", roomId, e.getMessage());
        }
    }

    private GameRoomVO buildRoomVO(Room room) {
        GameRoomVO vo = new GameRoomVO();
        vo.setId(room.getId());
        vo.setRoomId(room.getRoomId());
        vo.setCreatorId(room.getCreatorId());
        vo.setState(room.getState());
        vo.setCreatedAt(room.getCreatedAt());

        User creator = userMapper.selectById(room.getCreatorId());
        if (creator != null) {
            vo.setCreatorNickname(creator.getNickname());
        }

        List<RoomMember> members = getRoomMembers(room.getId());
        List<GameRoomVO.MemberInfo> memberInfos = new ArrayList<>();
        for (RoomMember m : members) {
            GameRoomVO.MemberInfo info = new GameRoomVO.MemberInfo();
            info.setUserId(m.getUserId());
            info.setScore(m.getScore());
            info.setPainterOrder(m.getPainterOrder());
            User u = userMapper.selectById(m.getUserId());
            if (u != null) {
                info.setNickname(u.getNickname());
            }
            memberInfos.add(info);
        }
        vo.setMembers(memberInfos);
        return vo;
    }
}
