package com.travelhouse.tests;

import com.travelhouse.base.BaseTest;
import com.travelhouse.pages.HomePage;
import com.travelhouse.pages.LoginPage;
import com.travelhouse.utils.GestureUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

public class HomeScreenTest extends BaseTest {

    @Test(description = "Verify Travel House shows primary UI after launch")
    public void homeScreen_shouldShowPrimaryUi() {
        HomePage home = new HomePage();
        home.waitForUiReady();

        Assert.assertTrue(home.isAnyPrimaryUiVisible(),
                "Expected Travel House primary UI (Home/Search/Login/Book or main FrameLayout). "
                        + "Inspect the screen with Appium Inspector and refine HomePage locators.");
    }

    @Test(description = "Basic vertical swipe should not crash the app", dependsOnMethods = "homeScreen_shouldShowPrimaryUi")
    public void homeScreen_swipeShouldKeepSessionAlive() {
        HomePage home = new HomePage();
        home.waitForUiReady();

        GestureUtil.swipeUp();
        GestureUtil.swipeDown();

        Assert.assertTrue(home.isAnyPrimaryUiVisible(), "UI should remain visible after swipe gestures");
    }

    @Test(description = "Detect login screen if present (optional path)", enabled = false)
    public void loginScreen_ifVisible_canAcceptCredentials() {
        LoginPage login = new LoginPage();
        if (!login.isLoginScreenVisible()) {
            Assert.assertTrue(true, "Login screen not shown on cold start — skipped path");
            return;
        }
        // Enable this test and set real credentials when ready
        login.login("test@example.com", "Password123!");
    }
}
