# 电子书维护

电子书从 `docs/course/catalog.json` 指向的正文生成，书籍专用的前言、阅读指南、分篇导读和术语索引在本目录维护。技术正文只维护一份；代码块逐字校验，Mermaid 图以静态图进入离线版本。

## 构建

需要 Python 3.12、Pandoc 3.9.0.2、Poppler，以及 WeasyPrint 所需的 Pango 库。macOS 可通过 Homebrew 安装 `pandoc poppler pango`；Windows 可按 WeasyPrint 官方安装说明配置 Pango，或使用 WSL。GitHub Actions 使用 Ubuntu 构建。

```bash
python -m pip install -r book/requirements.txt
python book/build.py
python book/check.py
```

首次构建下载固定版本的 Noto 中文字体到忽略的 `book/.cache/`，并校验 SHA-256。`BOOK_FONT_DIR` 可指定已有字体目录。输出为 `site/dist/downloads/` 下的 PDF、EPUB 和版本清单，生成文件不提交到源码仓库。

## 修改与核对

- `edition.json`：作者、版本、分篇导读与术语索引；正文修订后更新版本与日期。
- `preface.md`、`reading-guide.md`：书籍专用前置内容。
- `print.css`、`epub.css`：纸面与流式阅读各自的版式。
- 修改章节后核对目录、内链、图表和代码保真；抽查封面、目录、章节首页、长代码、宽表及图示。
- 书中引用源码的链接固定到构建时的提交；版本清单保存内容与文件摘要，便于反馈勘误。

PDF 包含嵌入字体、书签和带页码的目录；EPUB 包含阅读器导航、封面、语言和作者元数据。发行检查还需使用 EPUBCheck 验证 EPUB，并查看实际渲染结果。

正文与图示的强调色直接读取阅读站的浅色主题。优先使用仓库内的 SVG 图源；宽图在 PDF 中独立横排，截图保留完整分辨率。网站仍保留原图放大入口，EPUB 内嵌图像，免于联网加载。
