#!/usr/bin/env python3
"""重新生成前端用的省市区数据 `owner-web/src/data/regions.json`。

数据源是 npm 包 china-division（国家统计局行政区划），只取到区县一级，
去掉行政编码（后端 user_address 只存名称），并做两处加工：

1. 直辖市压平：`北京市 / 市辖区 / 朝阳区` → `北京市 / 北京市 / 朝阳区`
   （重庆的「市辖区」与「县」两组会合并）
2. 补充港澳台：大陆统计口径的 31 个省市区不含港澳台

用法：
    python scripts/gen-regions.py            # 自动下载并生成
    python scripts/gen-regions.py pca.json   # 使用本地已有的 pca-code.json

行政区划调整时重跑一次即可，前端无需改代码。
"""

from __future__ import annotations

import json
import os
import sys
import tempfile
import urllib.request

PKG = "china-division"
VERSION = "2.7.0"
URL = f"https://registry.npmmirror.com/{PKG}/{VERSION}/files/dist/pca-code.json"
OUT = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "owner-web", "src", "data", "regions.json",
)

DIRECT = {"北京市", "天津市", "上海市", "重庆市"}

HK = ["中西区", "湾仔区", "东区", "南区", "油尖旺区", "深水埗区", "九龙城区", "黄大仙区",
      "观塘区", "葵青区", "荃湾区", "屯门区", "元朗区", "北区", "大埔区", "沙田区",
      "西贡区", "离岛区"]
MO = ["花地玛堂区", "圣安多尼堂区", "大堂区", "望德堂区", "风顺堂区", "嘉模堂区",
      "圣方济各堂区", "路氹填海区"]
TW = ["台北市", "新北市", "桃园市", "台中市", "台南市", "高雄市", "基隆市", "新竹市",
      "嘉义市", "新竹县", "苗栗县", "彰化县", "南投县", "云林县", "嘉义县", "屏东县",
      "宜兰县", "花莲县", "台东县", "澎湖县", "金门县", "连江县"]


def strip_codes(nodes: list[dict]) -> list[dict]:
    """只保留 name / children。"""
    out = []
    for n in nodes:
        item: dict = {"name": n["name"]}
        kids = n.get("children") or []
        if kids:
            item["children"] = strip_codes(kids)
        out.append(item)
    return out


def load_source(argv: list[str]) -> list[dict]:
    if len(argv) > 1:
        with open(argv[1], encoding="utf-8") as f:
            return json.load(f)
    print(f"下载 {URL} ...")
    with urllib.request.urlopen(URL, timeout=60) as resp:
        return json.loads(resp.read().decode("utf-8"))


def main() -> None:
    src = load_source(sys.argv)
    result = []

    for p in src:
        name = p["name"]
        if name in DIRECT:
            merged: list[dict] = []
            for child in p.get("children", []):
                merged.extend(child.get("children", []))
            result.append({
                "name": name,
                "children": [{"name": name, "children": strip_codes(merged)}],
            })
        else:
            result.append(strip_codes([p])[0])

    result.append({"name": "香港特别行政区",
                   "children": [{"name": "香港特别行政区",
                                 "children": [{"name": x} for x in HK]}]})
    result.append({"name": "澳门特别行政区",
                   "children": [{"name": "澳门特别行政区",
                                 "children": [{"name": x} for x in MO]}]})
    result.append({"name": "台湾省", "children": [{"name": x} for x in TW]})

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, separators=(",", ":"))

    provinces = len(result)
    cities = sum(len(p.get("children", [])) for p in result)
    districts = sum(len(c.get("children", [])) for p in result for c in p.get("children", []))
    print(f"写出 {OUT}")
    print(f"  {os.path.getsize(OUT) / 1024:.1f} KB / 省 {provinces} / 市 {cities} / 区县 {districts}")


if __name__ == "__main__":
    main()
