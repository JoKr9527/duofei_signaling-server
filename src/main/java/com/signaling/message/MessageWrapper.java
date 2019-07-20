package com.signaling.message;

import lombok.Data;

/**
 * 消息包装对象
 * @author DJZ-HXF
 * @date 2019/6/12
 */
@Data
public class MessageWrapper<T> {
    /**
     * 消息类型
     */
    private String msgType;
    /**
     * 消息体
     */
    private T msgBody;
}
