#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
一次性重命名脚本：把工程内所有 mt* 前缀标识替换为 owner。

覆盖范围：
  1) 目录名 / 文件名（模块目录、Java 包路径、类文件名）
  2) 文本文件内容（Java / XML / YAML / SQL / MD / Dockerfile / 脚本等）

替换规则（长 token 优先，避免 "MtlntelliPortal" 被 "Mtl" 规则截断）：
  MtlntelliPortal  -> Owner        （类名 MtlntelliPortalApplication -> OwnerApplication）
  mtlntelliportal  -> owner        （模块名 / 包名 / 库名）
  mtlntelli        -> owner        （mtlntelli@2026 口令）
  MTL              -> OWNER        （MTL_WEBSOCKET_BROADCAST 等常量）
  mtl / Mtl        -> owner/Owner  （mtl.* 配置前缀、mtl-net、mtl-async- 线程名前缀）
"""
import os
import sys

ROOT = r"D:\code\onlyone"

SKIP_DIRS = {"target", ".git", "node_modules", "logs", ".workbuddy", "dist", ".idea"}

TEXT_EXT = {
    ".java", ".xml", ".yml", ".yaml", ".sql", ".md", ".mjs", ".js", ".ts", ".tsx",
    ".json", ".txt", ".properties", ".conf", ".sh", ".cmd", ".bat", ".html", ".css",
}

# 顺序敏感：长的在前
CONTENT_RULES = [
    ("MtlntelliPortal", "Owner"),
    ("mtlntelliportal", "owner"),
    ("Mtlntelliportal", "Owner"),
    ("MTLNTELLIPORTAL", "OWNER"),
    ("mtlntelli", "owner"),
    ("Mtlntelli", "Owner"),
    ("MTLNTELLI", "OWNER"),
    ("mtl", "owner"),
    ("Mtl", "Owner"),
    ("MTL", "OWNER"),
]

# 路径分量（目录名/文件名）替换规则：不带扩展名比较，这里直接对整段做同名替换
PATH_RULES = [
    ("MtlntelliPortal", "Owner"),
    ("mtlntelliportal", "owner"),
    ("Mtlntelliportal", "Owner"),
    ("MTLNTELLIPORTAL", "OWNER"),
    ("mtlntelli", "owner"),
    ("Mtlntelli", "Owner"),
    ("mtl", "owner"),
    ("Mtl", "Owner"),
    ("MTL", "OWNER"),
]

# 替换后的人类可读品牌名微调
POST_FIX = [("OWNER IntelliPortal", "Owner IntelliPortal")]


def apply_rules(text, rules):
    for old, new in rules:
        if old in text:
            text = text.replace(old, new)
    for old, new in POST_FIX:
        text = text.replace(old, new)
    return text


def is_text_file(path):
    name = os.path.basename(path)
    if name in {"Dockerfile", ".gitignore", ".dockerignore", ".java-version", "mvnw", "mvnw.cmd"}:
        return True
    return os.path.splitext(name)[1].lower() in TEXT_EXT


def main():
    changed_content = []
    renamed_paths = []
    errors = []

    # ---------- 1. 改内容 ----------
    for dirpath, dirnames, filenames in os.walk(ROOT, topdown=True):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
        for fn in filenames:
            full = os.path.join(dirpath, fn)
            if not is_text_file(full):
                continue
            try:
                with open(full, "r", encoding="utf-8", newline="") as f:
                    raw = f.read()
            except (UnicodeDecodeError, OSError):
                continue
            new = apply_rules(raw, CONTENT_RULES)
            if new != raw:
                with open(full, "w", encoding="utf-8", newline="") as f:
                    f.write(new)
                changed_content.append(os.path.relpath(full, ROOT))

    # ---------- 2. 改路径（深度优先，最深的先改） ----------
    all_paths = []
    for dirpath, dirnames, filenames in os.walk(ROOT, topdown=True):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
        for d in dirnames:
            all_paths.append(os.path.join(dirpath, d))
        for fn in filenames:
            all_paths.append(os.path.join(dirpath, fn))

    all_paths.sort(key=lambda p: p.count(os.sep), reverse=True)

    for p in all_paths:
        if not os.path.exists(p):
            continue
        base = os.path.basename(p)
        new_base = apply_rules(base, PATH_RULES)
        if new_base != base:
            target = os.path.join(os.path.dirname(p), new_base)
            if os.path.exists(target):
                errors.append("目标已存在，跳过: %s -> %s" % (p, target))
                continue
            try:
                os.rename(p, target)
                renamed_paths.append("%s -> %s" % (os.path.relpath(p, ROOT), new_base))
            except OSError as e:
                errors.append("%s: %s" % (p, e))

    print("=== 内容被修改的文件 (%d) ===" % len(changed_content))
    for c in sorted(changed_content):
        print("  " + c)
    print()
    print("=== 被重命名的路径 (%d) ===" % len(renamed_paths))
    for r in sorted(renamed_paths):
        print("  " + r)
    if errors:
        print()
        print("=== 错误 (%d) ===" % len(errors))
        for e in errors:
            print("  " + e)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
