# Continuum

Share files from your phone to Mac in one tap.

<p align="center">
    <img src=".github/continuum.gif">
</p>

## WARNING: This is an experiment - it might be completely unstable and security is NON-EXISTENT! You have been warned.

### Android side:

Continuum uses `<intent-filter>` with `Intent.ACTION_SEND` to appear in Share sheet.
Bonjour/mDNS is used to discover receivers with Continuum service available.
Files are pushed using HTTP.

### Mac side:

[Hammerspoon](https://www.hammerspoon.org/) script creates an HTTP server and publishes it over Bonjour.
Received files are saved to disk and notification is displayed.


This is a proof of concept, but there are much more possibilities:
- Copy and paste between devices
- Uploading files to Android device
- "Take Photo" using Android


## Bugs

- All data are sent using an unsecured HTTP channel, there is no encryption/security at this moment.
- Hammerspoon `hs.httpserver` stops working after a few connections.
- No pairing process, the first available receiver is used.

## Install
```
git clone https://github.com/JaCzekanski/Continuum
cd Continuum
./gradlew installDebug
# Install Hammerspoon and copy server/server.lua to Hammerspoon config
```

## Author
Jakub Czekański

## License
MIT
