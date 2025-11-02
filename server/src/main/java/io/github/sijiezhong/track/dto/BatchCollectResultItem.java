package io.github.sijiezhong.track.dto;

/**
 * 批量上报结果条目
 */
public class BatchCollectResultItem {
    private int index; // 原请求数组中的下标
    private String status; // created/failed
    private String message; // 失败原因（可选）

    public BatchCollectResultItem() {}

    public BatchCollectResultItem(int index, String status, String message) {
        this.index = index;
        this.status = status;
        this.message = message;
    }

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
