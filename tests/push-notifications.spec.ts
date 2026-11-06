import { test, expect, Page, Locator } from '@playwright/test';
import path from 'path';
import { spawn } from 'child_process';

const BANNER_IMAGE_PATH = path.resolve(__dirname, '../assets/banner.png');

/**
 * Non-blocking helper to bring the Chromium window to front if launched from a background runner.
 */
function bringChromiumToFrontAsync() {
  try {
    const psScript = `
Add-Type -TypeDefinition @'
using System;
using System.Text;
using System.Runtime.InteropServices;
public class Win32Show {
  public delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);
  [DllImport("user32.dll")] public static extern bool EnumWindows(EnumWindowsProc lpEnumFunc, IntPtr lParam);
  [DllImport("user32.dll", CharSet = CharSet.Auto)] public static extern int GetClassName(IntPtr hWnd, StringBuilder lpClassName, int nMaxCount);
  [DllImport("user32.dll", CharSet = CharSet.Auto)] public static extern int GetWindowText(IntPtr hWnd, StringBuilder lpString, int nMaxCount);
  [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
  public static void Unhide() {
    EnumWindows((hWnd, lParam) => {
      var cls = new StringBuilder(256);
      GetClassName(hWnd, cls, cls.Capacity);
      if (cls.ToString() == "Chrome_WidgetWin_1") {
        var title = new StringBuilder(512);
        GetWindowText(hWnd, title, title.Capacity);
        if (title.Length > 0) {
          ShowWindow(hWnd, 9);
          ShowWindow(hWnd, 3);
          SetForegroundWindow(hWnd);
        }
      }
      return true;
    }, IntPtr.Zero);
  }
}
'@
[Win32Show]::Unhide()
`;
    const encoded = Buffer.from(psScript, 'utf16le').toString('base64');
    spawn('powershell', ['-NoProfile', '-WindowStyle', 'Hidden', '-EncodedCommand', encoded], {
      detached: true,
      stdio: 'ignore',
      windowsHide: true,
    }).unref();
  } catch {
    // Ignore errors
  }
}

/**
 * Helper to visibly click and type text character-by-character into an input or textarea.
 */
async function typeVisibly(locator: Locator, text: string) {
  await locator.scrollIntoViewIfNeeded();
  await expect(locator).toBeVisible();
  await locator.click();
  await locator.fill('');
  await locator.pressSequentially(text, { delay: 45 });
}

/**
 * Helper to visibly open a custom <app-dropdown> component and click an option.
 */
async function selectDropdownOption(page: Page, dropdownLabel: string, optionText: string) {
  const dropdown = page.locator(`app-dropdown[label="${dropdownLabel}"]`);
  const input = dropdown.locator('.custom-select-input');
  await input.scrollIntoViewIfNeeded();
  await expect(input).toBeVisible();
  await input.click();

  const option = dropdown
    .locator('.custom-options-list .custom-option')
    .filter({ hasText: new RegExp(`^\\s*${optionText}\\s*$`, 'i') })
    .first();

  await option.scrollIntoViewIfNeeded();
  await expect(option).toBeVisible();
  await option.click();
  await expect(input).toHaveValue(new RegExp(optionText, 'i'));
}

/**
 * Helper to upload the banner image on every loop and show preview.
 */
async function uploadBannerImage(page: Page, imagePath: string) {
  const fileInput = page.locator('#notificationBannerImage');
  await fileInput.setInputFiles([]);
  await fileInput.setInputFiles(imagePath);
  await page.waitForTimeout(1000);
}

/**
 * Helper to click Send Notification, wait for confirmation dialog, and close it.
 */
async function submitNotification(page: Page) {
  const sendBtn = page.locator('button.push-notifications-btn', { hasText: 'Send Notification' });
  await sendBtn.scrollIntoViewIfNeeded();
  await expect(sendBtn).toBeEnabled();
  await sendBtn.click();

  // Wait for confirmation alert dialog and click "Ok"
  const dialog = page.getByRole('dialog');
  await expect(dialog).toBeVisible({ timeout: 30_000 });
  await page.waitForTimeout(800);
  const okButton = dialog.getByRole('button', { name: 'Ok' });
  await expect(okButton).toBeVisible();
  await okButton.click();

  // Ensure dialog and overlay backdrop disappear before proceeding to next loop
  await expect(dialog).toBeHidden();
  await expect(page.locator('.cdk-overlay-backdrop-showing')).toBeHidden();
}

