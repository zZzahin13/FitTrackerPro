# FitTrackerPro — Viva Preparation & Demo Guide

This guide provides **one concise, code-backed answer per syllabus topic**, a **system architecture diagram**, and a **5-to-7 minute live demo script** for your defense.

---

## 1. Architecture Diagram

```
+-------------------------------------------------------------------------+
|                           UI LAYER (Activities & Fragments)             |
|  HomeFragment | WorkoutFragment | NutritionFragment | ActivityFragment  |
|  ProfileFragment | TrackingActivity | ProgressActivity | SettingsActivity|
|  Custom Views: CircularProgressView, TrendChartView (Canvas onDraw)     |
+------------------------------------+------------------------------------+
                                     | observes LiveData<T> (ViewBinding)
                                     v
+-------------------------------------------------------------------------+
|                           VIEWMODEL LAYER (AndroidViewModel)            |
|  DashboardViewModel | WorkoutViewModel | NutritionViewModel             |
|  ActivityViewModel  | ProgressViewModel                                 |
+------------------------------------+------------------------------------+
                                     | calls async methods
                                     v
+-------------------------------------------------------------------------+
|                           REPOSITORY LAYER                              |
|  UserRepository | WorkoutRepository | NutritionRepository               |
|  ActivityRepository | WeightRepository                                  |
+-----------+------------------------+------------------------+-----------+
            |                        |                        |
            v                        v                        v
+-----------------------+  +-------------------+  +-----------------------+
|   LOCAL DATA (Room)   |  | REMOTE REST API   |  | CLOUD & BACKGROUND    |
|  FitTrackerDatabase   |  | Retrofit + Gson   |  | Firebase Auth         |
|  7 Entities, 7 DAOs   |  | WorkoutXApi       |  | Firebase Firestore    |
|  dbExecutor (4 thrds) |  | Animation/Ex sync |  | WorkManager:          |
+-----------------------+  +-------------------+  |  - SyncWorker         |
                                                  |  - ReminderWorker     |
                                                  | Foreground Service:   |
                                                  |  - ActivityTrackingSvc|
                                                  +-----------------------+
```

---

## 2. One-Paragraph Viva Answer Per Syllabus Topic

### 1. Project Structure, Gradle, Manifest, Lifecycle & ViewBinding
> **Answer:** We enabled `viewBinding true` and `buildConfig true` in `app/build.gradle`, reading the `WORKOUTX_API_KEY` safely from `local.properties` so secrets are never committed to Git. In Fragments (such as `HomeFragment` and `ProfileFragment`), we inflate the binding in `onCreateView()` and null out `binding = null` in `onDestroyView()` to prevent memory leaks when the Fragment's view outlives its container.

### 2. Explicit vs. Implicit Intents
> **Answer:** We use **Explicit Intents** whenever navigating to our own components—for example, `new Intent(requireContext(), ProgressActivity.class)` or starting `ActivityTrackingService` with `ACTION_START`. We use an **Implicit Intent** in `ProgressActivity.shareProgressImplicitIntent()`, where we create `new Intent(Intent.ACTION_SEND)` with MIME type `"text/plain"`, attach the formatted fitness summary in `Intent.EXTRA_TEXT`, and wrap it in `Intent.createChooser(...)` so Android lets the user share to WhatsApp, Gmail, or Messages.

### 3. RecyclerView, ViewHolders, Multiple View Types & UX
> **Answer:** In `NutritionFragment`, `MealLogAdapter` implements **multiple view types** (`TYPE_HEADER` for meal section headers like Breakfast/Lunch/Dinner and `TYPE_ITEM` for logged food items) inside a single `RecyclerView`. Both `NutritionFragment`, `ActivityFragment`, and `ProgressActivity` attach an `ItemTouchHelper.SimpleCallback` for **swipe-to-delete** and show a Material `Snackbar` with an **"UNDO"** action that re-inserts the row with its original primary key (`OnConflictStrategy.REPLACE`). Notice in `MealLogAdapter` we override `getSwipeDirs()` to return `0` for `TYPE_HEADER` so section headers cannot be swiped away.

