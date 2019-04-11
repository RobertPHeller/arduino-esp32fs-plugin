#!/usr/bin/env bash

if [[ -z "$INSTALLDIR" ]]; then
    INSTALLDIR="$HOME/Arduino"
fi
echo "INSTALLDIR: $INSTALLDIR"

pde_path=`find ../../../ -name pde.jar 2>/dev/null`
core_path=`find ../../../ -name arduino-core.jar 2>/dev/null`
lib_path=`find ../../../ -name commons-codec-1.7.jar 2>/dev/null`
if [[ -z "$core_path" || -z "$pde_path" ]]; then
    echo "Some java libraries have not been built yet (did you run ant build?)"
    return 1
fi
echo "pde_path: $pde_path"
echo "core_path: $core_path"
echo "lib_path: $lib_path"

set -e

rm -rf bin
mkdir -p bin
echo "javac ... src/ESP32FS.java"
javac -target 1.8 -cp "$pde_path:$core_path:$lib_path" \
      -d bin src/ESP32FS.java

pushd bin
mkdir -p $INSTALLDIR/tools
rm -rf $INSTALLDIR/tools/ESP32FS
mkdir -p $INSTALLDIR/tools/ESP32FS/tool
zip -r $INSTALLDIR/tools/ESP32FS/tool/esp32fs.jar *
popd


rm -rf bin
mkdir -p bin
echo "javac ... src/MessageSiphonBS.java"
javac -target 1.8 -cp "$pde_path:$core_path:$lib_path" \
       -d bin src/MessageSiphonBS.java
echo "javac ... src/ESP32FSDL.java"
javac -target 1.8 -cp "$pde_path:$core_path:$lib_path:bin" \
      -d bin src/ESP32FSDL.java

pushd bin
mkdir -p $INSTALLDIR/tools
rm -rf $INSTALLDIR/tools/ESP32FSDL
mkdir -p $INSTALLDIR/tools/ESP32FSDL/tool
zip -r $INSTALLDIR/tools/ESP32FSDL/tool/esp32fsdl.jar *
popd



dist=$PWD/dist
rev=$(git describe --tags)
mkdir -p $dist
pushd $INSTALLDIR/tools
zip -r $dist/ESP32FS-$rev.zip ESP32FS/
zip -r $dist/ESP32FSDL-$rev.zip ESP32FSDL/
popd
