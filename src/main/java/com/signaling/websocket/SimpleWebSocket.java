package com.signaling.websocket;

import com.alibaba.fastjson.JSONObject;
import com.signaling.bean.SimpleUser;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 会话描述协议websocket
 *
 * @author DJZ-HXF
 * @date 2019/6/11
 */
@ServerEndpoint("/simpleSdpExchange")
@Component
public class SimpleWebSocket {

    /**
     * 存储用户对应的session
     */
    private static Map<String, Session> sessionIdEntityMap = new HashMap<>();

    /**
     * sessionid 对应的user
     */
    private static Map<String, SimpleUser> sessionIdUserMap = new HashMap<>();

    /**
     * 用户名对应的session
     */
    private static Map<String, Session> userNameSessionMap = new HashMap<>();

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session) {
        sessionIdEntityMap.put(session.getId(), session);
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        System.out.println("connection close！");
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        if (message.contains("nothing knows me")) {
            Object object = JSONObject.parse(message);

            Map<String, String> map = (Map) object;
            SimpleUser user = new SimpleUser();
            user.setName(map.get("name"));
            user.setRemote(map.get("remote"));
            user.setToken(map.get("token"));
            sessionIdUserMap.put(session.getId(), user);
            userNameSessionMap.put(user.getName(), session);
        }
        // 无论接受到什么sdp，则发送给其对等体（远程）
        if (!message.contains("nothing knows me")) {
            String id = session.getId();
            SimpleUser user = sessionIdUserMap.get(id);
            Session remoteSession = userNameSessionMap.get(user.getRemote());
            if (remoteSession != null && remoteSession.isOpen()) {
                try {
                    remoteSession.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 发生错误时调用
     */
    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }

}
