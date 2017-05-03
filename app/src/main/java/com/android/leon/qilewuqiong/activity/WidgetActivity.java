package com.android.leon.qilewuqiong.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import io.dcloud.EntryProxy;
import io.dcloud.RInformation;
import io.dcloud.common.DHInterface.IApp;
import io.dcloud.common.DHInterface.ICore;
import io.dcloud.common.DHInterface.IOnCreateSplashView;
import io.dcloud.common.DHInterface.ISysEventListener;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.DHInterface.IWebviewStateListener;
import io.dcloud.feature.internal.sdk.SDK;

/**
 *
 * 独立应用方式启动的activity
 */
public class WidgetActivity extends Activity {

    private EntryProxy mEntryProxy = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("zc","onCreate");
        if (mEntryProxy==null){
            FrameLayout layout = new FrameLayout(this);
            // 创建5+内核运行事件监听
            WebappModeListener wm = new WebappModeListener(this, layout);
            // 初始化5+内核
            mEntryProxy = EntryProxy.init(this, wm);
            // 启动5+内核
            mEntryProxy.onCreate(this, savedInstanceState, SDK.IntegratedMode.WEBAPP, null);
            setContentView(layout);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return mEntryProxy.onActivityExecute(this, ISysEventListener.SysEventType.onCreateOptionMenu, menu);
    }

    @Override
    public void onPause() {
        super.onPause();
        mEntryProxy.onPause(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mEntryProxy.onResume(this);
    }

    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getFlags() != 0x10600000) {
            // 非点击icon调用activity时才调用newintent事件
            mEntryProxy.onNewIntent(this, intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mEntryProxy.onStop(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean _ret = mEntryProxy.onActivityExecute(this, ISysEventListener.SysEventType.onKeyDown, new Object[] { keyCode, event });
        return _ret ? _ret : super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean _ret = mEntryProxy.onActivityExecute(this, ISysEventListener.SysEventType.onKeyUp, new Object[] { keyCode, event });
        return _ret ? _ret : super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        boolean _ret = mEntryProxy.onActivityExecute(this, ISysEventListener.SysEventType.onKeyLongPress, new Object[] { keyCode, event });
        return _ret ? _ret : super.onKeyLongPress(keyCode, event);
    }

    public void onConfigurationChanged(Configuration newConfig) {
        try {
            int temp = this.getResources().getConfiguration().orientation;
            if (mEntryProxy != null) {
                mEntryProxy.onConfigurationChanged(this, temp);
            }
            super.onConfigurationChanged(newConfig);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mEntryProxy.onActivityExecute(this, ISysEventListener.SysEventType.onActivityResult, new Object[] { requestCode, resultCode, data });
    }


    class WebappModeListener implements ICore.ICoreStatusListener, IOnCreateSplashView {
        Activity activity;
        View splashView = null;
        ViewGroup rootView;
        IApp app = null;
        ProgressDialog pd = null;

        public WebappModeListener(Activity activity, ViewGroup rootView) {
            this.activity = activity;
            this.rootView = rootView;
        }

        /**
         * 5+内核初始化完成时触发
         * */
        @Override
        public void onCoreInitEnd(ICore coreHandler) {

            // 表示Webapp的路径在 file:///android_asset/apps/HelloH5
            String appBasePath = "/apps/H5497C5A1";

            // 设置启动参数,可在页面中通过plus.runtime.arguments;方法获取到传入的参数
            String args = "{url:'http://www.baidu.com'}";

            // 启动启动独立应用的5+ Webapp
            app = SDK.startWebApp(activity, appBasePath, args, new IWebviewStateListener() {
                // 设置Webview事件监听，可在监监听内获取WebIvew加载内容的进度
                @Override
                public Object onCallBack(int pType, Object pArgs) {
                    switch (pType) {
                        case IWebviewStateListener.ON_WEBVIEW_READY:
                            // WebApp准备加载事件
                            // 准备完毕之后添加webview到显示父View中，
                            // 设置排版不显示状态，避免显示webview时html内容排版错乱问题
                            View view = ((IWebview) pArgs).obtainApp().obtainWebAppRootView().obtainMainView();
                            view.setVisibility(View.INVISIBLE);
                            rootView.addView(view, 0);
                            break;
                        case IWebviewStateListener.ON_PAGE_STARTED:
                            pd = ProgressDialog.show(activity, "加载中", "0/100");
                            break;
                        case IWebviewStateListener.ON_PROGRESS_CHANGED:
                            // WebApp首页面加载进度变化事件
                            if (pd != null) {
                                pd.setMessage(pArgs + "/100");
                            }
                            break;
                        case IWebviewStateListener.ON_PAGE_FINISHED:
                            // WebApp首页面加载完成事件
                            if (pd != null) {
                                pd.dismiss();
                                pd = null;
                            }
                            // 页面加载完毕，设置显示webview
                            app.obtainWebAppRootView().obtainMainView().setVisibility(View.VISIBLE);
                            break;
                    }
                    return null;
                }
            }, this);

            app.setIAppStatusListener(new IApp.IAppStatusListener() {
                // 设置APP运行事件监听
                @Override
                public boolean onStop() {
                    // 应用运行停止时调用
                    rootView.removeView(app.obtainWebAppRootView().obtainMainView());
                    // TODO Auto-generated method stub
                    return false;
                }

                @Override
                public String onStoped(boolean b, String s) {
                    return null;
                }

                @Override
                public void onStart() {
                    // 独立应用启动时触发事件
                }

                @Override
                public void onPause(IApp arg0, IApp arg1) {
                    // WebApp暂停运行时触发事件

                }
            });
        }

        @Override
        public void onCoreReady(ICore coreHandler) {
            // 初始化SDK并将5+引擎的对象设置给SDK
            SDK.initSDK(coreHandler);
            //
            SDK.requestAllFeature();
        }

        @Override
        public boolean onCoreStop() {
            // 当返回false时候回关闭activity
            return false;
        }

        @Override
        public Object onCreateSplash(Context pContextWrapper) {
            splashView = new FrameLayout(activity);
            splashView.setBackgroundResource(RInformation.DRAWABLE_SPLASH);
            rootView.addView(splashView);
            return null;
        }



        @Override
        public void onCloseSplash() {
            rootView.removeView(splashView);
        }
    }
}
