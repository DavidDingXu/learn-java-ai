import assert from 'node:assert/strict';
import { existsSync, readFileSync, statSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const catalog = JSON.parse(readFileSync(path.join(root, 'docs/course/catalog.json'), 'utf8'));
const errors = [];
const documents = new Set(['README.md', 'CONTRIBUTING.md', 'docs/course/README.md']);
const images = new Set();
let checkedLinks = 0;

function withoutFencedCode(text) {
  let fence = null;
  return text.split(/\r?\n/).map((line) => {
    const marker = line.match(/^\s{0,3}(`{3,}|~{3,})/);
    if (marker && (!fence || (marker[1][0] === fence[0] && marker[1].length >= fence.length))) {
      fence = fence ? null : marker[1];
      return '';
    }
    return fence ? '' : line;
  }).join('\n');
}

function contained(target) {
  const rel = path.relative(root, target);
  return rel !== '..' && !rel.startsWith(`..${path.sep}`) && !path.isAbsolute(rel);
}

function verifyLink(document, rawUrl, image = false) {
  let url = rawUrl.replace(/^<|>$/g, '');
  const ownPrefix = `https://github.com/${catalog.repository}/`;
  const ownFile = url.startsWith(ownPrefix) && /^\/(?:blob|tree)\/main\//.test(url.slice(ownPrefix.length - 1));
  if (ownFile) {
    url = '/' + url.slice(ownPrefix.length).replace(/^(?:blob|tree)\/main\//, '');
  } else if (/^(?:[a-z][a-z\d+.-]*:|\/\/)/i.test(url)) {
    return;
  }
  const [pathname, fragment] = url.split('#');
  let decoded;
  try { decoded = decodeURIComponent(pathname.split('?')[0]); }
  catch { errors.push(`${document}: invalid URL ${rawUrl}`); return; }
  const target = decoded === '' ? path.join(root, document)
    : decoded.startsWith('/') ? path.join(root, decoded.slice(1))
    : path.resolve(root, path.dirname(document), decoded);
  if (!contained(target) || !existsSync(target)) {
    errors.push(`${document}: missing local target ${rawUrl}`);
    return;
  }
  if (fragment?.startsWith('module-') && target.endsWith('.md')) {
    const htmlId = `<a id="${fragment}"></a>`;
    if (!readFileSync(target, 'utf8').includes(htmlId)) errors.push(`${document}: missing module anchor ${rawUrl}`);
  }
  if (image) {
    if (!statSync(target).isFile() || statSync(target).size === 0) errors.push(`${document}: empty image ${rawUrl}`);
    images.add(path.relative(root, target));
  }
  checkedLinks++;
}

assert.equal(catalog.chapters.length, catalog.articleCount, 'catalog article count');
assert.equal(new Set(catalog.chapters.map(ch => ch.id)).size, catalog.articleCount, 'unique chapter ids');
assert.equal(new Set(catalog.chapters.map(ch => ch.path)).size, catalog.articleCount, 'unique chapter paths');
const index = readFileSync(path.join(root, 'docs/course/README.md'), 'utf8');

for (const chapter of catalog.chapters) {
  documents.add(chapter.path);
  const file = path.resolve(root, chapter.path);
  if (!contained(file) || !existsSync(file)) {
    errors.push(`missing chapter ${chapter.path}`);
    continue;
  }
  const body = readFileSync(file, 'utf8');
  if (body.split(/\r?\n/, 1)[0] !== `# ${chapter.title}`) errors.push(`${chapter.path}: title differs from catalog`);
  if (body.length < 1000) errors.push(`${chapter.path}: full article content is missing`);
  if (!body.includes('[完整目录](') || !body.includes('[课程首页](')) errors.push(`${chapter.path}: chapter navigation is missing`);
  const encoded = encodeURI(path.posix.relative('docs/course', chapter.path));
  if (!index.includes(`](${encoded})`)) errors.push(`${chapter.path}: not linked from course index`);
  verifyLink('README.md', chapter.practice.path);
}

for (const document of documents) {
  const file = path.join(root, document);
  if (!existsSync(file)) { errors.push(`missing document ${document}`); continue; }
  const content = withoutFencedCode(readFileSync(file, 'utf8'));
  if (/\/Users\/|file:\/\/|\/private\/var\/folders\//.test(content)) errors.push(`${document}: local workstation path`);
  for (const match of content.matchAll(/(!?)\[[^\]\n]*\]\(\s*(<[^>]+>|[^\s)]+)(?:\s+["'][^\n]*?["'])?\s*\)/g)) {
    verifyLink(document, match[2], match[1] === '!');
  }
  for (const match of content.matchAll(/<(?:img|a)\b[^>]*?\b(src|href)=["']([^"']+)["']/g)) {
    verifyLink(document, match[2], match[1] === 'src');
  }
}

if (errors.length) {
  console.error(errors.join('\n'));
  process.exitCode = 1;
} else {
  console.log(`${catalog.repository}: ${catalog.articleCount} full articles, ${images.size} images, ${checkedLinks} local links checked`);
}
