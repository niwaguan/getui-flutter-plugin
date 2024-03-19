package com.getui.getuiflut;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.os.Handler;


import androidx.annotation.NonNull;

import com.igexin.sdk.PushConsts;
import com.igexin.sdk.PushManager;
import com.igexin.sdk.Tag;
import com.igexin.sdk.message.GTNotificationMessage;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;

/**
 * GetuiflutPlugin
 */
public class GetuiflutPlugin implements MethodCallHandler, FlutterPlugin, PluginRegistry.NewIntentListener, ActivityAware {

    private static final String TAG = "GetuiflutPlugin";
    private static final int FLUTTER_CALL_BACK_CID = 1;
    private static final int FLUTTER_CALL_BACK_MSG = 2;
    private static final int FLUTTER_CALL_BACK_MSG_USER = 3;
    private static final int FLUTTER_CALL_BACK_MSG_NEW_INTENT = 4;


    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel channel;
    private Context fContext;
    private  ActivityPluginBinding activityPluginBinding;
    public static GetuiflutPlugin instance;

    public GetuiflutPlugin() {
        instance = this;
    }

    @Override
    public void onAttachedToEngine(FlutterPlugin.FlutterPluginBinding flutterPluginBinding) {
        fContext = flutterPluginBinding.getApplicationContext();
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "getuiflut");
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onDetachedFromEngine(FlutterPlugin.FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    boolean handleNotificationIntent(Intent intent, boolean onActivityAttached) {
        if (intent == null) {
            return false;
        }
        boolean isAppPush = intent.getBooleanExtra("app_push", false);
        if (!isAppPush) {
            return false;
        }
        Map<String, Object> message = new HashMap<>();
        String payload = intent.getStringExtra("payload");
        if (payload != null) {
            message.put("payload", payload);
        }
        if (onActivityAttached) {
            message.put("initApp", true);
        }

        Message msg = Message.obtain();
        msg.what = FLUTTER_CALL_BACK_MSG_NEW_INTENT;
        msg.obj = message;
        flutterHandler.sendMessage(msg);

        pushClick(intent);

        return true;
    }

    @Override
    public boolean onNewIntent(@NonNull Intent intent) {
        return handleNotificationIntent(intent, false);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activityPluginBinding = binding;
        activityPluginBinding.addOnNewIntentListener(this);
        handleNotificationIntent(binding.getActivity().getIntent(), true);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        activityPluginBinding.removeOnNewIntentListener(this);
        activityPluginBinding = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        activityPluginBinding = binding;
        activityPluginBinding.addOnNewIntentListener(this);
    }

    @Override
    public void onDetachedFromActivity() {
        activityPluginBinding.removeOnNewIntentListener(this);
        activityPluginBinding = null;
    }


    enum MessageType {
        Default,
        onReceiveMessageData,
        onNotificationMessageArrived,
        onNotificationMessageClicked
    }

    enum StateType {
        Default,
        onReceiveClientId,
        onReceiveOnlineState
    }


    private static Handler flutterHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FLUTTER_CALL_BACK_CID:
                    if (msg.arg1 == StateType.onReceiveClientId.ordinal()) {
                        GetuiflutPlugin.instance.channel.invokeMethod("onReceiveClientId", msg.obj);
                        Log.d("flutterHandler", "onReceiveClientId >>> " + msg.obj);

                    } else if (msg.arg1 == StateType.onReceiveOnlineState.ordinal()) {
                        GetuiflutPlugin.instance.channel.invokeMethod("onReceiveOnlineState", msg.obj);
                        Log.d("flutterHandler", "onReceiveOnlineState >>> " + msg.obj);
                    } else {
                        Log.d(TAG, "default state type...");
                    }
                    break;
                case FLUTTER_CALL_BACK_MSG:
                    if (msg.arg1 == MessageType.onReceiveMessageData.ordinal()) {
                        GetuiflutPlugin.instance.channel.invokeMethod("onReceiveMessageData", msg.obj);
                        Log.d("flutterHandler", "onReceiveMessageData >>> " + msg.obj);

                    } else if (msg.arg1 == MessageType.onNotificationMessageArrived.ordinal()) {
                        GetuiflutPlugin.instance.channel.invokeMethod("onNotificationMessageArrived", msg.obj);
                        Log.d("flutterHandler", "onNotificationMessageArrived >>> " + msg.obj);

                    } else if (msg.arg1 == MessageType.onNotificationMessageClicked.ordinal()) {
                        GetuiflutPlugin.instance.channel.invokeMethod("onNotificationMessageClicked", msg.obj);
                        Log.d("flutterHandler", "onNotificationMessageClicked >>> " + msg.obj);
                    } else {
                        Log.d(TAG, "default Message type...");
                    }
                    break;

                case FLUTTER_CALL_BACK_MSG_USER:
                    GetuiflutPlugin.instance.channel.invokeMethod("onTransmitUserMessageReceive", msg.obj);
                    Log.d(TAG, "default user Message >>> " + msg.obj);
                    break;
                case FLUTTER_CALL_BACK_MSG_NEW_INTENT:
                    GetuiflutPlugin.instance.channel.invokeMethod("onIntentReceive", msg.obj);
                    Log.d(TAG, "onIntentReceive >>> " + msg.obj);
                    break;
                default:
                    break;
            }

        }
    };


    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (call.method.equals("getPlatformVersion")) {
            result.success("Android " + android.os.Build.VERSION.RELEASE);
        } else if (call.method.equals("initGetuiPush")) {
            initGtSdk();
        } else if (call.method.equals("getClientId")) {
            result.success(getClientId());
        } else if (call.method.equals("resume")) {
            resume();
        } else if (call.method.equals("stopPush")) {
            stopPush();
        } else if (call.method.equals("bindAlias")) {
            Log.d(TAG, "bindAlias:" + call.argument("alias").toString() + call.argument("aSn").toString());
            bindAlias(call.argument("alias").toString(), call.argument("aSn").toString());
        } else if (call.method.equals("unbindAlias")) {
            Log.d(TAG, "unbindAlias:" + call.argument("alias").toString() + call.argument("aSn").toString() + call.argument("isSelf").toString());
            unbindAlias(call.argument("alias").toString(), call.argument("aSn").toString(), Boolean.parseBoolean( call.argument("isSelf").toString()));
        } else if (call.method.equals("setTag")) {
            Log.d(TAG, "tags:" + (ArrayList<String>) call.argument("tags"));
            setTag((ArrayList<String>) call.argument("tags"));
        } else if (call.method.equals("onActivityCreate")) {
            Log.d(TAG, "do onActivityCreate");
            onActivityCreate();
        } else if (call.method.equals("setBadge")) {
            Log.d(TAG, "do setBadge");
            setBadge((int) call.argument("badge"));
        } else {
            result.notImplemented();
        }
    }

    private void initGtSdk() {
        Log.d(TAG, "init getui sdk...test");
        try {
            PushManager.getInstance().initialize(fContext);
        } catch (Throwable e) {
            try {
                Method setPrivacyPolicyStrategy = PushManager.class.getDeclaredMethod("setPrivacyPolicyStrategy", Context.class, boolean.class);
                setPrivacyPolicyStrategy.invoke(PushManager.getInstance(), fContext, true);
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
            PushManager.getInstance().registerPushIntentService(fContext, FlutterIntentService.class);
            PushManager.getInstance().initialize(fContext, FlutterPushService.class);
        }

    }

    private void onActivityCreate() {
        try {
            Method method = PushManager.class.getDeclaredMethod("registerPushActivity", Context.class, Class.class);
            method.setAccessible(true);
            method.invoke(PushManager.getInstance(), fContext, GetuiPluginActivity.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void setBadge(int badgeNum) {
        try {
            Method method = PushManager.class.getDeclaredMethod("setBadgeNum", Context.class, int.class);
            method.setAccessible(true);
            method.invoke(PushManager.getInstance(), fContext, badgeNum);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private String getClientId() {
        Log.d(TAG, "get client id");
        return PushManager.getInstance().getClientid(fContext);
    }

    private void resume() {
        Log.d(TAG, "resume push service");
        PushManager.getInstance().turnOnPush(fContext);
    }

    private void stopPush() {
        Log.d(TAG, "stop push service");
        PushManager.getInstance().turnOffPush(fContext);
    }

    /**
     * 绑定别名功能:后台可以根据别名进行推送
     *
     * @param alias 别名字符串
     * @param aSn   绑定序列码, Android中无效，仅在iOS有效
     */
    public void bindAlias(String alias, String aSn) {
        PushManager.getInstance().bindAlias(fContext, alias);
    }

    /**
     * 取消绑定别名功能
     *
     * @param alias  别名字符串
     * @param aSn    绑定序列码, Android中无效，仅在iOS有效
     * @param isSelf boolean 是否只对当前cid有效，如果是true，只对当前cid做解绑；如果是false，对所有绑定该别名的cid列表做解绑.
     */
    public void unbindAlias(String alias, String aSn, boolean isSelf) {
        PushManager.getInstance().unBindAlias(fContext, alias, isSelf, aSn);
    }

    /**
     * 给用户打标签 , 后台可以根据标签进行推送
     *
     * @param tags 别名数组
     */
    public void setTag(List<String> tags) {
        if (tags == null || tags.size() == 0) {
            return;
        }

        Tag[] tagArray = new Tag[tags.size()];
        for (int i = 0; i < tags.size(); i++) {
            Tag tag = new Tag();
            tag.setName(tags.get(i));
            tagArray[i] = tag;
        }

        PushManager.getInstance().setTag(fContext, tagArray, "setTag");
    }

    static void transmitMessageReceive(String message, String func) {
        if (instance == null) {
            Log.d(TAG, "Getui flutter plugin doesn't exist");
            return;
        }
        int type;
        if (func.equals("onReceiveClientId")) {
            type = StateType.onReceiveClientId.ordinal();
        } else if (func.equals("onReceiveOnlineState")) {
            type = StateType.onReceiveOnlineState.ordinal();
        } else {
            type = StateType.Default.ordinal();
        }
        Message msg = Message.obtain();
        msg.what = FLUTTER_CALL_BACK_CID;
        msg.arg1 = type;
        msg.obj = message;
        flutterHandler.sendMessage(msg);
    }

    static void transmitMessageReceive(Map<String, Object> message, String func) {
        if (instance == null) {
            Log.d(TAG, "Getui flutter plugin doesn't exist");
            return;
        }
        int type;
        if (func.equals("onReceiveMessageData")) {
            type = MessageType.onReceiveMessageData.ordinal();
        } else if (func.equals("onNotificationMessageArrived")) {
            type = MessageType.onNotificationMessageArrived.ordinal();
        } else if (func.equals("onNotificationMessageClicked")) {
            type = MessageType.onNotificationMessageClicked.ordinal();
        } else {
            type = MessageType.Default.ordinal();
        }
        Message msg = Message.obtain();
        msg.what = FLUTTER_CALL_BACK_MSG;
        msg.arg1 = type;
        msg.obj = message;
        flutterHandler.sendMessage(msg);
    }

    public static void transmitUserMessage(Map<String, Object> message) {
        Message msg = Message.obtain();
        msg.what = FLUTTER_CALL_BACK_MSG_USER;
        msg.obj = message;
        flutterHandler.sendMessage(msg);
    }
    /**
     * 由于华为、oppo 无点击数报表返回，vivo无单推点击数报表返回，所以需要您在客户端埋点上报。
     * 点击厂商通知以后，在触发的activity的onCreate()方法里面接收相关参数，上报这 3 个离线厂商消息的点击数据。
     * 开发者可直接使用此方法示例
     *
     * @param intent
     * @return
     */
    public boolean pushClick(Intent intent) {
        boolean result = false;
        try {
            String taskid = intent.getStringExtra("gttask");
            String gtaction = intent.getStringExtra("gtaction");
            String clientid = PushManager.getInstance().getClientid(fContext.getApplicationContext());
            String messageid;
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            // 这里的messageid需要自定义， 保证每条消息汇报的都不相同
            String contentToDigest = taskid + clientid + uuid;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                byte[] md5s = MessageDigest.getInstance("MD5").digest(contentToDigest.getBytes(StandardCharsets.UTF_8));
                messageid = new BigInteger(1, md5s).toString(16);
            } else {
                messageid = contentToDigest;
            }

            /***
             * 第三方回执调用接口，可根据业务场景执行
             * 注意：只能用下面回执对应的机型进行上报点击测试，其它机型获取不到 gttask 字段
             *
             * 60020 华为点击
             * 60030 oppo点击
             * 60040 vivo点击
             * 60070 荣耀点击
             *
             * 埋点接口对应填写获取到的actionid值，如果有获取到 actionid 值，就上报埋点，如 果没有则不用上报。
             *
             */
            if (gtaction != null) {
                int actionid = Integer.parseInt(gtaction);
                result = PushManager.getInstance().sendFeedbackMessage(fContext.getApplicationContext(), taskid, messageid, actionid);
            }
        } catch (Exception e) {
            //…………
        }
        return result;
    }
    void onNotificationIntentReceived(Map<String, Object> message, boolean onActivityAttached) {
        Map<String, Object> notification = new HashMap<>(message);
        if (onActivityAttached) {
            notification.put("launchPoint", true);
        }

        Message msg = Message.obtain();
        msg.what = FLUTTER_CALL_BACK_MSG_NEW_INTENT;
        msg.obj = notification;
        flutterHandler.sendMessage(msg);
    }
}
