"""Compile a reviewed book edition from the course's single article source."""
from collections import Counter
from datetime import datetime, timezone
import argparse
import hashlib
import html
import json
import os
from pathlib import Path
import platform
import re
import shutil
import subprocess
import tempfile
from urllib.parse import quote, unquote, urlsplit
from urllib.request import urlopen

from bs4 import BeautifulSoup, Comment, NavigableString
import markdown
from markdown.extensions.toc import slugify_unicode

ROOT = Path(__file__).resolve().parents[1]
BOOK = ROOT / 'book'

def esc(value):
    return html.escape(str(value), quote=True)

def fragment(value):
    return BeautifulSoup(value, 'html.parser')

def node(value):
    return fragment(value).find()

def render_md(value):
    return markdown.markdown(value, extensions=['fenced_code', 'tables', 'sane_lists', 'md_in_html', 'toc'], extension_configs={'toc': {'slugify': slugify_unicode}})

def strip_navigation(value):
    marker = '\n\n---\n\n[课程首页]('
    return value.rsplit('\n\n---\n\n', 1)[0] if marker in value else value

def fonts():
    directory = Path(os.environ.get('BOOK_FONT_DIR', BOOK / '.cache/fonts')).resolve()
    directory.mkdir(parents=True, exist_ok=True)
    records = json.loads((BOOK / 'fonts.json').read_text(encoding='utf-8'))
    for font in records:
        target = directory / font['file']
        if not target.exists():
            with urlopen(font['url'], timeout=90) as response:
                data = response.read()
            if hashlib.sha256(data).hexdigest() != font['sha256']:
                raise ValueError('Font checksum mismatch: ' + font['file'])
            target.write_bytes(data)
        if hashlib.sha256(target.read_bytes()).hexdigest() != font['sha256']:
            raise ValueError('Font checksum mismatch: ' + font['file'])
    return directory

