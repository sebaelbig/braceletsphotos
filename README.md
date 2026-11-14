# Bracelet Photos (Kotlin, Android)

Reads an NFC bracelet, links it to a Google Drive folder and notifies you of new photos (WorkManager, every ~15 min).

## Run
1. Open this folder in Android Studio (Koala or newer) and let Gradle sync (it generates the wrapper).
2. Google Cloud Console: enable **Google Drive API**; create an **OAuth client ID (Android)** using package
   `com.example.braceletphotos` and your debug SHA-1 (`./gradlew signingReport`).
3. OAuth consent screen: add your Google account as a test user.
4. Run on a physical NFC phone. Tap the bracelet, sign in, choose the folder.
5. Set the app's battery usage to "Unrestricted" for timely checks.
