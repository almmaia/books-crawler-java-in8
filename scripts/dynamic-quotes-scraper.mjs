import fs from "node:fs/promises";
import path from "node:path";
import { chromium } from "playwright";

const outputDir = path.resolve("output-dynamic");
const timestamp = new Date().toISOString().replace(/[-:]/g, "").replace(/\..+/, "").replace("T", "-");
const jsonPath = path.join(outputDir, `quotes-${timestamp}.json`);
const csvPath = path.join(outputDir, `quotes-${timestamp}.csv`);

await fs.mkdir(outputDir, { recursive: true });

const browser = await chromium.launch({ headless: true });
const context = await browser.newContext({
  userAgent: "books-crawler-java-dynamic-bonus/1.0"
});
const page = await context.newPage();

await page.goto("https://quotes.toscrape.com/js/", { waitUntil: "networkidle", timeout: 60000 });

const quotes = await page.locator(".quote").evaluateAll((nodes) =>
  nodes.map((quote) => {
    const text = quote.querySelector(".text")?.textContent?.trim() ?? "";
    const author = quote.querySelector(".author")?.textContent?.trim() ?? "";
    const tags = Array.from(quote.querySelectorAll(".tags .tag")).map((tag) => tag.textContent?.trim() ?? "");
    return { text, author, tags };
  })
);

await fs.writeFile(jsonPath, JSON.stringify(quotes, null, 2), "utf8");

const csvHeader = "text,author,tags\n";
const csvBody = quotes
  .map((quote) => {
    const values = [quote.text, quote.author, quote.tags.join(" | ")];
    return values
      .map((value) => `"${String(value).replaceAll("\"", "\"\"")}"`)
      .join(",");
  })
  .join("\n");

await fs.writeFile(csvPath, csvHeader + csvBody + "\n", "utf8");

console.log(`Dynamic scrape completed successfully.`);
console.log(`Quotes collected: ${quotes.length}`);
console.log(`JSON output: ${jsonPath}`);
console.log(`CSV output: ${csvPath}`);

await browser.close();
