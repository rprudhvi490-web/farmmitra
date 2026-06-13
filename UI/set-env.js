const fs = require('fs');
const path = require('path');

// Target paths for BOTH files Angular expects to see
const devPath = path.join(__dirname, './src/environments/environment.ts');
const prodPath = path.join(__dirname, './src/environments/environment.prod.ts');

// Ensure the directory exists (just in case)
const dir = path.join(__dirname, './src/environments');
if (!fs.existsSync(dir)){
    fs.mkdirSync(dir, { recursive: true });
}

// Environment file blueprint reading straight from Netlify's system environment variables
const envConfigFile = `export const environment = {
  production: true,
  firebaseConfig: {
    apiKey: '${process.env.FIREBASE_API_KEY || ""}',
    authDomain: '${process.env.FIREBASE_AUTH_DOMAIN || ""}',
    projectId: '${process.env.FIREBASE_PROJECT_ID || ""}',
    storageBucket: '${process.env.FIREBASE_STORAGE_BUCKET || ""}',
    messagingSenderId: '${process.env.FIREBASE_MESSAGING_SENDER_ID || ""}',
    appId: '${process.env.FIREBASE_APP_ID || ""}'
  }
};
`;

// Write to BOTH locations so the build handles cross-references flawlessly
fs.writeFileSync(devPath, envConfigFile, 'utf8');
fs.writeFileSync(prodPath, envConfigFile, 'utf8');

console.log('Angular environment files generated dynamically for both dev and prod targets.');