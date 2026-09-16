#include <jni.h>
#include <string>
#include <mutex>

static std::mutex g_mutex;
static std::string g_last;

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_nuvio_simkl_android_DiscordBridge_nativeAvailable(JNIEnv*, jobject) {
    // The official Discord Social SDK is a partner download and is not bundled.
    return JNI_FALSE;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_nuvio_simkl_android_DiscordBridge_nativeSetPresence(
        JNIEnv* env, jobject, jlong applicationId,
        jstring details, jstring state,
        jstring largeImageUrl, jstring smallImageUrl,
        jlong startTimestampMs, jlong endTimestampMs) {
    (void)applicationId;
    (void)details;
    (void)state;
    (void)largeImageUrl;
    (void)smallImageUrl;
    (void)startTimestampMs;
    (void)endTimestampMs;
    // This is the integration seam for discordpp::Client::UpdateRichPresence.
    // Replace this body after adding the official Discord Social SDK package.
    std::lock_guard<std::mutex> lock(g_mutex);
    g_last = "SDK not linked";
    return JNI_FALSE;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_nuvio_simkl_android_DiscordBridge_nativeClearPresence(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    g_last.clear();
}
