#!/usr/bin/env bash
set -euo pipefail
ROOT="$1"

copy() {
  src="$1"; dst="$2"
  mkdir -p "$ROOT/$(dirname "$dst")"
  cp "$GITHUB_WORKSPACE/patches/$src" "$ROOT/$dst"
}

copy AutoCoordinatorService.kt app/src/main/java/com/datanet/share/AutoCoordinatorService.kt
copy AutoLaunchActivity.kt app/src/main/java/com/datanet/share/AutoLaunchActivity.kt
copy BootReceiver.kt app/src/main/java/com/datanet/share/BootReceiver.kt
copy TrafficMeter.kt app/src/main/java/com/datanet/share/common/TrafficMeter.kt
copy NetworkState.kt app/src/main/java/com/datanet/share/common/NetworkState.kt
copy WifiDirectClient.kt app/src/main/java/com/datanet/share/receiver/WifiDirectClient.kt

python3 - "$ROOT/app/src/main/AndroidManifest.xml" <<'PY'
from pathlib import Path
import sys
p = Path(sys.argv[1])
s = p.read_text()

# Replace the existing launcher MainActivity block with AutoLaunchActivity,
# while keeping MainActivity available as a normal exported activity.
old = '''        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTask">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>'''
new = '''        <activity
            android:name=".AutoLaunchActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTask" />'''
s = s.replace(old, new)

if 'android:name=".AutoCoordinatorService"' not in s:
    s = s.replace('''        <service
            android:name=".receiver.ReceiverService"''', '''        <service
            android:name=".AutoCoordinatorService"
            android:exported="false"
            android:foregroundServiceType="connectedDevice" />

        <receiver
            android:name=".BootReceiver"
            android:exported="false">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
            </intent-filter>
        </receiver>

        <service
            android:name=".receiver.ReceiverService"''')
p.write_text(s)
PY
