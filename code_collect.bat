@echo off
setlocal enabledelayedexpansion

:: 1. Create the output directory
set "OUTPUT_DIR=collected_services"
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

echo Starting code collection with file counting...

:: 2. Loop through all directories (services)
for /d %%S in (*) do (
    set "SERVICE_NAME=%%S"
    
    :: Skip the output directory
    if /i "!SERVICE_NAME!" neq "%OUTPUT_DIR%" (
        
        :: Check if it's a Spring Boot service (contains src folder)
        if exist "%%S\src" (
            echo Processing service: !SERVICE_NAME!
            set "FILE_PATH=%cd%\%OUTPUT_DIR%\!SERVICE_NAME!.txt"
            
            :: --- Part A: Count Total Files First ---
            set /a FILE_COUNT=0
            
            :: Count root files
            for %%F in (pom.xml Dockerfile docker-compose.yml docker-compose.yaml) do (
                if exist "%%S\%%F" set /a FILE_COUNT+=1
            )
            :: Count resources
            if exist "%%S\src\main\resources" (
                for /f "delims=" %%C in ('dir /b "%%S\src\main\resources\application.*" 2^>nul') do set /a FILE_COUNT+=1
            )
            :: Count Java files
            if exist "%%S\src\main\java" (
                for /f "delims=" %%J in ('dir /s /b "%%S\src\main\java\*.java" 2^>nul') do set /a FILE_COUNT+=1
            )

            :: --- Part B: Initialize File with Count ---
            echo ======================================== > "!FILE_PATH!"
            echo SERVICE: !SERVICE_NAME! >> "!FILE_PATH!"
            echo TOTAL FILES COLLECTED: !FILE_COUNT! >> "!FILE_PATH!"
            echo ======================================== >> "!FILE_PATH!"
            echo. >> "!FILE_PATH!"

            :: --- Part C: Append Content (Root Files) ---
            for %%F in (pom.xml Dockerfile docker-compose.yml docker-compose.yaml) do (
                if exist "%%S\%%F" (
                    echo **%%F** >> "!FILE_PATH!"
                    type "%%S\%%F" >> "!FILE_PATH!"
                    echo. >> "!FILE_PATH!"
                    echo. >> "!FILE_PATH!"
                )
            )

            :: --- Part D: Append Content (Config Files) ---
            if exist "%%S\src\main\resources" (
                for /f "delims=" %%C in ('dir /b "%%S\src\main\resources\application.*" 2^>nul') do (
                    echo **%%C** >> "!FILE_PATH!"
                    type "%%S\src\main\resources\%%C" >> "!FILE_PATH!"
                    echo. >> "!FILE_PATH!"
                    echo. >> "!FILE_PATH!"
                )
            )

            :: --- Part E: Append Content (Java Files) ---
            if exist "%%S\src\main\java" (
                for /f "delims=" %%J in ('dir /s /b "%%S\src\main\java\*.java" 2^>nul') do (
                    set "FULL_PATH=%%J"
                    set "REL_PATH=!FULL_PATH:*src\main\java\=!"
                    
                    echo **!REL_PATH!** >> "!FILE_PATH!"
                    type "%%J" >> "!FILE_PATH!"
                    echo. >> "!FILE_PATH!"
                    echo. >> "!FILE_PATH!"
                )
            )
        )
    )
)

echo.
echo Done! Files are ready in the '%OUTPUT_DIR%' folder.
pause