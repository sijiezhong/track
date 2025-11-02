package io.github.sijiezhong.track.util;

public final class UserAgentParser {

    public static class ParsedUA {
        public final String device;
        public final String os;
        public final String browser;
        public ParsedUA(String device, String os, String browser) {
            this.device = device;
            this.os = os;
            this.browser = browser;
        }
    }

    private UserAgentParser() {}

    public static ParsedUA parse(String ua) {
        if (ua == null || ua.isBlank()) {
            return new ParsedUA(null, null, null);
        }
        String device = inferDevice(ua);
        String os = inferOs(ua);
        String browser = inferBrowser(ua);
        return new ParsedUA(device, os, browser);
    }

    private static String inferDevice(String ua) {
        String l = ua.toLowerCase();
        if (l.contains("mobile") || l.contains("iphone") || l.contains("android")) return "Mobile";
        if (l.contains("ipad") || l.contains("tablet")) return "Tablet";
        return "Desktop";
    }

    private static String inferOs(String ua) {
        if (ua.contains("iPhone") || ua.contains("iPad") || ua.contains("iOS")) return "iOS";
        if (ua.contains("Mac OS X") || ua.contains("Macintosh")) return "Mac OS";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("Linux")) return "Linux";
        return null;
    }

    private static String inferBrowser(String ua) {
        if (ua.contains("Chrome")) return "Chrome";
        if (ua.contains("Firefox")) return "Firefox";
        if (ua.contains("Safari") && !ua.contains("Chrome")) return "Safari";
        if (ua.contains("Edge")) return "Edge";
        return null;
    }
}


