package io.github.sijiezhong.track.testsupport;

import org.springframework.test.web.servlet.ResultMatcher;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Common assertion utilities for testing.
 * 
 * <p>This class provides reusable assertion methods to reduce code duplication
 * and improve test readability. It focuses on common patterns like:
 * <ul>
 *   <li>Pagination response validation</li>
 *   <li>JSON response structure validation</li>
 *   <li>HTTP status code validation</li>
 *   <li>Event data validation</li>
 * </ul>
 * 
 * <p>Usage example:
 * <pre>
 * mockMvc.perform(get("/api/v1/events").header("X-Tenant-Id", "1"))
 *     .andExpect(TestAssertions.successfulPaginationResponse(0, 20))
 *     .andExpect(TestAssertions.hasEventWithDetails(1L, "page_view", 1, 100L));
 * </pre>
 */
public final class TestAssertions {

    private TestAssertions() {
        // Utility class - prevent instantiation
    }

    /**
     * Asserts that the response has a successful pagination structure with the given parameters.
     * Supports both old format (direct PageResult) and new format (ApiResponse<PageResult>).
     * 
     * @param expectedTotal Expected total number of items
     * @param expectedPage Expected page number (0-based)
     * @param expectedSize Expected page size
     * @return ResultMatcher for pagination validation
     */
    public static ResultMatcher successfulPaginationResponse(long expectedTotal, int expectedPage, int expectedSize) {
        return result -> {
            status().isOk().match(result);
            // Try new format first (ApiResponse wrapper)
            try {
                jsonPath("$.code").value(200).match(result);
                jsonPath("$.data.total").value(expectedTotal).match(result);
                jsonPath("$.data.page").value(expectedPage).match(result);
                jsonPath("$.data.size").value(expectedSize).match(result);
                jsonPath("$.data.content").isArray().match(result);
            } catch (AssertionError e) {
                // Fallback to old format (direct PageResult)
                jsonPath("$.total").value(expectedTotal).match(result);
                jsonPath("$.page").value(expectedPage).match(result);
                jsonPath("$.size").value(expectedSize).match(result);
                jsonPath("$.content").isArray().match(result);
            }
        };
    }

    /**
     * Asserts that the response has an empty pagination result.
     * Supports both old format (direct PageResult) and new format (ApiResponse<PageResult>).
     * 
     * @param expectedPage Expected page number (0-based)
     * @param expectedSize Expected page size
     * @return ResultMatcher for empty pagination validation
     */
    public static ResultMatcher emptyPaginationResponse(int expectedPage, int expectedSize) {
        return result -> {
            status().isOk().match(result);
            // Try new format first (ApiResponse wrapper)
            try {
                jsonPath("$.code").value(200).match(result);
                jsonPath("$.data.total").value(0).match(result);
                jsonPath("$.data.page").value(expectedPage).match(result);
                jsonPath("$.data.size").value(expectedSize).match(result);
                jsonPath("$.data.content").isArray().match(result);
                jsonPath("$.data.content").isEmpty().match(result);
            } catch (AssertionError e) {
                // Fallback to old format (direct PageResult)
                jsonPath("$.total").value(0).match(result);
                jsonPath("$.page").value(expectedPage).match(result);
                jsonPath("$.size").value(expectedSize).match(result);
                jsonPath("$.content").isArray().match(result);
                jsonPath("$.content").isEmpty().match(result);
            }
        };
    }

    /**
     * Asserts that the pagination content has the expected size.
     * Supports both old format (direct PageResult) and new format (ApiResponse<PageResult>).
     * 
     * @param expectedSize Expected number of items in content array
     * @return ResultMatcher for content size validation
     */
    public static ResultMatcher paginationContentSize(int expectedSize) {
        return result -> {
            // Try new format first (ApiResponse wrapper)
            try {
                jsonPath("$.code").value(200).match(result);
                jsonPath("$.data.content.length()").value(expectedSize).match(result);
            } catch (AssertionError e) {
                // Fallback to old format (direct PageResult)
                jsonPath("$.content.length()").value(expectedSize).match(result);
            }
        };
    }

    /**
     * Asserts that an event at the specified index has the given details.
     * Supports both old format (direct PageResult) and new format (ApiResponse<PageResult>).
     * 
     * @param index Index in the content array
     * @param expectedId Expected event ID
     * @param expectedEventName Expected event name
     * @param expectedTenantId Expected tenant ID
     * @return ResultMatcher for event details validation
     */
    public static ResultMatcher eventAtIndex(int index, Long expectedId, String expectedEventName, Integer expectedTenantId) {
        return result -> {
            // Try new format first (ApiResponse wrapper)
            try {
                jsonPath("$.code").value(200).match(result);
                jsonPath("$.data.content[" + index + "].id").value(expectedId).match(result);
                jsonPath("$.data.content[" + index + "].eventName").value(expectedEventName).match(result);
                if (expectedTenantId != null) {
                    jsonPath("$.data.content[" + index + "].tenantId").value(expectedTenantId).match(result);
                }
                jsonPath("$.data.content[" + index + "].properties").exists().match(result);
            } catch (AssertionError e) {
                // Fallback to old format (direct PageResult)
                jsonPath("$.content[" + index + "].id").value(expectedId).match(result);
                jsonPath("$.content[" + index + "].eventName").value(expectedEventName).match(result);
                if (expectedTenantId != null) {
                    jsonPath("$.content[" + index + "].tenantId").value(expectedTenantId).match(result);
                }
                jsonPath("$.content[" + index + "].properties").exists().match(result);
            }
        };
    }

