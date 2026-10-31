@echo off
setlocal
if not defined JAVA_HOME (
    if exist "%USERPROFILE%\.jdks\jdk-25.0.2" (
        set "JAVA_HOME=%USERPROFILE%\.jdks\jdk-25.0.2"
    )
)
if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)
set "MVN_EXEC=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.16\0daed3be3ebd1c706f0e69e8b07c6b73f5cc4ea3dfce72a8d0ec2e849ca2ddb0\bin\mvn.cmd"
if exist "%MVN_EXEC%" (
    call "%MVN_EXEC%" %*
) else (
    call mvn %*
)

