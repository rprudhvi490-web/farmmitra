const fs = require('fs');
const path = require('path');

// Target paths for BOTH files Angular expects to see
const devPath = path.join(__dirname, './src/environments/environment.ts');
const prodPath = path.join(__dirname, './src/environments/environment.prod.ts');

// Ensure the directory exists
const dir = path.join(__dirname, './src/environments');
if (!fs.existsSync(dir)){
    fs.mkdirSync(dir, { recursive: true });
}

// Match your EXACT application structure perfectly
const envConfigFile = `export const environment = {
  production: true,
  apiBaseUrl: 'https://farmmitra-backend.onrender.com/weekendbasket/api',
  notificationDurationMs: 7000,
  notificationErrorDurationMs: 12000,
  firebase: {
    apiKey: '${process.env.FIREBASE_API_KEY || ""}',
    authDomain: '${process.env.FIREBASE_AUTH_DOMAIN || ""}',
    projectId: '${process.env.FIREBASE_PROJECT_ID || ""}',
    storageBucket: '${process.env.FIREBASE_STORAGE_BUCKET || ""}',
    messagingSenderId: '${process.env.FIREBASE_MESSAGING_SENDER_ID || ""}',
    appId: '${process.env.FIREBASE_APP_ID || ""}'
  }
};
`;

// Write to both locations so the compiler finds all expected fields
fs.writeFileSync(devPath, envConfigFile, 'utf8');
fs.writeFileSync(prodPath, envConfigFile, 'utf8');

console.log('Angular environment targets cleanly generated with complete API parameters.');