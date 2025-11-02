package io.github.sijiezhong.track.dto;

import java.util.List;

public class PageResult<T> {
    private long total;
    private int page;
    private int size;
    private List<T> content;

    public PageResult() { }
    public PageResult(long total, int page, int size, List<T> content) {
        this.total = total;
        this.page = page;
        this.size = size;
        this.content = content;
    }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public List<T> getContent() { return content; }
    public void setContent(List<T> content) { this.content = content; }
}
