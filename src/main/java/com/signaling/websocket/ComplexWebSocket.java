package com.signaling.websocket;

import com.alibaba.fastjson.JSONObject;
import com.signaling.bean.SignalingChannel;
import com.signaling.bean.User;
import com.signaling.constant.MsgConsts;
import com.signaling.message.MessageCenter;
import com.signaling.message.MessageWrapper;
import com.signaling.message.SdpWrapper;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 会话描述协议websocket
 *
 * @author DJZ-HXF
 * @date 2019/6/11
 */
@ServerEndpoint("/complexSdpExchange")
@Component
public class ComplexWebSocket {

    /**
     * 存储用户对应的session
     */
    private static final Map<String, Session> sessionIdEntityMap = new HashMap<>();

    /**
     * sessionid 对应的user
     */
    private static final Map<String, User> sessionIdUserMap = new HashMap<>();

    /**
     * 用户名对应的session
     */
    private static final Map<String, Session> userNameSessionMap = new HashMap<>();

    /**
     * sessionId 对应的signaling
     */
    private static final Map<String, SignalingChannel> sessionIdSignalingChannelMap = new HashMap<>();

    /**
     * 房间id 对应的信道
     */
    private static final Map<String, Set<SignalingChannel>> roomIdSignalingChannelsMap = new ConcurrentHashMap<>();

