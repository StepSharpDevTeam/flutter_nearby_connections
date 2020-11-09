package com.nankai.flutter_nearby_connections

import android.app.Activity
import android.util.Log
import androidx.annotation.NonNull
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.Strategy
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar


const val SERVICE_ID = "flutter_nearby_connections"

const val initNearbyService = "init_nearby_service"
const val startAdvertisingPeer = "start_advertising_peer"
const val startBrowsingForPeers = "start_browsing_for_peers"

const val stopAdvertisingPeer = "stop_advertising_peer"
const val stopBrowsingForPeers = "stop_browsing_for_peers"

const val invitePeer = "invite_peer"
const val disconnectPeer = "disconnect_peer"

const val sendMessage = "send_message"

const val INVOKE_CHANGE_STATE_METHOD = "invoke_change_state_method"
const val INVOKE_MESSAGE_RECEIVE_METHOD = "invoke_message_receive_method"

/** FlutterNearbyConnectionsPlugin */
class FlutterNearbyConnectionsPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
    private lateinit var channel: MethodChannel
    private var locationHelper: LocationHelper? = null
    private lateinit var activity: Activity
    private var binding: ActivityPluginBinding? = null
    private lateinit var callbackUtils: CallbackUtils

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.flutterEngine.dartExecutor, viewTypeId)
        channel.setMethodCallHandler(this)
        callbackUtils = CallbackUtils(channel)
    }

    companion object {
        private const val viewTypeId = "flutter_nearby_connections"

        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), viewTypeId)
            channel.setMethodCallHandler(FlutterNearbyConnectionsPlugin())
        }
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            initNearbyService -> {
                locationHelper?.requestLocationPermission(result)
                locationHelper?.requestLocationEnable(result)
                locationHelper?.requestLocationEnable(result)
            }
            startAdvertisingPeer -> {
                Log.d("nearby_connections", "startAdvertisingPeer")
                val userNickName = "nearby_connections_advertising"
                val advertisingOptions = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
                Nearby.getConnectionsClient(activity).startAdvertising(userNickName, SERVICE_ID,
                        callbackUtils.advertConnectionLifecycleCallback, advertisingOptions)
                        .addOnSuccessListener {
                            Log.d("nearby_connections", "startAdvertising")
                            result.success(true)
                        }.addOnFailureListener { e -> result.error("Failure", e.message, null) }
            }
            startBrowsingForPeers -> {
                Log.d("nearby_connections", "startBrowsingForPeers")
                val discoveryOptions = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
                Nearby.getConnectionsClient(activity)
                        .startDiscovery(SERVICE_ID, callbackUtils.endpointDiscoveryCallback, discoveryOptions)
                        .addOnSuccessListener {
                            Log.d("nearby_connections", "startDiscovery")
                            result.success(true)
                        }.addOnFailureListener { e -> result.error("Failure", e.message, null) }
            }
            stopAdvertisingPeer -> {
                Log.d("nearby_connections", "stopAdvertisingPeer")
                Nearby.getConnectionsClient(activity).stopAdvertising()
                result.success(null)
            }
            stopBrowsingForPeers -> {
                Log.d("nearby_connections", "stopDiscovery")
                Nearby.getConnectionsClient(activity).stopDiscovery()
                result.success(null)
            }
            invitePeer -> {
                Log.d("nearby_connections", "invitePeer")
                val deviceID = call.argument<Any>("deviceID") as? String?
                val displayName = call.argument<Any>("displayName") as? String?
                Nearby.getConnectionsClient(activity)
                        .requestConnection(displayName!!, deviceID!!, callbackUtils.discoverConnectionLifecycleCallback)
                        .addOnSuccessListener { result.success(true) }.addOnFailureListener { e -> result.error("Failure", e.message, null) }
            }
            disconnectPeer -> {
                Log.d("nearby_connections", "disconnectPeer")
                val deviceID = call.argument<Any>("deviceID") as? String?
                Nearby.getConnectionsClient(activity).acceptConnection(deviceID!!, callbackUtils.payloadCallback)
                        .addOnSuccessListener { result.success(true) }.addOnFailureListener { e -> result.error("Failure", e.message, null) }
            }
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        locationHelper = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        this.binding = binding
        activity = binding.activity
        locationHelper = LocationHelper(binding.activity)
        locationHelper?.let {
            binding.addActivityResultListener(it)
            binding.addRequestPermissionsResultListener(it)
        }
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    }

    override fun onDetachedFromActivity() {
        locationHelper?.let {
            binding?.removeRequestPermissionsResultListener(it)
            binding?.removeActivityResultListener(it)
        }
        binding = null
    }

    override fun onDetachedFromActivityForConfigChanges() {
    }
}
