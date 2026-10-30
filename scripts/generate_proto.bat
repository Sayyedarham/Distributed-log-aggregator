@echo off
REM =============================================================================
REM Distributed Log Aggregator — Protobuf Code Generation (Windows)
REM =============================================================================
REM Compiles .proto files into Python and Java source code.
REM
REM Prerequisites:
REM   - protoc (Protocol Buffers compiler) installed and on PATH
REM   - grpcio-tools Python package (pip install grpcio-tools)
REM
REM Usage:
REM   scripts\generate_proto.bat
REM =============================================================================

setlocal enabledelayedexpansion

REM ---------------------------------------------------------------------------
REM Configuration
REM ---------------------------------------------------------------------------
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%.."
set "PROTO_DIR=%PROJECT_ROOT%\proto"
set "PROTO_FILE=%PROTO_DIR%\metrics.proto"

REM Output directories
set "PYTHON_OUT=%PROJECT_ROOT%\edge-agent\src\generated"
set "JAVA_OUT=%PROJECT_ROOT%\ingestion-gateway\src\main\java"

REM ---------------------------------------------------------------------------
REM Validation
REM ---------------------------------------------------------------------------
echo [proto] Validating environment...

where protoc >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] 'protoc' is not installed or not on PATH.
    echo         Install from: https://github.com/protocolbuffers/protobuf/releases
    exit /b 1
)

if not exist "%PROTO_FILE%" (
    echo [ERROR] Proto file not found: %PROTO_FILE%
    exit /b 1
)

for /f "tokens=*" %%v in ('protoc --version 2^>^&1') do echo [proto] Using %%v

REM ---------------------------------------------------------------------------
REM Python Code Generation
REM ---------------------------------------------------------------------------
echo [proto] Generating Python code...

if not exist "%PYTHON_OUT%" mkdir "%PYTHON_OUT%"

python -m grpc_tools.protoc ^
    --proto_path="%PROTO_DIR%" ^
    --python_out="%PYTHON_OUT%" ^
    --pyi_out="%PYTHON_OUT%" ^
    --grpc_python_out="%PYTHON_OUT%" ^
    "%PROTO_FILE%"

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Python protobuf generation failed.
    exit /b 1
)

REM Create __init__.py so the generated module is importable
if not exist "%PYTHON_OUT%\__init__.py" (
    type nul > "%PYTHON_OUT%\__init__.py"
)

echo [proto] Python code generated in: %PYTHON_OUT%

REM ---------------------------------------------------------------------------
REM Java Code Generation
REM ---------------------------------------------------------------------------
echo [proto] Generating Java code...

if not exist "%JAVA_OUT%" mkdir "%JAVA_OUT%"

protoc ^
    --proto_path="%PROTO_DIR%" ^
    --java_out="%JAVA_OUT%" ^
    "%PROTO_FILE%"

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Java protobuf generation failed.
    exit /b 1
)

echo [proto] Java code generated in: %JAVA_OUT%

REM ---------------------------------------------------------------------------
REM Summary
REM ---------------------------------------------------------------------------
echo.
echo [proto] Code generation complete!
echo [proto]    Python : %PYTHON_OUT%
echo [proto]    Java   : %JAVA_OUT%
echo.
echo [proto] Generated Python files:
dir /b /s "%PYTHON_OUT%\*.py" 2>nul
echo.
echo [proto] Generated Java files:
dir /b /s "%JAVA_OUT%\*MetricsProto*.java" 2>nul

endlocal
