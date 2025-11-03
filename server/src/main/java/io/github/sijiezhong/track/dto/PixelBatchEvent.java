package io.github.sijiezhong.track.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 像素批量上报中的单个事件（压缩格式）
 * 
 * <p>
 * 用于接收批量像素上报中的事件数据，使用压缩字段名以减少URL长度：
 * - "type" -> "t" (事件类型压缩码)
 * - "content" -> "c" (事件内容)
 * 
 * @author sijie
 */
public class PixelBatchEvent {
    /** 事件类型（压缩码：pv, ck, pf, er, ct） */
    @JsonProperty("t")
    private String type;
    
    /** 事件内容（JSON对象） */
    @JsonProperty("c")
    private com.fasterxml.jackson.databind.JsonNode content;
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public com.fasterxml.jackson.databind.JsonNode getContent() {
        return content;
    }
    
    public void setContent(com.fasterxml.jackson.databind.JsonNode content) {
        this.content = content;
    }
}

