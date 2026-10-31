@echo off
set "JAVA_HOME=%USERPROFILE%\.jdks\jdk-25.0.2"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "PORT=8081"
set "DB_HOST=localhost"
set "DB_PORT=3306"
set "DB_NAME=taskflow"
set "DB_USERNAME=root"
set "DB_PASSWORD=Pranav*8080"
set "SPRING_PROFILES_ACTIVE=dev"
set "TASKFLOW_COOKIE_SECURE=false"
set "TASKFLOW_CORS_ALLOWED_ORIGINS=http://localhost:3000"
set "TASKFLOW_SEED_DEMO=true"
set "TASKFLOW_DEMO_PASSWORD=TaskFlowDemo2026!"

"%JAVA_HOME%\bin\java.exe" -jar "%~dp0backend\target\taskflow-backend-1.0.0.jar"
pause

