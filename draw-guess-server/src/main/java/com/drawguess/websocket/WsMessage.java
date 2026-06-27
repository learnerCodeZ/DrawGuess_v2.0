package com.drawguess.websocket;

import lombok.Data;

import java.util.Map;

@Data
public class WsMessage {

    private String event;
    private Object data;
    private long timestamp;

    public static WsMessage event(String event, Object data) {
        WsMessage msg = new WsMessage();
        msg.setEvent(event);
        msg.setData(data);
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }

    public static WsMessage event(String event) {
        return event(event, Map.of());
    }
}