    /**
     * Asserts that an event at the specified index has all common fields.
     * Supports both old format (direct PageResult) and new format (ApiResponse<PageResult>).
     * 
     * @param index Index in the content array
     * @param expectedId Expected event ID
     * @param expectedEventName Expected event name
     * @param expectedUserId Expected user ID
     * @param expectedSessionId Expected session ID
     * @param expectedTenantId Expected tenant ID
     * @return ResultMatcher for complete event validation
     */
    public static ResultMatcher eventAtIndex(int index, Long expectedId, String expectedEventName, 
                                             Integer expectedUserId, Long expectedSessionId, Integer expectedTenantId) {
        return result -> {
            // Try new format first (ApiResponse wrapper)
            try {
                jsonPath("$.code").value(200).match(result);
                jsonPath("$.data.content[" + index + "].id").value(expectedId).match(result);
                jsonPath("$.data.content[" + index + "].eventName").value(expectedEventName).match(result);
                if (expectedUserId != null) {
                    jsonPath("$.data.content[" + index + "].userId").value(expectedUserId).match(result);
                }
                if (expectedSessionId != null) {
                    jsonPath("$.data.content[" + index + "].sessionId").value(expectedSessionId).match(result);
                }
                if (expectedTenantId != null) {
                    jsonPath("$.data.content[" + index + "].tenantId").value(expectedTenantId).match(result);
                }
                jsonPath("$.data.content[" + index + "].properties").exists().match(result);
            } catch (AssertionError e) {
                // Fallback to old format (direct PageResult)
                jsonPath("$.content[" + index + "].id").value(expectedId).match(result);
                jsonPath("$.content[" + index + "].eventName").value(expectedEventName).match(result);
                if (expectedUserId != null) {
                    jsonPath("$.content[" + index + "].userId").value(expectedUserId).match(result);
                }
                if (expectedSessionId != null) {
                    jsonPath("$.content[" + index + "].sessionId").value(expectedSessionId).match(result);
                }
                if (expectedTenantId != null) {
                    jsonPath("$.content[" + index + "].tenantId").value(expectedTenantId).match(result);
                }
                jsonPath("$.content[" + index + "].properties").exists().match(result);
            }
        };
    }

    /**
     * Asserts that the response has a successful status code (200 OK).
     * 
     * @return ResultMatcher for success status
     */
    public static ResultMatcher successfulResponse() {
        return status().isOk();
    }

    /**
     * Asserts that the response has a created status code (201 Created).
     * 
     * @return ResultMatcher for created status
     */
    public static ResultMatcher createdResponse() {
        return status().isCreated();
    }

    /**
     * Asserts that the response has a bad request status code (400 Bad Request).
     * 
     * @return ResultMatcher for bad request status
     */
    public static ResultMatcher badRequestResponse() {
        return status().isBadRequest();
    }

    /**
     * Asserts that the response has a forbidden status code (403 Forbidden).
     * 
     * @return ResultMatcher for forbidden status
     */
    public static ResultMatcher forbiddenResponse() {
        return status().isForbidden();
    }

    /**
     * Asserts that the response has a not found status code (404 Not Found).
     * 
     * @return ResultMatcher for not found status
     */
    public static ResultMatcher notFoundResponse() {
        return status().isNotFound();
    }

    /**
     * Asserts that a JSON path exists in the response.
     * 
     * @param jsonPath JSON path expression
     * @return ResultMatcher for path existence
     */
    public static ResultMatcher jsonPathExists(String jsonPath) {
        return jsonPath(jsonPath).exists();
    }

    /**
     * Asserts that a JSON path has the expected value.
     * 
     * @param jsonPath JSON path expression
     * @param expectedValue Expected value
     * @return ResultMatcher for path value validation
     */
    public static ResultMatcher jsonPathValue(String jsonPath, Object expectedValue) {
        return jsonPath(jsonPath).value(expectedValue);
    }

    /**
     * Asserts that a JSON path is an array.
     * 
     * @param jsonPath JSON path expression
     * @return ResultMatcher for array validation
     */
    public static ResultMatcher jsonPathIsArray(String jsonPath) {
        return jsonPath(jsonPath).isArray();
    }

    /**
     * Asserts that a JSON path array is empty.
     * 
     * @param jsonPath JSON path expression
     * @return ResultMatcher for empty array validation
     */
    public static ResultMatcher jsonPathIsEmpty(String jsonPath) {
        return jsonPath(jsonPath).isEmpty();
    }

    /**
     * Asserts that the response has a pagination structure with content containing exactly N items.
     * Supports both old format (direct PageResult) and new format (ApiResponse<PageResult>).
     * 
     * @param expectedTotal Expected total count
     * @param expectedPage Expected page number
     * @param expectedSize Expected page size
     * @param expectedContentSize Expected number of items in content array
     * @return ResultMatcher that validates pagination structure with content size
     */
    public static ResultMatcher paginationWithContent(long expectedTotal, int expectedPage, int expectedSize, int expectedContentSize) {
        return result -> {
            successfulPaginationResponse(expectedTotal, expectedPage, expectedSize).match(result);
            paginationContentSize(expectedContentSize).match(result);
        };
    }
    
    /**
     * 检查响应是否为ApiResponse格式（新格式）
     * 
     * @param result MockMvc结果
     * @return 如果是ApiResponse格式返回true，否则返回false
     */
    private static boolean isApiResponseFormat(org.springframework.test.web.servlet.MvcResult result) {
        try {
            String content = result.getResponse().getContentAsString();
            if (content == null || content.isEmpty()) {
                return false;
            }
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(content);
            return root.has("code") && root.has("data") && root.has("message");
        } catch (Exception e) {
            return false;
        }
    }
}

