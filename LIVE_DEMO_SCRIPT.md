# FitTrackerPro — Live Demonstration & Video Presentation Script

This script is structured around the **6 official rubric areas** so you hit every grading point in a logical, 6–8 minute flow while showing both the **running emulator** and the **exact Java/XML files in Android Studio**.

---

## Before You Start (30-Second Pre-Flight Checklist)
1. **Android Studio**: Open `D:\FitTrackerPro` and keep these files open in tabs at the top so you can click them instantly when asked:
   - [`MainActivity.java`](file:///D:/FitTrackerPro/app/src/main/java/com/example/fittrackerpro/activities/MainActivity.java) (Navigation & ViewBinding)
   - [`ProgressActivity.java`](file:///D:/FitTrackerPro/app/src/main/java/com/example/fittrackerpro/activities/ProgressActivity.java) (Implicit Intent `ACTION_SEND`)
   - [`MealLogAdapter.java`](file:///D:/FitTrackerPro/app/src/main/java/com/example/fittrackerpro/adapters/MealLogAdapter.java) (RecyclerView with multiple view types)
   - [`ActivityTrackingService.java`](file:///D:/FitTrackerPro/app/src/main/java/com/example/fittrackerpro/services/ActivityTrackingService.java) (Foreground Service + Notification tray updates)
   - [`TrackingActivity.java`](file:///D:/FitTrackerPro/app/src/main/java/com/example/fittrackerpro/activities/TrackingActivity.java) (Runtime Permission + Dynamic Low-Battery BroadcastReceiver)
   - [`ProfilePhotoHelper.java`](file:///D:/FitTrackerPro/app/src/main/java/com/example/fittrackerpro/utils/ProfilePhotoHelper.java) (Content Provider `ContentResolver.openInputStream` + Picture storage)
   - [`TipsXmlParser.java`](file:///D:/FitTrackerPro/app/src/main/java/com/example/fittrackerpro/utils/TipsXmlParser.java) (XML Parsing with `XmlPullParser`)
   - [`SyncWorker.java`](file:///D:/FitTrackerPro/app/src/main/java/com/example/fittrackerpro/workers/SyncWorker.java) (WorkManager + Firebase Firestore sync)
2. **Browser**: Keep your **Firebase Console** open to **Authentication** and **Firestore Database (`users` collection)**.
3. **Emulator**: Keep the **Extended Controls (`...`) → Battery** window open on the side (set to 80% initially, ready to drag down to 15% during Part 4).

---

## PART 1: Overview & Features (1 Minute)

### What to Show on Screen
- Launch **FitTrackerPro** showing the custom splash screen (`SplashActivity`), then land on the **Home Dashboard** (`HomeFragment`).

### What to Say
> *"Hello everyone, this is **FitTrackerPro**, an all-in-one fitness, workout, and nutrition tracking Android application built in Java using **MVVM architecture** and **Room SQLite** with **Firebase Cloud backup**.*
> *The app solves a practical problem: instead of using separate apps for calorie counting, workout plans, and activity timers, FitTrackerPro calculates personalized BMR, TDEE, calorie, and macronutrient targets using the Mifflin-St Jeor formula (`CalorieCalculator.java`), generates a 7-day workout schedule, tracks meals and hydration, runs live timed sessions in a Foreground Service, and charts long-term weight and BMI trends."*

### Quick Feature Tour on the Home Tab
1. Point out the **3 custom-drawn Canvas rings** (`CircularProgressView`): **Calories (Orange)**, **Steps (Cyan)**, and **Active Time (Emerald)**.
2. Point out the **Weekly Goal** progress bar and **Today's Schedule** card.
3. Tap **+250 ml** on the Hydration strip to show live reactive updates via `LiveData`.

---

## PART 2: Architecture, Multi-Activity Navigation, Intents & ViewBinding (1 Minute)

### What to Show on Screen
1. In the app, tap **Open Analytics & Weight Tracker** on Home to open `ProgressActivity`, then tap **Share My Progress Summary** to open the Android system share chooser.
2. In Android Studio, briefly show [`MainActivity.java`](file:///D:/FitTrackerPro/app/src/main/java/com/example/fittrackerpro/activities/MainActivity.java) and [`ProgressActivity.java`](file:///D:/FitTrackerPro/app/src/main/java/com/example/fittrackerpro/activities/ProgressActivity.java).

### What to Say
> *"For architecture and navigation:*
> - ***MVVM + Repository Pattern**: Every screen observes `LiveData` from a `ViewModel` (`DashboardViewModel`, `WorkoutViewModel`, `NutritionViewModel`, `ActivityViewModel`, `ProgressViewModel`), which talks to Repositories backed by Room and Firebase.*
> - ***ViewBinding & Event Handling**: We enabled `viewBinding true` in `app/build.gradle`. Every Activity and Fragment inflates its generated binding class (like `FragmentHomeBinding` or `ActivityProgressBinding`)—eliminating `findViewById` and preventing `NullPointerException`s. In Fragments, we set `binding = null` in `onDestroyView()` to avoid memory leaks.*
> - ***Explicit Intents**: Used for multi-Activity navigation across our 13 Activities—for example, navigating from `HomeFragment` to `ProgressActivity`, `TrackingActivity`, or `WorkoutDayActivity` passing `EXTRA_PLAN_ID` and `EXTRA_DAY`.*
> - ***Implicit Intent**: Here in `ProgressActivity.shareProgressImplicitIntent()`, tapping **Share My Progress Summary** constructs an `Intent(Intent.ACTION_SEND)` with `text/plain` and launches `Intent.createChooser(...)` so the user can share their streak, weight, and BMI to WhatsApp, Gmail, or Messages."*

---

## PART 3: UI Design, Custom Views & Dynamic Lists (`RecyclerView`) (1.5 Minutes)

### What to Show on Screen
1. Show the **Nutrition** tab (`NutritionFragment`): scroll through Breakfast, Lunch, Evening Snack, and Dinner. Tap **+ Add** on Breakfast (`AddFoodActivity`), add a food, then **swipe it left** to delete and tap **UNDO** on the `Snackbar`.
2. Show [`MealLogAdapter.java`](file:///D:/FitTrackerPro/app/src/main/java/com/example/fittrackerpro/adapters/MealLogAdapter.java) and [`TrendChartView.java`](file:///D:/FitTrackerPro/app/src/main/java/com/example/fittrackerpro/customviews/TrendChartView.java) in Android Studio.

### What to Say
> *"For UI layout design and dynamic lists:*
> - ***Layouts & Custom Canvas Views**: Our screens combine `LinearLayout`, `ConstraintLayout`, `ScrollView`, and `NestedScrollView` with Material 3-inspired dark slate cards. Instead of using external chart libraries, we wrote two custom `View` classes from scratch using `Canvas` and `Paint`: `CircularProgressView` (which supports single rings and concentric dual-rings on the Nutrition screen) and `TrendChartView` (which draws smooth cubic Bezier curves with gradient area fills in `MODE_LINE` and gradient pill bars in `MODE_BAR`).*
> - ***RecyclerView, Adapters & Multiple ViewHolders**: We have 5 `RecyclerView` adapters (`ExerciseAdapter`, `PlanDayAdapter`, `MealLogAdapter`, `ActivityLogAdapter`, `WeightHistoryAdapter`). Most notably, `MealLogAdapter` implements **multiple view types** (`TYPE_HEADER` for meal section headers and `TYPE_ITEM` for logged food items) inside a single `RecyclerView`, plus `ItemTouchHelper` swipe-to-delete with an **UNDO** `Snackbar`."*

---

## PART 4: Android System Components & Runtime Permissions (2 Minutes)
*(This is the highest-weighted rubric section—demonstrate all 3 components clearly!)*

### 4A. Foreground Service with Live Notification Tray Updates + Runtime Permission
1. Go to **Activity → Start Timed Session** (`TrackingActivity`).
2. Tap **Start Session**.
   - Point out the **Android 13+ Runtime Permission (`POST_NOTIFICATIONS`)** rationale dialog (`checkPermissionAndStart()` in `TrackingActivity.java`), and grant permission.
3. Pull down the **Android Notification Shade** at the top of the screen:
   - Show the persistent **Foreground Service notification** (`ActivityTrackingService.java`, `foregroundServiceType="specialUse"`) ticking live with a chronometer and **Pause / Resume / Stop** buttons (`PendingIntent` actions).
   - Tap **Pause** and **Resume** right inside the notification tray to show two-way communication between the notification and the UI via `ActivitySessionStore`.

### 4B. Broadcast Receivers (Dynamic Low-Battery + Static Boot Receiver)
1. While `TrackingActivity` is still running on screen, open the Emulator's **Extended Controls (`...`) → Battery**.
2. Drag the **Charge level** slider down from 80% to **15%**.
3. Point out the amber **Low battery (15%)!** warning card and Toast that immediately appear inside `TrackingActivity`!
   - **Explain**: *"In `TrackingActivity.java`, we dynamically register `lowBatteryReceiver` in `onStart()` and unregister it in `onStop()` listening for `ACTION_BATTERY_LOW` and `ACTION_BATTERY_CHANGED`. When battery drops to 15% or below during a workout, the app warns the user to save their session before the phone shuts off. We also have a static `BootReminderReceiver` registered in `AndroidManifest.xml` for `BOOT_COMPLETED` that re-schedules all daily reminders after a device restart."*
4. Drag battery back up, tap **Finish & Save** on the timer, and save the session to Room.

### 4C. Content Provider (Android Photo Picker) + Picture Storage + WorkManager
1. Go to the **Profile** tab (`ProfileFragment`).
2. Tap **Change Photo** (or tap the circular avatar).
3. Pick an image from the **Android Photo Picker**.
   - **Explain**: *"This demonstrates both **Content Providers** and **Media/Picture Storage**. `ActivityResultContracts.PickVisualMedia` returns a `content://` URI from Android's `MediaStore` Content Provider. In `ProfilePhotoHelper.java`, we call `context.getContentResolver().openInputStream(uri)` to stream the image bytes from the Content Provider, center-crop and scale the `Bitmap` to 512×512, and save it into private internal app storage (`context.getFilesDir()/avatar_<uid>.jpg`). You can see the saved photo updates on both the Profile tab and the Home dashboard header."*
4. Tap **Reminders & Notifications** (`SettingsActivity`), toggle on **Workout Reminder**, and tap **Send test reminder now** to show **`WorkManager` (`ReminderWorker`)** firing a background notification immediately.

---

## PART 5: Networking (JSON & XML Parsing) and Mandatory Firebase Services (1.5 Minutes)

### 5A. HTTP & JSON Parsing (Retrofit + WorkoutX API)
1. Go to **Workout → Library** (`ExerciseLibraryActivity`) and tap an exercise (`ExerciseDetailActivity`).
2. **Explain**: *"We use **Retrofit 2**, **OkHttp**, and **Gson** (`WorkoutXApi.java` & `RetrofitClient.java`) to make HTTP GET requests to the WorkoutX API, authenticating via our API key stored safely in `local.properties`. The JSON response is parsed into `Exercise` models and cached in our Room database so exercises and animations load even offline."*

### 5B. XML Parsing (`XmlPullParser`)
1. Go to the **Home** tab, scroll to the bottom **Daily Tip • XML** card, and tap **Next tip ›** a couple of times.
2. **Explain**: *"To demonstrate native **XML parsing**, we bundled `res/xml/fitness_tips.xml` and parsed it in `TipsXmlParser.java` using Android's `XmlPullParser` (`START_TAG`, `TEXT`, `END_TAG` loop) to display categorized training, hydration, and nutrition tips on the Home screen."*

### 5C. Firebase Authentication & Cloud Firestore Sync
1. Go to the **Profile** tab and point to the **Cloud Backup (Firestore)** card.
2. Tap **Sync to Cloud Now**. Watch the status change from `Syncing to Firestore...` to `All synced`.
3. Switch to your browser showing the **Firebase Console**:
   - Show **Authentication → Users** (your registered email & UID).
   - Show **Firestore Database → Data → `users/{uid}`** and its subcollections (`activity_logs`, `workout_sessions`, `weight_history`), plus the **Rules** tab (`request.auth.uid == userId`).
   - **Explain**: *"Every Room table has an `isSynced` flag. When we tap Sync, `SyncWorker` runs on a background thread via `WorkManager`, batches all unsynced rows into a single atomic Firestore `WriteBatch` under `users/{userId}`, commits them to the cloud, and marks `isSynced = 1` in Room."*

---

## PART 6: Layout Responsiveness & Rotation Support (30 Seconds)

### What to Show on Screen
1. Rotate the emulator to **Landscape mode** (`Ctrl + Left Arrow` or click the Rotate icon on the emulator sidebar) while on the **Home** or **Analytics (`ProgressActivity`)** screen.
2. Scroll smoothly through the cards and show that the custom rings (`CircularProgressView`) and charts (`TrendChartView`) automatically resize to fit the wider screen without losing any state, then rotate back to Portrait.

### What to Say
> *"Finally, for layout responsiveness: all screens are wrapped in `ScrollView` or `NestedScrollView` with weighted `LinearLayout` columns, our custom Canvas views dynamically calculate their dimensions in `onDraw()` using `getWidth()`, `getHeight()`, and display density (`dp`), and because our UI state is held in lifecycle-aware `ViewModel`s and `MainActivity` checks `if (savedInstanceState == null)` before resetting the fragment, the app survives screen rotation seamlessly."*
