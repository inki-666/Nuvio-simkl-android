package com.nuvio.simkl.android

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class ProfileStore(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            "nuvio_profiles",
            Context.MODE_PRIVATE
        )

    fun saveProfiles(
        profiles: List<NuvioProfile>
    ) {

        val array =
            JSONArray()

        profiles.forEach {

            val objectJson =
                JSONObject()

            objectJson.put(
                "id",
                it.id
            )

            objectJson.put(
                "name",
                it.name
            )

            objectJson.put(
                "avatarUrl",
                it.avatarUrl
            )

            objectJson.put(
                "rpcEnabled",
                it.rpcEnabled
            )

            array.put(
                objectJson
            )
        }

        preferences.edit()
            .putString(
                "profiles",
                array.toString()
            )
            .apply()
    }

    fun loadProfiles(): List<NuvioProfile> {

        val raw =
            preferences.getString(
                "profiles",
                null
            ) ?: return emptyList()

        return try {

            val array =
                JSONArray(raw)

            val result =
                mutableListOf<NuvioProfile>()

            for (
                index in 0 until array.length()
            ) {

                val objectJson =
                    array.getJSONObject(index)

                result.add(
                    NuvioProfile(
                        id =
                            objectJson.getString(
                                "id"
                            ),

                        name =
                            objectJson.getString(
                                "name"
                            ),

                        avatarUrl =
                            objectJson.optString(
                                "avatarUrl"
                            ).ifBlank {
                                null
                            },

                        rpcEnabled =
                            objectJson.optBoolean(
                                "rpcEnabled",
                                true
                            )
                    )
                )
            }

            result

        } catch (
            exception: Exception
        ) {

            emptyList()
        }
    }

    fun setRpcEnabled(
        profileId: String,
        enabled: Boolean
    ) {

        val profiles =
            loadProfiles()
                .map {

                    if (
                        it.id == profileId
                    ) {

                        it.copy(
                            rpcEnabled =
                                enabled
                        )

                    } else {
                        it
                    }
                }

        saveProfiles(
            profiles
        )
    }
}
