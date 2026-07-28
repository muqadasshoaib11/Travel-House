package com.travelhouse.utils;

import org.testng.ITestResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * HTML summary with expected app behaviour + pass/fail for each case.
 */
public final class HtmlSummaryReport {

    private static final List<Row> ROWS = new CopyOnWriteArrayList<>();
    private static String suiteName = "Travel House Dual Route Booking";

    private static final Map<String, String> APP_BEHAVIOUR = new LinkedHashMap<>();

    static {
        APP_BEHAVIOUR.put("step01_loginOrHome",
                "App launches. If logged out, Login screen appears → user enters saved credentials → "
                        + "notifications Allowed, biometric Cancelled → Home screen. "
                        + "If already logged in, app opens directly on Home.");
        APP_BEHAVIOUR.put("step02_verifyHomeScreenContent",
                "Home shows flight search (One Way, Return, Flying From, Going to, Departure, Search Flight), "
                        + "bottom tabs, Most travelled destinations, and Your Upcoming Flights. "
                        + "Scrolling up/down keeps all sections visible with no missing/empty content.");
        APP_BEHAVIOUR.put("step03_returnLondonJeddah_bookToMyTravellers",
                "User selects Return, London → Jeddah, Search Flight. Results load with Cheapest/Fastest. "
                        + "Scrolling results shows complete cards (route, times, duration, price). "
                        + "User books (Cheapest) → Proceed with payment → Price Summary / T&C → Continue → "
                        + "My Travellers screen. Contact email uses Sign-In email.");
        APP_BEHAVIOUR.put("step04_backToHomeAfterReturn",
                "After the Return booking path, user navigates back to Home. "
                        + "Flight search form is ready again for the One Way journey.");
        APP_BEHAVIOUR.put("step05_oneWayLondonKarachi_bookToMyTravellers",
                "User selects One Way, London → Karachi, Search Flight. Results validated top-to-bottom "
                        + "(no empty fields). Book Cheapest → Price Summary → My Travellers "
                        + "(same booking process as Return).");

        // Legacy / search-only names (if present)
        APP_BEHAVIOUR.put("step01_login",
                "Login with configured email/password; allow notifications; cancel biometric; reach Home.");
        APP_BEHAVIOUR.put("step02_verifyHomePageContents",
                "Scroll Home and verify destinations, upcoming flights, and search form content.");
        APP_BEHAVIOUR.put("step03_returnSearch_londonToJeddah",
                "Return search London → Jeddah; scroll and validate all result listings.");
        APP_BEHAVIOUR.put("step04_oneWaySearch_londonToKarachi",
                "One Way search London → Karachi; scroll and validate all result listings.");
    }

    private HtmlSummaryReport() {
    }

    public static void setSuiteName(String name) {
        if (name != null && !name.isBlank()) {
            suiteName = name.trim();
        }
    }

    public static void record(ITestResult result) {
        String method = result.getMethod().getMethodName();
        String feature = result.getMethod().getDescription();
        if (feature == null || feature.isBlank()) {
            feature = method;
        }
        String behaviour = APP_BEHAVIOUR.getOrDefault(method,
                "Expected app flow for: " + feature);

        String status;
        String reason = "—";
        switch (result.getStatus()) {
            case ITestResult.SUCCESS -> {
                status = "PASSED";
                reason = "App behaviour matched expected flow.";
            }
            case ITestResult.FAILURE -> {
                status = "FAILED";
                reason = result.getThrowable() != null
                        ? safe(result.getThrowable().getMessage())
                        : "Unknown failure";
            }
            case ITestResult.SKIP -> {
                status = "SKIPPED";
                reason = result.getThrowable() != null
                        ? safe(result.getThrowable().getMessage())
                        : "Skipped due to dependency or prior failure";
            }
            default -> status = "UNKNOWN";
        }
        long durationMs = result.getEndMillis() - result.getStartMillis();
        ROWS.add(new Row(feature, method, behaviour, status, durationMs, reason));
    }

