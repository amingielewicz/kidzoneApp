import { copyFile, mkdir } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const source = resolve(root, 'app/src/main/res/drawable-nodpi/ic_splash_screen_icon.png');
const destinationDir = resolve(root, 'public/assets');
const destination = resolve(destinationDir, 'kidzone-logo.png');

await mkdir(destinationDir, { recursive: true });
await copyFile(source, destination);
console.log(`Copied kidZone logo to ${destination}`);
