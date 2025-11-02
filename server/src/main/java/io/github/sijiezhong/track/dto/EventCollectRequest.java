package io.github.sijiezhong.track.dto;

import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * 事件上报请求体
 * 仅包含当前用例所需字段，后续可扩展
 */
public class EventCollectRequest {
    @NotBlank(message = "eventName 不能为空")
    @JsonAlias({"event_type"})
    private String eventName; // 事件名称（必填）

    @NotBlank(message = "sessionId 不能为空")
    private String sessionId; // 会话ID（必填）

    private Integer userId; // 用户ID（可选，匿名则为空）

    @JsonAlias({"project_id"})
    private Integer tenantId; // 租户ID（可选）

    @JsonAlias({"event_content"})
    private JsonNode properties; // 事件属性(JSON对象，可选)

    // 由服务端补齐/解析的结构化字段（可选）
    private String ua;
    private String referrer;
    private String ip;
    private String device;
    private String os;
    private String browser;
    private String channel;
    @JsonAlias({"anonymous_id"})
    private String anonymousId;

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getTenantId() { return tenantId; }
    public void setTenantId(Integer tenantId) { this.tenantId = tenantId; }

    public JsonNode getProperties() { return properties; }
    public void setProperties(JsonNode properties) { this.properties = properties; }

    public String getUa() { return ua; }
    public void setUa(String ua) { this.ua = ua; }

    public String getReferrer() { return referrer; }
    public void setReferrer(String referrer) { this.referrer = referrer; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getDevice() { return device; }
    public void setDevice(String device) { this.device = device; }

    public String getOs() { return os; }
    public void setOs(String os) { this.os = os; }

    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getAnonymousId() { return anonymousId; }
    public void setAnonymousId(String anonymousId) { this.anonymousId = anonymousId; }
}
