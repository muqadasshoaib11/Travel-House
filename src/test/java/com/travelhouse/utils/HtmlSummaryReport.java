package com.travelhouse.utils;

import org.testng.ITestResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Builds a concise HTML summary: feature name, status, and failure reason.
 */
public final class HtmlSummaryReport {

    private static final List<Row> ROWS = new CopyOnWriteArrayList<>();

    private HtmlSummaryReport() {
    }

    public static void record(ITestResult result) {
        String method = result.getMethod().getMethodName();
        String feature = result.getMethod().getDescription();
        if (feature == null || feature.isBlank()) {
            feature = method;
        }
        String status;
        String reason = "—";
        switch (result.getStatus()) {
            case ITestResult.SUCCESS -> status = "PASSED";
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
                        : "Skipped due to dependency or configuration";
            }
            default -> status = "UNKNOWN";
        }
        long durationMs = result.getEndMillis() - result.getStartMillis();
        ROWS.add(new Row(feature, method, status, durationMs, reason));
    }

    public static Path write(Path reportDir) {
        try {
            Files.createDirectories(reportDir);
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path out = reportDir.resolve("TravelHouse_Summary_" + stamp + ".html");
            Path latest = reportDir.resolve("TravelHouse_FlightBooking_Summary.html");

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
                String reasonHtml = "PASSED".equals(row.status)
                        ? "<span class=\"ok-note\">—</span>"
                        : "<div class=\"reason\">" + escape(row.reason) + "</div>";
                rowsHtml.append("<tr>")
                        .append("<td>").append(i++).append("</td>")
                        .append("<td><div class=\"feature\">").append(escape(row.feature)).append("</div>")
                        .append("<div class=\"method\">").append(escape(row.method)).append("</div></td>")
                        .append("<td><span class=\"badge ").append(badgeClass).append("\">")
                        .append(row.status).append("</span></td>")
                        .append("<td>").append(String.format("%.1fs", row.durationMs / 1000.0)).append("</td>")
                        .append("<td>").append(reasonHtml).append("</td>")
                        .append("</tr>\n");
            }

            String html = TEMPLATE
                    .replace("{{GENERATED}}", escape(generated))
                    .replace("{{TOTAL}}", String.valueOf(total))
                    .replace("{{PASSED}}", String.valueOf(passed))
                    .replace("{{FAILED}}", String.valueOf(failed))
                    .replace("{{SKIPPED}}", String.valueOf(skipped))
                    .replace("{{ROWS}}", rowsHtml.toString());

            Files.writeString(out, html, StandardCharsets.UTF_8);
            Files.writeString(latest, html, StandardCharsets.UTF_8);
            System.out.println("[REPORT] HTML summary: " + latest.toAbsolutePath());
            return latest;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write HTML summary report", e);
        } finally {
            ROWS.clear();
        }
    }

    private static String safe(String message) {
        if (message == null || message.isBlank()) {
            return "No failure message provided";
        }
        return message.trim();
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

    private record Row(String feature, String method, String status, long durationMs, String reason) {
    }

    private static final String TEMPLATE = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1" />
              <title>Travel House — Flight Booking Test Report</title>
              <style>
                :root {
                  --bg: #f4f6f8; --card: #ffffff; --ink: #1a2332; --muted: #5b6b7c;
                  --pass: #1b7f4e; --pass-bg: #e8f7ef; --fail: #b42318; --fail-bg: #fdeceb;
                  --skip: #8a6d3b; --skip-bg: #fff6e5; --line: #e2e8f0; --accent: #0b6e99;
                }
                * { box-sizing: border-box; }
                body { margin: 0; font-family: "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
                  background: linear-gradient(180deg, #eaf3f8 0%, var(--bg) 220px); color: var(--ink); line-height: 1.45; }
                .wrap { max-width: 1100px; margin: 0 auto; padding: 32px 20px 48px; }
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
                th { background: #f8fafc; color: var(--muted); font-size: 0.78rem; text-transform: uppercase; }
                tr:last-child td { border-bottom: none; }
                .badge { display: inline-block; padding: 4px 10px; border-radius: 999px; font-size: 0.78rem; font-weight: 700; }
                .badge.pass { background: var(--pass-bg); color: var(--pass); }
                .badge.fail { background: var(--fail-bg); color: var(--fail); }
                .badge.skip { background: var(--skip-bg); color: var(--skip); }
                .feature { font-weight: 600; }
                .method { color: var(--muted); font-size: 0.85rem; margin-top: 4px; font-family: Consolas, monospace; }
                .reason { background: var(--fail-bg); border-left: 3px solid var(--fail); padding: 10px 12px;
                  border-radius: 0 8px 8px 0; color: #7a1c14; white-space: pre-wrap; word-break: break-word; }
                .ok-note { color: var(--pass); font-weight: 600; }
                footer { margin-top: 18px; color: var(--muted); font-size: 0.85rem; }
              </style>
            </head>
            <body>
              <div class="wrap">
                <header>
                  <h1>Travel House Flight Booking — Automation Report</h1>
                  <p class="subtitle">Passed / failed cases with feature names and failure reasons</p>
                  <div class="meta">
                    <div><strong>Generated:</strong> {{GENERATED}}</div>
                    <div><strong>Suite:</strong> Travel House Regression</div>
                    <div><strong>Platform:</strong> Android</div>
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
                      <th>Status</th>
                      <th>Duration</th>
                      <th>Failure Reason</th>
                    </tr>
                  </thead>
                  <tbody>
                    {{ROWS}}
                  </tbody>
                </table>
                <footer>Auto-generated by Travel House Appium HtmlSummaryReport</footer>
              </div>
            </body>
            </html>
            """;
}
