import { useEffect, useState } from 'react';
import { PLACEHOLDER } from '@/utils/format';

type Props = React.ImgHTMLAttributes<HTMLImageElement> & {
  src?: string | null;
};

/**
 * 带兜底的商品图。
 *
 * 地址为空、或者地址失效（404 / 域名不存在）时自动换成灰底「暂无图片」，
 * 不会再出现浏览器默认的破图标。
 */
export default function SafeImage({ src, alt = '', ...rest }: Props) {
  const [failed, setFailed] = useState(false);

  // src 变化时重置失败状态，避免换图后仍显示占位图
  useEffect(() => setFailed(false), [src]);

  return (
    <img
      {...rest}
      src={!src || failed ? PLACEHOLDER : src}
      alt={alt}
      onError={(event) => {
        setFailed(true);
        rest.onError?.(event);
      }}
    />
  );
}
