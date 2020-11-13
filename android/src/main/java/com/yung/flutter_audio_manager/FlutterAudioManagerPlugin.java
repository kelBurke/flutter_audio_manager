package com.yung.flutter_audio_manager;

import androidx.annotation.NonNull;
import androidx.mediarouter.media.MediaControlIntent;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;

import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/** FlutterAudioManagerPlugin */
public class FlutterAudioManagerPlugin implements FlutterPlugin, MethodCallHandler {
  private MethodChannel channel;
  private AudioManager audioManager;
  private Context activeContext;
  private AudioChangeReceiver receiver;
  private MediaRouter mediaRouter;
  private MediaRouter.Callback mediaRouteCallback;
  AudioEventListener listener;

    @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    onAttachedToEngine(binding.getApplicationContext(), binding.getBinaryMessenger());
  }

  public static void registerWith(io.flutter.plugin.common.PluginRegistry.Registrar registrar) {
    final FlutterAudioManagerPlugin instance = new FlutterAudioManagerPlugin();
    instance.onAttachedToEngine(registrar.context(), registrar.messenger());
 }

  private void onAttachedToEngine(Context applicationContext, BinaryMessenger messenger) {
    listener = new AudioEventListener() {
      @Override
      public void onChanged() {
        channel.invokeMethod("inputChanged", 1);
      }
    };

      channel = new MethodChannel(messenger, "flutter_audio_manager");
      channel.setMethodCallHandler(this);
      receiver = new AudioChangeReceiver(listener);
      IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
      activeContext = applicationContext;
      activeContext.registerReceiver(receiver, filter);
      audioManager = (AudioManager) activeContext.getSystemService(Context.AUDIO_SERVICE);

      mediaRouter = MediaRouter.getInstance(activeContext);
      mediaRouteCallback =  new MediaRouter.Callback() {
          @Override
          public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo route) {
              mediaRouter.unselect(MediaRouter.UNSELECT_REASON_DISCONNECTED);
              super.onRouteRemoved(router, route);
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
    listener.onChanged();
    return true;
  }

  private Boolean changeToSpeaker() {
    audioManager.setMode(AudioManager.MODE_NORMAL);
    audioManager.stopBluetoothSco();
    audioManager.setBluetoothScoOn(false);
    audioManager.setSpeakerphoneOn(true);
    listener.onChanged();
    return true;
  }

  private Boolean changeToHeadphones() {
    return changeToReceiver();
  }

  private Boolean changeToBluetooth() {
    audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
    audioManager.startBluetoothSco();
    audioManager.setBluetoothScoOn(true);
    listener.onChanged();
    return true;
  }

  private List<String> getCurrentOutput() {
    List<String> info = new ArrayList();
    MediaRouter.RouteInfo currentRoute =  mediaRouter.getSelectedRoute();
    if(currentRoute == null) {
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

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    activeContext.unregisterReceiver(receiver);
    mediaRouter.removeCallback(mediaRouteCallback);


    if(channel != null){
      channel.setMethodCallHandler(null);
      channel = null;
    }

    audioManager = null;
    activeContext = null;
    mediaRouter = null;
    mediaRouteCallback = null;
    receiver = null;
    listener = null;
  }
}
