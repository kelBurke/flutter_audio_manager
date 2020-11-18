package com.yung.flutter_audio_manager;

import android.content.Context;
import android.media.AudioManager;
import androidx.annotation.NonNull;
import androidx.mediarouter.media.MediaControlIntent;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;
import androidx.mediarouter.media.MediaRouter.RouteInfo;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * FlutterAudioManagerPlugin
 */
public class FlutterAudioManagerPlugin implements FlutterPlugin, MethodCallHandler {

    private MethodChannel channel;
    private AudioManager audioManager;
    private MediaRouter mediaRouter;
    private MediaRouter.Callback mediaRouteCallback;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        onAttachedToEngine(binding.getApplicationContext(), binding.getBinaryMessenger());
    }

    public static void registerWith(io.flutter.plugin.common.PluginRegistry.Registrar registrar) {
        final FlutterAudioManagerPlugin instance = new FlutterAudioManagerPlugin();
        instance.onAttachedToEngine(registrar.context(), registrar.messenger());
    }

    private void onAttachedToEngine(Context applicationContext, BinaryMessenger messenger) {
        channel = new MethodChannel(messenger, "flutter_audio_manager");
        channel.setMethodCallHandler(this);
        audioManager = (AudioManager) applicationContext.getSystemService(Context.AUDIO_SERVICE);

        mediaRouter = MediaRouter.getInstance(applicationContext);
        mediaRouteCallback = new MediaRouter.Callback() {
            @Override
            public void onRouteChanged(MediaRouter router, RouteInfo route) {
                super.onRouteChanged(router, route);

                if (channel != null) {
                    channel.invokeMethod("inputChanged", 1);
                }
            }

            @Override
            public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo route) {
                super.onRouteRemoved(router, route);

                mediaRouter.unselect(MediaRouter.UNSELECT_REASON_DISCONNECTED);
            }
        };

        mediaRouter.addCallback(new MediaRouteSelector.Builder().addControlCategory(
            MediaControlIntent.CATEGORY_LIVE_AUDIO).build(),
            mediaRouteCallback
        );
    }


    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        switch (call.method) {
            case "getCurrentOutput":
                result.success(getCurrentOutput());
                break;
            case "getAvailableInputs":
                result.success(getAvailableInputs());
                break;
            case "changeToReceiver":
                result.success(changeToReceiver());
                break;
            case "changeToSpeaker":
                result.success(changeToSpeaker());
                break;
            case "changeToHeadphones":
                result.success(changeToHeadphones());
                break;
            case "changeToBluetooth":
                result.success(changeToBluetooth());
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private Boolean changeToReceiver() {
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.stopBluetoothSco();
        audioManager.setBluetoothScoOn(false);
        audioManager.setSpeakerphoneOn(false);
        onChanged();
        return true;
    }

    private Boolean changeToSpeaker() {
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.stopBluetoothSco();
        audioManager.setBluetoothScoOn(false);
        audioManager.setSpeakerphoneOn(true);
        onChanged();
        return true;
    }

    private Boolean changeToHeadphones() {
        return changeToReceiver();
    }

    private Boolean changeToBluetooth() {
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.startBluetoothSco();
        audioManager.setBluetoothScoOn(true);
        onChanged();
        return true;
    }

    private List<String> getCurrentOutput() {
        List<String> info = new ArrayList();
        MediaRouter.RouteInfo currentRoute = mediaRouter.getSelectedRoute();
        if (currentRoute == null) {
            currentRoute = mediaRouter.getDefaultRoute();
        }
        info.add(currentRoute.getName());
        info.add(_getDeviceType(currentRoute.getDeviceType()));

        return info;
    }

    private List<List<String>> getAvailableInputs() {
        List<List<String>> list = new ArrayList();
        list.add(Arrays.asList("Receiver", "1"));
        if (audioManager.isWiredHeadsetOn()) {
            list.add(Arrays.asList("Headset", "3"));
        }
        if (audioManager.isBluetoothScoOn()) {
            list.add(Arrays.asList("Bluetooth", "4"));
        }
        return list;
    }

    private String _getDeviceType(int type) {
        switch (type) {
            case 3:
                return "3";
            case 2:
                return "2";
            case 1:
                return "4";
            default:
                return "0";
        }
    }

    public void onChanged() {
        if (channel != null) {
            channel.invokeMethod("inputChanged", 1);
        }
    }


    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        mediaRouter.removeCallback(mediaRouteCallback);

        if (channel != null) {
            channel.setMethodCallHandler(null);
            channel = null;
        }

        audioManager = null;
        mediaRouter = null;
        mediaRouteCallback = null;
    }
}
