package com.travelhouse.pages;

import com.travelhouse.base.DriverManager;
import com.travelhouse.utils.ExtentReportManager;
import com.travelhouse.utils.GestureUtil;
import com.travelhouse.utils.UiHelper;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Screen / sheet that lists installment plans after tapping "Pay in Installment".
 * Plans are discovered dynamically from accessibility labels.
 */
public class InstallmentPlansPage {

    private static final Pattern DESC_ATTR = Pattern.compile("content-desc=\"([^\"]+)\"");
    private static final Pattern PLAN_HINT = Pattern.compile(
            "(?i)(\\d+\\s*months?|pay\\s*in\\s*\\d+|\\d+\\s*x\\b|instal+ments?|£\\s*\\d+(?:\\.\\d{2})?\\s*/\\s*m|"
                    + "per\\s*month|/mo\\b|plan\\s*\\d+)");

    private final AndroidDriver driver;

    public InstallmentPlansPage() {
        this.driver = DriverManager.getDriver();
    }

    public boolean isDisplayed() {
        return !discoverPlanLabelsFromSource(UiHelper.getPageSourceSafe()).isEmpty()
                || UiHelper.waitForDescContains("month", 2)
                || (UiHelper.waitForDescContains("Installment", 2)
                && !UiHelper.waitForDescContains("Cheapest", 1));
    }

