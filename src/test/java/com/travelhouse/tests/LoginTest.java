package com.travelhouse.tests;

import com.travelhouse.base.BaseTest;
import com.travelhouse.base.DriverManager;
import com.travelhouse.config.Credentials;
import com.travelhouse.pages.HomePage;
import com.travelhouse.pages.LoginPage;
import com.travelhouse.pages.OnboardingPage;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest {

    @Test(description = "Skip onboarding and log in to Travel House with configured credentials")
    public void login_withValidCredentials_shouldEnterApp() {
        if (!Credentials.isConfigured()) {
            throw new SkipException(
                    "Set login.email and login.password in config.properties before running login tests");
        }

        HomePage home = new HomePage();
        home.waitForUiReady();

        OnboardingPage onboarding = new OnboardingPage();
        if (onboarding.isVisible()) {
            onboarding.dismissToLogin();
        }

        LoginPage login = new LoginPage();
        try {
            login.waitUntilVisible();
        } catch (Exception e) {
            System.out.println("[Login] Page source:\n" + DriverManager.getDriver().getPageSource());
            throw e;
        }
        Assert.assertTrue(login.isLoginScreenVisible(), "Expected Travel House login screen");

        login.login(Credentials.email(), Credentials.password());

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        home.waitForUiReady();

        Assert.assertFalse(login.isStillOnLogin(),
                "Still on login screen after submitting credentials — check email/password or post-login UI");
        Assert.assertTrue(home.isAnyPrimaryUiVisible(), "Expected app UI after successful login");
        System.out.println("[Login] Login succeeded — package=" + home.getCurrentPackage()
                + " activity=" + home.getCurrentActivity());
    }
}
