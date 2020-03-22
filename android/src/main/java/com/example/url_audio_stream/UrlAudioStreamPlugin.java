package com.example.url_audio_stream;

import android.app.Activity;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Build.VERSION;

import java.io.IOException;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

public class UrlAudioStreamPlugin implements MethodCallHandler {

    private MediaPlayer player;
    private Result result;
    private static AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;

    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "url_audio_stream");
        audioManager = (AudioManager) registrar.activity().getSystemService(Context.AUDIO_SERVICE);
        channel.setMethodCallHandler(new UrlAudioStreamPlugin());
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        this.result = result;

        String action = call.method;

        switch (action) {
            case "start":
                String url = call.arguments.toString();
                initializePlayer(url);
                startPlayer();
                break;
            case "stop":
                stopPlayer();
                break;
            case "pause":
                pausePlayer();
                break;
            default:
                resumePlayer();
                break;
        }
    }

    private void initializePlayer(String url) {
        try {
            if (player != null) {
                player.stop();
                player.reset();
                player.release();
                player = null;
            }

            player = new MediaPlayer();

            if (VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                player.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
            } else {
                player.setAudioStreamType(AudioManager.STREAM_SYSTEM);
            }

            player.setDataSource(url);

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startPlayer() {
        try {
            if (player != null) {

                requestFocus();

                player.prepareAsync();
                player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    public void onPrepared(MediaPlayer mp) {
                        try {
                            player.start();
                        } catch (IllegalStateException e) {
                            afterException(e);
                        }
                    }
                });
                player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        try {
                            abandonFocus();
                            result.success(true);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } else {
                result.error("player null", null, null);
            }
        } catch (Exception e) {
            afterException(e);
        }
    }

    private void requestFocus() {
        if (VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                AudioAttributes mPlaybackAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build();

                audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        .setAudioAttributes(mPlaybackAttributes)
                        .setOnAudioFocusChangeListener(new AudioManager.OnAudioFocusChangeListener() {
                            @Override
                            public void onAudioFocusChange(int i) {

                            }
                        })
                        .build();

                audioManager.requestAudioFocus(audioFocusRequest);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void abandonFocus() {
        try {
            if (VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void stopPlayer() {
        try {
            if (player != null) {
                abandonFocus();
                player.stop();
                player.reset();
                player.release();
                player = null;
            }
            result.success(true);
        } catch (Exception e) {
            afterException(e);
        }
    }

    private void pausePlayer() {
        try {
            if (player != null && player.isPlaying()) {
                abandonFocus();
                player.pause();
            }
            result.success(true);
        } catch (Exception e) {
            afterException(e);
        }
    }

    private void resumePlayer() {
        try {
            if (player != null && !player.isPlaying()) {
                abandonFocus();
                player.start();
            }
            result.success(true);
        } catch (Exception e) {
            afterException(e);
        }
    }

    private void afterException(Exception e){
        e.printStackTrace();
        abandonFocus();
        result.error(e.getMessage(), e.getMessage(), e.getCause());
    }
}
