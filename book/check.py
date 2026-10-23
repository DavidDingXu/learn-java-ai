"""Verify the published PDF/EPUB against the edited manuscript and source catalog."""
from collections import Counter
import hashlib,json,posixpath,re
from pathlib import Path
from urllib.parse import unquote,urlsplit
from zipfile import ZipFile
from xml.etree import ElementTree as ET
from bs4 import BeautifulSoup
from pypdf import PdfReader
from build import compile_manuscript,ROOT,BOOK


def check():
    content,edition,current=compile_manuscript()
    directory=ROOT/'site/dist/downloads';slug=edition['slug']
    manifest=json.loads((directory/(slug+'-manifest.json')).read_text(encoding='utf-8'))
    # Source changes are never silently paired with an older offline edition.
    assert manifest['sources']==current['sources'],'Offline edition has stale sources'
    for name,digest in manifest['files'].items():
        assert hashlib.sha256((directory/name).read_bytes()).hexdigest()==digest,'File checksum: '+name
    pdf=PdfReader(directory/(slug+'.pdf'))
    assert pdf.metadata.author==edition['author'],'PDF author'
    assert pdf.metadata.title==edition['title'],'PDF title'
    assert len(pdf.pages)==manifest['pdf_pages'] and len(pdf.pages)>manifest['articles'],'Incomplete PDF'
    assert pdf.outline,'PDF chapter bookmarks missing'
    forbidden=re.compile(r'/Users/|/private/(?:tmp|var)/|file://|作者侧验收|正式实战文章只能|visual-origin:')
    chapter_ids={'chapter-'+row['id'] for row in json.loads((ROOT/'docs/course/catalog.json').read_text())['chapters']}
    source_soup=BeautifulSoup(content,'html.parser')
    original=Counter(x.get_text().rstrip('\n') for x in source_soup.select('pre code'))
    documents={};epub_codes=[];collected_ids=set()
    with ZipFile(directory/(slug+'.epub')) as archive:
        assert archive.namelist()[0]=='mimetype' and archive.read('mimetype')==b'application/epub+zip'
        names=set(archive.namelist())
        for name in names:
            if name.endswith('.svg'):
                svg=archive.read(name).decode('utf-8');ET.fromstring(svg)
                assert '<foreignObject' not in svg,'SVG label depends on HTML: '+name
                assert not re.search(r'\bfont\s*:\s*\d{3}\s+\d',svg),'SVG font shorthand not expanded: '+name
                assert not re.search(r'(?:href|src)="https?://',svg),'SVG requires network: '+name
        container=ET.fromstring(archive.read('META-INF/container.xml'))
        package=next(x for x in container.iter() if x.tag.endswith('rootfile')).attrib['full-path']
        opf=ET.fromstring(archive.read(package));ns={'dc':'http://purl.org/dc/elements/1.1/','opf':'http://www.idpf.org/2007/opf'}
        assert opf.find('.//dc:creator',ns).text==edition['author'],'EPUB author'
        assert opf.find('.//dc:title',ns).text==edition['title'],'EPUB title'
        assert opf.find('.//dc:language',ns).text=='zh-CN','EPUB language'
        assert opf.find('.//dc:identifier',ns).text.endswith(':'+edition['version']+':zh-CN'),'EPUB edition identifier'
        assert opf.find('.//opf:item[@properties="nav"]',ns) is not None,'EPUB navigation'
        assert any('cover-image' in x.attrib.get('properties','') for x in opf.findall('.//opf:item',ns)),'EPUB library cover'
        for name in names:
            if not name.endswith('.xhtml'):continue
            raw=archive.read(name).decode('utf-8');ET.fromstring(raw)
            assert not forbidden.search(raw),'Private/author material in EPUB: '+name
            soup=BeautifulSoup(raw,'html.parser');documents[name]=soup
            assert not soup.select('script,iframe,details'),'Interactive dependency in EPUB'
            ids=[x['id'] for x in soup.select('[id]')]
            assert len(ids)==len(set(ids)),'Duplicate IDs: '+name
            collected_ids.update(ids)
            epub_codes.extend(x.get_text().rstrip('\n') for x in soup.select('pre code'))
        for name,soup in documents.items():
            for element in soup.select('[href],[src]'):
                value=element.get('href',element.get('src'));uri=urlsplit(value)
                if uri.scheme or uri.netloc:
                    assert element.name!='img','Image requires network'
                    continue
                target=posixpath.normpath(posixpath.join(posixpath.dirname(name),unquote(uri.path))) if uri.path else name
                assert target in names,f'Missing embedded target: {name}: {value}'
                if uri.fragment and target in documents:
                    assert documents[target].find(id=unquote(uri.fragment)),f'Broken EPUB anchor: {value}'
    assert chapter_ids<=collected_ids,'A chapter is missing from EPUB'
    assert Counter(epub_codes)==original,'EPUB changed or omitted technical examples'
    # Every PDF page must have text or an intentional visual; complete fonts are embedded by WeasyPrint.
    for index,page in enumerate(pdf.pages):
        assert min(page.mediabox.width,page.mediabox.height)>590 and max(page.mediabox.width,page.mediabox.height)>840,'Unexpected A4 book trim size'
        if index==0:assert edition['author'].replace(' ','') in page.extract_text().replace(' ',''),'Cover author'
        for font in page.get('/Resources',{}).get('/Font',{}).values():
            obj=font.get_object();desc=obj.get('/DescendantFonts',[obj])[0].get_object();descriptor=desc.get('/FontDescriptor')
            if descriptor:
                assert any(k in descriptor.get_object() for k in ('/FontFile','/FontFile2','/FontFile3')),'PDF font is not embedded'
    print(json.dumps({'title':edition['title'],'author':edition['author'],'pages':len(pdf.pages),'articles':manifest['articles'],'preserved_code_blocks':len(epub_codes),'embedded_xhtml_files':len(documents),'status':'verified'},ensure_ascii=False))

if __name__=='__main__':check()
