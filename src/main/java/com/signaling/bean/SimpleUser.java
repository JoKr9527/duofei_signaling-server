package com.signaling.bean;

import lombok.Data;

/**
 * @author DJZ-HXF
 * @date 2019/6/14
 */
@Data
public class SimpleUser {
    private String token;
    private String name;
    private String remote;
}
