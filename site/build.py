"""Build a static reading site from the repository's course catalog and Markdown."""
import argparse
import hashlib
import html
from html.parser import HTMLParser
import json
import os
from pathlib import Path
import re
import shutil
import struct
from urllib.parse import quote, unquote, urlsplit

import markdown
from markdown.extensions.toc import slugify_unicode

ROOT = Path(__file__).resolve().parents[1]
SITE = ROOT / 'site'
ICONS = {
    'arrow': '<path d="M5 12h14M13 6l6 6-6 6"/>',
    'back': '<path d="M19 12H5m6-6-6 6 6 6"/>',
    'search': '<circle cx="10.8" cy="10.8" r="6.8"/><path d="m16 16 4.5 4.5"/>',
    'menu': '<path d="M4 6h16M4 12h16M4 18h16"/>',
    'close': '<path d="m6 6 12 12M18 6 6 18"/>',
    'external': '<path d="M14 4h6v6m0-6-9 9M10 4H4v16h16v-6"/>',
    'focus': '<path d="M8 3H3v5m13-5h5v5M3 16v5h5m13-5v5h-5"/>',
    'sun': '<circle cx="12" cy="12" r="4"/><path d="M12 2v2m0 16v2M2 12h2m16 0h2M5 5l1.5 1.5m11 11L19 19M5 19l1.5-1.5m11-11L19 5"/>',
    'moon': '<path d="M20.5 14.5A9 9 0 0 1 9.5 3.5a9 9 0 1 0 11 11Z"/>',
    'code': '<path d="m8 7-5 5 5 5m8-10 5 5-5 5M14 4l-4 16"/>',
}


def icon(name):
    return '<svg class="icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' + ICONS[name] + '</svg>'


def esc(value):
    return html.escape(str(value), quote=True)


def title_text(value):
    return str(value).replace('`', '')


def rel(target, page):
    return quote(Path(os.path.relpath(target, Path(page).parent)).as_posix(), safe='/#-_.~')


class SearchSections(HTMLParser):
    def __init__(self):
        super().__init__()
        self.sections = [{'heading': '', 'anchor': '', 'parts': []}]
        self.in_heading = False

    def handle_starttag(self, tag, attrs):
        if tag == 'h2':
            self.sections.append({'heading': '', 'anchor': dict(attrs).get('id', ''), 'parts': []})
            self.in_heading = True

    def handle_endtag(self, tag):
        if tag == 'h2':
            self.in_heading = False
        if tag in {'p', 'li', 'pre', 'h2', 'h3', 'td'}:
            self.sections[-1]['parts'].append(' ')

    def handle_data(self, data):
        if self.in_heading:
            self.sections[-1]['heading'] += data
        self.sections[-1]['parts'].append(data)


