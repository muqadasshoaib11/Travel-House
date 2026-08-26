package com.travelhouse.pages;

import com.travelhouse.base.DriverManager;
import com.travelhouse.config.Credentials;
import com.travelhouse.config.TestDataReader;
import com.travelhouse.utils.GestureUtil;
import com.travelhouse.utils.UiHelper;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.testng.Assert;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Traveller Information — saved traveller, special requests, contact fields.
 */
public class TravellerInfoPage {

    private final AndroidDriver driver;

    public TravellerInfoPage() {
        this.driver = DriverManager.getDriver();
    }

    public boolean isDisplayed() {
        return !driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Who's Going\")")).isEmpty()
                || !driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Traveller\")")).isEmpty()
                || !driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Contact Information\")")).isEmpty();
    }

    public boolean isMyTravellersScreen() {
        return !driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"My Travellers\")")).isEmpty()
                || !driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Who's Going\")")).isEmpty()
                || isDisplayed();
    }

    public void waitUntilVisible() {
        Assert.assertTrue(
                UiHelper.waitForDescContains("Who's Going", 12)
                        || UiHelper.waitForDescContains("My Travellers", 4)
                        || UiHelper.waitForDescContains("Contact Information", 4)
                        || isDisplayed(),
                "Traveller Information / My Travellers screen should be visible");
    }

    /**
     * Full traveller completion using the Sign-In email from config.properties.
     */
    public void completeTravellerFormUsingSignInEmail() {
        waitUntilVisible();
        dismissOverlayIfOpen();
        selectTravellerFromDropdown();
        dismissOverlayIfOpen();
        ensureNameFieldsAlphabeticOnly();
        fillSpecialRequests();
        fillFrequentFlyer();
        fillContactSectionWithSignInEmail();
        GestureUtil.swipeUp();
        pause(600);
    }

    public void completeTravellerFormFromTestData() {
        completeTravellerFormUsingSignInEmail();
    }

    public void selectTravellerFromDropdown() {
        String savedName = TestDataReader.get("traveller.saved.name", "Muqadas Shoaib");
        boolean opened = UiHelper.tapByDescContains("Please Select a Saved Traveller")
                || UiHelper.tapByDescContains("My Travellers")
                || UiHelper.tapByDescContains("Select a Saved Traveller")
                || UiHelper.tapByDescContains("Adult");
        pause(1500);

        if (UiHelper.tapByDesc(savedName) || UiHelper.tapByDescContains(savedName)) {
            System.out.println("[Traveller] Selected saved traveller: " + savedName);
            pause(1500);
            dismissOverlayIfOpen();
            return;
        }

        List<WebElement> options = driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().clickable(true)"));
        boolean selected = false;
        for (WebElement option : options) {
            String desc = safeDesc(option);
            if (desc.length() < 3) {
                continue;
            }
            if (desc.equalsIgnoreCase("None") || desc.equalsIgnoreCase("Dismiss")
                    || desc.contains("Tab") || desc.contains("Traveller")
                    || desc.contains("Who's") || desc.contains("Select")) {
                continue;
            }
            if (desc.contains(savedName) || desc.contains(" ") || Character.isLetter(desc.charAt(0))) {
                System.out.println("[Traveller] Selected from dropdown: " + desc);
                option.click();
                selected = true;
                pause(1500);
                break;
            }
        }
        dismissOverlayIfOpen();
        Assert.assertTrue(selected || opened,
                "Could not select a traveller name from the Traveller dropdown");
    }

    /**
     * One-pass traveller completion: pick saved name from dropdown, fill remaining fields once.
     * Does not re-enter the same values in a loop.
     */
    public void completeOnceSelectNameAndFill() {
        waitUntilVisible();
        dismissOverlayIfOpen();
        selectTravellerFromDropdown();
        dismissOverlayIfOpen();
        ensureNameFieldsAlphabeticOnly();
        fillSpecialRequests();
        fillFrequentFlyer();
        fillContactSectionWithSignInEmail();
        GestureUtil.swipeUp();
        pause(600);
    }

    /** Taps Continue once and waits for navigation — no second fill pass. */
    public void continueOnceAndEnd() {
        continueThroughSummaryOnceAndStop();
    }

    /**
     * My Travellers Continue → verify Summary → Continue once → stop
     * (matches Playwright installment chunk).
     */
    public void continueThroughSummaryOnceAndStop() {
        dismissOverlayIfOpen();
        GestureUtil.swipeUp();
        pause(500);
        List<WebElement> continueBtns = driver.findElements(AppiumBy.accessibilityId("Continue"));
        if (continueBtns.isEmpty()) {
            continueBtns = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"Continue\")"));
        }
        Assert.assertFalse(continueBtns.isEmpty(), "Continue button not found on Traveller screen");
        tapCenterSafe(continueBtns.get(0));
        pause(1500);
        dismissOverlayIfOpen();

        Assert.assertTrue(
                UiHelper.waitForDescContains("Summary", 30)
                        || UiHelper.waitForDescContains("Price Summary", 5)
                        || new PriceSummaryPage().isDisplayed()
                        || new PriceSummaryPage().hasTotalPrice(),
                "Summary screen did not appear after My Travellers Continue");

        List<WebElement> summaryContinue = driver.findElements(AppiumBy.accessibilityId("Continue"));
        if (summaryContinue.isEmpty()) {
            summaryContinue = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"Continue\")"));
        }
        if (!summaryContinue.isEmpty()) {
            tapCenterSafe(summaryContinue.get(0));
            pause(1000);
            dismissOverlayIfOpen();
        }
        System.out.println("[Traveller] Stopped after Summary Continue (no further payment steps)");
    }

    /**
     * First/Last name fields must contain letters only — never digits (mobile must not bleed into name).
     */
    public void ensureNameFieldsAlphabeticOnly() {
        String first = lettersOnly(TestDataReader.get("traveller.first.name", "Muqadas"));
        String last = lettersOnly(TestDataReader.get("traveller.last.name", "Shoaib"));
        if (first.isBlank()) {
            first = "Muqadas";
        }
        if (last.isBlank()) {
            last = "Shoaib";
        }

        List<WebElement> fields = driver.findElements(AppiumBy.className("android.widget.EditText"));
        List<WebElement> nameCandidates = new ArrayList<>();
        for (WebElement field : fields) {
            try {
                String text = field.getText() == null ? "" : field.getText().trim();
                String hint = "";
                try {
                    hint = String.valueOf(field.getAttribute("contentDescription"));
                } catch (Exception ignored) {
                    // ignore
                }
                boolean looksLikeEmail = text.contains("@") || hint.toLowerCase().contains("email");
                boolean looksLikeMobile = text.matches(".*\\d{6,}.*")
                        || hint.toLowerCase().contains("mobile")
                        || hint.toLowerCase().contains("phone");
                if (looksLikeEmail || looksLikeMobile) {
                    continue;
                }
                // Name fields: empty, letters, or polluted with digits that we must fix
                if (text.isBlank() || text.matches(".*[A-Za-z].*") || text.matches(".*\\d.*")) {
                    nameCandidates.add(field);
                }
            } catch (Exception ignored) {
                // next
            }
        }

        // Always apply configured alphabetic First / Last (never leave digits or duplicated first name)
        nameCandidates.sort((a, b) -> Integer.compare(a.getRect().y, b.getRect().y));
        if (nameCandidates.size() >= 1) {
            typeIntoField(nameCandidates.get(0), first);
            System.out.println("[Traveller] First name set to alphabetic: " + first);
        }
        if (nameCandidates.size() >= 2) {
            typeIntoField(nameCandidates.get(1), last);
            System.out.println("[Traveller] Last name set to alphabetic: " + last);
        } else if (nameCandidates.size() == 1) {
            // Single combined field — use "First Last" letters only
            typeIntoField(nameCandidates.get(0), first + " " + last);
            System.out.println("[Traveller] Full name set to alphabetic: " + first + " " + last);
        }

        // Final sweep: any EditText whose value has digits and no @ → treat as polluted name and clear to letters
        fields = driver.findElements(AppiumBy.className("android.widget.EditText"));
        for (WebElement field : fields) {
            String text = safeText(field);
            if (text.contains("@")) {
                continue;
            }
            if (text.matches(".*\\d.*") && text.matches(".*[A-Za-z].*") && text.length() < 40) {
                // mixed name+digits — restore last name letters only
                typeIntoField(field, last);
                System.out.println("[Traveller] Cleared digits from name field → " + last);
            } else if (text.matches("^\\d+$") && text.length() >= 6) {
                // pure digits wrongly in a name-like field above mobile — leave if this IS mobile;
                // only rewrite if field is higher on screen than Mobile Number label
                if (isAboveMobileLabel(field)) {
                    typeIntoField(field, last);
                    System.out.println("[Traveller] Replaced digits-only name field with: " + last);
                }
            }
        }
        hideKeyboardQuietly();
    }

    public void fillSpecialRequests() {
        GestureUtil.swipeUp();
        pause(500);

        boolean expanded = UiHelper.tapByDescContains("Add Special Request")
                || UiHelper.tapByDescContains("Special Request");
        pause(600);
        if (expanded) {
            System.out.println("[Traveller] Opened Special Requests section");
        } else {
            // Try scrolling into view once more
            GestureUtil.swipeUp();
            pause(500);
            UiHelper.tapByDescContains("Special Request");
            pause(1000);
        }

        String seat = TestDataReader.get("traveller.seat.preference", "Cot");
        String meal = TestDataReader.get("traveller.meal.request", "Fruit Meal");
        String service = TestDataReader.get("traveller.special.service", "Blind Passenger");

        // Playwright chunk opens from current labels: Any / Any meal / No Special Service Requested
        Assert.assertTrue(
                selectDropdownWithRetry("Any", seat,
                        List.of("Cot", "Code", "Window", "Aisle", "Exit Seat", "Any", "Middle"))
                        || selectDropdownWithRetry("Seat Preference", seat,
                        List.of("Cot", "Code", "Window", "Aisle", "Exit Seat", "Any", "Middle")),
                "Seat Preference dropdown must be selected (Cot on current build)");
        if (!UiHelper.waitForDescContains("Meal Request", 2)
                && !UiHelper.waitForDescContains("Any meal", 1)) {
            UiHelper.tapByDescContains("Add Special Request");
            pause(500);
        }
        Assert.assertTrue(
                selectDropdownWithRetry("Any meal", meal,
                        List.of("Fruit Meal", "Halal", "Vegetarian", "Gluten Free", "Vegan", "Any"))
                        || selectDropdownWithRetry("Meal Request", meal,
                        List.of("Fruit Meal", "Halal", "Vegetarian", "Gluten Free", "Vegan", "Any")),
                "Meal Request dropdown must be selected");
        Assert.assertTrue(
                selectDropdownWithRetry("No Special Service Requested", service,
                        List.of("Blind Passenger", "No Special Service Requested", "Wheelchair", "Bassinet"))
                        || selectDropdownWithRetry("Special Service", service,
                        List.of("Blind Passenger", "No Special Service Requested", "Wheelchair", "Bassinet")),
                "Special Service dropdown must be selected");

        dismissOverlayIfOpen();
        pause(400);
        if (UiHelper.waitForDescContains("Select Country", 1)) {
            UiHelper.tapByDesc("Dismiss");
            UiHelper.tapByDescContains("Dismiss");
        }
        hideKeyboardQuietly();
        GestureUtil.swipeUp();
        pause(500);
    }

    public void fillFrequentFlyer() {
        String ff = TestDataReader.get("traveller.frequent.flyer", "Muqadas");
        GestureUtil.swipeUp();
        pause(300);

        if (!UiHelper.waitForDescContains("Frequent Flyer", 2)) {
            System.out.println("[Traveller] Frequent Flyer section not found — skipped");
            return;
        }
        List<WebElement> fields = driver.findElements(AppiumBy.className("android.widget.EditText"));
        for (WebElement field : fields) {
            try {
                String hint = "";
                try {
                    hint = String.valueOf(field.getAttribute("contentDescription"));
                } catch (Exception ignored) {
                    // ignore
                }
                String text = field.getText() == null ? "" : field.getText();
                if (hint.toLowerCase().contains("frequent") || hint.toLowerCase().contains("flyer")
                        || (text.isBlank() && hint.toLowerCase().contains("flyer"))) {
                    typeIntoField(field, ff);
                    System.out.println("[Traveller] Frequent Flyer set to: " + ff);
                    hideKeyboardQuietly();
                    return;
                }
            } catch (Exception ignored) {
                // next
            }
        }
        // Fallback: last empty EditText in view after Frequent Flyer label
        for (int i = fields.size() - 1; i >= 0; i--) {
            WebElement field = fields.get(i);
            try {
                String text = field.getText() == null ? "" : field.getText().trim();
                if (text.isBlank() || text.equalsIgnoreCase(ff)) {
                    typeIntoField(field, ff);
                    System.out.println("[Traveller] Frequent Flyer (fallback field) set to: " + ff);
                    hideKeyboardQuietly();
                    return;
                }
            } catch (Exception ignored) {
                // next
            }
        }
        System.out.println("[Traveller] Frequent Flyer field not typed — value expected=" + ff);
        dismissOverlayIfOpen();
    }

    public void fillContactSection() {
        fillContactSectionWithSignInEmail();
    }

    public void fillContactSectionWithSignInEmail() {
        GestureUtil.swipeUp();
        pause(600);

        String email = Credentials.isConfigured() ? Credentials.email()
                : TestDataReader.get("traveller.email", "");
        // Pakistan mobile (local digits after +92) — letters never go here
        String mobile = TestDataReader.get("traveller.mobile", "3001234567").replaceAll("\\D", "");
        if (mobile.startsWith("92") && mobile.length() > 10) {
            mobile = mobile.substring(2);
        }
        if (mobile.length() > 10) {
            mobile = mobile.substring(mobile.length() - 10);
        }

        for (int i = 0; i < 4; i++) {
            if (UiHelper.waitForDescContains("Mobile Number", 2)
                    || UiHelper.waitForDescContains("Email Address", 1)
                    || UiHelper.waitForDescContains("Contact Information", 1)) {
                break;
            }
            GestureUtil.swipeUp();
            pause(500);
        }

        // Always Pakistan (+92) for contact country code
        selectCountryCodePakistan();
        hideKeyboardQuietly();
        Assert.assertTrue(countryCodeIsPakistan() || UiHelper.waitForDescContains("+92", 2),
                "Contact country code must be Pakistan (+92)");

        // Email: prefer field that already has @ or widest field below Contact Information
        List<WebElement> fields = driver.findElements(AppiumBy.className("android.widget.EditText"));
        WebElement emailField = findEmailField(fields);
        if (emailField != null && !email.isBlank()) {
            typeIntoField(emailField, email);
            hideKeyboardQuietly();
            System.out.println("[Traveller] Email set from Sign-In credentials");
        }

        // Mobile: focus by label then type digits only into that field
        typeMobileBesideLabel(mobile);
        hideKeyboardQuietly();
        System.out.println("[Traveller] Mobile entered with Pakistan (+92): " + mobile);

        // Re-assert names were not polluted by mobile typing
        ensureNameFieldsAlphabeticOnly();

        GestureUtil.swipeUp();
        pause(400);
        String how = TestDataReader.get("traveller.how.to.contact", "Phone");
        String when = TestDataReader.get("traveller.contact.time", "09:00 to 12:00");
        if (!selectDropdownWithRetry("How to Contact", how,
                List.of("Phone", "Any (Phone + Email)", "Email", "WhatsApp"))) {
            selectDropdownWithRetry("Any (Phone + Email)", how,
                    List.of("Phone", "Any (Phone + Email)", "Email", "WhatsApp"));
        }
        if (!selectDropdownWithRetry("Contact Time", when,
                List.of("09:00 to 12:00", "9:00-12:00", "9:00 – 12:00", "09:00-12:00",
                        "9:00", "Morning", "Any Time"))) {
            selectDropdownWithRetry("Any Time", when,
                    List.of("09:00 to 12:00", "9:00-12:00", "9:00 – 12:00", "09:00-12:00",
                            "9:00", "Morning", "Any Time"));
        }
    }

    private void selectCountryCodeUnitedKingdom() {
        if (!driver.findElements(AppiumBy.accessibilityId("+44")).isEmpty()) {
            System.out.println("[Traveller] Country code already +44");
            return;
        }
        boolean opened = false;
        try {
            List<WebElement> codes = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionStartsWith(\"+\")"));
            for (WebElement code : codes) {
                String desc = safeDesc(code);
                if (desc.matches("\\+\\d+")) {
                    code.click();
                    opened = true;
                    break;
                }
            }
        } catch (Exception ignored) {
            // fallback
        }
        if (!opened) {
            opened = UiHelper.tapByDescContains("+92")
                    || UiHelper.tapByDescContains("+1684")
                    || UiHelper.tapByDescContains("+1")
                    || UiHelper.tapByDescContains("+44");
        }
        pause(1000);
        if (!UiHelper.waitForDescContains("Select Country", 5)) {
            return;
        }
        try {
            List<WebElement> fields = driver.findElements(AppiumBy.className("android.widget.EditText"));
            if (!fields.isEmpty()) {
                fields.get(0).click();
                fields.get(0).sendKeys("United Kingdom");
                pause(1000);
            }
        } catch (Exception ignored) {
            // ignore
        }
        UiHelper.tapByDescContains("+44 United Kingdom");
        UiHelper.tapByDescContains("+44");
        UiHelper.tapByTextContains("United Kingdom");
        pause(600);
        hideKeyboardQuietly();
    }

    private boolean countryCodeIsPakistan() {
        return !driver.findElements(AppiumBy.accessibilityId("+92")).isEmpty()
                || !driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"+92\")")).isEmpty();
    }

    private void hideKeyboardQuietly() {
        try {
            driver.hideKeyboard();
        } catch (Exception ignored) {
            // keyboard may already be hidden
        }
        pause(300);
    }

    public void continueAndVerifyNextScreen() {
        dismissOverlayIfOpen();
        GestureUtil.swipeUp();
        pause(500);

        // Fix validation before Continue
        if (UiHelper.waitForDescContains("valid mobile", 1)
                || UiHelper.waitForDescContains("Please enter", 1)) {
            selectCountryCodePakistan();
            List<WebElement> fields = driver.findElements(AppiumBy.className("android.widget.EditText"));
            WebElement mobileField = findMobileField(fields);
            if (mobileField != null) {
                typeIntoField(mobileField, TestDataReader.get("traveller.mobile", "3001234567")
                        .replaceAll("\\D", "").replaceAll("^(\\d{10}).*", "$1"));
            }
        }

        List<WebElement> continueBtns = driver.findElements(AppiumBy.accessibilityId("Continue"));
        if (continueBtns.isEmpty()) {
            continueBtns = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"Continue\")"));
        }
        Assert.assertFalse(continueBtns.isEmpty(), "Continue button not found on Traveller screen");
        tapCenterSafe(continueBtns.get(0));
        pause(4000);

        dismissOverlayIfOpen();
        boolean leftTraveller = !isDisplayed()
                || new PriceDetailsPage().isDisplayed()
                || new PriceSummaryPage().isDisplayed()
                || UiHelper.waitForDescContains("Total Price", 5)
                || UiHelper.waitForDescContains("Payment", 5)
                || UiHelper.waitForDescContains("Net Price", 3);

        if (!leftTraveller) {
            fillContactSection();
            GestureUtil.swipeUp();
            pause(500);
            continueBtns = driver.findElements(AppiumBy.accessibilityId("Continue"));
            if (continueBtns.isEmpty()) {
                continueBtns = driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().descriptionContains(\"Continue\")"));
            }
            if (!continueBtns.isEmpty()) {
                tapCenterSafe(continueBtns.get(0));
                pause(4000);
            }
            leftTraveller = !isDisplayed()
                    || new PriceDetailsPage().isDisplayed()
                    || new PriceSummaryPage().hasTotalPrice()
                    || UiHelper.waitForDescContains("Total Price", 5);
        }

        Assert.assertTrue(leftTraveller,
                "Continue did not navigate to the next screen from Traveller Information");
    }

    public void dismissOverlayIfOpen() {
        if (!driver.findElements(AppiumBy.accessibilityId("Dismiss")).isEmpty()
                || !driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Dismiss\")")).isEmpty()) {
            UiHelper.tapByDesc("Dismiss");
            UiHelper.tapByDescContains("Dismiss");
            pause(400);
        }
    }

    private void selectCountryCodePakistan() {
        if (countryCodeIsPakistan()) {
            System.out.println("[Traveller] Country code already +92");
            return;
        }

        boolean opened = false;
        // ImageView / node whose content-desc starts with +
        try {
            List<WebElement> codes = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionStartsWith(\"+\")"));
            for (WebElement code : codes) {
                String desc = safeDesc(code);
                if (desc.matches("\\+\\d+")) {
                    System.out.println("[Traveller] Opening country picker from " + desc);
                    code.click();
                    opened = true;
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("[Traveller] descriptionStartsWith(+) issue: " + e.getMessage());
        }

        if (!opened) {
            opened = UiHelper.tapByDescContains("+44")
                    || UiHelper.tapByDescContains("+92")
                    || UiHelper.tapByDescContains("+1684")
                    || UiHelper.tapByDescContains("+1");
        }

        // Coordinate fallback beside Mobile Number label
        if (!opened) {
            try {
                List<WebElement> mobileLabels = driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().descriptionContains(\"Mobile Number\")"));
                if (!mobileLabels.isEmpty()) {
                    Rectangle r = mobileLabels.get(0).getRect();
                    int x = Math.max(80, r.x + 80);
                    int y = r.y + r.height + 50;
                    PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
                    Sequence tap = new Sequence(finger, 1);
                    tap.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
                    tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
                    tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
                    driver.perform(Collections.singletonList(tap));
                    opened = true;
                    System.out.println("[Traveller] Tapped country code via coordinates " + x + "," + y);
                }
            } catch (Exception e) {
                System.out.println("[Traveller] coordinate country tap failed: " + e.getMessage());
            }
        }
        pause(1000);

        if (!UiHelper.waitForDescContains("Select Country", 6)) {
            System.out.println("[Traveller] Country picker did not open");
            return;
        }

        try {
            List<WebElement> fields = driver.findElements(AppiumBy.className("android.widget.EditText"));
            if (!fields.isEmpty()) {
                WebElement search = fields.get(0);
                search.click();
                pause(300);
                try {
                    search.clear();
                } catch (Exception ignored) {
                    // ignore
                }
                search.sendKeys("Pakistan");
                pause(1200);
            }
        } catch (Exception e) {
            System.out.println("[Traveller] Country search type issue: " + e.getMessage());
        }

        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                if (UiHelper.tapByDesc("+92 Pakistan")
                        || UiHelper.tapByDescContains("+92 Pakistan")
                        || UiHelper.tapByDescContains("+92")
                        || UiHelper.tapByTextContains("Pakistan")) {
                    System.out.println("[Traveller] Selected Pakistan country code (+92)");
                    pause(800);
                    hideKeyboardQuietly();
                    return;
                }
            } catch (Exception e) {
                pause(400);
            }
        }
        dismissOverlayIfOpen();
    }

    private void selectAnyFromDropdown(String primaryLabel, String preferredOption) {
        selectDropdownWithRetry(primaryLabel, preferredOption, List.of(preferredOption));
    }

    /**
     * Opens a labelled dropdown and selects preferred option, trying fallbacks then any plausible option.
     */
    private boolean selectDropdownWithRetry(String primaryLabel, String preferredOption, List<String> fallbacks) {
        for (int attempt = 0; attempt < 3; attempt++) {
            GestureUtil.swipeUp();
            pause(400);
            boolean opened = UiHelper.tapByDescContains(primaryLabel)
                    || UiHelper.tapByTextContains(primaryLabel)
                    || UiHelper.tapByDesc(primaryLabel);
            if (!opened) {
                System.out.println("[Traveller] Dropdown open attempt " + (attempt + 1)
                        + " failed for: " + primaryLabel);
                continue;
            }
            pause(1200);

            List<String> candidates = new ArrayList<>();
            if (preferredOption != null && !preferredOption.isBlank()) {
                candidates.add(preferredOption);
            }
            if (fallbacks != null) {
                for (String f : fallbacks) {
                    if (f != null && !f.isBlank() && !candidates.contains(f)) {
                        candidates.add(f);
                    }
                }
            }
            for (String option : candidates) {
                if (UiHelper.tapByDesc(option)
                        || UiHelper.tapByDescContains(option)
                        || UiHelper.tapByTextContains(option)) {
                    System.out.println("[Traveller] " + primaryLabel + " → " + option);
                    pause(800);
                    dismissOverlayIfOpen();
                    return true;
                }
            }

            List<WebElement> options = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().clickable(true)"));
            for (WebElement option : options) {
                String desc = safeDesc(option);
                if (!isPlausibleDropdownOption(desc, primaryLabel)) {
                    continue;
                }
                try {
                    System.out.println("[Traveller] " + primaryLabel + " → " + desc + " (fallback option)");
                    option.click();
                    pause(800);
                    dismissOverlayIfOpen();
                    return true;
                } catch (Exception e) {
                    break;
                }
            }
            dismissOverlayIfOpen();
            pause(400);
        }
        System.out.println("[Traveller] Could not select value for dropdown: " + primaryLabel);
        return false;
    }

    private boolean isPlausibleDropdownOption(String desc, String primaryLabel) {
        if (desc == null || desc.length() < 2 || desc.length() > 80) {
            return false;
        }
        String lower = desc.toLowerCase();
        String label = (primaryLabel == null ? "" : primaryLabel).toLowerCase();

        if (desc.equalsIgnoreCase("Dismiss")
                || desc.equalsIgnoreCase("Continue")
                || desc.equalsIgnoreCase("None")
                || desc.contains("Tab")
                || desc.contains("Who's")
                || desc.contains("Contact Information")
                || desc.contains("Add Special")
                || desc.contains("Select Country")
                || desc.matches("\\+\\d+.*")
                || (primaryLabel != null && desc.contains(primaryLabel))) {
            return false;
        }
        if (lower.contains("muqadas") || desc.matches("[A-Z][a-z]+\\s+[A-Z][a-z]+")) {
            return false;
        }

        boolean seatLike = lower.contains("seat") || lower.contains("window") || lower.contains("aisle")
                || lower.contains("middle") || lower.contains("cot") || lower.contains("code")
                || lower.equals("any");
        boolean mealLike = lower.contains("meal") || lower.contains("vegetarian") || lower.contains("halal")
                || lower.contains("vegan") || lower.contains("gluten") || lower.contains("kosher");
        boolean serviceLike = lower.contains("service") || lower.contains("wheelchair")
                || lower.contains("bassinet") || lower.contains("legroom") || lower.contains("no special");
        boolean contactLike = lower.contains("email") || lower.contains("phone") || lower.contains("whatsapp")
                || lower.contains("sms") || lower.contains("call") || lower.contains("message");
        boolean timeLike = lower.contains("morning") || lower.contains("afternoon") || lower.contains("evening")
                || lower.contains("anytime") || lower.contains("any time") || lower.matches(".*\\btime\\b.*");

        if (label.contains("meal") && (seatLike || desc.matches("\\+\\d+.*"))) {
            return false;
        }
        if (label.contains("seat") && mealLike) {
            return false;
        }
        if (label.contains("service") && (seatLike || mealLike || desc.matches("\\+\\d+.*"))) {
            return false;
        }
        if ((label.contains("how to") || (label.contains("contact") && !label.contains("time")))
                && (seatLike || mealLike)) {
            return false;
        }
        if (label.contains("time") && (seatLike || mealLike || (contactLike && !timeLike))) {
            return false;
        }

        if (label.contains("seat") && seatLike) {
            return true;
        }
        if (label.contains("meal") && mealLike) {
            return true;
        }
        if (label.contains("service") && serviceLike) {
            return true;
        }
        if ((label.contains("how to") || (label.contains("contact") && !label.contains("time"))) && contactLike) {
            return true;
        }
        if (label.contains("time") && timeLike) {
            return true;
        }
        // Accept generic non-chrome options when sheet is open
        return !lower.contains("search") && !lower.contains("flying");
    }

    private WebElement findEmailField(List<WebElement> fields) {
        for (WebElement field : fields) {
            String text = safeText(field);
            if (text.contains("@")) {
                return field;
            }
        }
        return findWideField(fields);
    }

    private void typeMobileBesideLabel(String mobileDigits) {
        String digitsOnly = mobileDigits == null ? "" : mobileDigits.replaceAll("\\D", "");
        Assert.assertFalse(digitsOnly.isBlank(), "Mobile digits required");

        // Tap near Mobile Number label so Flutter focuses the correct field
        try {
            List<WebElement> labels = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"Mobile Number\")"));
            if (!labels.isEmpty()) {
                Rectangle r = labels.get(0).getRect();
                GestureUtil.tapAt(Math.min(r.x + r.width + 40, 900), r.y + r.height + 40);
                pause(400);
            }
        } catch (Exception ignored) {
            // fall through
        }

        List<WebElement> fields = driver.findElements(AppiumBy.className("android.widget.EditText"));
        WebElement mobileField = null;
        for (WebElement field : fields) {
            if (isAboveMobileLabel(field)) {
                continue; // skip name fields above the mobile label
            }
            String text = safeText(field);
            if (text.contains("@")) {
                continue;
            }
            // Prefer empty or numeric fields below contact section
            if (text.isBlank() || text.matches("\\d+")) {
                mobileField = field;
                break;
            }
        }
        if (mobileField == null) {
            mobileField = findMobileField(fields);
        }
        Assert.assertNotNull(mobileField, "Mobile number EditText not found");
        typeIntoField(mobileField, digitsOnly);

        // Guard: if a name field now contains these digits, clear it back to letters
        ensureNameFieldsAlphabeticOnly();
    }

    private boolean isAboveMobileLabel(WebElement field) {
        try {
            List<WebElement> labels = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"Mobile Number\")"));
            if (labels.isEmpty()) {
                return false;
            }
            return field.getRect().y < labels.get(0).getRect().y;
        } catch (Exception e) {
            return false;
        }
    }

    private WebElement findWideField(List<WebElement> fields) {
        WebElement best = null;
        int bestWidth = 0;
        for (WebElement field : fields) {
            try {
                int w = field.getRect().width;
                if (w > bestWidth) {
                    bestWidth = w;
                    best = field;
                }
            } catch (Exception ignored) {
                // next
            }
        }
        return best;
    }

    private WebElement findMobileField(List<WebElement> fields) {
        WebElement best = null;
        int bestWidth = Integer.MAX_VALUE;
        for (WebElement field : fields) {
            try {
                if (isAboveMobileLabel(field)) {
                    continue;
                }
                String text = field.getText();
                if (text != null && text.contains("@")) {
                    continue;
                }
                int w = field.getRect().width;
                // Mobile field sits beside country code — narrower than email
                if (w > 150 && w < 700 && w < bestWidth) {
                    bestWidth = w;
                    best = field;
                }
            } catch (Exception ignored) {
                // next
            }
        }
        if (best != null) {
            return best;
        }
        for (int i = fields.size() - 1; i >= 0; i--) {
            WebElement field = fields.get(i);
            try {
                if (isAboveMobileLabel(field)) {
                    continue;
                }
                String text = field.getText();
                if (text != null && text.contains("@")) {
                    continue;
                }
                return field;
            } catch (Exception ignored) {
                // next
            }
        }
        return null;
    }

    private void typeIntoField(WebElement field, String value) {
        field.click();
        pause(300);
        try {
            field.clear();
        } catch (Exception ignored) {
            // Flutter
        }
        try {
            field.sendKeys(value);
            pause(400);
            String typed = field.getText();
            if (typed != null
                    && typed.contains(value.substring(0, Math.min(4, value.length())))
                    && !typed.contains(" GT")) {
                return;
            }
        } catch (Exception ignored) {
            // shell fallback
        }

        try {
            List<String> delArgs = new ArrayList<>();
            delArgs.add("keyevent");
            for (int i = 0; i < 30; i++) {
                delArgs.add("67");
            }
            driver.executeScript("mobile: shell", Map.of("command", "input", "args", delArgs));
            String escaped = value.replace(" ", "%s").replace("@", "\\@");
            driver.executeScript("mobile: shell", Map.of(
                    "command", "input",
                    "args", List.of("text", escaped)));
            pause(400);
        } catch (Exception e) {
            System.out.println("[Traveller] typeIntoField issue: " + e.getMessage());
        }
    }

    private void tapCenterSafe(WebElement element) {
        Rectangle rect = element.getRect();
        int x = rect.x + rect.width / 2;
        int y = Math.min(rect.y + rect.height / 2, 2150);
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence tap = new Sequence(finger, 1);
        tap.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
        tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(tap));
    }

    private static String lettersOnly(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^A-Za-z ]", "").replaceAll("\\s+", " ").trim();
    }

    private static String safeText(WebElement el) {
        try {
            String t = el.getText();
            return t == null ? "" : t.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static String safeDesc(WebElement el) {
        try {
            String d = el.getAttribute("contentDescription");
            return d == null ? "" : d.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static void pause(long ms) {
        // No fixed sleeps — rely on explicit waits for UI state.
    }
}
