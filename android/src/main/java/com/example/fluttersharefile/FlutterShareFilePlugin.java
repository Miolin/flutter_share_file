package com.example.fluttersharefile;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.service.chooser.ChooserTarget;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import io.flutter.app.FlutterActivity;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;

/**
 * FlutterShareFilePlugin
 */
public class FlutterShareFilePlugin extends FlutterActivity implements MethodCallHandler {
    /**
     * Plugin registration.
     */
    private static Registrar instance;

    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_share_file");
        channel.setMethodCallHandler(new FlutterShareFilePlugin());
        instance = registrar;
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (call.method.equals("shareimage")) {
            Object arguments = call.arguments;
            HashMap<String, String> argsMap = (HashMap<String, String>) arguments;
            String fileName = argsMap.get("fileName");
            String message = argsMap.get("message");
            shareFile(fileName, message);
        } else {
            result.notImplemented();
        }
    }

    private void shareFile(String fileName, String message) {
        File imageFile = new File(instance.activeContext().getCacheDir(), fileName);
        String appPackageName = instance.activeContext().getPackageName();
        Uri contentUri = FileProvider.getUriForFile(instance.activeContext(), appPackageName, imageFile);

        PackageManager pm = instance.activeContext().getPackageManager();
        Intent imageIntent = new Intent(Intent.ACTION_SEND);
        imageIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        imageIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        imageIntent.setDataAndType(contentUri, "image/png");

        Intent stickerIntent = new Intent("com.instagram.share.ADD_TO_STORY");
        stickerIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        stickerIntent.putExtra("interactive_asset_uri", contentUri);
        stickerIntent.putExtra("top_background_color", "#E5E5E5");
        stickerIntent.putExtra("bottom_background_color", "#E5E5E5");
        stickerIntent.setType("image/png");
        stickerIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        instance.activity().grantUriPermission(
                "com.instagram.android", contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);


        Intent openInChooser = Intent.createChooser(imageIntent, "Share in...");

        Spannable forEditing = new SpannableString(" (as sticker)");
        int color = ResourcesCompat.getColor(instance.activeContext().getResources(), R.color.dark_red, null);
        forEditing.setSpan(new ForegroundColorSpan(color), 0, forEditing.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        List<ResolveInfo> resInfo = pm.queryIntentActivities(stickerIntent, 0);
        Intent[] extraIntents = new Intent[resInfo.size()];
        Log.d("MIODEBUG", "activitys: " + resInfo.size());
        for (int i = 0; i < resInfo.size(); i++) {
            // Extract the label, append it, and repackage it in a LabeledIntent
            ResolveInfo ri = resInfo.get(i);
            String packageName = ri.activityInfo.packageName;
            Intent intent = new Intent("com.instagram.share.ADD_TO_STORY");
            ComponentName component = new ComponentName(packageName, ri.activityInfo.name);
            intent.setComponent(component);
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            intent.putExtra("interactive_asset_uri", contentUri);
            intent.putExtra("top_background_color", "#E5E5E5");
            intent.putExtra("bottom_background_color", "#E5E5E5");
            intent.setType("image/png");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            CharSequence label = TextUtils.concat("Stories", forEditing);
            extraIntents[i] = new LabeledIntent(intent, packageName, label, ri.icon);
        }
        openInChooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents);
        instance.activity().startActivity(openInChooser);
    }
}
