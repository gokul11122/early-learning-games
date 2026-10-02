package io.github.gokul11122.earlylearning

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebViewAssetLoader

/**
 * One full-screen WebView that plays the games bundled in the APK's assets.
 *
 * The games are served from https://appassets.androidplatform.net/assets/ (WebViewAssetLoader),
 * never from file://. That origin is what the children's progress (localStorage) is stored under,
 * so it must stay the same in every future version, or progress would appear to be lost.
 */
class MainActivity : ComponentActivity() {

    private lateinit var root: FrameLayout
    private var webView: WebView? = null
    private val rendererCrashTimes = ArrayDeque<Long>()

    private val assetLoader by lazy {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
    }

    // Back inside a game returns to the launcher; back on the launcher leaves the app as usual.
    private val backToLauncher = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            webView?.loadUrl(HOME_URL)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(PAPER, PAPER),
            // Second colour = the scrim Android 7.x needs behind its always-white navigation buttons.
            navigationBarStyle = SystemBarStyle.light(PAPER, INK),
        )
        super.onCreate(savedInstanceState)

        root = FrameLayout(this).apply { setBackgroundColor(PAPER) }
        // Keep the games clear of the status bar, navigation bar and camera cut-out.
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        setContentView(root)
        onBackPressedDispatcher.addCallback(this, backToLauncher)

        // After Android killed the app in the background, reopen the page the child was on.
        val restoreUrl = savedInstanceState?.getString(KEY_URL)?.takeIf { isOurPage(it) }
        showWebView(restoreUrl ?: HOME_URL)
    }

    /** Creates the WebView, or shows a plain message if the phone's WebView is unavailable. */
    private fun showWebView(url: String) {
        val view = try {
            if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                WebView.setWebContentsDebuggingEnabled(true)
            }
            createWebView()
        } catch (e: Throwable) {
            // Happens while "Android System WebView" is being updated by the Play Store.
            Log.e(TAG, "WebView unavailable", e)
            showMessage(getString(R.string.webview_missing))
            return
        }
        webView = view
        root.addView(view, FrameLayout.LayoutParams(MATCH, MATCH))
        view.loadUrl(url)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView = WebView(this).apply {
        setBackgroundColor(PAPER)
        overScrollMode = View.OVER_SCROLL_NEVER
        isHapticFeedbackEnabled = false
        // No text-selection pop-up when a child presses and holds (the games handle holds themselves).
        setOnLongClickListener { true }

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true                 // localStorage = the children's progress
            mediaPlaybackRequiresUserGesture = false // Web Audio feedback tones
            allowFileAccess = false                  // everything comes through the asset loader
            allowContentAccess = false
            setSupportZoom(false)                    // no accidental pinch-zoom by toddlers
            builtInZoomControls = false
            displayZoomControls = false
            textZoom = 100                           // phone "large font" setting must not break layouts
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                assetLoader.shouldInterceptRequest(request.url)

            // Stay inside the app; the games have no outside links, so anything else is dropped.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                !isOurPage(request.url.toString())

            override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                backToLauncher.isEnabled = url != null && isOurPage(url) && !isHome(url)
            }

            // If the WebView's renderer crashes or is killed for memory, Android would otherwise close
            // the whole app. Instead, throw the dead WebView away and start a fresh one on the launcher.
            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                Log.e(TAG, "WebView renderer gone (crashed=${detail.didCrash()}), restarting")
                if (view === webView) {
                    root.removeView(view)
                    webView = null
                    view.destroy()
                    // A page that kills the renderer every time must not restart forever.
                    val now = System.currentTimeMillis()
                    rendererCrashTimes.addLast(now)
                    while (now - rendererCrashTimes.first() > CRASH_WINDOW_MS) rendererCrashTimes.removeFirst()
                    if (rendererCrashTimes.size >= MAX_CRASHES_IN_WINDOW) {
                        showMessage(getString(R.string.webview_missing))
                    } else {
                        showWebView(HOME_URL)
                    }
                }
                return true
            }
        }

        // The games' console.log() lines go to Logcat: adb logcat -s EarlyLearning
        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                Log.d(TAG, "${message.message()} -- ${message.sourceId()}:${message.lineNumber()}")
                return true
            }
        }
    }

    private fun showMessage(text: String) {
        backToLauncher.isEnabled = false
        root.removeAllViews()
        root.addView(TextView(this).apply {
            this.text = text
            textSize = 20f
            setTextColor(INK)
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }, FrameLayout.LayoutParams(MATCH, MATCH))
    }

    override fun onResume() {
        super.onResume()
        // Coming back after the "WebView unavailable" message: try again.
        if (webView == null) {
            root.removeAllViews()
            showWebView(HOME_URL)
        }
        webView?.onResume()
        webView?.resumeTimers()
    }

    // Leaving the app pauses the game (animations, timers, sound) instead of running in the background.
    override fun onPause() {
        webView?.onPause()
        webView?.pauseTimers()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView?.url?.let { outState.putString(KEY_URL, it) }
    }

    override fun onDestroy() {
        webView?.let {
            root.removeView(it)
            it.destroy()
        }
        webView = null
        super.onDestroy()
    }

    private companion object {
        const val TAG = "EarlyLearning"
        const val KEY_URL = "url"
        const val HOST = WebViewAssetLoader.DEFAULT_DOMAIN // appassets.androidplatform.net — never change
        const val HOME_URL = "https://$HOST/assets/index.html"
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val CRASH_WINDOW_MS = 30_000L
        const val MAX_CRASHES_IN_WINDOW = 3
        val PAPER = Color.parseColor("#F6F1E7")
        val INK = Color.parseColor("#4A4036")

        fun isOurPage(url: String): Boolean = Uri.parse(url).let { it.scheme == "https" && it.host == HOST }
        fun isHome(url: String): Boolean = Uri.parse(url).path.let { it == "/assets/index.html" || it == "/assets/" }
    }
}
