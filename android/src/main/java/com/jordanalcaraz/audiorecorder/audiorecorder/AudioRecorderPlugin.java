package com.jordanalcaraz.audiorecorder.audiorecorder;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;

import com.jordanalcaraz.audiorecorder.audiorecorder.WavRecorder;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * AudioRecorderPlugin
 */
public class AudioRecorderPlugin implements MethodCallHandler, FlutterPlugin, ActivityAware {
  private MethodChannel channel;
  private FlutterPluginBinding flutterPluginBinding;
  private Activity activity;
  private boolean isRecording = false;
  private static final String LOG_TAG = "AudioRecorder";
  private MediaRecorder mRecorder = null;
  private static String mFilePath = null;
  private Date startTime = null;
  private String mExtension = "";
  private WavRecorder wavRecorder;


  @Override
  public void onMethodCall(MethodCall call, Result result) {
    switch (call.method) {
      case "start":
        Log.d(LOG_TAG, "Start");
        String path = call.argument("path");
        mExtension = call.argument("extension");
        startTime = Calendar.getInstance().getTime();
        if (path != null) {
          mFilePath = path;
        } else {
          String fileName = String.valueOf(startTime.getTime());
          mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fileName + mExtension;
        }
        Log.d(LOG_TAG, mFilePath);
        startRecording();
        isRecording = true;
        result.success(null);
        break;
      case "stop":
        Log.d(LOG_TAG, "Stop");
        stopRecording();
        long duration = Calendar.getInstance().getTime().getTime() - startTime.getTime();
        Log.d(LOG_TAG, "Duration : " + String.valueOf(duration));
        isRecording = false;
        HashMap<String, Object> recordingResult = new HashMap<>();
        recordingResult.put("duration", duration);
        recordingResult.put("path", mFilePath);
        recordingResult.put("audioOutputFormat", mExtension);
        result.success(recordingResult);
        break;
      case "isRecording":
        Log.d(LOG_TAG, "Get isRecording");
        result.success(isRecording);
        break;
      case "hasPermissions":
        Log.d(LOG_TAG, "Get hasPermissions");
        PackageManager pm = activity.getPackageManager();
        int hasStoragePerm = pm.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, activity.getPackageName());
        int hasRecordPerm = pm.checkPermission(Manifest.permission.RECORD_AUDIO, activity.getPackageName());
        boolean hasPermissions = hasStoragePerm == PackageManager.PERMISSION_GRANTED && hasRecordPerm == PackageManager.PERMISSION_GRANTED;
        result.success(hasPermissions);
        break;
      default:
        result.notImplemented();
        break;
    }
  }

  private void startRecording() {
    if (isOutputFormatWav()) {
      startWavRecording();
    } else {
      startNormalRecording();
    }
  }

  private void startNormalRecording() {
    mRecorder = new MediaRecorder();
    mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
    mRecorder.setOutputFormat(getOutputFormatFromString(mExtension));
    mRecorder.setOutputFile(mFilePath);
    mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

    try {
      mRecorder.prepare();
    } catch (IOException e) {
      Log.e(LOG_TAG, "prepare() failed");
    }

    mRecorder.start();
  }

  private void startWavRecording() {
    wavRecorder = new WavRecorder(activity, mFilePath);
    wavRecorder.startRecording();
  }

  private void stopRecording() {
    if (isOutputFormatWav()) {
      stopWavRecording();
    } else {
      stopNormalRecording();
    }
  }

  private void stopNormalRecording() {
    if (mRecorder != null){
      mRecorder.stop();
      mRecorder.reset();
      mRecorder.release();
      mRecorder = null;
    }
  }

  private void stopWavRecording() {
    wavRecorder.stopRecording();
  }

  private int getOutputFormatFromString(String outputFormat) {
    switch (outputFormat) {
      case ".aac":
        return MediaRecorder.OutputFormat.AAC_ADTS;
      case ".mp4":
      case ".m4a":
        return MediaRecorder.OutputFormat.MPEG_4;
      default:
        return MediaRecorder.OutputFormat.MPEG_4;
    }
  }

  private boolean isOutputFormatWav() {
    return mExtension.equals(".wav");
  }

  @Override
  public void onAttachedToEngine(@NonNull FlutterPlugin.FlutterPluginBinding binding) {
    this.flutterPluginBinding = binding;
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPlugin.FlutterPluginBinding binding) {

  }

  @Override
  public void onAttachedToActivity(@NonNull @org.jetbrains.annotations.NotNull ActivityPluginBinding binding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "audio_recorder");
    channel.setMethodCallHandler(this);
    activity = binding.getActivity();
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {

  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull @org.jetbrains.annotations.NotNull ActivityPluginBinding binding) {

  }

  @Override
  public void onDetachedFromActivity() {
    channel.setMethodCallHandler(null);
  }
}