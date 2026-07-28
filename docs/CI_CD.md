# CI/CD — Travel House Appium

## What runs in CI
GitHub Actions workflow: `.github/workflows/android-tests.yml`

| Trigger | Behaviour |
|--------|-----------|
| **workflow_dispatch** | Manual run — pick suite (`dual-route`, `smoke`, `regression`, …) |
| **push** to `main` / `feature/login-flight-search` | Auto-run when tests/pom/scripts/workflow change |
| **pull_request** to `main` | Auto-run on PR |

Default suite on this branch: **dual-route** (`dual-route-booking.xml`).

## Self-hosted runner (required)
Appium needs your physical phone, so use a **self-hosted** Windows runner on the machine with:

1. JDK 17, Maven, Node.js  
2. `npm i -g appium` + `appium driver install uiautomator2`  
3. Android `adb` + authorized device (`adb devices`)  
4. GitHub Actions runner installed and online  

Docs: https://docs.github.com/en/actions/hosting-your-own-runners

## Secrets
Repo → **Settings → Secrets and variables → Actions**:

| Secret | Purpose |
|--------|---------|
| `LOGIN_EMAIL` | Test account email |
| `LOGIN_PASSWORD` | Test account password |
| `DEVICE_UDID` | Optional device serial |
| `APPIUM_SERVER_URL` | Optional (default `http://127.0.0.1:4723`) |

These override `config.properties` via `ConfigReader` / `scripts/ci-run.ps1`.

## Local CI-style run
```powershell
cd E:\travelHouseAppium
$env:LOGIN_EMAIL = "your@email.com"
$env:LOGIN_PASSWORD = "your-password"
.\scripts\ci-run.ps1 -SuiteXml "src/test/resources/dual-route-booking.xml"
```

## Artifacts
Each workflow uploads:

- `reports/` (App Behaviour Pass/Fail HTML)
- `screenshots/`
- `target/surefire-reports/`
