# 网站与电子书构建

直接阅读课程或运行配套代码，不需要以下工具。这里说明贡献者如何预览文章、修改网站和校正电子书。

## 目录

- `docs/course/catalog.json`：文章顺序、标题与配套资源；正文保持单一来源。
- `tools/site/`：阅读站配置、样式、脚本和生成器。
- `tools/book/`：前言、阅读指南、分篇导读、术语索引、排版样式和电子书生成器。

## 预览网站

使用 Python 3.12，在仓库根目录执行。macOS 可用 `python3`，Windows 可用 `py`。

```bash
python -m pip install -r tools/site/requirements.txt
python tools/site/build.py
python -m http.server 8879 --bind 127.0.0.1 --directory tools/site/dist
```

打开 `http://127.0.0.1:8879/`。本地下载入口在生成电子书后可用；只预览文章时无需生成电子书。只改正文和图源，不直接修改生成页面。推送主分支后，GitHub Actions 自动构建并部署网站。

## 构建电子书

额外需要 Pandoc 3.9.0.2、Poppler 和 Pango。macOS 可通过 Homebrew 安装；Windows 可按 [WeasyPrint 官方说明](https://doc.courtbouillon.org/weasyprint/stable/first_steps.html) 配置 Pango，或使用 WSL。正式发布由固定依赖的 Ubuntu CI 环境完成。

```bash
python -m pip install -r tools/book/requirements.txt
python tools/book/build.py
python tools/book/check.py
python tools/site/build.py
python tools/site/check.py
```

输出到 `tools/site/dist/downloads/`，包含 PDF、EPUB 和 SHA-256 版本清单。首次构建会下载并校验固定版本的 Noto 字体；`BOOK_FONT_DIR` 可指定已有字体。字体缓存保存在忽略的 `tools/book/.cache/`。

修改书籍编排时编辑 `tools/book/edition.json`、`preface.md`、`reading-guide.md`、`print.css` 或 `epub.css`；正文修订后同步版本与日期。PDF 主题读取网站浅色主题，优先使用 SVG 图源，复杂宽图独立横排，截图保留完整分辨率。EPUB 内嵌图片并使用独立流式样式。

检查程序核对正文来源、技术内容块、目录、内链、作者、书签、字体与页面尺寸。发布还需通过 EPUBCheck 5.4.0，并实际查看封面、目录、正文、长代码、表格和配图。

## 提交范围

提交文章、代码、图源及维护所需的构建源码。PDF、EPUB、生成页面、缓存、虚拟环境和临时日志不进入 Git；读者从在线阅读站下载电子书。不要提交作者备稿、排期、私有讨论、真实密钥或未经脱敏的运行记录。
