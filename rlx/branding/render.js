// RLX Patches
// https://github.com/Ramik4/rlx-patches
//
// Renders an SVG to a PNG of the given size with Playwright's Chromium:
//   node rlx/branding/render.js in.svg out.png WIDTH HEIGHT
const { chromium } = require('playwright');
const fs = require('fs');

(async () => {
    const [input, output, width, height] = process.argv.slice(2);
    const browser = await chromium.launch();
    const page = await browser.newPage({ viewport: { width: +width, height: +height }, deviceScaleFactor: 1 });
    const svg = fs.readFileSync(input, 'utf8');
    await page.setContent(`<html><body style="margin:0">${svg}</body></html>`);
    await page.locator('svg').screenshot({ path: output, omitBackground: true });
    await browser.close();
})();
