/**
 * 获取 DOM 元素的路径
 * 从元素向上遍历到根，生成唯一的选择器路径
 *
 * @param element - 目标元素
 * @returns DOM 路径字符串，格式：html > body > div > button#test-btn
 */
export function getDomPath(element: HTMLElement): string {
  const path: string[] = [];
  let current: HTMLElement | null = element;

  while (current && current.nodeType === Node.ELEMENT_NODE) {
    let selector = current.nodeName.toLowerCase();

    // 如果有 id，使用 id 作为选择器并终止（id 是唯一的）
    if (current.id) {
      selector += `#${current.id}`;
      path.unshift(selector);
      break;
    }

    // 否则使用标签名 + nth-of-type
    let sibling: HTMLElement | null = current;
    let nth = 1;
    while (sibling && sibling.previousElementSibling) {
      sibling = sibling.previousElementSibling as HTMLElement;
      if (sibling.nodeName.toLowerCase() === selector) {
        nth++;
      }
    }

    if (nth !== 1) {
      selector += `:nth-of-type(${nth})`;
    }

    path.unshift(selector);
    current = current.parentElement;
  }

  return path.join(" > ");
}
