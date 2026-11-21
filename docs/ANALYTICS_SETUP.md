# Firebase Analytics setup

This app uses Google Analytics for Firebase only when configured. App users do
not create accounts or sign in.

1. Create a Firebase project for eSIM Checker and enable Google Analytics.
2. Register an Android app with package name com.esim.checker.
3. Download its google-services.json and place it at app/google-services.json.
4. Build and publish the updated app, then check Firebase Analytics Realtime
   and Events reports.

The generated config contains project and app identifiers, not private keys.
Do not place credentials or signing secrets in it. This change does not include
a Firebase config because the Firebase project has not yet been created.
Without the config, analytics events are ignored.

Events use fixed names and categories. Do not add the compatibility result,
device model, carrier, selected files, report data, or free-text exceptions as
event parameters. Review the Google Play Data safety answers and public privacy
policy before releasing a configured build.
