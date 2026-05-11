# HiddenSpot — Android Setup Guide

## Project Structure
```
HiddenSpot/
├── app/
│   ├── build.gradle
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/hiddengems/app/
│   │   │   ├── activities/
│   │   │   │   ├── SplashActivity.java
│   │   │   │   ├── AuthActivity.java
│   │   │   │   ├── MainActivity.java
│   │   │   │   ├── PlaceDetailsActivity.java
│   │   │   │   └── AddPlaceActivity.java
│   │   │   ├── fragments/
│   │   │   │   ├── HomeFragment.java
│   │   │   │   ├── SearchFragment.java
│   │   │   │   ├── FavoritesFragment.java
│   │   │   │   └── ProfileFragment.java
│   │   │   ├── adapters/
│   │   │   │   └── PlaceAdapter.java
│   │   │   ├── models/
│   │   │   │   └── Place.java
│   │   │   └── utils/
│   │   │       └── FirebaseHelper.java
│   │   └── res/
│   │       ├── layout/          (8 XML layouts)
│   │       ├── drawable/        (icons + backgrounds)
│   │       ├── values/          (colors, strings, themes)
│   │       ├── menu/            (bottom_nav_menu.xml)
│   │       └── font/            (poppins, inter)
│   └── google-services.json     ← YOU ADD THIS
├── build.gradle
└── settings.gradle
```

---

## Step 1 — Open in Android Studio
1. Open Android Studio → **File → Open** → select the `HiddenSpot/` folder
2. Wait for Gradle sync to finish

---

## Step 2 — Add google-services.json
1. Go to **Firebase Console** → your project → Android app
2. Download `google-services.json`
3. Copy it into: `app/google-services.json`
4. ⚠️ Must be inside `/app/` folder, NOT the root

---

## Step 3 — Add Fonts
Download from Google Fonts (fonts.google.com):
- **Poppins**: Regular (400), SemiBold (600), Bold (700)
- **Inter**: Regular (400)

Rename and place in `app/src/main/res/font/`:
```
poppins_regular.ttf
poppins_semibold.ttf
poppins_bold.ttf
inter.ttf
```

---

## Step 4 — Add App Logo
Place your `logo.png` in:
```
app/src/main/res/drawable/logo.png
```
Also add launcher icons in all mipmap folders:
```
res/mipmap-mdpi/ic_launcher.png       (48x48)
res/mipmap-hdpi/ic_launcher.png       (72x72)
res/mipmap-xhdpi/ic_launcher.png      (96x96)
res/mipmap-xxhdpi/ic_launcher.png     (144x144)
res/mipmap-xxxhdpi/ic_launcher.png    (192x192)
```

---

## Step 5 — Firebase Setup
In **Firebase Console**, enable:
1. **Authentication** → Email/Password sign-in method
2. **Firestore Database** → Create database → Start in test mode
3. **Storage** → Get started

### Firestore Security Rules (paste in Firebase Console):
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /gems/{gemId} {
      allow read: if true;
      allow create: if request.auth != null;
      allow update: if request.auth != null;
    }
    match /users/{userId} {
      allow read, write: if request.auth.uid == userId;
    }
    match /saves/{saveId} {
      allow read, write: if request.auth != null;
    }
    match /reviews/{reviewId} {
      allow read: if true;
      allow create: if request.auth != null;
    }
  }
}
```

### Storage Rules:
```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /gem_images/{imageId} {
      allow read: if true;
      allow write: if request.auth != null && request.resource.size < 5 * 1024 * 1024;
    }
  }
}
```

---

## Step 6 — Google Maps (optional)
1. Go to Google Cloud Console → Enable Maps SDK for Android
2. Get an API key
3. Replace `YOUR_MAPS_KEY` in `AndroidManifest.xml`

---

## Step 7 — Run the App!
Click **▶ Run** in Android Studio

### App Flow:
```
SplashScreen (2s)
    ↓
AuthActivity (Login / Register)  ← Firebase Auth
    ↓
MainActivity (Bottom Nav)
    ├── HomeFragment      — browse gems + category filter
    ├── SearchFragment    — search by keyword, city, category
    ├── AddPlaceActivity  — photo upload + form → Firestore
    ├── FavoritesFragment — saved gems from Firestore
    └── ProfileFragment   — user stats + my posts + logout
         ↓
PlaceDetailsActivity — full detail, upvote/downvote, maps, share
```

---

## Package Name
`com.hiddengems.app`
Change in: `app/build.gradle` → `applicationId` and `AndroidManifest.xml` → `package`

---

## Dependencies Used
| Library | Purpose |
|---------|---------|
| Firebase Auth | Login / Register |
| Firestore | Store all gems, users, saves |
| Firebase Storage | Image uploads |
| Glide | Image loading + caching |
| Material Components | Buttons, TextInput, BottomNav |
| CircleImageView | Round avatar |
| DotsIndicator | Image gallery dots |
| Google Maps | Open in Maps feature |
