/* 无构建框架或远端服务；正文在关闭 JavaScript 时仍完整可读。 */
(() => {
  'use strict';
  const root = document.body.dataset.root || './';
  const page = document.body.dataset.page;
  const data = window.LEARNING_DATA || { lessons: [], sections: [] };
  const isEnglish = document.documentElement.lang === 'en';
  const t = value => data.labels?.[value] || value;
  const lessons = new Map(data.lessons.map(lesson => [lesson.id, lesson]));
  const storageKey = 'haohai-learn-java-ai-reading-v1';
  const defaultState = () => ({ last: null, fontSize: 18, focus: false });
  let state = defaultState();
  let storageAvailable = true;
  try {
    const stored = JSON.parse(localStorage.getItem(storageKey) || 'null');
    if (stored && typeof stored === 'object') {
      if (stored.last && lessons.has(stored.last.id)) {
        state.last = { id: stored.last.id, anchor: typeof stored.last.anchor === 'string' ? stored.last.anchor : '', language: stored.last.language };
      }
      state.fontSize = [16, 18, 20, 22].includes(stored.fontSize) ? stored.fontSize : 18;
      state.focus = stored.focus === true;
    }
  } catch (_) { storageAvailable = false; }
  const save = () => {
    try { localStorage.setItem(storageKey, JSON.stringify(state)); }
    catch (_) { storageAvailable = false; }
  };
  const relativeURL = value => new URL(root + value, location.href).href;
  const toast = document.getElementById('toast');
  let toastTimer;
  function announce(message) {
    if (!toast) return;
    clearTimeout(toastTimer);
    toast.textContent = message;
    toast.classList.add('visible');
    toastTimer = setTimeout(() => toast.classList.remove('visible'), 2400);
  }
  document.documentElement.classList.add('js');
  const themeButton = document.querySelector('.theme-toggle');
  function applyTheme(theme) {
    document.documentElement.dataset.theme = theme;
    const label = t(theme === 'dark' ? '切换到浅色主题' : '切换到深色主题');
    themeButton?.setAttribute('aria-label', label);
    themeButton?.setAttribute('title', label);
  }
  applyTheme(document.documentElement.dataset.theme || 'light');
  themeButton?.addEventListener('click', () => {
    const theme = document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark';
    applyTheme(theme);
    try { localStorage.setItem('haohai-course-theme', theme); } catch (_) { /* 本次仍可切换。 */ }
  });
  matchMedia('(prefers-color-scheme:dark)').addEventListener('change', event => {
    try { if (!localStorage.getItem('haohai-course-theme')) applyTheme(event.matches ? 'dark' : 'light'); }
    catch (_) { applyTheme(event.matches ? 'dark' : 'light'); }
  });

  const continueLink = document.getElementById('continue-learning');
  if (continueLink && state.last) {
    const lesson = lessons.get(state.last.id);
    continueLink.href = relativeURL(lesson.url) + (state.last.anchor && state.last.language === data.language ? '#' + encodeURIComponent(state.last.anchor) : '');
    continueLink.querySelector('span').textContent = t('继续学习');
    document.getElementById('resume-label').textContent = t('上次读到：') + lesson.title;
  }

  // 字号和专注状态保存在当前浏览器，不向远端发送阅读记录。
  function applyFont() {
    document.documentElement.style.setProperty('--body-size', state.fontSize + 'px');
    const label = document.getElementById('font-size-label');
    if (label) label.textContent = state.fontSize;
    document.querySelectorAll('[data-font]').forEach(button => {
      button.disabled = Number(button.dataset.font) < 0 ? state.fontSize <= 16 : state.fontSize >= 22;
    });
  }
  applyFont();
  document.querySelectorAll('[data-font]').forEach(button => button.addEventListener('click', () => {
    state.fontSize = Math.min(22, Math.max(16, state.fontSize + Number(button.dataset.font) * 2));
    applyFont(); save();
  }));
  const focusButton = document.querySelector('.focus-toggle');
  function applyFocus() {
    if (!focusButton) return;
    document.body.classList.toggle('focus-mode', state.focus);
    focusButton.setAttribute('aria-pressed', String(state.focus));
    focusButton.querySelector('span').textContent = t(state.focus ? '退出专注' : '专注阅读');
  }
  applyFocus();
  focusButton?.addEventListener('click', () => { state.focus = !state.focus; applyFocus(); save(); });

  // 手机目录作为可关闭的导航抽屉：锁定背景、限制焦点，关闭后返回按钮。
  const sidebar = document.getElementById('course-sidebar');
  const menuButton = document.querySelector('.menu-toggle');
  const menuClose = document.querySelector('.menu-close');
  const scrim = document.querySelector('.sidebar-scrim');
  let oldOverflow = '';
  let inerted = [];
  function closeMenu(returnFocus = true) {
    if (!sidebar?.classList.contains('mobile-open')) return;
    sidebar.classList.remove('mobile-open');
    sidebar.removeAttribute('role'); sidebar.removeAttribute('aria-modal');
    scrim.hidden = true;
    menuButton.setAttribute('aria-expanded', 'false');
    document.body.style.overflow = oldOverflow;
    inerted.forEach(el => el.inert = false); inerted = [];
    if (returnFocus) menuButton.focus();
  }
  menuButton?.addEventListener('click', () => {
    if (sidebar.classList.contains('mobile-open')) { closeMenu(); return; }
    sidebar.classList.add('mobile-open');
    sidebar.setAttribute('role', 'dialog'); sidebar.setAttribute('aria-modal', 'true');
    scrim.hidden = false; menuButton.setAttribute('aria-expanded', 'true');
    oldOverflow = document.body.style.overflow; document.body.style.overflow = 'hidden';
    inerted = [...document.querySelectorAll('.reader-main,.context-sidebar,.site-header')].filter(el => !el.inert);
    inerted.forEach(el => el.inert = true);
    menuClose.focus();
  });
  menuClose?.addEventListener('click', () => closeMenu());
  scrim?.addEventListener('click', () => closeMenu());
  sidebar?.addEventListener('keydown', event => {
    if (!sidebar.classList.contains('mobile-open')) return;
    if (event.key === 'Escape') { event.preventDefault(); closeMenu(); }
    if (event.key === 'Tab') {
      const focusable = [...sidebar.querySelectorAll('a,button,summary')].filter(el => el.getClientRects().length);
      const first = focusable[0], last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
      else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
    }
  });
  matchMedia('(min-width:861px)').addEventListener('change', event => { if (event.matches) closeMenu(false); });

  // 搜索按二级标题定位；纯文本节点与 mark，关键词不会成为 HTML。
  const searchDialog = document.getElementById('search-dialog');
  const searchInput = document.getElementById('course-search');
  const searchResults = document.getElementById('search-results');
  const searchStatus = document.getElementById('search-status');
  function addMarkedText(element, text, terms) {
    if (!terms.length) { element.textContent = text; return; }
    const pattern = terms.map(term => term.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).join('|');
    const matcher = new RegExp(pattern, 'gi');
    let start = 0;
    for (const match of text.matchAll(matcher)) {
      element.append(document.createTextNode(text.slice(start, match.index)));
      const mark = document.createElement('mark'); mark.textContent = match[0]; element.append(mark);
      start = match.index + match[0].length;
    }
    element.append(document.createTextNode(text.slice(start)));
  }
  function resultLink(result, terms) {
    const link = document.createElement('a'); link.className = 'search-result'; link.href = relativeURL(result.url);
    if (terms.length) {
      const destination = new URL(link.href);
      destination.searchParams.set('q', terms.join(' '));
      link.href = destination.href;
    }
    const title = document.createElement('strong');
    addMarkedText(title, result.title, terms); link.append(title);
    if (result.heading) {
      const section = document.createElement('span'); addMarkedText(section, result.heading, terms); link.append(section);
    }
    if (result.text && terms.length) {
      const normalized = result.text.toLocaleLowerCase();
      const positions = terms.map(term => normalized.indexOf(term)).filter(index => index >= 0);
      const position = positions.length ? Math.min(...positions) : 0;
      const start = Math.max(0, position - 28);
      const snippet = (start ? '…' : '') + result.text.slice(start, start + 112) + (result.text.length > start + 112 ? '…' : '');
      const excerpt = document.createElement('p'); addMarkedText(excerpt, snippet, terms); link.append(excerpt);
    }
    return link;
  }
  function search() {
    searchResults.replaceChildren();
    const query = searchInput.value.trim().slice(0, 160).toLocaleLowerCase();
    const terms = [...new Set(query.split(/\s+/).filter(Boolean))].slice(0, 8);
    if (!terms.length) {
      searchStatus.textContent = isEnglish ? `Search all ${data.lessons.length} articles, including code and folded answers.` : `搜索全部 ${data.lessons.length} 篇文章，包含代码与折叠答案。`;
      data.lessons.slice(0, 5).forEach(lesson => searchResults.append(resultLink(lesson, [])));
      return;
    }
    const matches = data.sections.map(section => {
      const title = section.title.toLocaleLowerCase();
      const heading = section.heading.toLocaleLowerCase();
      const text = section.text.toLocaleLowerCase();
      const all = title + ' ' + heading + ' ' + text;
      if (!terms.every(term => all.includes(term))) return null;
      const score = terms.reduce((sum, term) => sum + (title.includes(term) ? 6 : 0) + (heading.includes(term) ? 9 : 0) + (text.includes(term) ? 1 : 0), 0);
      return { ...section, score };
    }).filter(Boolean).sort((a, b) => b.score - a.score);
    searchStatus.textContent = matches.length ? (isEnglish ? `${matches.length} matches${matches.length > 40 ? '; showing the first 40' : ''}` : `找到 ${matches.length} 处内容${matches.length > 40 ? '，显示前 40 项' : ''}`) : t('没有找到匹配内容');
    if (!matches.length) {
      const empty = document.createElement('p'); empty.className = 'search-empty';
      empty.textContent = t('试试较短的关键词，例如“上下文”“验证”或“权限”。'); searchResults.append(empty);
    }
    matches.slice(0, 40).forEach(result => searchResults.append(resultLink(result, terms)));
  }
  function openSearch() {
    if (!searchDialog || typeof searchDialog.showModal !== 'function') return;
    closeMenu(false); searchDialog.showModal(); search(); searchInput.focus();
  }
  document.querySelectorAll('.search-trigger').forEach(button => {
    if (!/Mac|iPhone|iPad/.test(navigator.platform)) button.querySelector('kbd').textContent = 'Ctrl K';
    button.addEventListener('click', openSearch);
  });
  searchInput?.addEventListener('input', search);
  searchDialog?.addEventListener('keydown', event => {
    const items = [...searchResults.querySelectorAll('a')];
    if (!items.length) return;
    const index = items.indexOf(document.activeElement);
    if (event.key === 'ArrowDown') { event.preventDefault(); items[(index + 1) % items.length].focus(); }
    if (event.key === 'ArrowUp') { event.preventDefault(); (index <= 0 ? searchInput : items[index - 1]).focus(); }
    if (event.key === 'Enter' && document.activeElement === searchInput) { event.preventDefault(); items[0].click(); }
  });
  searchResults?.addEventListener('click', event => { if (event.target.closest('a')) searchDialog.close(); });
  document.addEventListener('keydown', event => {
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') { event.preventDefault(); openSearch(); }
  });
  searchDialog?.addEventListener('click', event => {
    if (event.target !== searchDialog) return;
    const rect = searchDialog.getBoundingClientRect();
    if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) searchDialog.close();
  });

  // 仅为可见代码着色。复制始终使用原始 textContent，保留空格和换行。
  function highlight(code) {
    if (!code.classList.contains('language-python')) return;
    const original = code.textContent;
    const tokens = /("""[\s\S]*?"""|'''[\s\S]*?'''|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*')|(#[^\n]*)|\b(False|None|True|and|as|assert|async|await|break|class|continue|def|del|elif|else|except|finally|for|from|global|if|import|in|is|lambda|nonlocal|not|or|pass|raise|return|try|while|with|yield)\b|\b(print|range|len|zip|max|min|sum|enumerate|float|int|list|dict|set|tuple|isinstance)\b|\b(\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)\b/g;
    const fragment = document.createDocumentFragment(); let cursor = 0;
    for (const match of original.matchAll(tokens)) {
      fragment.append(document.createTextNode(original.slice(cursor, match.index)));
      const span = document.createElement('span');
      span.className = match[1] ? 'token-string' : match[2] ? 'token-comment' : match[3] ? 'token-keyword' : match[4] ? 'token-builtin' : 'token-number';
      span.textContent = match[0]; fragment.append(span); cursor = match.index + match[0].length;
    }
    fragment.append(document.createTextNode(original.slice(cursor))); code.replaceChildren(fragment);
  }
  document.querySelectorAll('#article-body pre').forEach(pre => {
    const code = pre.querySelector('code'); if (!code) return;
    const text = code.textContent;
    const wrapper = document.createElement('div'); wrapper.className = 'code-block';
    const toolbar = document.createElement('div'); toolbar.className = 'code-toolbar';
    const label = document.createElement('span');
    label.textContent = [...code.classList].find(value => value.startsWith('language-'))?.slice(9) || 'text';
    const copy = document.createElement('button'); copy.type = 'button'; copy.className = 'code-copy'; copy.textContent = t('复制代码');
    copy.addEventListener('click', async () => {
      try {
        if (navigator.clipboard?.writeText) await navigator.clipboard.writeText(text);
        else {
          const field = document.createElement('textarea'); field.value = text;
          field.style.position = 'fixed'; field.style.opacity = '0'; document.body.append(field); field.select();
          const ok = document.execCommand('copy'); field.remove(); copy.focus();
          if (!ok) throw new Error('Copy unavailable');
        }
        copy.textContent = t('已复制'); setTimeout(() => copy.textContent = t('复制代码'), 1800);
      } catch (_) { announce(t('复制未成功，请选中代码后手动复制')); }
    });
    toolbar.append(label, copy); pre.replaceWith(wrapper); wrapper.append(toolbar, pre); highlight(code);
  });

  const viewer = document.getElementById('image-viewer');
  if (viewer && typeof viewer.showModal === 'function') {
    const picture = document.getElementById('large-image');
    const sizeButton = document.getElementById('image-size');
    const canvas = document.querySelector('.image-canvas');
    let opener = null;
    document.querySelectorAll('#article-body img').forEach(img => {
      if (img.closest('a')) return;
      const button = document.createElement('button'); button.type = 'button'; button.className = 'image-open';
      button.setAttribute('aria-label', t('放大配图') + ': ' + (img.alt || (isEnglish ? 'Figure' : '正文配图')));
      const visual = img.parentElement.tagName === 'PICTURE' ? img.parentElement : img;
      if (visual.tagName === 'PICTURE') button.classList.add('wide-figure-button');
      visual.replaceWith(button); button.append(visual);
      button.addEventListener('click', () => {
        opener = button; picture.src = img.currentSrc || img.src; picture.alt = img.alt;
        picture.classList.remove('actual-size'); sizeButton.textContent = t('原尺寸');
        document.getElementById('image-description').textContent = img.alt;
        document.getElementById('original-image').href = picture.src;
        viewer.showModal(); canvas.scrollTop = 0; canvas.scrollLeft = 0;
      });
    });
    document.getElementById('close-image').addEventListener('click', () => viewer.close());
    sizeButton.addEventListener('click', () => {
      sizeButton.textContent = t(picture.classList.toggle('actual-size') ? '适应屏幕' : '原尺寸');
      canvas.scrollTop = 0; canvas.scrollLeft = 0;
    });
    viewer.addEventListener('close', () => { opener?.focus(); picture.removeAttribute('src'); });
  }

  // 实验在本页打开，关闭后回到原来的段落和焦点；也可选择独立窗口。
  document.querySelectorAll('.inline-experiment').forEach(panel => {
    const frame = panel.querySelector('iframe');
    panel.addEventListener('toggle', () => {
      if (panel.open) frame.src = frame.dataset.src;
      else frame.removeAttribute('src');
    });
  });
  const experimentViewer = document.getElementById('experiment-viewer');
  const experimentFrame = document.getElementById('experiment-frame');
  if (experimentViewer && typeof experimentViewer.showModal === 'function') {
    let experimentOpener;
    function openExperiment(event, address, title) {
      if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
      event.preventDefault();
      experimentOpener = event.currentTarget;
      document.getElementById('experiment-title').textContent = title;
      document.getElementById('experiment-standalone').href = address;
      experimentFrame.title = title;
      experimentFrame.src = address;
      experimentViewer.showModal();
      document.getElementById('close-experiment').focus();
    }
    document.querySelectorAll('.lesson-experiments a,.experiment-feature,.more-experiments a').forEach(link => {
      link.addEventListener('click', event => openExperiment(event, link.href, link.querySelector('h3')?.textContent || link.textContent.trim()));
    });
    document.querySelectorAll('#article-body a[href]').forEach(link => {
      if (link.closest('.inline-experiment')) return;
      const url = new URL(link.href);
      if (url.hostname !== 'daviddingxu.github.io') return;
      const match = url.pathname.match(/^\/llm-from-zero\/demos\/(\d{2})\/(.*)$/);
      if (!match) return;
      const candidates = lessons.get(match[1])?.demos || [];
      const tail = match[2];
      const target = candidates.find(item => tail === 'surface.html' ? item.url.includes('三维演示') : tail === 'model.html' ? item.url.includes('模型解剖台') : tail === 'training.html' ? item.url.includes('训练回放') : item.url.includes('交互演示'));
      if (target) link.addEventListener('click', event => openExperiment(event, relativeURL(target.url), link.textContent));
    });
    document.getElementById('close-experiment').addEventListener('click', () => experimentViewer.close());
    experimentViewer.addEventListener('close', () => { experimentFrame.src = 'about:blank'; experimentOpener?.focus({ preventScroll: true }); });
    experimentFrame.addEventListener('load', () => {
      try {
        const innerDocument = experimentFrame.contentDocument;
        if (!innerDocument) return;
        innerDocument.addEventListener('keydown', event => { if (event.key === 'Escape') { event.preventDefault(); experimentViewer.close(); } });
        innerDocument.querySelectorAll('a[href]').forEach(link => {
          if (link.closest('.inline-experiment')) return;
      const url = new URL(link.href);
          if (url.origin === location.origin && /\/(阅读(?:\.en)?\.html|正文(?:\.en)?\.md)$/.test(decodeURIComponent(url.pathname))) {
            link.addEventListener('click', event => { event.preventDefault(); experimentViewer.close(); });
          }
        });
      } catch (_) { /* 独立打开链接仍然可用。 */ }
    });
  }

  function openAncestors(target) {
    if (!target) return;
    for (let parent = target.parentElement; parent; parent = parent.parentElement) {
      if (parent.tagName === 'DETAILS') parent.open = true;
    }
  }
  function revealHash() {
    const query = new URLSearchParams(location.search).get('q');
    if (query) {
      const terms = query.toLocaleLowerCase().split(/\s+/).filter(Boolean);
      document.querySelectorAll('#article-body details').forEach(details => {
        const text = details.textContent.toLocaleLowerCase();
        if (terms.some(term => text.includes(term))) { details.open = true; openAncestors(details); }
      });
    }
    let id; try { id = decodeURIComponent(location.hash.slice(1)); } catch (_) { return; }
    const target = document.getElementById(id);
    if (target) { openAncestors(target); target.scrollIntoView({ block: 'start' }); }
  }
  document.querySelectorAll('.reading-route a').forEach(link => link.addEventListener('click', () => {
    let id; try { id = decodeURIComponent(link.hash.slice(1)); } catch (_) { return; }
    openAncestors(document.getElementById(id));
  }));
  addEventListener('hashchange', revealHash);
  if (location.hash || new URLSearchParams(location.search).has('q')) requestAnimationFrame(revealHash);
  const article = document.getElementById('article-body');
  const lessonVideo = document.getElementById('lesson-video');
  if (article && lessonVideo) {
    const heading = article.querySelector('h1');
    const next = heading?.nextElementSibling;
    const isByline = next?.tagName === 'P' && /^(手写小模型，看懂大模型|Build a Small Model, Understand Large Models)\s*·/.test(next.textContent);
    (isByline ? next : heading)?.after(lessonVideo);
    lessonVideo.addEventListener('toggle', () => { if (!lessonVideo.open) lessonVideo.querySelector('video')?.pause(); });
  }
  const headings = article ? [...article.querySelectorAll('h2[id]')] : [];
  const tocLinks = [...document.querySelectorAll('.reading-route a')];
  let currentAnchor = '';
  let frameRequested = false;
  function updateReading() {
    frameRequested = false;
    if (!article) return;
    const articleTop = article.getBoundingClientRect().top + scrollY;
    const length = Math.max(1, article.offsetHeight - innerHeight + 110);
    const progress = Math.min(100, Math.max(0, (scrollY - articleTop + 100) / length * 100));
    document.getElementById('reading-progress-bar').style.width = progress + '%';
    let current = '';
    for (const heading of headings) {
      if (heading.getClientRects().length && heading.getBoundingClientRect().top <= 150) current = heading.id;
    }
    tocLinks.forEach(link => {
      let id; try { id = decodeURIComponent(link.hash.slice(1)); } catch (_) { return; }
      const active = id === current;
      link.classList.toggle('active', active);
      if (active) link.setAttribute('aria-current', 'location'); else link.removeAttribute('aria-current');
    });
    if (lessons.has(page) && (current !== currentAnchor || state.last?.id !== page)) {
      currentAnchor = current; state.last = { id: page, anchor: current, language: data.language }; save();
    }
  }
  if (article) {
    if (lessons.has(page) && !location.hash) { state.last = { id: page, anchor: '', language: data.language }; save(); }
    updateReading();
    addEventListener('scroll', () => { if (!frameRequested) { frameRequested = true; requestAnimationFrame(updateReading); } }, { passive: true });
    addEventListener('resize', updateReading);
    addEventListener('pagehide', () => { state.last = { id: page, anchor: currentAnchor, language: data.language }; save(); });
  }
  const stageLinks = [...document.querySelectorAll('.path-rail nav a')];
  const stages = [...document.querySelectorAll('.catalog-stage')];
  if (stages.length) {
    const updateStage = () => {
      let selected = stages[0].id;
      for (const stage of stages) if (stage.getBoundingClientRect().top <= innerHeight * .4) selected = stage.id;
      stageLinks.forEach(link => {
        const active = link.hash === '#' + selected;
        link.classList.toggle('active', active);
        if (active) link.setAttribute('aria-current', 'location'); else link.removeAttribute('aria-current');
      });
    };
    updateStage(); addEventListener('scroll', updateStage, { passive: true });
  }
  // 打印包括折叠答案；退出打印后恢复阅读时的展开状态。
  let closedForPrint = [];
  addEventListener('beforeprint', () => {
    closedForPrint = [...document.querySelectorAll('#article-body details:not([open])')];
    closedForPrint.forEach(details => details.open = true);
  });
  addEventListener('afterprint', () => { closedForPrint.forEach(details => details.open = false); closedForPrint = []; });
})();
