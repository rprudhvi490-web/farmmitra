const fs = require('fs');
const path = require('path');

// Target path where the production environment file lives
const targetPath = path.join(__dirname, './src/environments/environment.prod.ts');

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

// Write the file to disk right before building the Angular app
fs.writeFileSync(targetPath, envConfigFile, 'utf8');
console.log('Angular environment.prod.ts generated dynamically.');