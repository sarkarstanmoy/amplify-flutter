/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazonaws.amplify.amplify_core

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.annotation.NonNull
import com.amplifyframework.AmplifyException
import com.amplifyframework.api.aws.AWSApiPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.AmplifyConfiguration
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.core.category.EmptyCategoryConfiguration
import com.amplifyframework.util.UserAgent
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import org.json.JSONObject
import java.io.Serializable


/** Core */
public class Core : FlutterPlugin, ActivityAware, MethodCallHandler {

    private var isConfigured: Boolean = false
    private lateinit var channel: MethodChannel
    private lateinit var context: Context
    private var mainActivity: Activity? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "com.amazonaws.amplify/core")
        channel.setMethodCallHandler(this);
        context = flutterPluginBinding.applicationContext;
        Log.i("Amplify Flutter", "Added Core plugin")
    }

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "com.amazonaws.amplify/core")
            Log.i("Amplify Flutter", "Added Core plugin")
        }
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {

        when (call.method) {
            "configure" -> 
                try {
                    val arguments = call.arguments as HashMap<*, *>
                    val version = arguments["version"] as String
                    val configuration = arguments["configuration"] as String

                    onConfigure(result, version, configuration)
                }
                catch (e: Exception) {
                    result.error("AmplifyException", "Error casting configuration map", e.message )
                }
            else -> result.notImplemented()
        }
    }


    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        this.mainActivity = binding.activity
    }

    override fun onDetachedFromActivity() {
        this.mainActivity = null
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    private fun onConfigure(@NonNull result: Result, @NonNull version: String, @NonNull config: String) {
        if (!isConfigured) {
            try {
                val configuration = AmplifyConfiguration.builder(JSONObject(config))
                        .addPlatform(UserAgent.Platform.FLUTTER, version)
                        .build()
                if(configuration.forCategoryType(CategoryType.API) !is EmptyCategoryConfiguration) {
                    Amplify.addPlugin(AWSApiPlugin())
                }
                Amplify.configure(configuration, context)
                isConfigured = true;
                result.success(true);
            } catch (e: AmplifyException) {
                result.error("AmplifyException", e.message, formatAmplifyException(e) )
            }
        } else {
            result.success(true)
        }
    }

    private fun formatAmplifyException(@NonNull e: AmplifyException): Map<String, Serializable?> {
        return mapOf(
            "cause" to e.cause,
            "recoverySuggestion" to e.recoverySuggestion
        )
    }

    public fun setConfigured(isConfigured:Boolean) {
        this.isConfigured = isConfigured
    }
}
