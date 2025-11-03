/**
 * 日志工具
 * @packageDocumentation
 */

/**
 * 日志级别
 */
export enum LogLevel {
  DEBUG = 'debug',
  INFO = 'info',
  WARN = 'warn',
  ERROR = 'error',
}

/**
 * 日志器类
 */
export class Logger {
  private enabled: boolean;
  private prefix: string;

  /**
   * 创建日志器实例
   * 
   * @param enabled - 是否启用日志
   * @param prefix - 日志前缀
   */
  constructor(enabled: boolean = false, prefix: string = '[TrackSDK]') {
    this.enabled = enabled;
    this.prefix = prefix;
  }

  /**
   * 设置是否启用日志
   * 
   * @param enabled - 是否启用
   */
  setEnabled(enabled: boolean): void {
    this.enabled = enabled;
  }

  /**
   * 输出调试日志
   * 
   * @param args - 日志参数
   */
  debug(...args: unknown[]): void {
    if (this.enabled) {
      console.debug(this.prefix, ...args);
    }
  }

  /**
   * 输出信息日志
   * 
   * @param args - 日志参数
   */
  info(...args: unknown[]): void {
    if (this.enabled) {
      console.info(this.prefix, ...args);
    }
  }

  /**
   * 输出警告日志
   * 
   * @param args - 日志参数
   */
  warn(...args: unknown[]): void {
    if (this.enabled) {
      console.warn(this.prefix, ...args);
    }
  }

  /**
   * 输出错误日志
   * 
   * @param args - 日志参数
   */
  error(...args: unknown[]): void {
    if (this.enabled) {
      console.error(this.prefix, ...args);
    }
  }

  /**
   * 输出日志（根据级别）
   * 
   * @param level - 日志级别
   * @param args - 日志参数
   */
  log(level: LogLevel, ...args: unknown[]): void {
    switch (level) {
      case LogLevel.DEBUG:
        this.debug(...args);
        break;
      case LogLevel.INFO:
        this.info(...args);
        break;
      case LogLevel.WARN:
        this.warn(...args);
        break;
      case LogLevel.ERROR:
        this.error(...args);
        break;
    }
  }
}

/**
 * 创建默认日志器实例
 * 
 * @param enabled - 是否启用
 * @returns 日志器实例
 */
export function createLogger(enabled: boolean = false): Logger {
  return new Logger(enabled);
}

