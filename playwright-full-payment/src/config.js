import 'dotenv/config';

const required = (name, fallback) => {
  const value = process.env[name] || fallback;
  if (!value) throw new Error(`Missing required environment variable: ${name}`);
  return value;
};

export const config = {
  appium: {
    hostname: process.env.APPIUM_HOST || '127.0.0.1',
    port: Number(process.env.APPIUM_PORT || 4723),
    path: '/'
  },
  android: {
    udid: required('ANDROID_UDID', '8797df69'),
    deviceName: process.env.ANDROID_DEVICE_NAME || 'Redmi 23129RAA4G',
    apkPath: required('APK_PATH', 'E:/app-release.apk'),
    appPackage: process.env.APP_PACKAGE || 'com.travelhouse.uk.app',
    appActivity: process.env.APP_ACTIVITY || 'com.example.travel_house.MainActivity'
  },
  account: {
    email: required('TEST_EMAIL'),
    password: required('TEST_PASSWORD')
  },
  traveller: {
    savedName: process.env.TRAVELLER_NAME || 'Muqadas Shoaib',
    frequentFlyerName: process.env.FREQUENT_FLYER_NAME || 'Muqadas',
    mobile: process.env.MOBILE_NUMBER || '3001234567'
  },
  flight: {
    from: 'London',
    to: process.env.DESTINATION || 'Islamabad',
    departure: '2027-03-02',
    returning: '2027-03-30'
  }
};
