package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.constants.ApiConstants;
import io.github.sijiezhong.track.constants.HttpHeaderConstants;
import io.github.sijiezhong.track.domain.Event;
import io.github.sijiezhong.track.repository.EventRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 事件导出控制器
 * 
 * <p>提供事件数据的导出功能，支持CSV和Parquet格式。
 * 所有导出都会进行应用隔离，确保数据安全。
 * 
 * @author sijie
 */
@RestController
@RequestMapping(ApiConstants.API_PREFIX + "/events")
@Tag(name = "Event Export", description = "事件数据导出")
public class EventExportController {

    private static final Logger log = LoggerFactory.getLogger(EventExportController.class);

    private final EventRepository eventRepository;

    public EventExportController(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * 导出事件CSV
     * 
     * <p>根据应用ID导出事件数据为CSV格式。支持字段选择和事件名过滤。
     * 只读用户将自动隐藏properties字段。
     * 
     * @param appId 应用ID请求头（必填）
     * @param fieldsCsv 导出列（逗号分隔，如 id,eventName,appId；为空则使用默认列）
     * @param filterEventName 按事件名过滤（可选）
     * @return CSV文件响应
     */
    @GetMapping(value = "/export.csv", produces = "text/csv")
    @Operation(summary = "导出事件CSV（按应用）", description = "根据 X-App-Id 导出该应用的事件CSV；支持 fields=列名逗号分隔 和 eventName 过滤")
    public ResponseEntity<byte[]> exportCsv(
            @Parameter(description = "应用头，必填") @RequestHeader(HttpHeaderConstants.HEADER_APP_ID) Integer appId,
            @Parameter(description = "导出列，逗号分隔，如 id,eventName,appId；为空则使用默认列") @RequestParam(name = "fields", required = false) String fieldsCsv,
            @Parameter(description = "按事件名过滤，可选") @RequestParam(name = "eventName", required = false) String filterEventName) {
        
        log.info("收到CSV导出请求: appId={}, fields={}, filterEventName={}", appId, fieldsCsv, filterEventName);
        
        List<Event> list = eventRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        List<Event> filtered = list.stream()
                .filter(e -> appId.equals(e.getAppId()))
                .filter(e -> filterEventName == null || filterEventName.equals(e.getEventName()))
                .collect(Collectors.toList());

        boolean hideProperties = currentHasRole("ROLE_READONLY");
        String[] selected = null;
        if (fieldsCsv != null && !fieldsCsv.isBlank()) {
            selected = fieldsCsv.split(",");
        }
        StringBuilder sb = new StringBuilder();
        if (selected != null) {
            // 按选择列输出表头
            sb.append(String.join(",", selected)).append('\n');
        } else {
            if (hideProperties) {
                sb.append("id,eventName,userId,sessionId,appId,eventTime,ua,referrer,ip,device,os,browser\n");
            } else {
                sb.append("id,eventName,userId,sessionId,appId,eventTime,ua,referrer,ip,device,os,browser,properties\n");
            }
        }
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        for (Event e : filtered) {
            if (selected != null) {
                // 动态列输出
                for (int i = 0; i < selected.length; i++) {
                    String col = selected[i].trim();
                    sb.append(csv(valueOf(col, e, fmt, hideProperties)));
                    if (i < selected.length - 1) sb.append(',');
                }
                sb.append('\n');
            } else {
                sb.append(csv(e.getId()))
                  .append(',').append(csv(e.getEventName()))
                  .append(',').append(csv(e.getUserId()))
                  .append(',').append(csv(e.getSessionId()))
                  .append(',').append(csv(e.getAppId()))
                  .append(',').append(csv(e.getEventTime() == null ? null : e.getEventTime().format(fmt)))
                  .append(',').append(csv(e.getUa()))
                  .append(',').append(csv(e.getReferrer()))
                  .append(',').append(csv(e.getIp()))
                  .append(',').append(csv(e.getDevice()))
                  .append(',').append(csv(e.getOs()))
                  .append(',').append(csv(e.getBrowser()));
                if (!hideProperties) {
                    sb.append(',').append(csv(e.getProperties()));
                }
                sb.append('\n');
            }
        }
        
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        
        log.info("CSV导出完成: appId={}, rowCount={}, size={} bytes", appId, filtered.size(), bytes.length);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=events.csv")
                .contentType(MediaType.valueOf("text/csv"))
                .body(bytes);
    }

    /**
     * 导出事件Parquet
     * 
     * <p>根据应用ID导出事件数据为Parquet格式（当前为最小可用实现）。
     * 
     * @param appId 应用ID请求头（必填）
     * @return Parquet文件响应
     */
    @GetMapping(value = "/export.parquet", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "导出事件Parquet（按应用）", description = "根据 X-App-Id 导出该应用的事件Parquet（最小可用实现）")
    public ResponseEntity<byte[]> exportParquet(@Parameter(description = "应用头，必填") @RequestHeader(HttpHeaderConstants.HEADER_APP_ID) Integer appId) {
        log.info("收到Parquet导出请求: appId={}", appId);
        
        // 为避免引入额外依赖，这里用极简二进制占位（实际可替换为 Apache Parquet Writer）
        List<Event> list = eventRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        List<Event> filtered = list.stream().filter(e -> appId.equals(e.getAppId())).collect(Collectors.toList());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // 伪Parquet头（魔数占位）+ 简单行计数 + 每行以换行分隔的JSON（可被后续真实Parquet替换）
        byte[] magic = new byte[]{'P','A','R','1'}; // placeholder magic
        out.writeBytes(magic);
        String header = "rows=" + filtered.size() + "\n";
        out.writeBytes(header.getBytes(StandardCharsets.UTF_8));
        for (Event e : filtered) {
            String line = "{" +
                    "\"id\":" + e.getId() + "," +
                    "\"eventName\":\"" + (e.getEventName()==null?"":e.getEventName()) + "\"," +
                    "\"appId\":" + e.getAppId() +
                    "}" + "\n";
            out.writeBytes(line.getBytes(StandardCharsets.UTF_8));
        }
        byte[] bytes = out.toByteArray();
        
        log.info("Parquet导出完成: appId={}, rowCount={}, size={} bytes", appId, filtered.size(), bytes.length);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=events.parquet")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    /**
     * 检查当前用户是否具有指定角色
     * 
     * @param role 角色名称
     * @return 如果用户具有该角色返回true，否则返回false
     */
    private boolean currentHasRole(String role) {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null) return false;
        for (GrantedAuthority ga : a.getAuthorities()) {
            if (role.equals(ga.getAuthority())) return true;
        }
        return false;
    }

    /**
     * 将对象转换为CSV格式的字符串（处理引号和换行）
     * 
     * @param v 对象值
     * @return CSV格式的字符串
     */
    private String csv(Object v) {
        if (v == null) return "";
        String s = v.toString();
        boolean needQuote = s.contains(",") || s.contains("\n") || s.contains("\"");
        if (needQuote) {
            s = s.replace("\"", "\"\"");
            return "\"" + s + "\"";
        }
        return s;
    }

    /**
     * 根据列名获取事件对象的字段值
     * 
     * @param col 列名
     * @param e 事件对象
     * @param fmt 日期时间格式化器
     * @param hideProps 是否隐藏properties字段
     * @return 字段值
     */
    private Object valueOf(String col, Event e, DateTimeFormatter fmt, boolean hideProps) {
        return switch (col) {
            case "id" -> e.getId();
            case "eventName" -> e.getEventName();
            case "userId" -> e.getUserId();
            case "sessionId" -> e.getSessionId();
            case "appId" -> e.getAppId();
            case "eventTime" -> e.getEventTime() == null ? null : e.getEventTime().format(fmt);
            case "ua" -> e.getUa();
            case "referrer" -> e.getReferrer();
            case "ip" -> e.getIp();
            case "device" -> e.getDevice();
            case "os" -> e.getOs();
            case "browser" -> e.getBrowser();
            case "properties" -> hideProps ? null : e.getProperties();
            default -> null;
        };
    }
}
