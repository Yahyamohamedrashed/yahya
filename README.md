# Wasla Android

Android client for the Wasla employee transport application.

The client connects to a separately deployed HTTPS server. Usernames, passwords, employee records and account configuration are not included in this repository.

Android source is under `android/`. GitHub Actions compiles a debug-signed test APK and verifies its signature and package metadata. Actual operation requires a configured backend; device testing and a production signing key are separate release steps.
