package com.travelhouse.utils;

import com.travelhouse.base.DriverManager;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.Collections;

public final class GestureUtil {

    private GestureUtil() {
    }

    public static void swipeUp() {
        swipe(0.5, 0.75, 0.5, 0.25);
    }

    public static void swipeDown() {
        swipe(0.5, 0.25, 0.5, 0.75);
    }

    public static void swipe(double startXRatio, double startYRatio, double endXRatio, double endYRatio) {
        AndroidDriver driver = DriverManager.getDriver();
        Dimension size = driver.manage().window().getSize();
        Point start = new Point((int) (size.width * startXRatio), (int) (size.height * startYRatio));
        Point end = new Point((int) (size.width * endXRatio), (int) (size.height * endYRatio));

        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 1);
        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), start.x, start.y));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(new Pause(finger, Duration.ofMillis(200)));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(600), PointerInput.Origin.viewport(), end.x, end.y));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(swipe));
    }

    public static void scrollToText(String text) {
        DriverManager.getDriver().findElement(AppiumBy.androidUIAutomator(
                "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains(\""
                        + text + "\"))"));
    }
}
