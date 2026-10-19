/**
 * 中国行政区划（省 / 市 / 区县）离线数据。
 *
 * <p>数据随前端打包，**不依赖任何第三方接口**：地址选择器打开时才动态加载
 * （Vite 会把它拆成独立 chunk），首屏不受影响。</p>
 *
 * <p>覆盖：大陆 31 个省市区 + 香港 / 澳门 / 台湾，共 34 个省级、365 个市级、3082 个区县。
 * 直辖市已压平为「北京市 / 北京市 / 朝阳区」，避免出现「市辖区」这种中间层；
 * 台湾省下只有县市两级，区县可为空。</p>
 *
 * <p>数据来源：<code>china-division@2.7.0</code>（国家统计局行政区划，2024 版），
 * 生成脚本见 <code>scripts/gen-regions.py</code>。行政区划调整时重跑脚本即可。</p>
 */

export interface RegionNode {
  name: string;
  children?: RegionNode[];
}

/** 省市区三级路径，缺失的层级不占位（台湾只有两级） */
export type RegionPath = [string, string] | [string, string, string];

let cache: RegionNode[] | null = null;

/** 加载（或复用）省市区数据。首次调用会拉取独立 chunk。 */
export async function loadRegions(): Promise<RegionNode[]> {
  if (cache) return cache;
  // 兼容两种模块形状：dev 下 Vite 把 JSON 转成 `export default`，个别打包器会直接给数组
  const mod = (await import('./regions.json')) as unknown as RegionNode[] | { default: RegionNode[] };
  cache = Array.isArray(mod) ? mod : mod.default;
  return cache;
}

/** 同步读取已加载的数据（未加载时为 undefined），用于编辑时立即回显 */
export function regionsOf(): RegionNode[] | undefined {
  return cache ?? undefined;
}

/** Cascader 的字段映射：直接用名称当 value，省市区名称在同级下唯一 */
export const REGION_FIELD_NAMES = { label: 'name', value: 'name', children: 'children' } as const;

/** 去掉「省 / 市 / 自治区」等后缀，用于容错匹配历史脏数据 */
function bare(name: string): string {
  return name.replace(/(特别行政区|维吾尔自治区|壮族自治区|回族自治区|自治区|省|市|地区|盟|自治州|区|县)$/u, '');
}

function pick(nodes: RegionNode[] | undefined, target: string): RegionNode | undefined {
  if (!nodes?.length) return undefined;
  const t = target.trim();
  return nodes.find((n) => n.name === t) ?? nodes.find((n) => bare(n.name) === bare(t));
}

/**
 * 把数据库里存的省 / 市 / 区名称还原成 Cascader 的路径。
 *
 * <p>历史上地址是手填的，可能存在「浙江省 / 杭州 / 钱塘」这种缺后缀的写法，
 * 这里做一次宽松匹配；实在匹配不上就返回 undefined（组件显示为空，用户重选）。</p>
 */
export function findRegionPath(
  province?: string | null,
  city?: string | null,
  district?: string | null,
): string[] | undefined {
  const regions = regionsOf();
  if (!regions || !province || !city) return undefined;

  const p = pick(regions, province);
  if (!p) return undefined;

  // 直辖市只有一个与省同名的市级节点，历史数据里的「市辖区」也能兜住
  const single = p.children?.length === 1 && p.children[0].name === p.name ? p.children[0] : undefined;
  const c = pick(p.children, city) ?? single;
  if (!c) return undefined;
  if (!district) return [p.name, c.name];

  const d = pick(c.children, district);
  return d ? [p.name, c.name, d.name] : [p.name, c.name];
}

/** 把 Cascader 的路径拆成后端需要的三个字段（台湾等两级地区 district 为空串） */
export function splitRegion(path?: string[] | null): {
  province: string;
  city: string;
  district: string;
} {
  return {
    province: path?.[0] ?? '',
    city: path?.[1] ?? '',
    district: path?.[2] ?? '',
  };
}
