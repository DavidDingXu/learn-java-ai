import assert from 'node:assert/strict';
import { copyFileSync, mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

function fixture(t, newline = '\n') {
  const root = mkdtempSync(path.join(os.tmpdir(), 'course check '));
  t.after(() => rmSync(root, { recursive: true, force: true }));
  mkdirSync(path.join(root, 'scripts'), { recursive: true });
  mkdirSync(path.join(root, 'docs/course'), { recursive: true });
  copyFileSync(fileURLToPath(new URL('./check-course.mjs', import.meta.url)), path.join(root, 'scripts/check-course.mjs'));
  function write(file, text) {
    writeFileSync(path.join(root, file), text.replaceAll('\n', newline));
  }
  write('README.md', '# 示例课程\n\n[完整目录](docs/course/README.md)\n\n[示例章节](docs/course/01-example.md)\n');
  write('CONTRIBUTING.md', '# 贡献指南\n');
  write('docs/course/README.md', '# 目录\n\n[示例章节](01-example.md)\n');
  write('docs/course/01-example.md', '# 示例章节\n\n' + '这里是用于验证课程文件读取的完整内容。'.repeat(80)
    + '\n\n![示例图](sample.png)\n\n[课程首页](../../README.md) · [完整目录](README.md)\n');
  writeFileSync(path.join(root, 'docs/course/sample.png'), Buffer.from(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aGnwAAAAASUVORK5CYII=', 'base64'));
  write('docs/course/catalog.json', JSON.stringify({
    title: '示例课程', repository: 'example/course', articleCount: 1,
    chapters: [{ id: '01', title: '示例章节', path: 'docs/course/01-example.md', practice: { path: 'README.md' } }],
  }));
  return root;
}

function run(root) {
  return spawnSync(process.execPath, [path.join(root, 'scripts/check-course.mjs')], { cwd: root, encoding: 'utf8' });
}

for (const [name, newline] of [['LF', '\n'], ['CRLF', '\r\n']]) {
  test(`accepts a complete course checked out with ${name} line endings`, (t) => {
    const result = run(fixture(t, newline));
    assert.equal(result.status, 0, result.stderr);
  });
}

test('rejects an article whose referenced image is missing', (t) => {
  const root = fixture(t);
  rmSync(path.join(root, 'docs/course/sample.png'));
  const result = run(root);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /missing local target sample\.png/);
});


test('rejects a chapter missing from the root README navigation', (t) => {
  const root = fixture(t);
  writeFileSync(path.join(root, 'README.md'), '# 课程\n');
  const result = run(root);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /not linked from root README/);
});

test('rejects author-only production notes in published course pages', (t) => {
  const root = fixture(t);
  writeFileSync(path.join(root, 'CONTRIBUTING.md'), '# 指南\n正式实战文章只能基于这些记录展开。\n');
  const result = run(root);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /author-only material/);
});
