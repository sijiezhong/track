package io.github.sijiezhong.track.testsupport;

import java.time.LocalDateTime;

/**
 * Test constants for commonly used test data values.
 * Provides consistent test data across all test classes.
 */
public final class TestConstants {
    
    private TestConstants() {
        // Utility class, prevent instantiation
    }
    
    // Tenant IDs
    public static final Integer DEFAULT_TENANT_ID = 1;
    public static final Integer SECOND_TENANT_ID = 2;
    public static final Integer THIRD_TENANT_ID = 3;
    
    // User IDs
    public static final Integer DEFAULT_USER_ID = 10;
    public static final Integer SECOND_USER_ID = 11;
    public static final Integer THIRD_USER_ID = 12;
    
    // Session IDs
    public static final String DEFAULT_SESSION_ID = "test-session-1";
    public static final String SECOND_SESSION_ID = "test-session-2";
    public static final String THIRD_SESSION_ID = "test-session-3";
    
    // Event Names
    public static final String EVENT_PAGE_VIEW = "page_view";
    public static final String EVENT_CLICK = "click";
    public static final String EVENT_SUBMIT = "submit";
    public static final String EVENT_PURCHASE = "purchase";
    public static final String EVENT_ADD_TO_CART = "add_to_cart";
    
    // Properties
    public static final String DEFAULT_PROPERTIES = "{}";
    public static final String PROPERTIES_WITH_URL = "{\"url\":\"/home\"}";
    public static final String PROPERTIES_WITH_PRODUCT = "{\"url\":\"/prod\"}";
    
    // User Agent
    public static final String DEFAULT_UA = "Mozilla/5.0 TestUA";
    public static final String UA_CHROME = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    public static final String UA_MOBILE = "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X)";
    
    // IP Addresses
    public static final String DEFAULT_IP = "192.168.1.1";
    public static final String IP_198_51_100_1 = "198.51.100.1";
    public static final String IP_198_51_100_2 = "198.51.100.2";
    
    // Device/OS/Browser
    public static final String DEVICE_DESKTOP = "Desktop";
    public static final String DEVICE_MOBILE = "Mobile";
    public static final String OS_MAC = "Mac OS";
    public static final String OS_WINDOWS = "Windows";
    public static final String OS_ANDROID = "Android";
    public static final String BROWSER_CHROME = "Chrome";
    public static final String BROWSER_FIREFOX = "Firefox";
    public static final String CHANNEL_WEB = "web";
    public static final String CHANNEL_MOBILE = "mobile";
    
    // Referrers
    public static final String DEFAULT_REFERRER = "https://example.com";
    public static final String REFERRER_HOME = "https://ref1";
    public static final String REFERRER_PRODUCT = "https://ref2";
    
    // Fixed Time (for deterministic tests)
    public static final LocalDateTime FIXED_TIME = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
    
    // Webhook
    public static final String DEFAULT_WEBHOOK_URL = "https://example.com/webhook";
    public static final String DEFAULT_WEBHOOK_SECRET = "test-secret-key";
}