    static {
        // 初始化消息处理中心
        // 处理用户认证消息
        MessageCenter.addHandler(MsgConsts.USER_IDENTITY.getValue(), (Map<String, String> map, Session session) -> {
            User user = new User();
            user.setName(map.get("name"));
            user.setRoomId(map.get("roomId"));
            sessionIdUserMap.put(session.getId(), user);
            userNameSessionMap.put(user.getName(), session);
        });
        // 处理多个连接建立请求 并响应成员信息
        MessageCenter.addHandler(MsgConsts.CLIENT_MULTIPLY_CONNECTION_CREATEQUE.getValue(), (Map<String, String> map, Session session) -> {

            User user = new User();
            user.setName(map.get("name"));
            user.setRoomId(map.get("roomId"));
            Set<String> waitCalleeNames = new HashSet<>();
            synchronized (ComplexWebSocket.class){
                // 获取房间里获取尚未建立信道的成员
                Set<String> hasSignalingChannleUsers = Optional.ofNullable(roomIdSignalingChannelsMap.get(user.getRoomId())).orElseGet(HashSet::new)
                        .stream().map(u -> u.getCaller().getName()).collect(Collectors.toSet());
                sessionIdUserMap.forEach((id, u) -> {
                    if (user.getRoomId().equals(u.getRoomId()) && !hasSignalingChannleUsers.contains(u.getName()) && !user.getName().equals(u.getName())) {
                        waitCalleeNames.add(u.getName());
                    }
                });

                SignalingChannel signalingChannel = new SignalingChannel(user, user.getRoomId());
                signalingChannel.addCallees(waitCalleeNames);
                sessionIdSignalingChannelMap.put(session.getId(), signalingChannel);
                if (!roomIdSignalingChannelsMap.containsKey(user.getRoomId())) {
                    roomIdSignalingChannelsMap.put(user.getRoomId(), new HashSet<>());
                }
                roomIdSignalingChannelsMap.get(user.getRoomId()).add(signalingChannel);
            }

            if(waitCalleeNames.size()==0){
                return ;
            }
            // 响应成员信息
            MessageWrapper<Set<String>> result = new MessageWrapper();
            result.setMsgType(MsgConsts.SERVER_CALLEESRESP.getValue());
            result.setMsgBody(waitCalleeNames);
            try {
                session.getBasicRemote().sendText(JSONObject.toJSONString(result));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        // 处理type为offer的desc,
        MessageCenter.addHandler(MsgConsts.SDP_OFFER.getValue(), (Map<String, String> userNameSdp, Session session) -> {
            SignalingChannel signalingChannel = sessionIdSignalingChannelMap.get(session.getId());
            // 向远程发送offer
            signalingChannel.getCallees().forEach(userName -> {
                if(userNameSdp.containsKey(userName)){
                    Session remoteSession = userNameSessionMap.get(userName);
                    if (remoteSession != null && remoteSession.isOpen()) {
                        try {
                            SdpWrapper sdpWrapper = new SdpWrapper();
                            sdpWrapper.setSdp(userNameSdp.get(userName));
                            sdpWrapper.setUserName(signalingChannel.getCaller().getName());
                            MessageWrapper<SdpWrapper> result = new MessageWrapper();
                            result.setMsgType(MsgConsts.SDP_OFFER.getValue());
                            result.setMsgBody(sdpWrapper);
                            remoteSession.getBasicRemote().sendText(JSONObject.toJSONString(result));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        });
        // 处理type为answer的desc,
        MessageCenter.addHandler(MsgConsts.SDP_ANSWER.getValue(), (Map<String, String> map, Session session) -> {
            SignalingChannel signalingChannel = sessionIdSignalingChannelMap.get(session.getId());
            SdpWrapper sdpWrapper = new SdpWrapper();
            sdpWrapper.setUserName(signalingChannel.getCaller().getName());
            sdpWrapper.setSdp(JSONObject.toJSONString(map.get("sdp")));
            Set<SignalingChannel> signalingChannels = roomIdSignalingChannelsMap.get(signalingChannel.getRoomId());
            // 向其调用者发送 answer
            signalingChannels.forEach(sc -> {
                if(sc.getCaller().getName().equals(map.get("userName"))){
                    Session remoteSession = userNameSessionMap.get(sc.getCaller().getName());
                    if (remoteSession != null && remoteSession.isOpen()) {
                        MessageWrapper<SdpWrapper> result = new MessageWrapper();
                        result.setMsgType(MsgConsts.SDP_ANSWER.getValue());
                        result.setMsgBody(sdpWrapper);
                        try {
                            remoteSession.getBasicRemote().sendText(JSONObject.toJSONString(result));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        });

        // 处理 candidate
        MessageCenter.addHandler(MsgConsts.SDP_CANDIDATE.getValue(), (Map<String, String> map, Session session) -> {
            SignalingChannel signalingChannel = sessionIdSignalingChannelMap.get(session.getId());
            SdpWrapper sdpWrapper = new SdpWrapper();
            sdpWrapper.setUserName(signalingChannel.getCaller().getName());
            sdpWrapper.setSdp(JSONObject.toJSONString(map.get("sdp")));
            MessageWrapper<SdpWrapper> result = new MessageWrapper();
            result.setMsgType(MsgConsts.SDP_CANDIDATE.getValue());
            result.setMsgBody(sdpWrapper);
            // 向对等体发送 candidate
            signalingChannel.getCallees().forEach(userName -> {
                if(userName.equals(map.get("userName"))){
                    Session remoteSession = userNameSessionMap.get(userName);
                    if (remoteSession != null && remoteSession.isOpen()) {
                        try {
                            remoteSession.getBasicRemote().sendText(JSONObject.toJSONString(result));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            Set<SignalingChannel> signalingChannels = roomIdSignalingChannelsMap.get(signalingChannel.getRoomId());
            signalingChannels.forEach(sc -> {
                if (sc.getCaller().getName().equals(map.get("userName"))) {
                    Session remoteSession = userNameSessionMap.get(sc.getCaller().getName());
                    if (remoteSession != null && remoteSession.isOpen()) {
                        try {
                            remoteSession.getBasicRemote().sendText(JSONObject.toJSONString(result));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

        });
    }

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
    public void onClose(Session session) {
        System.out.println("connection close! ");
        sessionIdEntityMap.remove(session.getId());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        MessageCenter.handleMsg(message, session);
    }

    /**
     * 发生错误时调用
     */
    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
        System.out.println("connection error! ");
        sessionIdEntityMap.remove(session.getId());
    }

}
