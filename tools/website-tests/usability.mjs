// npm install --prefix tools/website-tests && serve site, then node tools/website-tests/usability.mjs
// Uses synthetic byte fixtures only. Never downloads real APKs or calls an external service.
import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { pathToFileURL } from 'node:url';
import path from 'node:path';
const modules = process.env.RP_TEST_MODULES || path.resolve('tools/website-tests/node_modules');
const { chromium } = await import(pathToFileURL(path.join(modules, 'playwright/index.mjs')));
const { default: AxeBuilder } = await import(pathToFileURL(path.join(modules, '@axe-core/playwright/dist/index.mjs')));
const { default: packagedChromium } = await import(pathToFileURL(path.join(modules, '@sparticuz/chromium/build/index.js')));
const browser = await chromium.launch({ executablePath: await packagedChromium.executablePath(), args: packagedChromium.args.filter(arg => !['--disable-web-security', '--single-process'].includes(arg)), headless: true });
const base = process.env.RP_SITE_URL || 'http://127.0.0.1:8000';
let passed = 0;
async function test(name, run, options = {}) {
  const context = await browser.newContext({ viewport: { width: 390, height: 844 }, acceptDownloads: false, ...options });
  const page = await context.newPage();
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  await page.route('**/*', route => {
    const url = new URL(route.request().url());
    if (url.origin !== new URL(base).origin || url.pathname.includes('/apks/')) return route.abort();
    return route.continue();
  });
  try { await run(page, context); assert.deepEqual(errors, [], 'no script errors'); console.log('PASS: ' + name); passed++; }
  finally { await context.close(); }
}
const count = page => page.locator('article.card:visible').count();
try {
  await test('all 14 cards and real publication dates; default Both', async page => {
    await page.goto(base);
    assert.equal(await page.locator('#site-language').inputValue(), 'both');
    assert.equal(await count(page), 14);
    assert.equal(await page.locator('time[data-release-tag]').count(), 14);
    assert.equal(await page.locator('.trust-ticker').count(), 1);
    assert.equal(await page.locator('.release-brief').count(), 14);
    assert.equal(await page.locator('a[data-apk][aria-label]').count(), 28);
    assert.equal(await page.locator('[data-copy-sha]').count(), 14);
  });
  await test('filters and table agree, and jump reveals/focuses a hidden app', async page => {
    await page.goto(base);
    for (const [category, expected] of [['security',3],['media',4],['productivity',3],['utility',4]]) {
      await page.locator(`[data-category-filter="${category}"]`).click();
      assert.equal(await count(page), expected);
      assert.equal(await page.locator('.latest tbody tr:visible').count(), expected);
      assert.equal(await page.locator(`[data-category-filter="${category}"]`).getAttribute('aria-pressed'), 'true');
    }
    await page.selectOption('#jump-app', 'app-live-guard');
    assert.equal(await count(page), 14);
    await page.waitForURL('**/#app-live-guard');
    assert.equal(await page.evaluate(() => document.activeElement.id), 'app-live-guard');
  });
  await test('Tamil/English paired safety guidance and local preference', async page => {
    await page.goto(base);
    for (const lang of ['ta','en']) {
      await page.selectOption('#site-language', lang);
      assert.equal(await page.locator('html').getAttribute('lang'), lang);
      assert.equal(await page.locator(`#install-help p[data-content-lang="${lang}"]:visible`).count(), 1);
      assert.ok(await page.locator(`.migration-notice p[data-content-lang="${lang}"]:visible`).count() >= 1);
      assert.equal(await page.locator(`[data-content-lang="${lang === 'ta' ? 'en' : 'ta'}"]:visible`).count(), 0);
      await page.reload();
      assert.equal(await page.locator('#site-language').inputValue(), lang);
    }
  });
  await test('copy SHA succeeds with exact published value and live feedback', async page => {
    await page.addInitScript(() => Object.defineProperty(navigator, 'clipboard', { value: { writeText: async value => { window.__copied = value; } } }));
    await page.goto(base);
    const button = page.locator('[data-copy-sha]').first();
    await button.locator('xpath=ancestor::details').evaluate(node => { node.open = true; });
    const expected = await button.getAttribute('data-copy-sha');
    await button.click();
    assert.equal(await page.evaluate(() => window.__copied), expected);
    await page.waitForFunction(() => document.querySelector('.copy-feedback').textContent.includes('Copied'));
  });
  await test('clipboard denial offers manual selection, never false success', async page => {
    await page.addInitScript(() => Object.defineProperty(navigator, 'clipboard', { value: { writeText: async () => { throw Error('denied'); } } }));
    await page.goto(base);
    const button = page.locator('[data-copy-sha]').first();
    await button.locator('xpath=ancestor::details').evaluate(node => { node.open = true; });
    await button.click();
    await page.waitForSelector('.manual-hash');
    assert.equal(await page.locator('.manual-hash').inputValue(), await button.getAttribute('data-copy-sha'));
    assert.equal(await page.locator('.manual-hash').getAttribute('readonly'), '');
    assert.match(await page.locator('.copy-feedback').first().textContent(), /unavailable/);
  });
  await test('synthetic wrong hash blocks download and never redirects', async page => {
    await page.goto(base);
    await page.route('**/apks/*.apk', route => route.fulfill({ body: 'synthetic-corrupt-fixture' }));
    let downloads = 0; page.on('download', () => downloads++);
    await page.locator('.latest a[data-apk]').first().click();
    await page.waitForFunction(() => document.getElementById('st-latest').textContent.includes('File hash mismatch'));
    assert.equal(downloads, 0);
    assert.equal(new URL(page.url()).origin, new URL(base).origin);
  });
  await test('synthetic matching hash downloads only after verification', async page => {
    await page.goto(base);
    const fixture = 'synthetic-not-an-apk';
    const hash = createHash('sha256').update(fixture).digest('hex');
    await page.route('**/apks/*.apk', route => route.fulfill({ body: fixture }));
    const link = page.locator('.latest a[data-apk]').first();
    await link.evaluate((node, value) => { node.dataset.sha = value; }, hash);
    await link.click();
    await page.waitForFunction(() => document.getElementById('st-latest').textContent.includes('not security approval'));
  });
  await test('320/390/768/1280 widths and 200% scale have no page overflow', async page => {
    await page.goto(base);
    for (const width of [320,390,768,1280]) {
      await page.setViewportSize({ width, height: 900 });
      for (const lang of ['both','ta','en']) {
        await page.selectOption('#site-language', lang);
        assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), true, `${width}/${lang}`);
      }
    }
    await page.setViewportSize({ width: 640, height: 900 });
    await page.evaluate(() => { document.documentElement.style.fontSize = '200%'; });
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), true);
    await page.screenshot({ path: '/tmp/rp-website-mobile.png', fullPage: false });
  });
  await test('ticker pause and reduced-motion are retained', async page => {
    await page.emulateMedia({ reducedMotion: 'reduce' });
    await page.goto(base);
    assert.equal(await page.locator('.trust-ticker__track').evaluate(node => getComputedStyle(node).animationName), 'none');
    await page.emulateMedia({ reducedMotion: 'no-preference' });
    await page.locator('[data-trust-pause]').click();
    assert.equal(await page.locator('[data-trust-pause]').getAttribute('aria-pressed'), 'true');
  });
  await test('keyboard controls and direct anchors work', async page => {
    await page.goto(base + '/#app-live-guard');
    assert.equal(await page.locator('#app-live-guard').isVisible(), true);
    await page.locator('[data-category-filter="all"]').focus();
    await page.keyboard.press('Tab'); await page.keyboard.press('Enter');
    assert.equal(await count(page), 3);
    assert.notEqual(await page.locator('[data-category-filter="security"]').evaluate(node => getComputedStyle(node).outlineStyle), 'none');
  });
  await test('without JavaScript all releases, warnings and anchors remain readable', async page => {
    await page.goto(base);
    assert.equal(await count(page), 14);
    assert.equal(await page.locator('.site-tools').isVisible(), false);
    assert.equal(await page.locator('.migration-notice p:visible').count(), 3);
    assert.equal(await page.locator('.latest tbody tr').count(), 14);
    await page.locator('a[href="#app-live-guard"]').first().click();
    await page.waitForURL('**/#app-live-guard');
    assert.equal(await page.locator('#app-live-guard').isVisible(), true);
  }, { javaScriptEnabled: false });
  await test('automated accessibility checks in Both, English and Tamil, including expanded evidence', async page => {
    await page.goto(base);
    await page.evaluate(() => document.querySelectorAll('details:not(.gallery)').forEach(node => { node.open = true; }));
    for (const lang of ['both', 'en', 'ta']) {
      await page.selectOption('#site-language', lang);
      const result = await new AxeBuilder({ page }).withTags(['wcag2a','wcag2aa','wcag21aa','wcag22aa']).analyze();
      assert.deepEqual(result.violations.map(item => ({ id: item.id, nodes: item.nodes.map(node => node.html) })), []);
    }
  });
  console.log(`${passed} browser checks passed.`);
} finally { await browser.close(); }