    public boolean waitUntilDisplayed(int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (isDisplayed() || !discoverAvailablePlans().isEmpty()) {
                return true;
            }
            pause(1000);
        }
        return !discoverAvailablePlans().isEmpty();
    }

    /**
     * Discovers all visible installment plan labels (scrolls a few times to catch off-screen plans).
     */
    public List<String> discoverAvailablePlans() {
        Set<String> plans = new LinkedHashSet<>();
        for (int i = 0; i < 5; i++) {
            plans.addAll(discoverPlanLabelsFromSource(UiHelper.getPageSourceSafe()));
            // Also inspect live nodes (more reliable than XML entities)
            for (WebElement el : driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().clickable(true)"))) {
                String desc = safeDesc(el);
                if (looksLikePlan(desc)) {
                    plans.add(normalize(desc));
                }
            }
            if (i < 4) {
                GestureUtil.swipeUp();
                pause(700);
            }
        }
        // Scroll back toward top so selection starts from a stable view
        for (int i = 0; i < 4; i++) {
            GestureUtil.swipeDown();
            pause(400);
        }
        List<String> list = new ArrayList<>(plans);
        if (list.isEmpty()
                && (UiHelper.waitForDescContains("Installment", 2)
                || UiHelper.waitForDescContains("Instalment", 2)
                || UiHelper.waitForDescContains("month", 2))) {
            list.add("Installment");
            ExtentReportManager.logInfo("No distinct plan labels — using single Installment option");
        }
        ExtentReportManager.logInfo("Discovered installment plans (" + list.size() + "): " + list);
        return list;
    }

    public void selectPlan(String planLabel) {
        Assert.assertNotNull(planLabel, "planLabel");
        Assert.assertFalse(planLabel.isBlank(), "planLabel blank");

        List<String> candidates = planTapCandidates(planLabel);
        for (int attempt = 0; attempt < 8; attempt++) {
            for (String candidate : candidates) {
                if (UiHelper.tapByDesc(candidate) || UiHelper.tapByDescContains(candidate)
                        || UiHelper.tapByTextContains(candidate)) {
                    ExtentReportManager.logInfo("Selected installment plan via: " + candidate
                            + " (from " + planLabel + ")");
                    pause(1500);
                    continueIfPresent();
                    return;
                }
            }
            // Try clicking live nodes that fuzzy-match the plan
            if (tapMatchingPlanNode(planLabel)) {
                pause(1500);
                continueIfPresent();
                return;
            }
            GestureUtil.swipeUp();
            pause(700);
        }
        throw new IllegalStateException("Could not select installment plan: " + planLabel
                + " (tried " + candidates + ")");
    }

    private List<String> planTapCandidates(String planLabel) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        String normalized = planLabel.replace('\u00a3', '£').replace("A£", "£").replace("Â£", "£").trim();
        out.add(normalized);
        out.add(planLabel);
        // Shorter stable fragments (avoid full price string encoding issues)
        java.util.regex.Matcher month = java.util.regex.Pattern
                .compile("(?i)(\\d+\\s*months?)").matcher(normalized);
        if (month.find()) {
            out.add(month.group(1));
        }
        java.util.regex.Matcher amount = java.util.regex.Pattern
                .compile("(\\d+\\.\\d{2})").matcher(normalized);
        if (amount.find()) {
            out.add(amount.group(1) + "/month");
            out.add(amount.group(1));
        }
        if (normalized.toLowerCase(Locale.ENGLISH).contains("from")) {
            int idx = normalized.toLowerCase(Locale.ENGLISH).indexOf("from");
            if (idx > 0) {
                out.add(normalized.substring(0, idx).trim());
            }
        }
        return new ArrayList<>(out);
    }

    private boolean tapMatchingPlanNode(String planLabel) {
        String needle = planLabel.toLowerCase(Locale.ENGLISH)
                .replace('\u00a3', '£').replace("a£", "£").replace("â£", "£");
        java.util.regex.Matcher month = java.util.regex.Pattern.compile("(\\d+)\\s*month").matcher(needle);
        String monthToken = month.find() ? month.group(1) + " month" : null;
        try {
            for (WebElement el : driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().clickable(true)"))) {
                String desc = safeDesc(el).toLowerCase(Locale.ENGLISH)
                        .replace('\u00a3', '£').replace("a£", "£");
                if (!looksLikePlan(desc)) {
                    continue;
                }
                boolean match = desc.contains(needle)
                        || (monthToken != null && desc.contains(monthToken));
                java.util.regex.Matcher amt = java.util.regex.Pattern.compile("(\\d+\\.\\d{2})").matcher(needle);
                if (!match && amt.find() && desc.contains(amt.group(1))) {
                    match = true;
                }
                if (match) {
                    el.click();
                    ExtentReportManager.logInfo("Selected installment plan node: " + safeDesc(el));
                    return true;
                }
            }
        } catch (Exception e) {
            ExtentReportManager.logInfo("Plan node tap note: " + e.getMessage());
        }
        return false;
    }

    public void continueIfPresent() {
        UiHelper.tapByDesc("Continue");
        UiHelper.tapByDescContains("Continue");
        UiHelper.tapByDescContains("Confirm");
        UiHelper.tapByDescContains("Select");
        UiHelper.tapByDescContains("Apply");
        UiHelper.tapByDescContains("Next");
    }

    private List<String> discoverPlanLabelsFromSource(String source) {
        List<String> found = new ArrayList<>();
        if (source == null || source.isBlank()) {
            return found;
        }
        Matcher m = DESC_ATTR.matcher(source);
        while (m.find()) {
            String raw = m.group(1)
                    .replace("&#10;", "\n")
                    .replace("&amp;", "&")
                    .trim();
            if (looksLikePlan(raw)) {
                found.add(normalize(raw));
            }
        }
        return found;
    }

    private boolean looksLikePlan(String desc) {
        if (desc == null || desc.isBlank()) {
            return false;
        }
        String lower = desc.toLowerCase(Locale.ENGLISH);
        if (lower.equals("pay in installment") || lower.equals("pay in instalment")) {
            return false;
        }
        if (lower.contains("cheapest") || lower.contains("fastest") || lower.contains("search flight")
                || lower.contains("proceed with payment") || lower.contains("full payment")
                || lower.startsWith("departure\n") || lower.contains("tab ")) {
            return false;
        }
        if (desc.length() > 160) {
            return false;
        }
        return PLAN_HINT.matcher(desc).find();
    }

    private String normalize(String desc) {
        String n = desc.replace('\n', ' ').replaceAll("\\s+", " ").trim();
        if (n.length() > 120) {
            n = n.substring(0, 120);
        }
        return n;
    }

    private String safeDesc(WebElement el) {
        try {
            String d = el.getAttribute("contentDescription");
            return d == null ? "" : d.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
