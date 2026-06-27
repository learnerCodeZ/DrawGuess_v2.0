package com.drawguess.service;

import java.util.Set;

public interface OnlineUserService {

    void userOnline(Long userId);

    void userOffline(Long userId);

    boolean isOnline(Long userId);

    Set<Long> getOnlineUserIds();

    long getOnlineCount();
}
