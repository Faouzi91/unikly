import { readdir, readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const appRoot = path.resolve(scriptDirectory, '../src/app');
const featureRoot = path.join(appRoot, 'features');
const failures = [];

async function typescriptFiles(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const nested = await Promise.all(entries.map(async (entry) => {
    const fullPath = path.join(directory, entry.name);
    if (entry.isDirectory()) return typescriptFiles(fullPath);
    return entry.isFile() && entry.name.endsWith('.ts') && !entry.name.endsWith('.spec.ts')
      ? [fullPath]
      : [];
  }));
  return nested.flat();
}

function importedSpecifiers(source) {
  const pattern = /(?:\bfrom\s*|\bimport\s*\(\s*)['"]([^'"]+)['"]/g;
  return [...source.matchAll(pattern)].map((match) => match[1]);
}

function featureFromPath(filePath) {
  const relative = path.relative(featureRoot, filePath);
  const parts = relative.split(path.sep);
  return parts.length > 1 ? parts[0] : null;
}

function aliasTarget(specifier) {
  if (!specifier.startsWith('@features/')) return null;
  return specifier.slice('@features/'.length).split('/')[0];
}

function isPublicEntry(specifier) {
  return specifier.endsWith('/public-api');
}

for (const filePath of await typescriptFiles(appRoot)) {
  const source = await readFile(filePath, 'utf8');
  const sourceFeature = featureFromPath(filePath);

  for (const specifier of importedSpecifiers(source)) {
    const targetFeature = aliasTarget(specifier);
    if (targetFeature && (!sourceFeature || targetFeature !== sourceFeature) && !isPublicEntry(specifier)) {
      failures.push(`${path.relative(appRoot, filePath)} imports feature internals via ${specifier}`);
      continue;
    }

    if (!specifier.startsWith('.')) continue;
    const resolved = path.resolve(path.dirname(filePath), specifier);
    const relativeToFeatures = path.relative(featureRoot, resolved);
    if (relativeToFeatures.startsWith('..') || path.isAbsolute(relativeToFeatures)) continue;
    const targetParts = relativeToFeatures.split(path.sep);
    const targetRelativePath = targetParts.slice(1).join('/');
    const relativeTargetFeature = targetParts[0];
    if ((!sourceFeature || relativeTargetFeature !== sourceFeature) && !targetRelativePath.startsWith('public-api')) {
      failures.push(`${path.relative(appRoot, filePath)} imports another feature's internals via ${specifier}`);
    }
  }
}

if (failures.length > 0) {
  console.error('Angular feature-boundary violations:');
  for (const failure of failures) console.error(`- ${failure}`);
  process.exitCode = 1;
} else {
  console.log('Angular feature boundaries are valid.');
}
