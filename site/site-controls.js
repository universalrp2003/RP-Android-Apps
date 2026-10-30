/* Local progressive enhancements. No analytics, remote libraries or network requests. */
(function () {
  'use strict';
  var root = document.documentElement;
  root.classList.add('has-js');
  var language = document.getElementById('site-language');
  var jump = document.getElementById('jump-app');
  var status = document.getElementById('filter-status');
  var currentLanguage = 'both';
  var groups = {
    'sms-guard': 'security', 'live-guard': 'security', 'secret-message': 'security',
    'ramesh-radio': 'media', 'pulseeq': 'media', 'rp-vocal-remover-studio': 'media', 'tamil-podcast': 'media',
    'daily-vault': 'productivity', 'expense-tracker': 'productivity', 'voice-notes': 'productivity',
    'oor-arivippu': 'utility', 'elderly-launcher': 'utility', 'vasanthi-ai': 'utility', 'tamilan-store': 'utility'
  };
  var cards = Array.from(document.querySelectorAll('article.card')).map(function (card) {
    var heading = card.querySelector('h2[id]');
    var slug = heading.id.replace(/^app-/, '');
    card.dataset.category = groups[slug];
    var option = document.createElement('option');
    option.value = heading.id;
    option.textContent = heading.textContent.trim();
    jump.appendChild(option);
    return { card: card, heading: heading, slug: slug, category: groups[slug] };
  });
  var rows = Array.from(document.querySelectorAll('.latest tbody tr'));
  function text(en, ta) { return currentLanguage === 'ta' ? ta : currentLanguage === 'en' ? en : ta + ' / ' + en; }
  function applyLanguage(value) {
    currentLanguage = ['both', 'en', 'ta'].includes(value) ? value : 'both';
    root.dataset.siteLanguage = currentLanguage;
    root.lang = currentLanguage === 'ta' ? 'ta' : 'en';
    language.value = currentLanguage;
    document.querySelectorAll('[data-copy-sha]').forEach(function (button) {
      button.textContent = text('Copy SHA-256', 'சரிபார்ப்பு மதிப்பை நகலெடு');
    });
  }
  // Only an explicitly selected language preference is remembered, on this browser alone.
  var saved = null;
  try { saved = localStorage.getItem('rp-site-language'); } catch (_) { /* storage can be disabled */ }
  applyLanguage(saved || 'both');
  language.addEventListener('change', function () {
    applyLanguage(language.value);
    try { localStorage.setItem('rp-site-language', currentLanguage); } catch (_) { /* optional */ }
    filter(activeFilter);
  });
  var activeFilter = 'all';
  function filter(group) {
    if (group !== 'all' && !Object.values(groups).includes(group)) group = 'all';
    activeFilter = group;
    var visible = 0;
    cards.forEach(function (entry) {
      entry.card.hidden = group !== 'all' && entry.category !== group;
      if (!entry.card.hidden) visible++;
    });
    rows.forEach(function (row) {
      var link = row.querySelector('.lname a');
      var slug = link.hash.replace('#app-', '');
      row.hidden = group !== 'all' && groups[slug] !== group;
    });
    document.querySelectorAll('[data-category-filter]').forEach(function (button) {
      button.setAttribute('aria-pressed', button.dataset.categoryFilter === group ? 'true' : 'false');
    });
    status.textContent = text('Showing ' + visible + ' of ' + cards.length + ' apps.', 'மொத்தம் ' + cards.length + ' செயலிகளில் ' + visible + ' காட்டப்படுகின்றன.');
  }
  document.querySelectorAll('[data-category-filter]').forEach(function (button) {
    var group = button.dataset.categoryFilter;
    button.querySelector('[data-filter-count]').textContent = group === 'all' ? cards.length : cards.filter(function (entry) { return entry.category === group; }).length;
    button.addEventListener('click', function () { filter(group); });
  });
  function revealHash() {
    var entry = cards.find(function (item) { return '#' + item.heading.id === window.location.hash; });
    if (entry && entry.card.hidden) filter('all');
  }
  window.addEventListener('hashchange', revealHash);
  jump.addEventListener('change', function () {
    if (!jump.value) return;
    filter('all');
    window.location.hash = jump.value;
    document.getElementById(jump.value).focus({ preventScroll: true });
  });
  filter('all');
  revealHash();

  cards.forEach(function (entry) {
    var link = entry.card.querySelector('a[data-apk]');
    var hash = link && link.dataset.sha;
    if (!hash || !/^[0-9a-f]{64}$/.test(hash)) return;
    entry.card.querySelectorAll('a[data-apk]').forEach(function (download) {
      download.setAttribute('aria-label', 'Download ' + entry.heading.textContent.trim() + ' APK, ' + entry.card.querySelector('.ver').textContent.trim());
    });
    var body = entry.card.querySelector('details.trust .body');
    if (!body) return;
    var button = document.createElement('button');
    button.type = 'button'; button.className = 'copy-sha'; button.dataset.copySha = hash;
    button.textContent = text('Copy SHA-256', 'சரிபார்ப்பு மதிப்பை நகலெடு');
    button.setAttribute('aria-label', 'Copy SHA-256 — ' + entry.heading.textContent.trim());
    var feedback = document.createElement('span');
    feedback.className = 'copy-feedback'; feedback.setAttribute('role', 'status');
    button.addEventListener('click', async function () {
      try {
        if (!navigator.clipboard || !navigator.clipboard.writeText) throw new Error('Clipboard unavailable');
        await navigator.clipboard.writeText(hash);
        feedback.textContent = text('Copied. This does not verify a downloaded file.', 'நகலெடுக்கப்பட்டது. இது பதிவிறக்கிய கோப்பைச் சரிபார்ப்பதல்ல.');
      } catch (_) {
        var field = body.querySelector('textarea.manual-hash');
        if (!field) { field = document.createElement('textarea'); field.className = 'manual-hash'; field.readOnly = true; field.setAttribute('aria-label', 'SHA-256 for manual copying'); body.appendChild(field); }
        field.value = hash; field.focus(); field.select();
        feedback.textContent = text('Automatic copy unavailable. Select and copy the hash manually.', 'தானாக நகலெடுக்க முடியவில்லை. மதிப்பைத் தேர்ந்தெடுத்து நகலெடுக்கவும்.');
      }
    });
    body.appendChild(button); body.appendChild(feedback);
  });
  rows.forEach(function (row) {
    var download = row.querySelector('a[data-apk]');
    download.setAttribute('aria-label', 'Download ' + row.querySelector('.lname').textContent.trim() + ' APK ' + row.querySelector('.lver').textContent);
  });
  document.querySelectorAll('.emoji, .feat-icon').forEach(function (icon) { icon.setAttribute('aria-hidden', 'true'); });
}());
