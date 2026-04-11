# Privacy Policy

**SwitchStream**
Last updated: April 11, 2026

SwitchStream is developed by Djwar Fettah ("SwitchSides", "we", "us", or "our"). This privacy policy explains how SwitchStream ("the app") handles your information.

## Overview

SwitchStream is a media client that connects to your personal Jellyfin media server. The app does not operate its own servers and does not collect, store, or transmit your data to us or any third party.

## Information the App Stores Locally

The app stores the following data locally on your device to function:

- **Server address**: The URL of your Jellyfin server, so the app can reconnect between sessions.
- **Authentication token**: A session token provided by your Jellyfin server, used to keep you signed in.
- **User ID and username**: Used to identify your account on your Jellyfin server.
- **App preferences**: Your chosen settings such as playback preferences.

This data is stored in Android's encrypted DataStore on your device only. It is never sent to us or any third party.

## Data Collection

We (SwitchSides) do not collect any personal data. No data is sent to us or any third party. Specifically:

- No analytics or usage tracking by us
- No advertising identifiers
- No crash reports sent to external services
- No location data
- No contacts, camera, or microphone access
- No data shared with third parties

The only data transmission is between your device and the Jellyfin server you configure, as described below.

## Data Transmitted to Your Server

When the app communicates with your Jellyfin server, the following data is transmitted:

- **Device identifier**: The Jellyfin SDK generates a persistent device ID that is sent with every API request. Your server uses this to manage active sessions and devices.
- **Device name**: Your device model name is sent to your server for session identification.
- **Client name and version**: The app identifies itself as "SwitchStream" to your server.
- **Authentication credentials**: Your username and password are sent to your server during login. After authentication, a session token is used for subsequent requests.
- **Playback progress**: The app reports playback position to your server in real-time so you can resume media where you left off.
- **User ID**: Your Jellyfin user ID is included in API requests to retrieve your libraries, preferences, and media.

All of this data is transmitted exclusively to the Jellyfin server address you provide. None of this data is sent to us, any third party, or any other external service. We have no visibility into this communication.

We recommend using HTTPS to connect to your Jellyfin server to ensure your data is encrypted in transit.

## Data Deletion

All locally stored data can be cleared by:

- Signing out within the app
- Clearing the app's data in Android Settings
- Uninstalling the app

## Children's Privacy

The app does not knowingly collect any information from children under 13. The app connects only to a server you configure and does not collect data from any user.

## Changes to This Policy

We may update this privacy policy from time to time. Any changes will be reflected by updating the "Last updated" date above.

## Contact

If you have any questions about this privacy policy, you can contact us at:

- Developer: Djwar Fettah
- Organization: SwitchSides
- Email: hello@switchsides.co.uk
