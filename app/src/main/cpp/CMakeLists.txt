cmake_minimum_required(VERSION 3.22.1)
project(nuvio_discord_bridge)

add_library(nuvio_discord_bridge SHARED discord_bridge.cpp)

# The official Discord Social SDK is intentionally NOT redistributed in this project.
# Place the Android SDK AAR/native package obtained from Discord in app/libs and
# wire its exported native library here according to the SDK version you download.
#
# The Java/Kotlin side remains usable without the SDK; Discord is reported as
# "SDK not installed" until the official package is linked.
target_compile_features(nuvio_discord_bridge PRIVATE cxx_std_20)
