package com.yung.flutter_audio_manager;

import androidx.annotation.NonNull;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaRouter;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** FlutterAudioManagerPlugin */
public class FlutterAudioManagerPlugin implements FlutterPlugin, MethodCallHandler {
  private MethodChannel channel;
  private AudioManager audioManager;
  private Context activeContext;
  private AudioChangeReceiver receiver;
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
    channel.setMethodCallHandler(new FlutterAudioManagerPlugin());
    receiver = new AudioChangeReceiver(listener);
    IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
    activeContext = applicationContext;
    activeContext.registerReceiver(receiver, filter);
    audioManager = (AudioManager) activeContext.getSystemService(Context.AUDIO_SERVICE);
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
    if (audioManager.isSpeakerphoneOn()) {
      info.add("Speaker");
      info.add("2");
    } else if (audioManager.isBluetoothScoOn()) {
      info.add("Bluetooth");
      info.add("4");
    } else if (audioManager.isWiredHeadsetOn()) {
      info.add("Headset");
      info.add("3");
    } else {
      info.add("Receiver");
      info.add("1");
    }
    return info;
    // if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
    // MediaRouter mr = (MediaRouter)
    // activeContext.getSystemService(Context.MEDIA_ROUTER_SERVICE);
    // MediaRouter.RouteInfo routeInfo =
    // mr.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_AUDIO);
    // Log.d("aaa", "getCurrentOutput:
    // "+audioManager.isSpeakerphoneOn()+audioManager.isWiredHeadsetOn()+audioManager.isSpeakerphoneOn());
    // info.add(routeInfo.getName().toString());
    // info.add(_getDeviceType(routeInfo.getDeviceType()));
    // } else {
    // info.add("unknow");
    // info.add("0");
    // }
    // return info;
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
    if(channel != null){
      channel.setMethodCallHandler(null);
      channel = null;
    }

    audioManager = null;
    activeContext = null;
    receiver = null;
    listener = null;
  }
}
