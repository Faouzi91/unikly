const fs = require('node:fs');
const path = require('node:path');

const envFile = path.resolve(__dirname, '../.env');
const fromShell = process.env.API_HOST_PORT;
const envText = fs.existsSync(envFile) ? fs.readFileSync(envFile, 'utf8') : '';
const fromFile = envText.match(/^\s*API_HOST_PORT\s*=\s*["']?(\d+)["']?\s*(?:#.*)?$/m)?.[1];
const apiPort = fromShell || fromFile || '8080';

if (!/^\d+$/.test(apiPort) || Number(apiPort) < 1 || Number(apiPort) > 65535) {
  throw new Error('API_HOST_PORT must be a valid TCP port number.');
}

module.exports = {
  '/api/**': {
    target: `http://127.0.0.1:${apiPort}`,
    secure: false,
    changeOrigin: false,
    logLevel: 'warn',
  },
};
