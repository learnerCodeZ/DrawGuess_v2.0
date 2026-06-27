package com.drawguess.service.impl;

import com.drawguess.service.OnlineUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OnlineUserServiceImpl implements OnlineUserService {

    private static final Logger log = LoggerFactory.getLogger(OnlineUserServiceImpl.class);
    private static final String ONLINE_KEY = "online:users";

    private final StringRedisTemplate stringRedisTemplate;

    public OnlineUserServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void userOnline(Long userId) {
        try {
            stringRedisTemplate.opsForSet().add(ONLINE_KEY, String.valueOf(userId));
            log.debug("用户上线: userId={}", userId);
        } catch (Exception e) {
            log.warn("记录在线状态失败: {}", e.getMessage());
        }
    }

    @Override
    public void userOffline(Long userId) {
        try {
            stringRedisTemplate.opsForSet().remove(ONLINE_KEY, String.valueOf(userId));
            log.debug("用户下线: userId={}", userId);
        } catch (Exception e) {
            log.warn("移除在线状态失败: {}", e.getMessage());
        }
    }

    @Override
    public boolean isOnline(Long userId) {
        try {
            return Boolean.TRUE.equals(
                    stringRedisTemplate.opsForSet().isMember(ONLINE_KEY, String.valueOf(userId))
            );
        } catch (Exception e) {
            log.warn("查询在线状态失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Set<Long> getOnlineUserIds() {
        try {
            Set<String> members = stringRedisTemplate.opsForSet().members(ONLINE_KEY);
            if (members == null) return Collections.emptySet();
            return members.stream()
                    .map(Long::valueOf)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("获取在线用户列表失败: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    @Override
    public long getOnlineCount() {
        try {
            Long count = stringRedisTemplate.opsForSet().size(ONLINE_KEY);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.warn("获取在线人数失败: {}", e.getMessage());
            return 0;
        }
    }
}
