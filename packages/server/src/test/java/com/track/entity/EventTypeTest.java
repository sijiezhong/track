package com.track.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * EventType 枚举测试
 * 验证枚举值与客户端一致，确保数据一致性
 */
class EventTypeTest {
    
    @Test
    void testEnumValues() {
        // 验证所有枚举值存在
        assertEquals(6, EventType.values().length);
        assertNotNull(EventType.PAGE_VIEW);
        assertNotNull(EventType.CLICK);
        assertNotNull(EventType.PERFORMANCE);
        assertNotNull(EventType.ERROR);
        assertNotNull(EventType.CUSTOM);
        assertNotNull(EventType.PAGE_STAY);
    }
    
    @Test
    void testCodeValues() {
        // 验证 code 值与客户端保持一致（1-6）
        assertEquals(1, EventType.PAGE_VIEW.getCode());
        assertEquals(2, EventType.CLICK.getCode());
        assertEquals(3, EventType.PERFORMANCE.getCode());
        assertEquals(4, EventType.ERROR.getCode());
        assertEquals(5, EventType.CUSTOM.getCode());
        assertEquals(6, EventType.PAGE_STAY.getCode());
    }
    
    @Test
    void testNameValues() {
        // 验证 name 值与数据库一致
        assertEquals("page_view", EventType.PAGE_VIEW.getName());
        assertEquals("click", EventType.CLICK.getName());
        assertEquals("performance", EventType.PERFORMANCE.getName());
        assertEquals("error", EventType.ERROR.getName());
        assertEquals("custom", EventType.CUSTOM.getName());
        assertEquals("page_stay", EventType.PAGE_STAY.getName());
    }
    
    @Test
    void testFromCode() {
        // 验证 fromCode 方法正确性
        assertEquals(EventType.PAGE_VIEW, EventType.fromCode(1));
        assertEquals(EventType.CLICK, EventType.fromCode(2));
        assertEquals(EventType.PERFORMANCE, EventType.fromCode(3));
        assertEquals(EventType.ERROR, EventType.fromCode(4));
        assertEquals(EventType.CUSTOM, EventType.fromCode(5));
        assertEquals(EventType.PAGE_STAY, EventType.fromCode(6));
    }
    
    @Test
    void testFromCodeInvalid() {
        // 验证无效 code 抛出异常
        assertThrows(IllegalArgumentException.class, () -> EventType.fromCode(0));
        assertThrows(IllegalArgumentException.class, () -> EventType.fromCode(7));
        assertThrows(IllegalArgumentException.class, () -> EventType.fromCode(-1));
        assertThrows(IllegalArgumentException.class, () -> EventType.fromCode(99));
    }
    
    @Test
    void testCodeUniqueness() {
        // 验证所有 code 值唯一
        int[] codes = new int[EventType.values().length];
        for (int i = 0; i < EventType.values().length; i++) {
            codes[i] = EventType.values()[i].getCode();
        }
        
        for (int i = 0; i < codes.length; i++) {
            for (int j = i + 1; j < codes.length; j++) {
                assertNotEquals(codes[i], codes[j], 
                    "EventType codes must be unique");
            }
        }
    }
}

