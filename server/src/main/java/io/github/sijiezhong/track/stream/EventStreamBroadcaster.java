package io.github.sijiezhong.track.stream;

import io.github.sijiezhong.track.domain.Event;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class EventStreamBroadcaster {

    private static final long DEFAULT_TIMEOUT_MS = Duration.ofMinutes(30).toMillis();

    private final Map<Integer, List<SseEmitter>> tenantEmitters = new ConcurrentHashMap<>();
    private final Map<Integer, String> lastMessageByTenant = new ConcurrentHashMap<>();

    public SseEmitter subscribe(int tenantId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT_MS);
        tenantEmitters.computeIfAbsent(tenantId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(tenantId, emitter));
        emitter.onTimeout(() -> removeEmitter(tenantId, emitter));
        emitter.onError(e -> removeEmitter(tenantId, emitter));
        try {
            emitter.send(SseEmitter.event().name("init").data("ok"));
        } catch (IOException ignored) {
        }
        return emitter;
    }

    public void broadcastEvent(Integer tenantId, Event event) {
        if (tenantId == null) return;
        String payload = toPayload(event);
        lastMessageByTenant.put(tenantId, payload);
        List<SseEmitter> emitters = tenantEmitters.getOrDefault(tenantId, List.of());
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("event").data(payload));
            } catch (IOException e) {
                removeEmitter(tenantId, emitter);
            }
        }
    }

    public String getLastMessageForTenant(Integer tenantId) {
        return lastMessageByTenant.get(tenantId);
    }

    private void removeEmitter(Integer tenantId, SseEmitter emitter) {
        List<SseEmitter> list = tenantEmitters.get(tenantId);
        if (list != null) {
            list.remove(emitter);
        }
    }

    private String toPayload(Event e) {
        String name = e.getEventName();
        Long id = e.getId();
        String t = e.getEventTime() == null ? null : e.getEventTime().toString();
        return "{" +
                "\"id\":" + id + "," +
                "\"eventName\":\"" + (name == null ? "" : name) + "\"," +
                "\"eventTime\":\"" + (t == null ? "" : t) + "\"" +
                "}";
    }
}


