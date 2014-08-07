MediaGridAndroid
================

Android client for MediaGrid.
Prerequisite:
To use MediaGridAndroid, you have to install MediaGrid-no-crypto from https://github.com/dismantl/MediaGrid/tree/no-crypto on a computer. (Suppose computer's IP and port is 192.168.1.100:5984)

Then, install the Android Client, you can install from compiled APK or build the source code yourself.
Install from APK
================
1. Download APK from  MediaGridAndroid/bin, then copy it to your Android device, and click to install.
2. Open the MediaGridAndroid, then click the configure button on the login page, input your computer's IP and port(i.e. 192.168.1.100:5984).
3. Login with your user name to explore more.

Install from source code
================
1. Download the source code.
2. Import MediaGridAndroid. Open Eclipse, click file->import->Existing Android Project into Workspace, choose the downloaded root direcotry, and tick the project MediaGridAndroid. DO remember to tick Copy Projects into Workapce.
3. Import Android support Libray. Click file->import->Existing Android Project into Workspace, choose the directory "AndroidSDK/extras/android/support/v7", tick the project "appcompat".DO remember to tick Copy Projects into Workapce.
4. Reference the support library. Click on MediaGrid->Properties->android, in the library section, remove the red-crossed project if any. Then click add->Android-support-v7-appcompat.
5. Then if no other errors, click run as Android application, or just copy the newly-compiled apk file from /bin/MediaGrid.apk to your Android Device. Just follow the "Install from APK" to start.