### 4. Custom Views (`Canvas` & `Paint`)
> **Answer:** We built two custom `View` classes from scratch without third-party chart libraries:
> 1. `CircularProgressView`: Pre-allocates `Paint` objects in `init()` (never allocating inside `onDraw`), computes the bounding `RectF` from `getWidth()`/`getHeight()`, draws a circular track and progress arc via `canvas.drawArc(...)`, supports custom colors for the 3-ring Home card and a concentric dual-ring mode for Nutrition, and calls `invalidate()` when data updates.
> 2. `TrendChartView`: Supports both `MODE_BAR` (drawing vertical cyan-to-emerald `LinearGradient` rounded bars over dark pill tracks) and `MODE_LINE` (drawing a smooth cubic Bezier curve via `Path.cubicTo(...)` with a gradient area fill underneath).

### 5. Foreground Services & Runtime Permissions
> **Answer:** `ActivityTrackingService` is a Foreground Service declared in `AndroidManifest.xml` with `android:foregroundServiceType="specialUse"` (required on Android 14 / API 34) and `FOREGROUND_SERVICE_SPECIAL_USE` permission. When started from `TrackingActivity`, it calls `startForeground(NOTIFICATION_ID, notification)` within 5 seconds, posting an ongoing notification on `CHANNEL_SESSION` (`IMPORTANCE_LOW`) with `setUsesChronometer(true)` and `PendingIntent` action buttons for **Pause/Resume** and **Stop**. On Android 13+ (`TIRAMISU`), `TrackingActivity` explains and requests `Manifest.permission.POST_NOTIFICATIONS` via `ActivityResultContracts.RequestPermission()`.

### 6. Broadcast Receivers (Static + Dynamic)
> **Answer:** We implemented both types of `BroadcastReceiver`:
> 1. **Static Receiver (`BootReminderReceiver`)**: Registered in `AndroidManifest.xml` with `RECEIVE_BOOT_COMPLETED` for `Intent.ACTION_BOOT_COMPLETED`. When the phone restarts, it calls `ReminderScheduler.rescheduleAllEnabled(context)` to restore scheduled reminders.
> 2. **Dynamic Receiver (`lowBatteryReceiver` in `TrackingActivity`)**: Registered in `onStart()` and unregistered in `onStop()` for `Intent.ACTION_BATTERY_LOW` and `Intent.ACTION_BATTERY_CHANGED`. If battery drops to `<= 15%` during a live session, it shows an on-screen warning banner so the user can save their session before the phone powers off.

### 7. Content Providers & Android Photo Picker
> **Answer:** In `ProfileFragment`, we use `ActivityResultContracts.PickVisualMedia` (`ImageOnly`) to let the user pick a profile photo without requesting broad storage permissions. The system picker returns a `content://` URI backed by Android's `MediaStore` **Content Provider**. In `ProfilePhotoHelper.copyUriToInternalStorage()`, we pass that URI to `context.getContentResolver().openInputStream(uri)`, center-crop and scale the decoded `Bitmap` to `512x512`, and save it into private internal storage (`context.getFilesDir()`) so it persists across restarts.

### 8. Retrofit, HTTP & JSON Parsing
> **Answer:** `RetrofitClient` configures a singleton `Retrofit` instance with `GsonConverterFactory` and an OkHttp interceptor that attaches the WorkoutX API key from `BuildConfig.WORKOUTX_API_KEY`. `WorkoutRepository` fetches exercises asynchronously, maps the JSON response into Room `Exercise` entities, and caches them locally in SQLite so the Exercise Library works even when offline.

### 9. XML Parsing (`XmlPullParser`)
> **Answer:** We bundled `res/xml/fitness_tips.xml` containing `<tip category="..." title="...">...</tip>` elements. In `TipsXmlParser.loadTips(context)`, we obtain an `XmlResourceParser` via `context.getResources().getXml(R.xml.fitness_tips)` and iterate through `START_TAG`, `TEXT`, and `END_TAG` events until `END_DOCUMENT`, displaying the parsed tips on `HomeFragment` with a **Next tip** button.

### 10. Room Database & MVVM
> **Answer:** `FitTrackerDatabase` is a singleton `@Database` with 7 entities (`UserProfile`, `Exercise`, `WorkoutPlan`, `WorkoutSession`, `FoodItem`, `NutritionLog`, `ActivityLog`, `WeightHistory`). All DAOs return `LiveData<T>` for reactive UI updates on the main thread, while all database writes (`insert`, `update`, `delete`) run on `FitTrackerDatabase.dbExecutor` (a fixed 4-thread pool) so the UI thread is never blocked.

