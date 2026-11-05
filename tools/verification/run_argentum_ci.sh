#!/usr/bin/env bash
# Match the Debian 13/Mesa driver used by the complete local acceptance matrix.
# Ubuntu 24 Mesa 25.2.8 stalled inside Argentum's native occlusion query. This
# changes the test driver, never the production renderer, fog quality or assertions.
set -euo pipefail
test -n "${JAVA_HOME:-}"
test -d "$RUNNER_TEMP/ornithe-mods"
docker run --rm --init \
  -v "$PWD:/work" -w /work \
  -v "$JAVA_HOME:/opt/java:ro" \
  -v "$HOME/.gradle:/gradle" \
  -v "$RUNNER_TEMP/ornithe-mods:/native-mods:ro" \
  -e JAVA_HOME=/opt/java -e GRADLE_USER_HOME=/gradle \
  -e TFL_NATIVE_VARIANT="${1:-standard}" \
  -e PATH=/opt/java/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \
  -e LIBGL_ALWAYS_SOFTWARE=1 -e LP_NUM_THREADS=2 -e ALSOFT_DRIVERS=null \
  debian:trixie-slim bash -euo pipefail -c '
    apt-get update
    apt-get install -y --no-install-recommends ca-certificates python3 xvfb xauth x11-utils x11-xserver-utils libxrandr2 libxcursor1 libxi6 libxinerama1 libxrender1 libxt6 libfreetype6 libfontconfig1 fonts-dejavu-core libgl1-mesa-dri libegl1 libegl-mesa0 libvulkan1 mesa-vulkan-drivers libopenal1 libasound2t64 libxtst6 ffmpeg
    dpkg-query -W libgl1-mesa-dri libegl-mesa0 mesa-vulkan-drivers
    export XDG_RUNTIME_DIR=/tmp/tfl-xdg
    mkdir -p "$XDG_RUNTIME_DIR"
    chmod 700 "$XDG_RUNTIME_DIR"
    export VK_ICD_FILENAMES="$(find /usr/share/vulkan/icd.d -maxdepth 1 -name "*lvp*.json" -print -quit)"
    test -n "$VK_ICD_FILENAMES"
    export MESA_LOADER_DRIVER_OVERRIDE=zink GALLIUM_DRIVER=zink
    variant=()
    case "$TFL_NATIVE_VARIANT" in standard) ;; nofog) variant+=(--no-fog);; *) exit 2;; esac
    xvfb-run -a -s "-screen 0 1920x1080x24" python3 tools/ornithe/run_smoke.py --profile "${variant[@]}" --production --large --unified --stack argentum --mods-dir /native-mods
    xvfb-run -a -s "-screen 0 1920x1080x24" python3 tools/ornithe/run_smoke.py --production --audit5 --stack argentum --mods-dir /native-mods
    vendor/trs-client/ornithe/gradlew -p vendor/trs-client/ornithe :test --no-daemon --max-workers=2
    version=$(sed -n "s/^mod_version=//p" vendor/trs-client/ornithe/gradle.properties)
    python3 tools/ornithe/verify_jar.py "vendor/trs-client/ornithe/build/libs/tflcore-1.8.9-$version.jar"
  '
