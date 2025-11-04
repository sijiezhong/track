import { EventData } from "../types";
import { removeNulls } from "../utils";

/**
 * 发送器类
 * 负责将事件数据发送到服务端
 * 支持 sendBeacon 和 fetch 两种方式
 */
export class Sender {
  private endpoint: string;
  private onSessionExpired?: () => void;

  constructor(endpoint: string, onSessionExpired?: () => void) {
    this.endpoint = endpoint.replace(/\/$/, ""); // 移除末尾的斜杠
    this.onSessionExpired = onSessionExpired;
  }

  /**
   * 发送事件数据
   * 优先使用 sendBeacon，失败则使用 fetch POST
   *
   * @param events - 事件数组
   * @throws 当发送失败时抛出错误
   */
  async sendEvents(events: EventData[]): Promise<void> {
    if (events.length === 0) {
      return;
    }

    // 构建 payload（压缩字段名，移除 null/undefined）
    const payload = {
      e: events.map((e) => ({
        t: e.type,
        id: e.eventId || undefined,
        p: removeNulls(e.properties),
      })),
    };

    // 移除空值
    const cleanedPayload = removeNulls(payload);

    // 策略1: sendBeacon (优先使用，页面卸载时最可靠)
    if (typeof navigator !== "undefined" && navigator.sendBeacon) {
      const blob = new Blob([JSON.stringify(cleanedPayload)], {
        type: "application/json",
      });

      const url = `${this.endpoint}/api/ingest`;
      if (navigator.sendBeacon(url, blob)) {
        return; // 成功即返回
      }
    }

    // 策略2: fetch POST (支持错误处理和重试)
    try {
      const response = await fetch(`${this.endpoint}/api/ingest`, {
        method: "POST",
        mode: "cors",
        credentials: "include", // 重要：允许携带 Cookie
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(cleanedPayload),
        keepalive: true, // 页面卸载时也能发送
      });

      // 处理 session 失效（401/403）
      if (response.status === 401 || response.status === 403) {
        // Session 失效，触发重新 init
        if (this.onSessionExpired) {
          this.onSessionExpired();
        }
        throw new Error("Session expired");
      }

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
    } catch (error) {
      // 网络错误或 session 失效，重新抛出以便调用者处理
      throw error;
    }
  }

  /**
   * 更新端点地址
   */
  setEndpoint(endpoint: string): void {
    this.endpoint = endpoint.replace(/\/$/, "");
  }
}