    public static Path write(Path reportDir) {
        try {
            Files.createDirectories(reportDir);
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path out = reportDir.resolve("TravelHouse_Summary_" + stamp + ".html");
            Path latest = reportDir.resolve("TravelHouse_FlightBooking_Summary.html");
            Path behaviourReport = reportDir.resolve("TravelHouse_AppBehaviour_PassFail_Report.html");

            int passed = 0, failed = 0, skipped = 0;
            for (Row row : ROWS) {
                switch (row.status) {
                    case "PASSED" -> passed++;
                    case "FAILED" -> failed++;
                    case "SKIPPED" -> skipped++;
                    default -> {
                    }
                }
            }
            int total = ROWS.size();
            String generated = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss"));

            StringBuilder rowsHtml = new StringBuilder();
            int i = 1;
            for (Row row : ROWS) {
                String badgeClass = switch (row.status) {
                    case "PASSED" -> "pass";
                    case "FAILED" -> "fail";
                    case "SKIPPED" -> "skip";
                    default -> "skip";
                };
                String reasonClass = "PASSED".equals(row.status) ? "ok-note" : "reason";
                rowsHtml.append("<tr>")
                        .append("<td>").append(i++).append("</td>")
                        .append("<td><div class=\"feature\">").append(escape(row.feature)).append("</div>")
                        .append("<div class=\"method\">").append(escape(row.method)).append("</div></td>")
                        .append("<td><div class=\"behaviour\">").append(escape(row.behaviour)).append("</div></td>")
                        .append("<td><span class=\"badge ").append(badgeClass).append("\">")
                        .append(row.status).append("</span></td>")
                        .append("<td>").append(String.format("%.1fs", row.durationMs / 1000.0)).append("</td>")
                        .append("<td><div class=\"").append(reasonClass).append("\">")
                        .append(escape(row.reason)).append("</div></td>")
                        .append("</tr>\n");
            }

            String html = TEMPLATE
                    .replace("{{GENERATED}}", escape(generated))
                    .replace("{{SUITE}}", escape(suiteName))
                    .replace("{{TOTAL}}", String.valueOf(total))
                    .replace("{{PASSED}}", String.valueOf(passed))
                    .replace("{{FAILED}}", String.valueOf(failed))
                    .replace("{{SKIPPED}}", String.valueOf(skipped))
                    .replace("{{ROWS}}", rowsHtml.toString());

            Files.writeString(out, html, StandardCharsets.UTF_8);
            Files.writeString(latest, html, StandardCharsets.UTF_8);
            Files.writeString(behaviourReport, html, StandardCharsets.UTF_8);
            System.out.println("[REPORT] HTML summary: " + latest.toAbsolutePath());
            System.out.println("[REPORT] App behaviour report: " + behaviourReport.toAbsolutePath());
            return behaviourReport;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write HTML summary report", e);
        } finally {
            ROWS.clear();
        }
    }

    /** Write a static report when a live TestNG run is not available. */
    public static Path writeStatic(Path reportDir, List<Row> rows, String suite) {
        ROWS.clear();
        ROWS.addAll(rows);
        setSuiteName(suite);
        return write(reportDir);
    }

    public static Row row(String feature, String method, String status, long durationMs, String reason) {
        String behaviour = APP_BEHAVIOUR.getOrDefault(method, "Expected app flow for: " + feature);
        if ("PASSED".equals(status) && (reason == null || reason.isBlank() || "—".equals(reason))) {
            reason = "App behaviour matched expected flow.";
        }
        return new Row(feature, method, behaviour, status, durationMs, reason == null ? "—" : reason);
    }

