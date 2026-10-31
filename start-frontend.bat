@echo off
cd /d "%~dp0frontend"
npx pnpm dev --host 0.0.0.0 --port 3000
pause

