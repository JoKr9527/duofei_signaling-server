package com.signaling.bean;

import lombok.Data;

import java.util.*;

/**
 * 信令通道
 * @author DJZ-HXF
 * @date 2019/6/12
 */
@Data
public class SignalingChannel {

    /**
     * 请求房间号
     */
    private String roomId;
    /**
     * 调用者
     */
    private User caller;
    /**
     * 远程对等用户名称
     */
    private Set<String> callees;

    public SignalingChannel(User user,String roomId) {
        this.caller = user;
        this.roomId = roomId;
        this.callees = new HashSet<>();
    }

    /**
     * 新增远程对象
     * @author DJZ-HXF
     * @date 2019/6/12 15:58
     * @param userName
     */
    public void addCallee(String userName){
        callees.add(userName);
    }

    public void addCallees(Set<String> userNames){
        callees.addAll(userNames);
    }

}