    private static String safe(String message) {
        if (message == null || message.isBlank()) {
            return "No failure message provided";
        }
        String trimmed = message.trim();
        return trimmed.length() > 500 ? trimmed.substring(0, 500) + "…" : trimmed;
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public record Row(String feature, String method, String behaviour, String status,
                      long durationMs, String reason) {
    }

    private static final String TEMPLATE = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1" />
              <title>Travel House — App Behaviour Pass/Fail Report</title>
              <style>
                :root {
                  --bg: #f4f6f8; --card: #ffffff; --ink: #1a2332; --muted: #5b6b7c;
                  --pass: #1b7f4e; --pass-bg: #e8f7ef; --fail: #b42318; --fail-bg: #fdeceb;
                  --skip: #8a6d3b; --skip-bg: #fff6e5; --line: #e2e8f0; --accent: #0b6e99;
                  --behaviour-bg: #f0f7fb;
                }
                * { box-sizing: border-box; }
                body { margin: 0; font-family: "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
                  background: linear-gradient(180deg, #eaf3f8 0%, var(--bg) 220px); color: var(--ink); line-height: 1.45; }
                .wrap { max-width: 1200px; margin: 0 auto; padding: 32px 20px 48px; }
                header { background: var(--card); border: 1px solid var(--line); border-radius: 14px;
                  padding: 28px; box-shadow: 0 8px 24px rgba(26, 35, 50, 0.06); }
                h1 { margin: 0 0 6px; font-size: 1.55rem; }
                .subtitle { color: var(--muted); margin: 0; }
                .meta { display: flex; flex-wrap: wrap; gap: 10px 18px; margin-top: 16px;
                  font-size: 0.92rem; color: var(--muted); }
                .meta strong { color: var(--ink); }
                .stats { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin: 22px 0 28px; }
                @media (max-width: 720px) { .stats { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
                .stat { background: var(--card); border: 1px solid var(--line); border-radius: 12px; padding: 16px; text-align: center; }
                .stat .n { font-size: 1.8rem; font-weight: 700; }
                .stat .l { margin-top: 6px; color: var(--muted); font-size: 0.85rem; text-transform: uppercase; }
                .stat.pass .n { color: var(--pass); } .stat.fail .n { color: var(--fail); }
                .stat.skip .n { color: var(--skip); } .stat.total .n { color: var(--accent); }
                table { width: 100%; border-collapse: collapse; background: var(--card); border: 1px solid var(--line);
                  border-radius: 14px; overflow: hidden; }
                th, td { padding: 14px 16px; text-align: left; vertical-align: top; border-bottom: 1px solid var(--line); }
                th { background: #f8fafc; color: var(--muted); font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.03em; }
                tr:last-child td { border-bottom: none; }
                .badge { display: inline-block; padding: 4px 10px; border-radius: 999px; font-size: 0.78rem; font-weight: 700; }
                .badge.pass { background: var(--pass-bg); color: var(--pass); }
                .badge.fail { background: var(--fail-bg); color: var(--fail); }
                .badge.skip { background: var(--skip-bg); color: var(--skip); }
                .feature { font-weight: 600; }
                .method { color: var(--muted); font-size: 0.82rem; margin-top: 4px; font-family: Consolas, monospace; }
                .behaviour { background: var(--behaviour-bg); border-left: 3px solid var(--accent); padding: 10px 12px;
                  border-radius: 0 8px 8px 0; color: #1a3a4a; font-size: 0.92rem; }
                .reason { background: var(--fail-bg); border-left: 3px solid var(--fail); padding: 10px 12px;
                  border-radius: 0 8px 8px 0; color: #7a1c14; white-space: pre-wrap; word-break: break-word; font-size: 0.9rem; }
                .ok-note { background: var(--pass-bg); border-left: 3px solid var(--pass); padding: 10px 12px;
                  border-radius: 0 8px 8px 0; color: var(--pass); font-weight: 600; font-size: 0.9rem; }
                footer { margin-top: 18px; color: var(--muted); font-size: 0.85rem; }
              </style>
            </head>
            <body>
              <div class="wrap">
                <header>
                  <h1>Travel House — App Behaviour &amp; Pass/Fail Report</h1>
                  <p class="subtitle">Expected app behaviour for each case, with pass / fail / skip outcome</p>
                  <div class="meta">
                    <div><strong>Generated:</strong> {{GENERATED}}</div>
                    <div><strong>Suite:</strong> {{SUITE}}</div>
                    <div><strong>Platform:</strong> Android (Appium)</div>
                    <div><strong>Branch:</strong> feature/login-flight-search</div>
                  </div>
                </header>
                <div class="stats">
                  <div class="stat total"><div class="n">{{TOTAL}}</div><div class="l">Total</div></div>
                  <div class="stat pass"><div class="n">{{PASSED}}</div><div class="l">Passed</div></div>
                  <div class="stat fail"><div class="n">{{FAILED}}</div><div class="l">Failed</div></div>
                  <div class="stat skip"><div class="n">{{SKIPPED}}</div><div class="l">Skipped</div></div>
                </div>
                <table>
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>Feature / Scenario</th>
                      <th>Expected App Behaviour</th>
                      <th>Status</th>
                      <th>Duration</th>
                      <th>Result / Failure Reason</th>
                    </tr>
                  </thead>
                  <tbody>
                    {{ROWS}}
                  </tbody>
                </table>
                <footer>Auto-generated by Travel House Appium HtmlSummaryReport · Dual Route: London→Jeddah Return + London→Karachi One Way</footer>
              </div>
            </body>
            </html>
            """;
}