def build(target=None):
    output = Path(target).resolve() if target else SITE / 'dist'
    if output in (ROOT, SITE) or ROOT.is_relative_to(output):
        raise ValueError('Output must be a dedicated generated directory')
    output.mkdir(parents=True, exist_ok=True)
    config = json.loads((SITE / 'course.json').read_text(encoding='utf-8'))
    catalog = json.loads((ROOT / 'docs/course/catalog.json').read_text(encoding='utf-8'))
    records = []
    for chapter in catalog['chapters']:
        name = {'INTRO': 'introduction', 'END': 'closing'}.get(chapter['id'], chapter['id'])
        records.append({**chapter, 'title': title_text(chapter['title']), 'url': f'course/{name}.html', 'topic': title_text(chapter['title']), 'stage': chapter['group'], 'demos': []})
    route_by_source = {(ROOT / row['path']).resolve(): row['url'] for row in records}
    route_by_source[(ROOT / 'README.md').resolve()] = 'index.html'
    route_by_source[(ROOT / 'docs/course/README.md').resolve()] = 'index.html#curriculum'
    route_by_source[(ROOT / 'LICENSING.md').resolve()] = 'license.html'
    groups = list(dict.fromkeys(row['group'] for row in records))
    github = f'https://github.com/{catalog["repository"]}'
    assets = output / 'assets'
    assets.mkdir(exist_ok=True)
    for source in (SITE / 'assets').rglob('*'):
        if source.is_file():
            destination = assets / source.relative_to(SITE / 'assets')
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, destination)
    copied = set()

    def head(title, page, digest='', mermaid=False):
        root = rel('index.html', page).removesuffix('index.html')
        tag = '<meta name="source-sha256" content="' + digest + '">' if digest else ''
        scripts = f'<script defer src="{root}assets/search-index.js"></script><script defer src="{root}assets/site.js"></script>'
        if mermaid:
            scripts += f'<script defer src="{root}assets/vendor/mermaid.min.js"></script><script defer src="{root}assets/diagrams.js"></script>'
        theme_key = 'haohai-course-theme'
        return f'''<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>{esc(title)} · {esc(config['brand'])}</title><meta name="description" content="{esc(config['description'])}">{tag}
<link rel="canonical" href="{config['url']}{page if page != 'index.html' else ''}"><link rel="icon" href="{root}assets/brand.png">
<script>try{{const t=localStorage.getItem('{theme_key}');document.documentElement.dataset.theme=t==='dark'||(t!=='light'&&matchMedia('(prefers-color-scheme:dark)').matches)?'dark':'light'}}catch(_){{document.documentElement.dataset.theme='light'}}</script>
<link rel="stylesheet" href="{root}assets/site.css">{scripts}</head>'''

    def header(page, article=False):
        root = rel('index.html', page).removesuffix('index.html')
        menu = f'<button class="icon-button menu-toggle js-only" type="button" aria-label="打开课程目录" aria-controls="course-sidebar" aria-expanded="false">{icon("menu")}</button>' if article else ''
        return f'''<a class="skip-link" href="#main-content">跳到正文</a><header class="site-header"><div class="header-inner">{menu}
<a class="brand" href="{root}index.html" aria-label="{esc(config['brand'])} · 学习站首页"><img src="{root}assets/brand.png" alt="" width="36" height="36"><span><strong>{esc(config['brand'])}</strong><small>{esc(catalog['title'])}</small></span></a>
<nav class="header-nav" aria-label="主导航"><a href="{root}index.html#curriculum">课程目录</a><a href="{root}index.html#downloads">离线阅读</a><a href="{github}" target="_blank" rel="noopener">GitHub {icon('external')}</a></nav>
<div class="header-controls"><button class="icon-button theme-toggle js-only" type="button" aria-label="切换到深色主题" title="切换到深色主题"><span class="theme-light-icon">{icon('moon')}</span><span class="theme-dark-icon">{icon('sun')}</span></button><button class="search-trigger js-only" type="button" aria-label="搜索课程">{icon('search')}<span>搜索课程</span><kbd>⌘ K</kbd></button></div></div><div class="reading-progress" aria-hidden="true"><span id="reading-progress-bar"></span></div></header>'''

    def dialogs():
        return f'''<dialog class="search-dialog" id="search-dialog" aria-labelledby="search-label"><form method="dialog" class="search-heading"><label for="course-search" id="search-label">{icon('search')}搜索课程</label><button class="icon-button" aria-label="关闭搜索">{icon('close')}</button></form><input id="course-search" type="search" placeholder="搜索概念、代码或问题…" autocomplete="off" spellcheck="false"><p class="search-status" id="search-status" role="status">搜索全部 {catalog['articleCount']} 篇文章。</p><div class="search-results" id="search-results"></div><footer>↑ ↓ 选择结果 <span>Enter 打开 · Esc 关闭</span></footer></dialog>
<dialog class="image-viewer" id="image-viewer" aria-label="放大配图"><header><span>查看配图</span><div class="image-actions"><button type="button" id="image-size">原尺寸</button><button type="button" id="close-image" autofocus>关闭</button></div></header><div class="image-canvas"><img id="large-image" alt=""></div><footer><p id="image-description"></p><a id="original-image" target="_blank" rel="noopener">打开原图 {icon('external')}</a></footer></dialog><div class="toast" id="toast" role="status"></div>'''

    def community(page, compact=False):
        qr = rel('assets/wechat-official-account.jpg', page)
        heading = '' if compact else '<h2>关注与交流 · 好好用 AI</h2>'
        content = f'''<div class="community-section"><div>{heading}<p>交流学习中的问题与实践心得。</p><p>代码问题和勘误，欢迎在 <a href="{github}/issues" target="_blank" rel="noopener">GitHub 提交 Issue</a>。</p></div><a class="qr-link" href="{qr}" target="_blank" rel="noopener" aria-label="查看好好用 AI 公众号二维码原图"><img src="{qr}" alt="好好用 AI 微信公众号二维码" width="130" height="130" loading="lazy"><p class="qr-caption">微信扫码 · 点击查看原图</p></a></div>'''
        return '<details class="community-mini"><summary>关注与交流 · 好好用 AI</summary>' + content + '</details>' if compact else content

    def footer(page):
        contact = '<div class="page-width" id="community">' + community(page) + '</div>' if page == 'index.html' else ''
        return contact + f'<footer class="site-footer page-width"><div><strong>{esc(config["brand"])}</strong><span>{esc(catalog["title"])}</span></div><a href="{rel("license.html", page)}">非商业使用 · 许可说明</a></footer>'

    def resolve_url(value, source, page, image=False):
        parsed = urlsplit(html.unescape(value))
        if parsed.scheme or parsed.netloc or not parsed.path:
            return html.unescape(value)
        original = (source.parent / unquote(parsed.path)).resolve()
        if not original.is_relative_to(ROOT):
            raise ValueError(f'Resource outside repository: {source}: {value}')
        if image:
            vector = original.parent.parent / 'source' / original.with_suffix('.svg').name
            if original.suffix.lower() == '.png' and vector.is_file():
                original = vector
            if not original.is_file():
                raise ValueError(f'Missing image: {source}: {value}')
            destination = Path('assets/content') / original.relative_to(ROOT)
            if original not in copied:
                (output / destination).parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(original, output / destination)
                copied.add(original)
            return rel(destination, page)
        if original in route_by_source:
            target = route_by_source[original]
            route, _, fragment = target.partition('#')
            return rel(route, page) + ('#' + quote(parsed.fragment or fragment) if parsed.fragment or fragment else '')
        if not original.exists():
            raise ValueError(f'Missing linked file: {source}: {value}')
        kind = 'tree' if original.is_dir() else 'blob'
        return github + f'/{kind}/main/' + quote(original.relative_to(ROOT).as_posix()) + ('#' + parsed.fragment if parsed.fragment else '')

    def render(source_text, source_path, page):
        has_mermaid = bool(re.search(r'^```mermaid\s*$', source_text, re.M))
        source_text = re.sub(r'^```mermaid\s*\n(.*?)^```\s*$', lambda m: '<pre class="mermaid">' + esc(m.group(1).rstrip()) + '</pre>\n', source_text, flags=re.M | re.S)
        parser = markdown.Markdown(extensions=['fenced_code', 'tables', 'sane_lists', 'md_in_html', 'toc'], extension_configs={'toc': {'toc_depth': '2-2', 'slugify': slugify_unicode}})
        body = parser.convert(source_text)
        body = body.replace('<table>', '<div class="table-scroll"><table>').replace('</table>', '</table></div>')
        body = re.sub(r'(href|src)="([^"]+)"', lambda m: m[1] + '="' + esc(resolve_url(m[2], source_path, page, m[1] == 'src')) + '"', body)
        def image_dimensions(match):
            tag = match.group(0)
            src = re.search(r'src="([^"]+)"', tag)
            if src and not urlsplit(src[1]).scheme:
                image = (output / Path(page).parent / unquote(html.unescape(src[1]))).resolve()
                with image.open('rb') as stream:
                    header = stream.read(24)
                if header[:8] == b'\x89PNG\r\n\x1a\n':
                    width, height = struct.unpack('>II', header[16:24])
                    return tag.removesuffix('>').rstrip().removesuffix('/').rstrip() + f' width="{width}" height="{height}" decoding="async">'
            return tag
        body = re.sub(r'<img\b[^>]*>', image_dimensions, body)
        return body, parser.toc, has_mermaid

    search_lessons, search_sections = [], []
    for index, row in enumerate(records):
        source_path = ROOT / row['path']
        source = source_path.read_text(encoding='utf-8')
        if '\n\n---\n\n[课程首页](' in source:
            source = source.rsplit('\n\n---\n\n', 1)[0]
        body, contents, has_mermaid = render(source, source_path, row['url'])
        search_lessons.append({key: row[key] for key in ('id', 'title', 'topic', 'url', 'stage', 'demos')})
        sections = SearchSections()
        sections.feed(body)
        for section in sections.sections:
            text = re.sub(r'\s+', ' ', ''.join(section['parts'])).strip()
            if text:
                search_sections.append({'id': row['id'], 'title': row['title'], 'topic': row['topic'], 'heading': section['heading'], 'text': text, 'url': row['url'] + ('#' + section['anchor'] if section['anchor'] else '')})
        sidebar_items = []
        for group in groups:
            rows = [record for record in records if record['group'] == group]
            links = ''.join(f'<a class="lesson-link" data-lesson="{r["id"]}" href="{rel(r["url"], row["url"])}"' + (' aria-current="page"' if r['id'] == row['id'] else '') + f'><span class="lesson-number">{r["id"] if r["id"] not in ("INTRO", "END") else "·"}</span><span>{esc(r["title"])}</span><span class="lesson-state" aria-hidden="true"></span></a>' for r in rows)
            sidebar_items.append(f'<details class="sidebar-stage"' + (' open' if row['group'] == group else '') + f'><summary><span class="stage-index">模块 {group}</span>{esc(rows[0]["groupTitle"])}</summary>{links}</details>')
        toc = f'<nav class="reading-route" aria-label="本篇路线"><details open><summary>本篇路线</summary>{contents}</details></nav>'
        neighbors = []
        for position, label, arrow in [(index - 1, '上一篇', 'back'), (index + 1, '下一篇', 'arrow')]:
            if 0 <= position < len(records):
                item = records[position]
                neighbors.append(f'<a href="{rel(item["url"], row["url"])}"><span>{label} {icon(arrow)}</span><strong>{esc(item["title"])}</strong></a>')
            else:
                neighbors.append('<span></span>')
        practice = github + '/blob/main/' + quote(row['practice']['path'])
        page = (head(row['title'], row['url'], hashlib.sha256(source.encode()).hexdigest(), has_mermaid)
                + f'<body class="article-page" data-root="../" data-page="{row["id"]}">' + header(row['url'], True)
                + '<div class="sidebar-scrim" hidden></div><div class="reader-layout"><aside class="course-sidebar" id="course-sidebar" aria-label="课程目录"><div class="sidebar-heading"><a href="../index.html#curriculum">课程目录</a><button class="icon-button menu-close js-only" type="button" aria-label="关闭课程目录">' + icon('close') + '</button></div><nav aria-label="全部文章">' + ''.join(sidebar_items) + '</nav><p class="sidebar-note">' + esc(config['readingNote']) + '</p></aside>'
                + f'<main class="reader-main" id="main-content"><div class="article-context"><span>{esc(row["groupTitle"])}</span><button type="button" class="focus-toggle js-only" aria-pressed="false">{icon("focus")}<span>专注阅读</span></button></div><div class="mobile-context">' + toc.replace('<details open>', '<details>') + f'</div><article id="article-body">{body}</article><div class="practice-link"><a href="{practice}" target="_blank" rel="noopener">{icon("code")} {esc(row["practice"]["label"])} {icon("external")}</a></div><nav class="chapter-nav" aria-label="前后篇">' + ''.join(neighbors) + '</nav><footer class="reader-footer">' + esc(catalog['title']) + ' · <a href="../license.html">非商业使用</a>' + community(row['url'], compact=True) + '</footer></main>'
                + '<aside class="context-sidebar" aria-label="本篇辅助导航"><div class="context-inner">' + toc + '<section class="reading-settings"><h2>阅读设置</h2><div class="font-controls js-only"><button type="button" data-font="-1" aria-label="减小正文字号">A−</button><span id="font-size-label">18</span><button type="button" data-font="1" aria-label="增大正文字号">A+</button></div></section></div></aside></div>' + dialogs() + '</body></html>')
        destination = output / row['url']
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_text(page, encoding='utf-8')

    data = {'language': 'zh', 'lessons': search_lessons, 'sections': search_sections}
    (assets / 'search-index.js').write_text('window.LEARNING_DATA=' + json.dumps(data, ensure_ascii=False, separators=(',', ':')) + ';\n', encoding='utf-8')
    build_home(config, catalog, records, groups, output, head, header, dialogs, footer, github)
    license_text = (ROOT / 'LICENSING.md').read_text(encoding='utf-8')
    body, _, _ = render(license_text, ROOT / 'LICENSING.md', 'license.html')
    (output / 'license.html').write_text(head('许可说明', 'license.html') + '<body class="home-page" data-root="./">' + header('license.html') + '<main class="license-page page-width" id="main-content"><article id="article-body">' + body + '</article></main>' + footer('license.html') + dialogs() + '</body></html>', encoding='utf-8')
    (output / '.nojekyll').touch()
    (output / '404.html').write_text(head('页面未找到', '404.html') + '<body class="home-page"><main class="license-page page-width"><h1>页面未找到</h1><p><a href="' + config['url'] + '">返回课程首页</a></p></main></body></html>', encoding='utf-8')
    (output / 'build-manifest.json').write_text(json.dumps({'repository': catalog['repository'], 'articles': [{'id': row['id'], 'title': row['title'], 'url': row['url'], 'source': row['path']} for row in records]}, ensure_ascii=False, indent=2), encoding='utf-8')
    print(f'{catalog["repository"]}: built {len(records)} articles and {len(search_sections)} searchable sections in {output}')
    return output


