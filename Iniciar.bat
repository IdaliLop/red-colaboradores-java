@echo off
setlocal
cd /d "%~dp0"

where javac >nul 2>nul
if errorlevel 1 (
    echo No se encontro javac. Instala un JDK 17 o posterior y vuelve a intentar.
    pause
    exit /b 1
)

if not exist "build\classes" mkdir "build\classes"

javac --release 17 -encoding UTF-8 -d "build\classes" ^
    "src\main\java\mx\edu\estructuras\red\Graph.java" ^
    "src\main\java\mx\edu\estructuras\red\GraphAnalyzer.java" ^
    "src\main\java\mx\edu\estructuras\red\CsvGraphReader.java" ^
    "src\main\java\mx\edu\estructuras\red\AnalysisReport.java" ^
    "src\main\java\mx\edu\estructuras\red\Main.java" ^
    "src\main\java\mx\edu\estructuras\red\GraphDataReader.java" ^
    "src\main\java\mx\edu\estructuras\red\GraphSessionFile.java" ^
    "src\main\java\mx\edu\estructuras\red\GraphStudio.java"
if errorlevel 1 (
    echo.
    echo No se pudo compilar. Revisa los mensajes anteriores.
    pause
    exit /b 1
)

java -cp "build\classes" mx.edu.estructuras.red.GraphStudio
if errorlevel 1 (
    echo.
    echo La aplicacion termino con un error.
    pause
    exit /b 1
)