test.describe('TravelHouse UK - Live Browser Push Notifications Flow (4 Loops)', () => {
  test('Login and send Global, Pending Payment, Region Wise, and Single User notifications live', async ({ page }) => {
    // 1. Navigate to Login Page & Authenticate immediately
    await test.step('Login to Admin Portal', async () => {
      await page.goto('https://assets-dev.travelhouseuk.co.uk/');
      bringChromiumToFrontAsync();
      await typeVisibly(page.locator('#email'), 'admin@traveluk.com');
      await typeVisibly(page.locator('#password'), '123123');
      await page.locator('button[type="submit"]', { hasText: 'Log In' }).click();

      await page.waitForURL('**/views/overviews**');
    });

    // 2. Navigate to Push Notifications from Side Menu
    await test.step('Open Push Notifications tab from Side Menu', async () => {
      const pushNotificationsMenu = page.locator('a[href="/views/push-notifications"]').first();
      await expect(pushNotificationsMenu).toBeVisible();
      await pushNotificationsMenu.click();

      await page.waitForURL('**/views/push-notifications**');
      await expect(page.locator('#notificationTitle')).toBeVisible();
    });

    // Loop 1: Global Notification
    await test.step('Loop 1: Global Notification', async () => {
      await typeVisibly(page.locator('#notificationTitle'), 'Global Notification');
      await typeVisibly(
        page.locator('#notificationMessage'),
        'Special global flight deals are now live! Book your next international flight with TravelHouse UK.'
      );

      await selectDropdownOption(page, 'Target Users', 'Global');
      await uploadBannerImage(page, BANNER_IMAGE_PATH);
      await submitNotification(page);
    });

    // Loop 2: Pending Payment Notification
    await test.step('Loop 2: Pending Payment Notification', async () => {
      await typeVisibly(page.locator('#notificationTitle'), 'Pending Payment');
      await typeVisibly(
        page.locator('#notificationMessage'),
        'Your flight booking has a pending payment. Please complete your payment to confirm your flight ticket.'
      );

      await selectDropdownOption(page, 'Target Users', 'Pending Payment');
      await uploadBannerImage(page, BANNER_IMAGE_PATH);
      await submitNotification(page);
    });

    // Loop 3: Region Wise Notification (Asia -> Pakistan -> Islamabad)
    await test.step('Loop 3: Region Wise Notification (Asia > Pakistan > Islamabad)', async () => {
      await typeVisibly(page.locator('#notificationTitle'), 'Region Wise');
      await typeVisibly(
        page.locator('#notificationMessage'),
        'Exclusive flight offers for Islamabad, Pakistan! Book your discounted flights from Asia today.'
      );

      await selectDropdownOption(page, 'Target Users', 'Region-Wise');
      await selectDropdownOption(page, 'Continent', 'Asia');
      await selectDropdownOption(page, 'Country', 'Pakistan');
      await selectDropdownOption(page, 'City', 'Islamabad');

      await uploadBannerImage(page, BANNER_IMAGE_PATH);
      await submitNotification(page);
    });

    // Loop 4: Single User Notification (muqadasshoaib123@gmail.com)
    await test.step('Loop 4: Single User Notification (muqadasshoaib123@gmail.com)', async () => {
      await typeVisibly(page.locator('#notificationTitle'), 'Single User');
      await typeVisibly(
        page.locator('#notificationMessage'),
        'Special personal flight discount voucher for your upcoming TravelHouse UK flight booking.'
      );

      await selectDropdownOption(page, 'Target Users', 'Single User');

      const emailField = page.locator('#notificationEmail');
      await typeVisibly(emailField, 'muqadasshoaib123@gmail.com');

      await uploadBannerImage(page, BANNER_IMAGE_PATH);
      await submitNotification(page);
    });

    // Show History tab at the end
    await test.step('Show History tab with all 4 notifications', async () => {
      const historyTab = page.locator('button.toggle-btn', { hasText: 'History' });
      await historyTab.click();
      await page.waitForTimeout(4000);
    });
  });
});
