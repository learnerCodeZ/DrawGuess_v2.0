package com.drawguess.common;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),

    // 4xx 客户端错误
    BAD_REQUEST(400, "参数错误"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "权限不足"),
    NOT_FOUND(404, "资源不存在"),

    // 用户相关 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "手机号已注册"),
    USER_PENDING(1003, "账号待审核"),
    USER_REJECTED(1004, "账号已被拒绝"),
    PASSWORD_ERROR(1005, "密码错误"),
    OLD_PASSWORD_ERROR(1006, "原密码错误"),
    PASSWORD_SAME(1007, "新密码不能与旧密码相同"),
    SUPER_ADMIN_FORCE_CHANGE(1008, "请先修改默认密码"),

    // 好友相关 2xxx
    FRIEND_REQUEST_EXISTS(2001, "已发送过好友请求"),
    ALREADY_FRIENDS(2002, "已经是好友"),
    CANNOT_ADD_SELF(2003, "不能添加自己为好友"),
    FRIEND_REQUEST_NOT_FOUND(2004, "好友请求不存在"),

    // 房间相关 3xxx
    ROOM_NOT_FOUND(3001, "房间不存在"),
    ROOM_FULL(3002, "房间已满"),
    ROOM_ALREADY_PLAYING(3003, "游戏正在进行中"),
    NOT_ROOM_CREATOR(3004, "非房主操作"),
    NOT_IN_ROOM(3005, "不在该房间中"),
    ALREADY_IN_ROOM(3006, "已在房间中"),

    // 游戏相关 4xxx
    NOT_ENOUGH_PLAYERS(4001, "玩家不足3人"),
    GAME_NOT_IN_PROGRESS(4002, "游戏未在进行中"),
    NOT_PAINTER(4003, "不是当前画家"),
    ALREADY_ANSWERED(4004, "已经答过"),
    ANSWER_CORRECT(4005, "回答正确"),

    // 词库相关 5xxx
    WORD_EXISTS(5001, "词语已存在"),
    DEFAULT_WORD_CANNOT_DELETE(5002, "默认词不可删除"),

    // 通用 9xxx
    INTERNAL_ERROR(9001, "服务器内部错误"),
    TOKEN_INVALID(9002, "Token无效"),
    TOKEN_EXPIRED(9003, "Token已过期");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
