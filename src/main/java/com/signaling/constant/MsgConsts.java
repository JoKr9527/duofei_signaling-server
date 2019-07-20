package com.signaling.constant;

/**
 * 消息类型
 * @author DJZ-HXF
 * @date 2019/6/12
 */
public enum MsgConsts {

    /**
     * 用户身份认证
     */
    USER_IDENTITY("USER_IDENTITY","用户身份认证"),
    SDP_OFFER("SDP_OFFER","type 为 offer 的 desc"),
    SDP_ANSWER("SDP_ANSWER","type 为 answer 的 desc"),
    SDP_CANDIDATE("SDP_CANDIDATE","type 为 offer 的 desc"),
    CLIENT_MULTIPLY_CONNECTION_CREATEQUE("CLIENT_MULTIPLY_CONNECTION_CREATEQUE","多个连接创建请求"),
    SERVER_CALLEESRESP("SERVER_CALLEESRESP","成员信息响应");
    private String value;
    private String desc;

    MsgConsts(String value,String desc){
       this.value = value;
       this.desc = desc;
    }

    public String getValue() {
        return value;
    }

    public String getDesc(){
        return desc;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