def compile_manuscript():
    edition = json.loads((BOOK / 'edition.json').read_text(encoding='utf-8'))
    light = re.search(r':root\{([^}]+)\}', (ROOT/'site/assets/site.css').read_text()).group(1)
    edition['theme'] = {name:value.strip() for name,value in re.findall(r'--([a-z-]+):([^;]+)',light) if name in ('paper','ink','muted','accent','accent-dark','accent-wash','line','soft')}
    edition['accent'] = edition['theme']['accent']
    catalog = json.loads((ROOT / 'docs/course/catalog.json').read_text(encoding='utf-8'))
    website = json.loads((ROOT / 'site/course.json').read_text(encoding='utf-8'))['url']
    repository = 'https://github.com/' + catalog['repository']
    revision = subprocess.check_output(['git','rev-parse','HEAD'], cwd=ROOT, text=True).strip()
    rows = catalog['chapters']
    parts = {p['group']:p for p in edition['parts']}
    sources, soups, anchors, labels, titles = {}, {}, {}, {}, {}
    stats = {'articles': len(rows), 'figures': 0, 'tables': 0, 'code_blocks': 0, 'diagrams': 0}
    original_codes = []
    for row in rows:
        cid = row['id']; source = ROOT / row['path']
        raw = source.read_text(encoding='utf-8')
        sources[row['path']] = hashlib.sha256(raw.encode()).hexdigest()
        soup = fragment(render_md(strip_navigation(raw)))
        for comment in list(soup.find_all(string=lambda value:isinstance(value, Comment))):
            comment.extract()
        if soup.find(['script', 'iframe']):
            raise ValueError('An interactive element needs a book adaptation: ' + row['path'])
        for detail in soup.find_all('details'):
            detail.name = 'aside'
            summary = detail.find('summary')
            if summary: summary.name = 'strong'
        prefix = {'INTRO':'导读', 'END':'结语', '00':'序章'}.get(cid, f'第{int(cid)}章' if cid.isdigit() else cid)
        if edition['slug']=='ai-coding-for-teams' and cid=='13': prefix='结语'
        title = row['title'].replace('`','')
        if cid in ('END','13') and prefix=='结语': title=re.sub(r'^(写在专栏结束时|写在最后)[：:]\s*','',title)
        if edition['slug']=='learn-java-ai': title=re.sub(r'^(专栏介绍|结束语)[：:]\s*','',title)
        titles[cid] = prefix + '　' + title
        labels[cid] = {'导读':'导','结语':'结','序章':'序'}.get(prefix,str(int(cid)) if cid.isdigit() else cid)
        headings = list(soup.find_all(re.compile('^h[1-6]$')))
        mapping = {}
        for index, heading in enumerate(headings):
            old = heading.get('id')
            new = 'chapter-'+cid if index==0 else f'chapter-{cid}-heading-{index}'
            if old: mapping[old] = new
            heading['id'] = new
        soup.h1.string = titles[cid]
        section_number = 0
        for heading in soup.find_all('h2'):
            section_number += 1
            plain = re.sub(r'^[一二三四五六七八九十百]+[、．.]\s*','',heading.get_text())
            heading.clear(); heading.append(f'{labels[cid]}.{section_number}　{plain}')
        anchors[cid] = mapping
        original_codes.extend(x.get_text() for x in soup.select('pre code') if 'language-mermaid' not in x.get('class',[]))
        soups[cid] = soup
    by_source = {(ROOT/row['path']).resolve():row['id'] for row in rows}
    diagram_manifest_path = BOOK/'diagrams/manifest.json'
    diagram_manifest = json.loads(diagram_manifest_path.read_text()) if diagram_manifest_path.exists() else {}
    chapters, toc_items, seen_parts = [], [], set()
    for row in rows:
        cid = row['id']; soup = soups[cid]; source = ROOT/row['path']; label = labels[cid]
        if row['group'] not in seen_parts:
            seen_parts.add(row['group']); part = parts[row['group']]; ordinal=len(seen_parts)
            part_title=f'第{ordinal}篇　{part["title"]}'
            chapters.append(f'<section class="part-page" id="part-{row["group"]}"><p class="part-number">PART {ordinal:02d}</p><h1>{esc(part_title)}</h1><p>{esc(part["introduction"])}</p><p class="part-outcome"><strong>阅读重点</strong><br>{esc(part["outcome"])}</p></section>')
            toc_items.append(f'<li class="toc-part"><a href="#part-{row["group"]}">{esc(part_title)}</a></li>')
        toc_items.append(f'<li><a href="#chapter-{cid}">{esc(titles[cid])}</a></li>')
        figures=0; tables=0; listings=0; diagrams=0
        for code in list(soup.select('pre code.language-mermaid')):
            diagrams+=1; key=f'{cid}-{diagrams}'
            record=diagram_manifest.get(key)
            digest=hashlib.sha256(code.get_text().rstrip().encode()).hexdigest()
            if not record or record['source_sha256']!=digest:
                raise ValueError('Missing or stale print diagram: '+key)
            image=BOOK/'diagrams'/record['file']
            code.parent.replace_with(node(f'<p><img src="{image.as_uri()}" alt="{esc(record["caption"])}"></p>'))
            stats['diagrams']+=1
        for image in list(soup.find_all('img')):
            uri=urlsplit(image['src'])
            if uri.scheme=='file': path=Path(unquote(uri.path))
            elif uri.scheme: raise ValueError('Offline image must be local: '+image['src'])
            else: path=(source.parent/unquote(uri.path)).resolve()
            if not path.is_relative_to(ROOT) or not path.is_file(): raise ValueError('Missing image: '+str(path))
            if path.suffix.lower()=='.gif': raise ValueError('Animation needs an edited static alternative: '+str(path))
            sources[path.relative_to(ROOT).as_posix()] = hashlib.sha256(path.read_bytes()).hexdigest()
            adaptation=next((x for x in edition.get('imageAdaptations',[]) if x['source']==path.relative_to(ROOT).as_posix()),None)
            if adaptation and adaptation['kind']=='reading-map-table':
                table=node('<table class="reading-map"><thead><tr><th>学习阶段</th><th>阅读重点</th></tr></thead><tbody></tbody></table>')
                for part in edition['parts']:
                    table.tbody.append(node(f'<tr><td><a href="#part-{part["group"]}">{esc(part["title"])}</a></td><td>{esc(part["outcome"])}</td></tr>'))
                holder=image.parent if image.parent.name=='p' and len(image.parent.contents)==1 else image
                holder.replace_with(table)
                for text in list(soup.find_all(string=True)):
                    if not text.find_parent(['pre','code']) and '这张图不是要求' in str(text):text.replace_with(str(text).replace('这张图不是要求','这份学习地图不是要求'))
                continue
            vector=path.parent.parent/'source'/path.with_suffix('.svg').name if path.suffix.lower()=='.png' else path
            wide=False
            if vector.suffix=='.svg' and vector.is_file():
                vector_text=vector.read_text(encoding='utf-8')
                if '<foreignObject' in vector_text: raise ValueError('Print SVG needs native text labels: '+str(vector))
                sources[vector.relative_to(ROOT).as_posix()]=hashlib.sha256(vector.read_bytes()).hexdigest()
                view=re.search(r'viewBox="([^"]+)"',vector_text)
                sizes=[float(x) for x in re.findall(r'font-size[=:]\s*["\']?(\d+(?:\.\d+)?)',vector_text)]
                sizes += [float(x) for x in re.findall(r'font:[^;]*? (\d+(?:\.\d+)?)px',vector_text)]
                if view and sizes:
                    _,_,width,height=map(float,view[1].split())
                    wide=width>height and min(sizes)*487/width<7
                # The original SVG stays editable; the book export uses the embedded book font.
                cache=BOOK/'.cache/figures';cache.mkdir(parents=True,exist_ok=True)
                exported=cache/vector.name
                family='BookSans, Noto Sans CJK SC, PingFang SC, Microsoft YaHei, sans-serif'
                vector_text=re.sub(r'\bfont\s*:\s*(\d{3})\s+(\d+(?:\.\d+)?)px\s+[^;]+;',lambda m:'font-weight:'+m[1]+';font-size:'+m[2]+'px;font-family:'+family+';',vector_text)
                vector_text=vector_text.replace('</svg>','<style>text,tspan{font-family:'+family+' !important}</style></svg>')
                exported.write_text(vector_text,encoding='utf-8');path=exported
            figures+=1; alt=image.get('alt') or titles[cid]
            image['src']=path.as_uri(); image['alt']=alt
            fig=node(f'<figure id="fig-{cid}-{figures}"><figcaption>图 {label}-{figures}　{esc(alt)}</figcaption></figure>')
            if wide: fig['class']=['wide-diagram']
            holder=image.parent if image.parent.name=='p' and len(image.parent.contents)==1 else image
            holder.replace_with(fig); fig.insert(0,image)
        for table in soup.find_all('table'):
            tables+=1; table['id']=f'table-{cid}-{tables}'
            heading=table.find_previous(['h2','h3'])
            name=re.sub(r'^[\d序导结]+\.\d+\s*','',heading.get_text()) if heading else row['title']
            cap=soup.new_tag('caption');cap.string=f'表 {label}-{tables}　{name}';table.insert(0,cap)
            cells=table.find('tr').find_all(['th','td'],recursive=False)
            if len(cells)>=5: table['class']=['wide-table']
        for pre in soup.find_all('pre'):
            code=pre.find('code'); lang=next((v.removeprefix('language-') for v in code.get('class',[]) if v.startswith('language-')), '') if code else ''
            pre['class']=['code-listing']
            lines=pre.get_text().splitlines()
            if len(lines)<=5: pre['class'].append('short-listing')
            if len(lines)>=6 or lang not in ('','text','plaintext'):
                listings+=1; pre['id']=f'listing-{cid}-{listings}'
                kind='示例' if lang in ('','text','plaintext') else '清单'
                heading=pre.find_previous(['h2','h3'])
                name=re.sub(r'^[\d序导结]+\.\d+\s*','',heading.get_text()) if heading else '本章示例'
                pre.insert_before(node(f'<p class="listing-caption">{kind} {label}-{listings}　{esc(name)}</p>'))
        resources={}
        for a in list(soup.find_all('a',href=True)):
            href=html.unescape(a['href']); parsed=urlsplit(href)
            target=None
            own_prefix=repository+'/blob/main/'
            if href.startswith(own_prefix): target=(ROOT/unquote(urlsplit(href[len(own_prefix):]).path)).resolve()
            elif not parsed.scheme and not parsed.netloc:
                target=(source.parent/unquote(parsed.path)).resolve() if parsed.path else source.resolve()
            if target in by_source:
                other=by_source[target]
                fragment_id=anchors[other].get(unquote(parsed.fragment)) if parsed.fragment else None
                if parsed.fragment and not fragment_id: raise ValueError('Unknown chapter anchor: '+href)
                a['href']='#'+(fragment_id or 'chapter-'+other)
                continue
            if target:
                if not target.is_relative_to(ROOT) or not target.exists(): raise ValueError('Invalid linked file: '+href)
                if target==(ROOT/'README.md').resolve() or target==(ROOT/'docs/course/README.md').resolve():
                    a['href']='#contents'; continue
                href=repository+('/tree/' if target.is_dir() else '/blob/')+revision+'/'+quote(target.relative_to(ROOT).as_posix())
                if parsed.fragment: href+='#'+quote(unquote(parsed.fragment))
            if href.startswith(('http://','https://')):
                href=href.replace(repository+'/blob/main/',repository+'/blob/'+revision+'/').replace(repository+'/tree/main/',repository+'/tree/'+revision+'/')
                if href not in resources: resources[href]=(len(resources)+1,a.get_text())
                num,_=resources[href]
                a['href']='#resource-'+cid+'-'+str(num)
                a.append(NavigableString(f'〔{label}-{num}〕'))
            elif href.startswith('mailto:'): a['href']=href
            else: raise ValueError('Unresolved book link: '+href)
        if resources:
            sec=node(f'<section class="chapter-resources"><h2>本章资源</h2><ol></ol></section>')
            for href,(num,name) in resources.items():
                display=unquote(href)
                if display.startswith(repository+'/'):
                    display=re.sub(re.escape(repository)+r'/(?:blob|tree)/[^/]+/','仓库路径：',display)
                sec.ol.append(node(f'<li id="resource-{cid}-{num}"><span class="resource-label">〔{label}-{num}〕 {esc(name)}</span><br><a href="{esc(href)}">{esc(display)}</a></li>'))
            soup.append(sec)
        stats['figures']+=figures;stats['tables']+=tables
        chapters.append(f'<section class="chapter" data-chapter="{cid}">{soup}</section>')
    result=fragment(''.join(chapters))
    compiled_codes=[x.get_text() for x in result.select('pre code')]
    if Counter(compiled_codes)!=Counter(original_codes): raise AssertionError('Technical examples were changed or omitted')
    stats['code_blocks']=len(compiled_codes)
    front=[]
    for filename,identifier in [('preface.md','preface'),('reading-guide.md','reading-guide')]:
        f=BOOK/filename;front.append(f'<section class="frontmatter" id="{identifier}">{render_md(f.read_text(encoding="utf-8"))}</section>')
    cover=f'''<section class="cover"><p class="eyebrow">{esc(edition['english'])}</p><h1>{esc(edition['title'])}</h1><p class="subtitle">{esc(edition['subtitle'])}</p><img class="cover-art" src="{(ROOT/'docs/assets/readme-cover.png').as_uri()}" alt="{esc(edition['title'])}封面插画"><p class="author">{esc(edition['author'])}　著</p><p class="edition">中文电子版 · 第 1 版<br>{edition['date'][:7].replace('-',' 年 ')} 月</p></section>'''
    titlepage=f'''<section class="title-page" id="title-page"><p class="eyebrow">{esc(edition['english'])}</p><h1 id="book-title">{esc(edition['title'])}</h1><p class="subtitle">{esc(edition['subtitle'])}</p><p class="author">{esc(edition['author'])}　著</p><p class="edition">第 1 版 · {edition['date']}</p></section>'''
    copyright=f'''<section class="frontmatter copyright" id="copyright"><h1>版本与许可</h1><p><strong>{esc(edition['title'])}</strong><br>{esc(edition['subtitle'])}</p><p>作者：{esc(edition['author'])}<br>公众号：好好用 AI<br>GitHub：{esc(edition['githubAuthor'])}</p><p>电子版版本：{edition['version']}<br>修订日期：{edition['date']}</p><p>原创正文与图示采用 <a href="https://creativecommons.org/licenses/by-nc-sa/4.0/">CC BY-NC-SA 4.0</a>，要求署名、非商业使用、相同方式共享。代码采用 PolyForm Noncommercial 1.0.0。第三方材料遵循各自许可。完整范围见仓库许可说明。</p><p>在线阅读：<a href="{website}">{website}</a><br>配套仓库：<a href="{repository}">{repository}</a></p><p>本版配套源码：{revision[:12]}。书中源码链接指向该提交。工具与框架可能变化，复现时请结合对应版本说明。</p><p>本书为作者自行编排的电子版。字体使用 Noto CJK，依 SIL Open Font License 1.1 提供；图示、截图与示例的来源和适用范围随正文保留。</p></section>'''
    glossary='<section class="appendix" id="glossary"><h1>术语索引</h1><dl>'
    for item in edition['glossary']:
        if item['chapter'] not in soups: raise ValueError('Unknown glossary chapter')
        glossary+=f'<dt>{esc(item["term"])}</dt><dd>{esc(item["definition"])} <a href="#chapter-{item["chapter"]}">参见{esc(titles[item["chapter"]].split("　")[0])}</a>。</dd>'
    glossary+='</dl></section>'
    resource='<section class="appendix" id="resources"><h1>章节与配套资源</h1><p>正文可独立阅读。需要复现时，按下表选择对应的工程阶段；每个入口提供本阶段的启动方式与验证范围。章节编号与目录一致，源码需联网获取。</p><table><thead><tr><th>章节</th><th>配套入口</th></tr></thead><tbody>'
    grouped={}
    for row in rows: grouped.setdefault(row['practice']['path'],[]).append(row)
    for path,members in grouped.items():
        target=(ROOT/path).resolve();internal=by_source.get(target)
        href='#chapter-'+internal if internal else repository+'/blob/'+revision+'/'+quote(path)
        display=members[0]['practice']['label'] if internal else path
        refs=[];i=0
        while i<len(members):
            first=members[i];last=i
            while last+1<len(members) and members[last]['id'].isdigit() and members[last+1]['id'].isdigit() and int(members[last+1]['id'])==int(members[last]['id'])+1: last+=1
            label={'INTRO':'导读','END':'结语'}.get(first['id'],first['id'])
            if last>i:label+='—'+members[last]['id']
            refs.append(f'<a href="#chapter-{first["id"]}">{esc(label)}</a>');i=last+1
        chapter_text='<a href="#contents">全书</a>' if len(members)==len(rows) else '、'.join(refs)
        resource+=f'<tr><td>{chapter_text}</td><td><a href="{href}">{esc(display)}</a></td></tr>'
    resource+='</tbody></table><h2>勘误与交流</h2><p>反馈时注明书名、电子版版本、章节和具体位置。代码问题请附运行环境与复现步骤。</p><p><a href="'+repository+'/issues">'+repository+'/issues</a></p><p>微信公众号：好好用 AI</p><img class="contact-qr" src="'+(ROOT/'site/assets/wechat-official-account.jpg').as_uri()+'" alt="好好用 AI 微信公众号二维码"></section>'
    toc='<nav class="book-toc" id="contents" aria-label="全书目录"><h1>目录</h1><ul><li><a href="#preface">前言</a></li><li><a href="#reading-guide">如何阅读本书</a></li>'+''.join(toc_items)+'<li class="toc-part"><a href="#glossary">术语索引</a></li><li><a href="#resources">章节与配套资源</a></li></ul></nav>'
    manuscript=cover+titlepage+copyright+''.join(front)+toc+str(result)+glossary+resource
    final=fragment(manuscript);ids=[x['id'] for x in final.select('[id]')]
    if len(ids)!=len(set(ids)): raise ValueError('Duplicate book anchor')
    for a in final.select('a[href^="#"]'):
        if a['href'][1:] not in ids: raise ValueError('Broken internal book link: '+a['href'])
    for file in sorted(BOOK.rglob('*')):
        if file.is_file() and '.cache' not in file.parts and '__pycache__' not in file.parts:
            sources[file.relative_to(ROOT).as_posix()]=hashlib.sha256(file.read_bytes()).hexdigest()
    for asset in ['site/assets/site.css','site/course.json','docs/course/catalog.json','docs/assets/readme-cover.png','site/assets/wechat-official-account.jpg']:
        sources[asset]=hashlib.sha256((ROOT/asset).read_bytes()).hexdigest()
    manifest={'title':edition['title'],'author':edition['author'],'version':edition['version'],'date':edition['date'],'repository':catalog['repository'],'git_commit':revision,'language':edition['language'],'sources':sources,**stats}
    manifest['content_sha256']=hashlib.sha256(json.dumps(sources,sort_keys=True).encode()).hexdigest()
    return manuscript,edition,manifest

