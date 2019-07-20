package com.signaling.message;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.signaling.bean.User;
import com.signaling.constant.MsgConsts;

import javax.websocket.Session;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 处理消息
 *
 * @author DJZ-HXF
 * @date 2019/6/12
 */
public class MessageCenter {

    /**
     * 消息对应的处理器
     */
    private static Map<String, BiConsumer<? extends Map<String,String>,Session>> handlers = new HashMap<>();

    public static void handleMsg(String msg, Session session) {
        MessageWrapper messageWrapper = JSONObject.parseObject(msg, MessageWrapper.class);
        BiConsumer consumer = handlers.get(messageWrapper.getMsgType());
        if (consumer != null) {
            if(messageWrapper.getMsgBody() instanceof JSONArray){
                JSONArray temp = (JSONArray) messageWrapper.getMsgBody();
                // 组装name 和 sdp 的映射
                Map<String,String> result = new HashMap<>();
                temp.forEach(jsonObject->{
                    if(jsonObject instanceof JSONObject){
                        Map tempMap = (Map) jsonObject;
                        result.put(tempMap.get("userName").toString(),tempMap.get("sdp").toString());
                    }
                });
                consumer.accept(result,session);
            }else{
                consumer.accept(messageWrapper.getMsgBody(), session);
            }
        }
    }

    /**
     * 新增消息处理器
     *
     * @param msgType    消息类型
     * @param biConsumer 消息处理者
     * @author DJZ-HXF
     * @date 2019/6/12 16:30
     */
    public static <T> void addHandler(String msgType, BiConsumer<? extends Map<String,String>, Session> biConsumer) {
        handlers.put(msgType, biConsumer);
    }
}
