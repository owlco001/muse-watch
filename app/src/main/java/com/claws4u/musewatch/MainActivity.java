package com.claws4u.musewatch;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

/**
 * Muse for H19 4G watch (Android 8.1) — GeckoView edition v2.3.
 * Watch UI optimizations for 466x466 square screen:
 * - Font boost for readability
 * - Larger tap targets
 * - Hidden scrollbars (cleaner)
 * - Native floating nav buttons (back/reload/home)
 * Plus v2.2 perf: no animations, system fonts, splash screen.
 */
public class MainActivity extends Activity {
    private static final String HOME_URL = "https://muse.ai";
    private static GeckoRuntime sRuntime;

    private static final String PERF_CSS =
        "*{animation:none!important;transition:none!important;scroll-behavior:auto!important}" +
        "*,*::before,*::after{animation-duration:0s!important;transition-duration:0s!important}" +
        "*{font-family:-apple-system,Roboto,'Noto Sans SC',sans-serif!important}" +
        "@font-face{font-display:swap!important}" +
        "html,body{overscroll-behavior:none!important}" +
        "*{backface-visibility:hidden}";

    // Watch UI adaptations for 466x466 ROUND screen
    private static final String WATCH_CSS =
        // Base font 18px (up from 16) for readability on tiny screen
        "html{font-size:18px!important}" +
        // Minimum 48px tap targets (watch-friendly)
        "button,input[type=button],input[type=submit],[role=button]{min-height:48px!important;min-width:48px!important}" +
        // Hide scrollbars for cleaner look
        "::-webkit-scrollbar{display:none!important}" +
        "*{scrollbar-width:none!important;-ms-overflow-style:none!important}" +
        // Prevent text size adjust weirdness
        "body{-webkit-text-size-adjust:100%!important}" +
        // ROUND SCREEN: keep content within visible circle (corners are cut off)
        // 32px safe padding approximates the circular safe area on 466px
        "body{padding-left:24px!important;padding-right:24px!important}" +
        // Ensure fixed/sticky elements don't hide in corners
        "*[style*='position:fixed']{max-width:418px!important;left:24px!important;right:24px!important}";

    private static final String INJECT_JS =
        "(function(){" +
        "var s=document.getElementById('musewatch-ui');" +
        "if(s)return;" +
        "s=document.createElement('style');" +
        "s.id='musewatch-ui';" +
        "s.textContent=\"" + (PERF_CSS + WATCH_CSS).replace("\"", "\\\"") + "\";" +
        "(document.head||document.documentElement).appendChild(s);" +
        // Ensure viewport for 466px width
        "var v=document.querySelector('meta[name=viewport]');" +
        "if(!v){v=document.createElement('meta');v.name='viewport';" +
        "document.head.appendChild(v);}" +
        "v.content='width=466,initial-scale=1,maximum-scale=1,user-scalable=no';" +
        "})();";

    private GeckoSession session;
    private GeckoView geckoView;
    private View splash;
    private boolean canGoBack = false;
    private boolean firstPageDone = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        hideSystemUi();

        if (sRuntime == null) {
            sRuntime = GeckoRuntime.create(this);
        }

        FrameLayout root = new FrameLayout(this);

        geckoView = new GeckoView(this);
        root.addView(geckoView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        // Splash
        TextView tv = new TextView(this);
        tv.setText("Muse\n加载中…");
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(20);
        tv.setGravity(Gravity.CENTER);
        tv.setBackgroundColor(Color.parseColor("#1a1a2e"));
        splash = tv;
        root.addView(splash, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        // Floating nav buttons: bottom-CENTER for round screen
        // (corners are cut off on circular display, so avoid bottom-right)
        LinearLayout navBar = new LinearLayout(this);
        navBar.setOrientation(LinearLayout.HORIZONTAL);
        FrameLayout.LayoutParams navParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        navParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        navParams.bottomMargin = 40;  // inset from round edge

        Button backBtn = makeNavButton("‹");
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { goBack(); }
        });
        Button reloadBtn = makeNavButton("↻");
        reloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (session != null) session.reload();
            }
        });
        Button homeBtn = makeNavButton("⌂");
        homeBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (session != null) session.loadUri(HOME_URL);
            }
        });

        navBar.addView(backBtn);
        navBar.addView(reloadBtn);
        navBar.addView(homeBtn);
        root.addView(navBar, navParams);

        setContentView(root);

        session = new GeckoSession();
        session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Override
            public void onCanGoBack(GeckoSession s, boolean v) {
                canGoBack = v;
            }
        });
        session.setContentDelegate(new GeckoSession.ContentDelegate() {});
        session.setProgressDelegate(new GeckoSession.ProgressDelegate() {
            @Override
            public void onPageStop(GeckoSession s, boolean success) {
                if (success) {
                    s.loadUri("javascript:" + INJECT_JS);
                }
                if (!firstPageDone && success) {
                    firstPageDone = true;
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            if (splash != null) splash.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
        geckoView.setSession(session);
        session.open(sRuntime);
        session.loadUri(HOME_URL);
    }

    private Button makeNavButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(20);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(Color.parseColor("#80000000"));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(84, 84);
        p.rightMargin = 10;
        p.leftMargin = 10;
        b.setLayoutParams(p);
        return b;
    }

    private void goBack() {
        if (canGoBack && session != null) {
            session.goBack();
        }
    }

    private void hideSystemUi() {
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUi();
    }

    @Override
    public void onBackPressed() {
        if (canGoBack && session != null) {
            session.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (session != null) {
            session.close();
            session = null;
        }
        super.onDestroy();
    }
}
