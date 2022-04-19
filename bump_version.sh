#!/usr/bin/env sh
# get current version code
tmp=$(sed -n 's/^.*versionCode //p' app/build.gradle)
VERSION_CODE="$((tmp+1))"
echo "New Version Code: $VERSION_CODE"
sed "0,/{\$COMICK_APP_VERSION_NAME}/s//$1/" "app/build.gradle.template" > "app/build.gradle"
sed -i "0,/{\$COMICK_APP_VERSION_CODE}/s//$VERSION_CODE/" "app/build.gradle"