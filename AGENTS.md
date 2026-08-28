# Codex Project Notes

- Before running PowerShell commands that may print Korean text, set UTF-8 output in that shell:

```powershell
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
```

- API Gradle project lives in `apps/api`.
- Use the Gradle Wrapper for API verification. On Windows:

```powershell
$env:JAVA_HOME = (Resolve-Path utils\bin\jdk-21).Path
Push-Location apps\api
.\gradlew.bat test
Pop-Location
```

- Do not call a globally installed `gradle` for normal API checks.
- `utils/bin/jdk-21` is the project-local JDK.
- `utils/bin/gradle-8.10.2` is only a bootstrap tool for regenerating the wrapper.
- When committing, use a Korean commit message and always prefix the subject with a conventional type such as `fix:` or `feat:`.
- Web verification command:

```powershell
npm run build --workspace apps/web
```
