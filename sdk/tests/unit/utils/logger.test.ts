/**
 * 日志工具测试
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { Logger, createLogger, LogLevel } from '../../../src/utils/logger';

describe('Logger', () => {
  let logger: Logger;
  let consoleSpy: {
    debug: ReturnType<typeof vi.spyOn>;
    info: ReturnType<typeof vi.spyOn>;
    warn: ReturnType<typeof vi.spyOn>;
    error: ReturnType<typeof vi.spyOn>;
  };

  beforeEach(() => {
    consoleSpy = {
      debug: vi.spyOn(console, 'debug').mockImplementation(() => {}),
      info: vi.spyOn(console, 'info').mockImplementation(() => {}),
      warn: vi.spyOn(console, 'warn').mockImplementation(() => {}),
      error: vi.spyOn(console, 'error').mockImplementation(() => {}),
    };
  });

  afterEach(() => {
    Object.values(consoleSpy).forEach((spy) => spy.mockRestore());
    vi.clearAllMocks();
  });

  describe('启用状态', () => {
    it('启用时应该输出日志', () => {
      logger = new Logger(true);
      logger.debug('test');
      expect(consoleSpy.debug).toHaveBeenCalled();
    });

    it('禁用时不应该输出日志', () => {
      logger = new Logger(false);
      logger.debug('test');
      expect(consoleSpy.debug).not.toHaveBeenCalled();
    });

    it('setEnabled 应该更新启用状态', () => {
      logger = new Logger(false);
      logger.setEnabled(true);
      logger.debug('test');
      expect(consoleSpy.debug).toHaveBeenCalled();
    });
  });

  describe('日志级别', () => {
    beforeEach(() => {
      logger = new Logger(true);
    });

    it('debug 应该调用 console.debug', () => {
      logger.debug('debug message');
      expect(consoleSpy.debug).toHaveBeenCalledWith('[TrackSDK]', 'debug message');
    });

    it('info 应该调用 console.info', () => {
      logger.info('info message');
      expect(consoleSpy.info).toHaveBeenCalledWith('[TrackSDK]', 'info message');
    });

    it('warn 应该调用 console.warn', () => {
      logger.warn('warn message');
      expect(consoleSpy.warn).toHaveBeenCalledWith('[TrackSDK]', 'warn message');
    });

    it('error 应该调用 console.error', () => {
      logger.error('error message');
      expect(consoleSpy.error).toHaveBeenCalledWith('[TrackSDK]', 'error message');
    });

    it('log 应该根据级别调用对应方法', () => {
      logger.log(LogLevel.DEBUG, 'message');
      expect(consoleSpy.debug).toHaveBeenCalled();

      logger.log(LogLevel.INFO, 'message');
      expect(consoleSpy.info).toHaveBeenCalled();

      logger.log(LogLevel.WARN, 'message');
      expect(consoleSpy.warn).toHaveBeenCalled();

      logger.log(LogLevel.ERROR, 'message');
      expect(consoleSpy.error).toHaveBeenCalled();
    });
  });

  describe('createLogger', () => {
    it('应该创建默认日志器', () => {
      const defaultLogger = createLogger();
      expect(defaultLogger).toBeInstanceOf(Logger);
    });

    it('应该创建启用状态的日志器', () => {
      const enabledLogger = createLogger(true);
      enabledLogger.debug('test');
      expect(consoleSpy.debug).toHaveBeenCalled();
    });
  });
});
