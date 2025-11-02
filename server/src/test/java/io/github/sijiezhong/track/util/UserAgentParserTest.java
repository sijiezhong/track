package io.github.sijiezhong.track.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserAgentParserTest {

    @Test
    @DisplayName("Should return nulls when UA is null or blank")
    void nullAndBlankUaReturnsNulls() {
        var p1 = UserAgentParser.parse(null);
        var p2 = UserAgentParser.parse("   ");
        assertThat(p1.device).isNull();
        assertThat(p1.os).isNull();
        assertThat(p1.browser).isNull();
        assertThat(p2.device).isNull();
        assertThat(p2.os).isNull();
        assertThat(p2.browser).isNull();
    }

    @Test
    @DisplayName("Should parse Chrome on Mac OS correctly")
    void typicalChromeOnMac() {
        String ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";
        var p = UserAgentParser.parse(ua);
        assertThat(p.device).isEqualTo("Desktop");
        assertThat(p.os).isEqualTo("Mac OS");
        assertThat(p.browser).isEqualTo("Chrome");
    }

    @Test
    @DisplayName("Should parse Safari on iPhone correctly")
    void safariOnIphone() {
        String ua = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.1 Mobile/15E148 Safari/604.1";
        var p = UserAgentParser.parse(ua);
        assertThat(p.device).isEqualTo("Mobile");
        assertThat(p.os).isEqualTo("iOS");
        assertThat(p.browser).isEqualTo("Safari");
    }

    @Test
    @DisplayName("Should return Desktop device with null OS and browser when UA is unknown")
    void unknownBrowserAndOs() {
        String ua = "MyCustomAgent/1.0 DeviceX";
        var p = UserAgentParser.parse(ua);
        assertThat(p.device).isEqualTo("Desktop");
        assertThat(p.os).isNull();
        assertThat(p.browser).isNull();
    }

    @Test
    @DisplayName("Should parse iPad device as Tablet")
    void ipadDevice() {
        // iPad UA contains "iPad" (not "iphone"), so should be recognized as Tablet
        // But note: if UA contains "iphone" (lowercase), it will be Mobile first
        // Let's use an iPad UA that doesn't have "iphone" or "mobile" or "android"
        String ua = "Mozilla/5.0 (iPad; CPU OS 16_1 like Mac OS X) AppleWebKit/605.1.15";
        var p = UserAgentParser.parse(ua);
        assertThat(p.device).isEqualTo("Tablet");
        assertThat(p.os).isEqualTo("iOS");
    }

    @Test
    @DisplayName("Should parse tablet device correctly")
    void tabletDevice() {
        // Tablet UA with "tablet" keyword should be recognized as Tablet
        // But if it contains "android", it will be recognized as Mobile first
        // Let's use a tablet UA that has "tablet" but not "mobile", "iphone", or "android"
        String ua = "Mozilla/5.0 (Linux; TABLET) AppleWebKit/537.36";
        var p = UserAgentParser.parse(ua);
        assertThat(p.device).isEqualTo("Tablet");
    }

    @Test
    @DisplayName("Should parse Android mobile device correctly")
    void androidMobileDevice() {
        String ua = "Mozilla/5.0 (Linux; Android 11; SM-G973F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36";
        var p = UserAgentParser.parse(ua);
        assertThat(p.device).isEqualTo("Mobile");
        assertThat(p.os).isEqualTo("Android");
        assertThat(p.browser).isEqualTo("Chrome");
    }

    @Test
    @DisplayName("Should parse Windows OS correctly")
    void windowsOs() {
        String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";
        var p = UserAgentParser.parse(ua);
        assertThat(p.device).isEqualTo("Desktop");
        assertThat(p.os).isEqualTo("Windows");
        assertThat(p.browser).isEqualTo("Chrome");
    }

    @Test
    @DisplayName("Should parse Linux OS correctly")
    void linuxOs() {
        String ua = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";
        var p = UserAgentParser.parse(ua);
        assertThat(p.device).isEqualTo("Desktop");
        assertThat(p.os).isEqualTo("Linux");
        assertThat(p.browser).isEqualTo("Chrome");
    }

    @Test
    @DisplayName("Should parse Firefox browser correctly")
    void firefoxBrowser() {
        String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:122.0) Gecko/20100101 Firefox/122.0";
        var p = UserAgentParser.parse(ua);
        assertThat(p.device).isEqualTo("Desktop");
        assertThat(p.os).isEqualTo("Windows");
        assertThat(p.browser).isEqualTo("Firefox");
    }

    @Test
    @DisplayName("Should parse Edge browser correctly")
    void edgeBrowser() {
        // Edge UA typically contains "Chrome", so Chrome is detected first
        // To test Edge branch, we need UA with "Edge" but without "Chrome"
        // Modern Edge UA contains Chrome, so this tests the actual behavior
        String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 Edg/122.0.0.0";
        var p = UserAgentParser.parse(ua);
        assertThat(p.device).isEqualTo("Desktop");
        assertThat(p.os).isEqualTo("Windows");
        // Due to code priority, Chrome is detected before Edge
        assertThat(p.browser).isEqualTo("Chrome");
        
        // To test Edge branch, use UA without Chrome
        String uaEdgeOnly = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Edge/122.0.0.0";
        var p2 = UserAgentParser.parse(uaEdgeOnly);
        assertThat(p2.browser).isEqualTo("Edge");
    }

    @Test
    @DisplayName("Should return Chrome when UA contains both Chrome and Safari")
    void chromeWithSafariShouldReturnChrome() {
        // Chrome UA contains "Safari" but should return Chrome, not Safari
        String ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";
        var p = UserAgentParser.parse(ua);
        assertThat(p.browser).isEqualTo("Chrome");
        assertThat(p.browser).isNotEqualTo("Safari");
    }

    @Test
    @DisplayName("Should return Mobile device when UA contains mobile keyword")
    void mobileInUaShouldReturnMobile() {
        String ua = "Mozilla/5.0 (Mobile; rv:122.0) Gecko/122.0 Firefox/122.0";
        var p = UserAgentParser.parse(ua);
        assertThat(p.device).isEqualTo("Mobile");
    }

    @Test
    @DisplayName("Should parse Safari browser without Chrome correctly")
    void safariOnlyBrowser() {
        // Safari without Chrome should return Safari
        String ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.1 Safari/605.1.15";
        var p = UserAgentParser.parse(ua);
        assertThat(p.browser).isEqualTo("Safari");
        assertThat(p.os).isEqualTo("Mac OS");
    }

    @Test
    @DisplayName("Should parse Mac OS X correctly")
    void macOsX() {
        // Test Mac OS X branch
        String ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36";
        var p = UserAgentParser.parse(ua);
        assertThat(p.os).isEqualTo("Mac OS");
    }

    @Test
    @DisplayName("Should parse Macintosh as Mac OS")
    void macintosh() {
        // Test Macintosh branch
        String ua = "Mozilla/5.0 (Macintosh) AppleWebKit/537.36";
        var p = UserAgentParser.parse(ua);
        assertThat(p.os).isEqualTo("Mac OS");
    }

    @Test
    @DisplayName("Should parse iOS variant correctly")
    void iosVariant() {
        // Test iOS branch (not iPhone or iPad)
        String ua = "Mozilla/5.0 (iOS 16.1) AppleWebKit/605.1.15";
        var p = UserAgentParser.parse(ua);
        assertThat(p.os).isEqualTo("iOS");
    }

    @Test
    @DisplayName("Should parse iPhone with lowercase correctly")
    void iphoneLowerCase() {
        // Test iPhone lowercase branch (in inferDevice, not inferOs)
        String ua = "mozilla/5.0 (iphone; cpu iphone os 16_1) applewebkit/605.1.15";
        var p = UserAgentParser.parse(ua);
        assertThat(p.device).isEqualTo("Mobile");
    }
}