def build_home(config, catalog, records, groups, output, head, header, dialogs, footer, github):
    by_id = {row['id']: row for row in records}
    pipeline = ''.join(f'<a href="{by_id[item["id"]]["url"]}" class="pipeline-row"><span class="pipeline-dot {color}"></span><span>{esc(item["title"])}<small>{esc(item["caption"])}</small></span><span class="pipeline-lesson">{item["id"]}</span>{icon("arrow")}</a>' for item, color in zip(config['pipeline'], ['blue', 'purple', 'teal']))
    rail, stages = [], []
    for group in groups:
        rows = [row for row in records if row['group'] == group]
        title = rows[0]['groupTitle']
        rail.append(f'<a href="#stage-{group}"><span>模块 {group}</span><strong>{esc(title)}</strong>{icon("arrow")}</a>')
        links = ''.join(f'<li><a class="course-row" href="{row["url"]}"><span class="course-number">{row["id"] if row["id"] not in ("INTRO", "END") else "·"}</span><span class="course-title">{esc(row["title"])}</span><span class="course-row-arrow">{icon("arrow")}</span></a></li>' for row in rows)
        stages.append(f'<section class="catalog-stage" id="stage-{group}"><header><span class="stage-marker">模块 {group}</span><div><h3>{esc(title)}</h3><p>{esc(config["groupNotes"].get(group, ""))}</p></div><span class="lesson-range">{len(rows)} 篇</span></header><ol class="course-rows">{links}</ol></section>')
    practices = ''.join(f'<a class="experiment-feature" href="{github}/blob/main/{quote(item["path"])}" target="_blank" rel="noopener"><span class="experiment-number">0{i+1}</span><div><h3>{esc(item["title"])}</h3><p>{esc(item["description"])}</p><span class="text-link">查看配套资源 {icon("external")}</span></div></a>' for i, item in enumerate(config['practice']))
    slug = catalog['repository'].split('/')[-1]
    edition = json.loads((ROOT / 'book/edition.json').read_text(encoding='utf-8'))
    downloads = f'<section class="downloads-section page-width" id="downloads"><div class="section-heading"><div><h2>把课程放进你的书架。</h2><p>完整正文与配图均可离线阅读。PDF 保留固定版式，EPUB 适合手机和平板。</p></div></div><div class="download-links"><a class="button primary" href="downloads/{slug}.pdf" download>下载 PDF {icon("arrow")}</a><a class="button secondary" href="downloads/{slug}.epub" download>下载 EPUB {icon("arrow")}</a></div><p class="download-note">作者：{esc(edition["author"])} · 第 1 版 · {esc(edition["date"])} · 非商业使用</p></section>'
    page = (head(catalog['title'], 'index.html') + '<body class="home-page" data-root="./">' + header('index.html')
            + '<main id="main-content"><div class="hero-band"><section class="home-hero page-width"><div class="hero-copy"><p class="book-eyebrow">' + esc(config['brand'].upper()) + ' · 免费阅读 · 非商业使用</p><h1>' + esc(config['headline'][0]) + '<br><em>' + esc(config['headline'][1]) + '</em></h1><p>' + esc(config['description']) + '</p><div class="hero-actions"><a class="button primary" id="continue-learning" href="' + records[0]['url'] + '"><span>开始学习</span>' + icon('arrow') + '</a><a class="hero-secondary" href="#curriculum">浏览完整目录</a></div><p class="hero-note" id="resume-label">课程已完结 · ' + str(len(records)) + ' 篇 · ' + str(len(groups)) + ' 个模块</p></div><div class="model-preview course-preview"><div class="model-caption"><span>' + esc(config['pipelineTitle']) + '</span></div><div class="model-pipeline">' + pipeline + '</div><p class="pipeline-note">' + esc(config['readingNote']) + '</p></div></section></div>'
            + '<section class="curriculum-section page-width" id="curriculum"><div class="section-heading"><div><h2>沿着问题，一步步学下去。</h2><p>' + esc(config['curriculumNote']) + '</p></div><div class="home-progress">完整课程<strong>' + str(len(records)) + ' 篇</strong></div></div><div class="curriculum-layout"><aside class="path-rail"><nav aria-label="课程模块">' + ''.join(rail) + '</nav></aside><div>' + ''.join(stages) + '</div></div></section>'
            + '<section class="experiments-section page-width" id="practice"><div class="section-heading"><div><h2>需要动手时，再打开配套资源。</h2><p>正文可以独立阅读。代码与模板用于进一步理解和验证。</p></div></div><div class="experiment-features">' + practices + '</div></section>' + downloads + '</main>' + footer('index.html') + dialogs() + '</body></html>')
    (output / 'index.html').write_text(page, encoding='utf-8')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, help='Generated static site directory')
    args = parser.parse_args()
    build(args.output)
