@echo off
start "TaskFlow Backend" cmd /k "%~dp0start-backend.bat"
start "TaskFlow Frontend" cmd /k "%~dp0start-frontend.bat"

