# 阅读站构建

网站从 [课程目录](../docs/course/README.md) 和各篇 Markdown 生成；只修改正文来源，不直接编辑 `dist/`。页面样式与交互沿用《手写小模型，看懂大模型》的阅读站，课程配置维护在 `course.json`。

准备 Python 3.10 或更高版本，在仓库根目录运行：

```bash
python -m pip install -r site/requirements.txt
python site/build.py
python site/check.py
python -m http.server 8879 --bind 127.0.0.1 --directory site/dist
```

打开 `http://127.0.0.1:8879/`。macOS 可使用 `python3`，Windows 可使用 `py`。网站提供全文搜索、课程目录、篇内定位、字号、专注模式和配图放大；阅读位置只保存在当前浏览器。

推送 `main` 后由 GitHub Actions 构建并部署到 [在线阅读](https://daviddingxu.github.io/learn-java-ai/)。`dist/`、搜索索引和页面都是生成产物，不提交到正文来源。
