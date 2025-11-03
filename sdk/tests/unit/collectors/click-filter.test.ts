/**
 * 点击过滤器测试
 */

import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { ClickFilter } from '../../../src/collectors/click-filter';
import { ClickFilterConfig } from '../../../src/types';

describe('ClickFilter', () => {
  beforeEach(() => {
    // 清理 DOM
    document.body.innerHTML = '';
  });

  afterEach(() => {
    // 清理 DOM
    document.body.innerHTML = '';
  });

  describe('白名单测试', () => {
    it('应该追踪白名单中的元素', () => {
      const filter = new ClickFilter({
        enabled: true,
        whitelist: ['.track-me', '#important-button'],
      });

      const div = document.createElement('div');
      div.className = 'track-me';
      document.body.appendChild(div);

      expect(filter.shouldTrack(div)).toBe(true);
    });

    it('应该追踪父元素在白名单中的元素', () => {
      const filter = new ClickFilter({
        enabled: true,
        whitelist: ['.track-me'],
      });

      const parent = document.createElement('div');
      parent.className = 'track-me';
      const child = document.createElement('span');
      parent.appendChild(child);
      document.body.appendChild(parent);

      expect(filter.shouldTrack(child)).toBe(true);
    });

    it('应该追踪有 data-track 属性的元素', () => {
      const filter = new ClickFilter({
        enabled: true,
      });

      const div = document.createElement('div');
      div.setAttribute('data-track', '');
      document.body.appendChild(div);

      expect(filter.shouldTrack(div)).toBe(true);
    });
  });

  describe('黑名单测试', () => {
    it('应该过滤黑名单中的元素', () => {
      const filter = new ClickFilter({
        enabled: true,
        blacklist: ['.ignore-me', '#no-track'],
      });

      const div = document.createElement('div');
      div.className = 'ignore-me';
      document.body.appendChild(div);

      expect(filter.shouldTrack(div)).toBe(false);
    });

    it('应该过滤父元素在黑名单中的元素', () => {
      const filter = new ClickFilter({
        enabled: true,
        blacklist: ['.ignore-me'],
      });

      const parent = document.createElement('div');
      parent.className = 'ignore-me';
      const child = document.createElement('span');
      parent.appendChild(child);
      document.body.appendChild(parent);

      expect(filter.shouldTrack(child)).toBe(false);
    });

    it('应该过滤有 data-track-ignore 属性的元素', () => {
      const filter = new ClickFilter({
        enabled: true,
      });

      const div = document.createElement('div');
      div.setAttribute('data-track-ignore', '');
      document.body.appendChild(div);

      expect(filter.shouldTrack(div)).toBe(false);
    });

    it('应该过滤 script、style、meta 等标签', () => {
      const filter = new ClickFilter({
        enabled: true,
      });

      const script = document.createElement('script');
      document.body.appendChild(script);

      expect(filter.shouldTrack(script)).toBe(false);
    });
  });

  describe('元素可见性测试', () => {
    it('应该过滤 display: none 的元素', () => {
      const filter = new ClickFilter({
        enabled: true,
      });

      const div = document.createElement('div');
      div.style.display = 'none';
      document.body.appendChild(div);

      expect(filter.shouldTrack(div)).toBe(false);
    });

    it('应该过滤 visibility: hidden 的元素', () => {
      const filter = new ClickFilter({
        enabled: true,
      });

      const div = document.createElement('div');
      div.style.visibility = 'hidden';
      document.body.appendChild(div);

      expect(filter.shouldTrack(div)).toBe(false);
    });

    it('应该过滤 opacity: 0 的元素', () => {
      const filter = new ClickFilter({
        enabled: true,
      });

      const div = document.createElement('div');
      div.style.opacity = '0';
      document.body.appendChild(div);

      expect(filter.shouldTrack(div)).toBe(false);
    });
  });

  describe('元素尺寸测试', () => {
    it('应该过滤过小的元素', () => {
      const filter = new ClickFilter({
        enabled: true,
        minClickSize: 10,
      });

      const div = document.createElement('div');
      div.style.width = '5px';
      div.style.height = '5px';
      document.body.appendChild(div);

      expect(filter.shouldTrack(div)).toBe(false);
    });

    it('应该追踪足够大的元素', () => {
      const filter = new ClickFilter({
        enabled: true,
        minClickSize: 10,
      });

      const div = document.createElement('div');
      div.style.width = '20px';
      div.style.height = '20px';
      document.body.appendChild(div);

      // 虽然是 div，但如果足够大且可见，根据默认规则可能会被过滤
      // 但这里我们主要测试尺寸检查逻辑
      // 实际上如果 div 不是交互元素且没有交互意图，会被过滤
      // 所以我们需要创建一个有交互意图的元素
      const button = document.createElement('button');
      button.style.width = '20px';
      button.style.height = '20px';
      document.body.appendChild(button);

      expect(filter.shouldTrack(button)).toBe(true);
    });
  });

  describe('交互元素测试', () => {
    it('应该追踪链接元素', () => {
      const filter = new ClickFilter({
        enabled: true,
      });

      const link = document.createElement('a');
      link.href = 'https://example.com';
      document.body.appendChild(link);

      expect(filter.shouldTrack(link)).toBe(true);
    });

    it('应该追踪按钮元素', () => {
      const filter = new ClickFilter({
        enabled: true,
      });

      const button = document.createElement('button');
      document.body.appendChild(button);

      expect(filter.shouldTrack(button)).toBe(true);
    });

    it('应该追踪表单控件', () => {
      const filter = new ClickFilter({
        enabled: true,
      });

      const input = document.createElement('input');
      input.type = 'text';
      document.body.appendChild(input);

      expect(filter.shouldTrack(input)).toBe(true);
    });

    it('应该过滤 hidden 类型的 input', () => {
      const filter = new ClickFilter({
        enabled: true,
      });

      const input = document.createElement('input');
      input.type = 'hidden';
      document.body.appendChild(input);

      expect(filter.shouldTrack(input)).toBe(false);
    });
  });

  describe('交互意图测试', () => {
    it('应该追踪有 cursor: pointer 样式的元素', () => {
      const filter = new ClickFilter({
        enabled: true,
      });

      const div = document.createElement('div');
      div.style.cursor = 'pointer';
      document.body.appendChild(div);

      expect(filter.shouldTrack(div)).toBe(true);
    });

    it('应该追踪有点击处理器的元素', () => {
      const filter = new ClickFilter({
        enabled: true,
      });

      const div = document.createElement('div');
      div.onclick = () => {};
      document.body.appendChild(div);

      expect(filter.shouldTrack(div)).toBe(true);
    });

    it('应该追踪有 tabindex 的元素', () => {
      const filter = new ClickFilter({
        enabled: true,
      });

      const div = document.createElement('div');
      div.setAttribute('tabindex', '0');
      document.body.appendChild(div);

      expect(filter.shouldTrack(div)).toBe(true);
    });
  });

  describe('空白区域测试', () => {
    it('应该过滤直接点击 body 元素', () => {
      const filter = new ClickFilter({
        enabled: true,
      });

      expect(filter.shouldTrack(document.body)).toBe(false);
    });

    it('应该过滤直接点击 html 元素', () => {
      const filter = new ClickFilter({
        enabled: true,
      });

      const html = document.documentElement;
      expect(filter.shouldTrack(html)).toBe(false);
    });
  });

  describe('过滤禁用测试', () => {
    it('禁用过滤时应该追踪所有元素', () => {
      const filter = new ClickFilter({
        enabled: false,
      });

      const div = document.createElement('div');
      div.style.display = 'none';
      document.body.appendChild(div);

      // 即使元素不可见，如果过滤禁用，也应该追踪
      expect(filter.shouldTrack(div)).toBe(true);
    });
  });

  describe('优先级测试', () => {
    it('白名单应该优先于黑名单', () => {
      const filter = new ClickFilter({
        enabled: true,
        whitelist: ['.track-me'],
        blacklist: ['.track-me'],
      });

      const div = document.createElement('div');
      div.className = 'track-me';
      document.body.appendChild(div);

      // 白名单优先，应该追踪
      expect(filter.shouldTrack(div)).toBe(true);
    });

    it('白名单应该优先于默认规则', () => {
      const filter = new ClickFilter({
        enabled: true,
        whitelist: ['.non-interactive'],
      });

      const div = document.createElement('div');
      div.className = 'non-interactive';
      // 没有交互意图
      div.style.display = 'block';
      div.style.width = '20px';
      div.style.height = '20px';
      document.body.appendChild(div);

      // 白名单优先，应该追踪
      expect(filter.shouldTrack(div)).toBe(true);
    });

    it('黑名单应该优先于默认规则', () => {
      const filter = new ClickFilter({
        enabled: true,
        blacklist: ['.ignore-button'],
      });

      const button = document.createElement('button');
      button.className = 'ignore-button';
      document.body.appendChild(button);

      // 黑名单优先，应该过滤
      expect(filter.shouldTrack(button)).toBe(false);
    });
  });

  describe('自定义属性名称测试', () => {
    it('应该支持自定义忽略属性名称', () => {
      const filter = new ClickFilter({
        enabled: true,
        ignoreAttribute: 'data-no-track',
      });

      const div = document.createElement('div');
      div.setAttribute('data-no-track', '');
      document.body.appendChild(div);

      expect(filter.shouldTrack(div)).toBe(false);
    });

    it('应该支持自定义追踪属性名称', () => {
      const filter = new ClickFilter({
        enabled: true,
        trackAttribute: 'data-force-track',
      });

      const div = document.createElement('div');
      div.setAttribute('data-force-track', '');
      document.body.appendChild(div);

      expect(filter.shouldTrack(div)).toBe(true);
    });
  });
});

