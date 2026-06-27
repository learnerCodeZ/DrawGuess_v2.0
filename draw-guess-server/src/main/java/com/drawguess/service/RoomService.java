package com.drawguess.service;

import com.drawguess.model.dto.GameRoomVO;
import com.drawguess.model.entity.Room;

import java.util.List;

public interface RoomService {

    Room createRoom(Long creatorId);

    GameRoomVO getRoomInfo(String roomId);

    void joinRoom(String roomId, Long userId);

    void leaveRoom(String roomId, Long userId);

    void dissolveRoom(String roomId, Long userId);

    void pauseRoom(String roomId);

    void resumeRoom(String roomId);

    void dismissRoom(String roomId);

    List<GameRoomVO> getOnlineRooms();

    Room getRoomByRoomId(String roomId);

    void updateRoom(Room room);

    List<Long> getMemberUserIds(String roomId);
}
