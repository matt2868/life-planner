# Life Planner

A planner and life tracker: a calendar with daily scores, notes and to-dos, lists (books, films, games, albums, shiny hunts and your own), stats, yearly goals with monthly check-ins, and a monthly or yearly wrapped.

It runs as an Android app with a home-screen to-do widget, and as a web page for computers. Your data is never stored in this repo. Each person keeps their own private data repo on GitHub, and the app syncs to it.

## Install the Android app

1. On your phone, open the [latest release](https://github.com/matt2868/life-planner/releases/latest) and download `life-planner.apk`.
2. Open the download. The first time, Android asks you to allow installs from your browser; allow it.
3. Open Planner and follow the setup screen.

Updates install over the top and keep your data. The app shows a banner when a new version is ready.

To add the widget, long-press the home screen, choose Widgets, and find Planner.

## Use it on a computer

Open https://matt2868.github.io/life-planner/ and connect the same data repo and a token.

## Set up your own data repo

1. Create an empty **private** repo on your own GitHub account, for example `life-planner-data`.
2. Make a token: GitHub Settings → Developer settings → Personal access tokens → Fine-grained tokens → Generate new token.
   - Repository access: Only select repositories → your data repo.
   - Permissions: Contents → Read and write.
3. In the app, enter the repo as `your-name/life-planner-data` and paste the token.

## How it's built

- `www/index.html` is the whole planner (HTML, CSS and JavaScript in one file).
- `android/` wraps that page in an Android app and adds the widget.
- Every push to `main` builds a signed APK, publishes it as a release, and deploys the web version to GitHub Pages. If a build fails, its log is pushed to the `ci-log` branch.
- The signing key is `android/release.p12`, encrypted with the `KEYSTORE_PASSWORD` repository secret. Keep that secret: without the same key, updates can't install over the existing app.
