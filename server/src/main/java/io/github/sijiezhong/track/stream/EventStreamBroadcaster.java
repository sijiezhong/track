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

    private final Map<Integer, List<SseEmitter>> appEmitters = new ConcurrentHashMap<>();
    private final Map<Integer, String> lastMessageByApp = new ConcurrentHashMap<>();

    public SseEmitter subscribe(int appId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT_MS);
        appEmitters.computeIfAbsent(appId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(appId, emitter));
        emitter.onTimeout(() -> removeEmitter(appId, emitter));
        emitter.onError(e -> removeEmitter(appId, emitter));
        try {
            emitter.send(SseEmitter.event().name("init").data("ok"));
        } catch (IOException ignored) {
        }
        return emitter;
    }

    public void broadcastEvent(Integer appId, Event event) {
        if (appId == null) return;
        String payload = toPayload(event);
        lastMessageByApp.put(appId, payload);
        List<SseEmitter> emitters = appEmitters.getOrDefault(appId, List.of());
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("event").data(payload));
            } catch (IOException e) {
                removeEmitter(appId, emitter);
            }
        }
    }

    public String getLastMessageForTenant(Integer appId) {
        return lastMessageByApp.get(appId);
    }

    private void removeEmitter(Integer appId, SseEmitter emitter) {
        List<SseEmitter> list = appEmitters.get(appId);
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