### 11. Background Operations (`WorkManager`)
> **Answer:** We use `WorkManager` for two deferrable background tasks:
> 1. `ReminderWorker` (`ReminderScheduler`): Enqueues `PeriodicWorkRequest` jobs (24-hour interval with calculated initial delay to the user's chosen `hour:minute`) for daily workout, water, and meal reminders, plus a `OneTimeWorkRequest` for instant testing in `SettingsActivity`.
> 2. `SyncWorker` (`SyncManager`): Enqueues a `OneTimeWorkRequest` with `NetworkType.CONNECTED` constraint to upload unsynced Room rows to Firestore.

### 12. Firebase Authentication & Cloud Firestore
> **Answer:** `FirebaseAuth` manages user sign-up, sign-in, password reset, and provides `getCurrentUser().getUid()`, which scopes every Room table (`userId` column). Every mutable Room entity has `isSynced` (`boolean`) and `updatedAt` (`long`). When `SyncWorker` runs, it queries all rows where `isSynced == 0`, uploads them to `FirebaseFirestore` under `users/{userId}/{subcollection}/{id}`, and calls `dao.markSynced(...)`. Our `firestore.rules` enforces `request.auth != null && request.auth.uid == userId` so users can only access their own data.

---

## 3. 5-to-7 Minute Live Demo Script

1. **Minute 1 — Home Dashboard, Custom Rings & XML Tips**
   - Show the **Home** tab: point out the 3 custom-drawn `CircularProgressView` rings (**Calories**, **Steps**, **Active Time**), the **Weekly Goal** bar, and **Today's Schedule**.
   - Tap **+250 ml** on Hydration to show reactive `LiveData` updating immediately.
   - Scroll to the bottom **Daily Tip (XML)** card and tap **Next tip** to demonstrate `XmlPullParser`.

2. **Minute 2 — Live Foreground Service Session & Dynamic Receiver**
   - Tap **Live Timer** (or go to **Activity → Start Timed Session**).
   - Tap **Start Session**: show the Android 13+ notification permission dialog (if first run) and pull down the notification shade to show the **Foreground Service** chronometer notification with **Pause** and **Stop** buttons.
   - Open Emulator **Extended Controls (`...`) → Battery** and drag **Charge level** to **15%** to trigger the dynamic `lowBatteryReceiver` warning banner live on screen.
   - Wait 30+ seconds, tap **Finish & Save**, and save the session to Room.

3. **Minute 3 — Workouts, WorkoutX API & Nutrition Dual-Ring**
   - Open the **Workout** tab: show the 7-day generated plan, open today's workout day, and briefly open **Library** to show Retrofit-cached exercises.
   - Open the **Nutrition** tab: point out the concentric dual-ring, the color-coded **Protein / Carbs / Fats** bars, add a food item to Breakfast, and swipe it away to demonstrate **Undo** `Snackbar`.

4. **Minute 4 — Analytics, Smooth Bezier Curve & Implicit Intent**
   - Open **Progress & Analytics** (from Home or Profile).
   - Tap **+ Log Today's Weight**, add a weight entry, and toggle between **Weight** and **BMI** on the smooth Bezier curve line chart (`TrendChartView.MODE_LINE`).
   - Toggle between **Calories / Active min** and **7d / 30d** on the gradient bar chart (`TrendChartView.MODE_BAR`).
   - Tap **Share My Progress Summary** to show the Android share chooser (`Intent.ACTION_SEND` Implicit Intent).

5. **Minute 5 — Profile Photo Picker, Reminders & Firestore Cloud Sync**
   - Open the **Profile** tab and tap **Change Photo** to select an image via the **Android Photo Picker** (`ContentResolver` → internal storage).
   - Open **Reminders & Notifications** (`SettingsActivity`), toggle a reminder, and tap **Send test reminder now** to show `WorkManager` (`ReminderWorker`) posting a notification immediately.
   - Return to **Profile**, point out the **pending unsynced count** on the Cloud Backup card, and tap **Sync to Cloud Now** (`SyncWorker`) to upload unsynced records to **Firebase Firestore** and watch the badge change to **"All synced"**.
