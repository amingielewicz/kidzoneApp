import { readdirSync, readFileSync } from 'node:fs';
import { extname, join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';

const rootDir = join(fileURLToPath(new URL('..', import.meta.url)), 'src');
const sourceExtensions = new Set(['.ts', '.tsx']);
const iconButtonPattern = /<IconButton\b[^>]*>/gs;
const accessibleNamePattern = /\baria-(label|labelledby)=/;

function walk(dir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) {
      return walk(path);
    }
    return sourceExtensions.has(extname(entry.name)) ? [path] : [];
  });
}

function lineNumberFor(content, index) {
  return content.slice(0, index).split('\n').length;
}

const failures = [];

for (const file of walk(rootDir)) {
  const content = readFileSync(file, 'utf8');
  for (const match of content.matchAll(iconButtonPattern)) {
    const tag = match[0];
    if (!accessibleNamePattern.test(tag)) {
      failures.push(`${relative(rootDir, file)}:${lineNumberFor(content, match.index)}`);
    }
  }
}

if (failures.length > 0) {
  console.error('IconButton elements without aria-label or aria-labelledby:');
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log('All IconButton elements expose an accessible name.');
