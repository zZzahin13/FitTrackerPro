# FitTrackerPro — Android Fitness, Workout & Nutrition Tracker

**FitTrackerPro** is a native Java/XML Android application (`minSdk 26`, `targetSdk 34`) built with **MVVM + Repository Architecture**, **Room SQLite Database**, **Firebase Authentication & Cloud Firestore**, **WorkoutX REST API (Retrofit)**, **Foreground Services**, **WorkManager**, **BroadcastReceivers**, **Content Providers (Android Photo Picker)**, and **Custom Canvas Views**.

---

## Key Features

1. **Authentication & Personalized Health Assessment**
   - Email/Password authentication with **Firebase Auth** (`LoginActivity`, `RegisterActivity`, `ForgotPasswordActivity`).
   - Multi-field fitness assessment (`AssessmentActivity`) computing **BMI**, **BMR (Mifflin-St Jeor)**, **TDEE**, daily calorie targets, and macro goals (`Protein`, `Carbs`, `Fats`) via `CalorieCalculator` and `BMIUtil`.

2. **Home Dashboard & Custom Canvas Views**
   - Live dashboard (`HomeFragment`) with time-of-day greeting, profile avatar shortcut, and **3 multi-color `CircularProgressView` rings** (Calories in Orange, Steps in Cyan, Active Time in Emerald).
   - Weekly Goal progress bar, Today's Scheduled Workout card, quick `+250 ml` water logging, 7-day active minutes bar chart (`TrendChartView`), and daily fitness tips parsed from bundled XML (`res/xml/fitness_tips.xml`) via `XmlPullParser`.

3. **Workout Library (WorkoutX API) & 7-Day Plan Generator**
   - Browse exercises with animated demonstrations fetched via **Retrofit + Gson** and cached locally in Room (`ExerciseLibraryActivity`, `ExerciseDetailActivity`).
   - Automatic 7-day workout split generator (`PlanGenerator`) with interactive workout execution timers and session history (`WorkoutDayActivity`, `WorkoutHistoryActivity`).

4. **Nutrition & Hydration Tracker**
   - Concentric dual-ring calorie & hydration display (`NutritionFragment`), color-coded macro progress bars (`Protein`, `Carbs`, `Fats`), meal-by-meal food logging (`AddFoodActivity`), swipe-to-delete with Undo, and 7-day calorie summary.

5. **Activity Log & Foreground Service Live Session Timer**
   - Manual activity logging (`ActivityFragment`) plus a live timed session (`TrackingActivity`) powered by a persistent **Foreground Service** (`ActivityTrackingService`, `foregroundServiceType="specialUse"`).
   - Ongoing notification with live chronometer and **Pause / Resume / Stop** `PendingIntent` action buttons, plus Android 13+ `POST_NOTIFICATIONS` permission rationale flow.

6. **Progress Analytics, Weight History & Implicit Intent Sharing**
   - Smooth cubic Bezier curve line chart (`TrendChartView.MODE_LINE`) with glowing gradient fill for **Weight (kg)** and **BMI** history (`ProgressActivity`).
   - 7-day and 30-day vertical gradient bar charts (`TrendChartView.MODE_BAR`) for daily calories and active minutes.
   - Active day streak counter and **Implicit Intent (`Intent.ACTION_SEND`)** progress sharing.

7. **Reminders, WorkManager & BroadcastReceivers**
   - Configurable daily **Workout**, **Water**, and **Meal** reminders (`SettingsActivity`, `ReminderScheduler`, `ReminderWorker`).
   - Static `BootReminderReceiver` (`BOOT_COMPLETED`) that restores scheduled reminders after device reboot.
   - Dynamic `lowBatteryReceiver` (`ACTION_BATTERY_LOW` / `ACTION_BATTERY_CHANGED`) inside `TrackingActivity` that warns the user to save their session when battery drops to `<= 15%`.

8. **Cloud Firestore Sync & Profile Photo Picker (Content Provider)**
   - Background `SyncWorker` uploads unsynced `UserProfile`, `ActivityLog`, `WorkoutSession`, and `WeightHistory` records (`isSynced == 0`) to **Firebase Firestore** under `users/{userId}/...`, protected by `firestore.rules`.
   - Profile photo selection via the **Android Photo Picker** (`ActivityResultContracts.PickVisualMedia`), read through `ContentResolver.openInputStream(uri)` and stored in private internal storage (`ProfilePhotoHelper`).

---

## Setup Instructions

1. **Clone & Open in Android Studio**
   - Open `D:\FitTrackerPro` in Android Studio (Hedgehog or newer recommended) with JDK 17.

2. **Configure `local.properties` (WorkoutX API Key)**
   - In the project root `local.properties` (ignored by Git), add your WorkoutX API key:
     ```properties
     sdk.dir=C\:\\Users\\<YourUser>\\AppData\\Local\\Android\\Sdk
     workoutx.api.key=YOUR_WORKOUTX_API_KEY
     ```
   - Even without a key, the app includes built-in fallback exercises so all workout features work offline.

3. **Firebase Configuration**
   - Place your `google-services.json` inside `app/google-services.json`.
   - In the Firebase Console:
     - Enable **Authentication → Email/Password**.
     - Enable **Cloud Firestore** and publish the security rules from [`firestore.rules`](file:///D:/FitTrackerPro/firestore.rules).

4. **Build & Run**
   - Click **Sync Project with Gradle Files**, then press **`Shift + F10`** to run on an Android 8.0+ (API 26–34) emulator or physical device.

---

## Recommended Screenshots for Your Project Report

1. **Home Dashboard**: Showing the 3 multi-color rings (Calories, Steps, Active Time), Weekly Goal bar, Today's Schedule, and XML Daily Tip card.
2. **Live Activity Timer & Notification Shade**: Showing `TrackingActivity` running alongside the persistent Foreground Service notification with Pause/Stop buttons.
3. **Analytics & Charts (`ProgressActivity`)**: Showing the smooth Bezier Weight/BMI curve, 7d/30d gradient bar chart, and the Android Share Sheet (`Intent.ACTION_SEND`).
4. **Nutrition Tracker**: Showing the concentric dual-ring, Protein/Carbs/Fats progress bars, and logged meals.
5. **Exercise Library & Workout Day**: Showing WorkoutX exercise animations and the 7-day plan schedule.
6. **Profile, Cloud Sync & Reminders**: Showing custom profile photo, `"All synced"` Firestore badge, and `SettingsActivity` WorkManager reminders.
