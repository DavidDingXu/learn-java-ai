"""Check generated articles, local resources, navigation anchors and source hashes."""
import hashlib
from html.parser import HTMLParser
import json
from pathlib import Path
from urllib.parse import unquote, urlsplit

ROOT = Path(__file__).resolve().parents[1]
DIST = ROOT / 'site/dist'


class Page(HTMLParser):
    def __init__(self, text):
        super().__init__()
        self.ids, self.links, self.images = set(), [], []
        self.digest = None
        self.article_depth = 0
        self.article_text = []
        self.feed(text)

    def handle_starttag(self, tag, attributes):
        attrs = dict(attributes)
        if attrs.get('id'):
            self.ids.add(attrs['id'])
        for key in ['href', 'src']:
            if attrs.get(key):
                self.links.append(attrs[key])
        if tag == 'img' and attrs.get('src'):
            self.images.append(attrs['src'])
        if tag == 'meta' and attrs.get('name') == 'source-sha256':
            self.digest = attrs.get('content')
        if tag == 'article' and attrs.get('id') == 'article-body':
            self.article_depth += 1

    def handle_endtag(self, tag):
        if tag == 'article' and self.article_depth:
            self.article_depth -= 1

    def handle_data(self, text):
        if self.article_depth:
            self.article_text.append(text)


def check():
    catalog = json.loads((ROOT / 'docs/course/catalog.json').read_text(encoding='utf-8'))
    manifest = json.loads((DIST / 'build-manifest.json').read_text(encoding='utf-8'))
    assert len(manifest['articles']) == catalog['articleCount'], 'Article count differs from catalog'
    pages = {path.resolve(): Page(path.read_text(encoding='utf-8')) for path in DIST.rglob('*.html')}
    links = 0
    images = set()
    for file, page in pages.items():
        for link in page.links:
            url = urlsplit(link)
            if url.scheme or url.netloc:
                continue
            target = (file.parent / unquote(url.path)).resolve() if url.path else file
            assert target.is_relative_to(DIST.resolve()), f'{file.name}: link leaves site: {link}'
            assert target.exists(), f'{file.name}: missing target: {link}'
            if url.fragment and target in pages:
                assert unquote(url.fragment) in pages[target].ids, f'{file.name}: missing anchor: {link}'
            links += 1
        for image in page.images:
            if not urlsplit(image).scheme:
                images.add((file.parent / unquote(urlsplit(image).path)).resolve())
    for article in manifest['articles']:
        source = (ROOT / article['source']).read_text(encoding='utf-8')
        if '\n\n---\n\n[课程首页](' in source:
            source = source.rsplit('\n\n---\n\n', 1)[0]
        page = pages[(DIST / article['url']).resolve()]
        assert page.digest == hashlib.sha256(source.encode()).hexdigest(), f'{article["id"]}: stale article'
        assert len(''.join(page.article_text)) > 1000, f'{article["id"]}: incomplete article'
    print(f'{catalog["repository"]}: {len(manifest["articles"])} article pages, {len(images)} images, {links} site links and anchors verified')


if __name__ == '__main__':
    check()
