# Location_BG_service_with_Activity_Recognition_Feature

## About API

The Location_BG_service_with_Activity_Recognition_Feature is a powerful library designed to provide a seamless background location service experience for Android applications. With this library, users can initiate a location service that operates in the background without disrupting the user interface. The service fetches the device's location at periodic intervals, which can be configured by the user. One of the standout features of this library is the integration of the Activity Recognition Feature (ARF), which allows users to set dynamic location fetching intervals based on their current activity.

The ARF leverages different user activities such as 'Still,' 'Walk,' 'Run,' 'Vehicle,' and more to adjust the location update interval. For instance, when the user is detected as 'Running,' the interval could be set to 2 minutes, while it might be reduced to 5 seconds when the user is 'In Vehicle.' This adaptive approach optimizes battery consumption and ensures location updates align with the user's actions.

## Features

- **Background Location Service**: The library offers a robust background location service that can run independently, ensuring that the user interface remains responsive.

- **Activity Recognition Feature**: The ARF dynamically adjusts location update intervals based on user activities, enhancing the service's efficiency and responsiveness.

- **Permissions**: The library simplifies permission management with the `getPermission()` method, allowing users to acquire all required permissions in one step.

- **Auto Start and Auto Stop**: The service can be configured to start and stop automatically at specified times, providing users with even more control over its behavior.

- **Reliable Background Operation**: The library maintains reliability even in the event of Android OS kill events, ensuring the service continues functioning as expected.

- **Configuration Flexibility**: Users can easily configure the library to suit their needs, tailoring parameters to their specific requirements.

- **Seamless Network Integration**: The `getLocationCallback()` function enables smooth integration with network calls. It provides latitude and longitude data at specified intervals, facilitating network requests and potential data storage on servers.

- **Operations Based on User Activity**: The library's `getActivityCallback()` method empowers users to perform various operations based on user activities, such as 'Still,' 'Walk,' 'Run,' and 'Vehicle.'

## Methods

- `setStartingTime(h: Int, m: Int, s: Int)`
- `setStopTime(h: Int, m: Int, s: Int)`
- `setInfoDialog(layout: Int, duration: Long)`
- `setNotificationLargeIcon(icon: Int)`
- `setNotificationSmallIcon(icon: Int)`
- `setNotificationTitle(title: String?)`
- `setNotificationContent(cont: String?)`
- `install()`
- `setDynamicLocationInterval(still: Long, walk: Long, run: Long, vehicle: Long, Unknown: Long)`
- `setStaticLocationIntervalInMillis(millis: Long)`
- `getPermission(context: Context)`
- `startService(context: Context)`
- `stopService()`
- `getLocationCallback(callback: (longitude: Double, latitude: Double) -> Unit)`
- `getActivityAndTransactionCallback(callback: (activity: String, transaction: String) -> Unit)`
- `registerLocationDriver(application: Application)`
- `withARF()`
- `withoutARF()`

## Getting Started

To use the Location_BG_service_with_Activity_Recognition_Feature library, follow these steps:

1. Include the library in your project by following these steps.

**Step 1: Add JitPack Repository**
In your project's root `build.gradle` file, add the JitPack repository to the `repositories` section:

```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

**Step 2: Add the Dependency**
In your app module's `build.gradle` file, add the dependency for the library:

```gradle
dependencies {
    implementation 'com.github.AkashSingh1505:Location_BG_service_with_ARF:Tag'
}
```

Replace `[Tag]` with the specific version tag you want to use.

2. Configure the desired settings using the provided methods.
3. Register the LocationDriver by calling `registerLocationDriver(application: Application)` in your application's file.
4. Initialize the service by calling `install()` and start it with `startService(context: Context)`.
5. Implement the necessary callbacks for location and activity updates using `getLocationCallback()` and `getActivityAndTransactionCallback()`.
6. Optionally, customize notification icons, content, and other parameters as needed.
7. Utilize the features provided by the library to enhance your application's background location functionality while optimizing battery efficiency.

Refer to the library's documentation for more detailed information on each method and its usage.


This library is provided by Akash Singh under the AkashSingh1505.

---

We hope this library proves to be a valuable addition to your Android application, enabling efficient background location services and dynamic activity-based location updates. If you have any questions, feedback, or issues, please don't hesitate to contact our support team.
