package com.example.earlylearning

import android.annotation.SuppressLint
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Hosts the games in a WebView.
 *
 * Copy the repo's `index.html` and `games/` folder into `app/src/main/assets/` so the
 * launcher and every game load from `file:///android_asset/`. The relative home-button
 * links (`../../index.html`) then work exactly as they do on the web.
 *
 * To open a single game directly, pass e.g. "games/shape-sorting/index.html".
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EducationalGameScreen(assetPath: String = "index.html") {
    var webView by remember { mutableStateOf<WebView?>(null) }

    // System back steps back inside the WebView (game -> launcher) before leaving the app.
    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true            // localStorage progress for the parent view
                    mediaPlaybackRequiresUserGesture = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    setSupportZoom(false)               // no accidental pinch-zoom by toddlers
                }
                // Keep every link inside the app instead of opening a browser.
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?) = false
                }
                // Route the games' console.log() output to Logcat (tag: LLM_Game_Debug).
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                        Log.d("LLM_Game_Debug", "${message.message()} -- ${message.sourceId()}:${message.lineNumber()}")
                        return true
                    }
                }
                loadUrl("file:///android_asset/$assetPath")
                webView = this
            }
        },
        onRelease = { webView = null }
    )
}