def build(output=None, html_only=False):
    output=Path(output).resolve() if output else ROOT/'site/dist/downloads'
    output.mkdir(parents=True,exist_ok=True)
    content,edition,manifest=compile_manuscript()
    font_dir=fonts()
    doc='<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><title>'+esc(edition['title'])+'</title><meta name="author" content="'+esc(edition['author'])+'"><meta name="description" content="'+esc(edition['subtitle'])+'"></head><body>'+content+'</body></html>'
    cache=BOOK/'.cache';cache.mkdir(exist_ok=True)
    (cache/'manuscript.html').write_text(doc,encoding='utf-8')
    if html_only: return manifest
    if platform.system()=='Darwin':
        lib=next((p for p in ('/opt/homebrew/lib','/usr/local/lib') if (Path(p)/'libpango-1.0.dylib').exists()),None)
        if lib:os.environ.setdefault('DYLD_FALLBACK_LIBRARY_PATH',lib)
    from weasyprint import HTML,CSS
    theme_css=':root{'+''.join('--'+k+':'+v+';' for k,v in edition['theme'].items())+'}'
    font_css=f'''@font-face{{font-family:BookMono;src:url("{(font_dir/'NotoSansMono-Regular.ttf').as_uri()}")}} @font-face{{font-family:BookSerif;src:url("{(font_dir/'NotoSerifCJKsc-Regular.otf').as_uri()}")}} @font-face{{font-family:BookSans;src:url("{(font_dir/'NotoSansCJKsc-Regular.otf').as_uri()}")}} :root{{--accent:{edition['accent']}}} @page{{@top-left{{content:"{edition['english']}"}}}}'''
    from weasyprint.text.fonts import FontConfiguration
    font_config=FontConfiguration()
    with tempfile.TemporaryDirectory(prefix='course-book-') as directory:
        temp=Path(directory);stem=edition['slug']
        print('Rendering PDF: '+edition['title'],flush=True)
        rendered=HTML(string=doc,base_url=str(ROOT)).render(stylesheets=[CSS(filename=str(BOOK/'print.css'),font_config=font_config),CSS(string=font_css+theme_css,font_config=font_config)],font_config=font_config)
        pdf=temp/(stem+'.pdf');rendered.write_pdf(pdf, optimize_images=True, jpeg_quality=92)
        manifest['pdf_pages']=len(rendered.pages)
        manifest['tools']={'pandoc':subprocess.check_output(['pandoc','--version'],text=True).splitlines()[0],'weasyprint':'68.0'}
        subprocess.run(['pdftoppm','-f','1','-singlefile','-r','110','-png',str(pdf),str(temp/'cover')],check=True,stdout=subprocess.DEVNULL)
        epub_soup=fragment(doc);epub_soup.select_one('section.cover').decompose()
        # EPUB readers supply their own navigation; retain a visible contents page without print-only page numbers.
        epub_style=temp/'epub.css';epub_style.write_text((BOOK/'epub.css').read_text().replace('#245f49',edition['accent']).replace('#252b30',edition['theme']['ink']).replace('#525c65',edition['theme']['muted']).replace('#f3f5f6',edition['theme']['soft']),encoding='utf-8')
        source=temp/'book.html';source.write_text(str(epub_soup),encoding='utf-8')
        env=dict(os.environ);env['SOURCE_DATE_EPOCH']=str(int(datetime.fromisoformat(edition['date']).replace(tzinfo=timezone.utc).timestamp()))
        subprocess.run(['pandoc',str(source),'-f','html','-t','epub3','--toc','--toc-depth=2','--split-level=1','--css',str(epub_style),'--epub-cover-image',str(temp/'cover.png'),'--metadata','title='+edition['title'],'--metadata','subtitle='+edition['subtitle'],'--metadata','author='+edition['author'],'--metadata','lang=zh-CN','--metadata','date='+edition['date'],'--metadata','identifier=urn:course:'+stem+':'+edition['version']+':zh-CN','--metadata','rights=正文与图示 CC BY-NC-SA 4.0；代码 PolyForm Noncommercial 1.0.0','-o',str(temp/(stem+'.epub'))],check=True,env=env)
        manifest['files']={name:hashlib.sha256((temp/name).read_bytes()).hexdigest() for name in (stem+'.pdf',stem+'.epub')}
        for name in manifest['files']:shutil.copy2(temp/name,output/name)
        (output/(stem+'-manifest.json')).write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        print(json.dumps({k:manifest[k] for k in ('title','articles','pdf_pages','code_blocks','figures','tables','diagrams')},ensure_ascii=False),flush=True)
    return manifest

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output',type=Path)
    parser.add_argument('--html-only',action='store_true')
    args=parser.parse_args();build(args.output,args.html_only)
