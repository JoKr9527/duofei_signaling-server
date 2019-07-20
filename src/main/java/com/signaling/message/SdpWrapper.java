package com.signaling.message;

import lombok.Data;

/**
 * sdp 消息格式封装类
 * @author DJZ-HXF
 * @date 2019/6/13
 */
@Data
public class SdpWrapper {

    /**
     * 发送者名称
     */
    private String userName;
    /**
     * 会话描述协议
     */
    private String sdp;
}
