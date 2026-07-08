# Search Tool Selection Guide

This project has `ripgrep` (rg) installed for improved code search.

## Tool Selection

| 场景 | 推荐工具 | 原因 |
|------|---------|------|
| 小范围搜索精确字符串 (< 500 文件) | 内置 Grep 工具 | 结果结构化，模型直接消费 |
| 大代码库搜索 (> 500 文件) | `rg` shell 命令 | 性能快 10-100x |
| 需要 PCRE2 复杂正则 | `rg -P` | 内置 Grep 不支持 PCRE2 |
| 文件名匹配/Glob 搜索 | 内置 Glob 工具 | rg 不能替代 Glob |
| 搜索时查看上下文 | `rg -C <n>` | 灵活控制上下文行数 |

## ripgrep 基础用法

`rg` v15.1.0 (with PCRE2)，位于 `/home/linuxbrew/.linuxbrew/bin/rg`。

```bash
rg "pattern"              # 基本搜索
rg -i "pattern"           # 忽略大小写
rg -C 3 "pattern"         # 上下各 3 行上下文
rg -g "*.ts" "pattern"    # 按文件类型过滤
rg -P "(foo\|bar)\d+"     # PCRE2 正则
rg -l "pattern"           # 只输出文件名
rg -n "pattern"           # 显示行号（默认启用）
rg --type-add 'web:*.{html,css,js}' -t web "pattern"  # 自定义文件类型
```

## 原则

优先使用**最适合当前场景**的工具，而非一律用 rg 或一律用内置工具。
