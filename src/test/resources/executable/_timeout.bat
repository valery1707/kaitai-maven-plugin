@echo off

REM Return exit code 1 if used
REM timeout 1 /NOBREAK

ping 127.0.0.1 -n 2 > nul
