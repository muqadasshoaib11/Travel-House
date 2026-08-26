import { remote } from 'webdriverio';
import { config } from './config.js';

export async function createMobileSession() {
  return remote({
    ...config.appium,
    logLevel: 'warn',
    capabilities: {
      platformName: 'Android',
      'appium:automationName': 'UiAutomator2',
      'appium:udid': config.android.udid,
      'appium:deviceName': config.android.deviceName,
      'appium:app': config.android.apkPath,
      'appium:appPackage': config.android.appPackage,
      'appium:appActivity': config.android.appActivity,
      'appium:noReset': true,
      'appium:autoGrantPermissions': false,
      'appium:newCommandTimeout': 900,
      'appium:uiautomator2ServerInstallTimeout': 120000,
      'appium:adbExecTimeout': 120000
    }
  });
}
