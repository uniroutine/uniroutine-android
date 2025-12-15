package com.uniroutine.uniroutine

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.core.view.WindowInsetsControllerCompat

@SuppressLint("SetJavaScriptEnabled")
class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView

    private val allowedHost = "uniroutine.bearcry.icu"
    private val allowedScheme = "https"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ---- STATUS BAR: ALWAYS BLACK ----
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.BLACK
        WindowInsetsControllerCompat(window, window.decorView)
            .isAppearanceLightStatusBars = false

        // ---- WEBVIEW INIT ----
        webView = WebView(this)
        configureWebView()

        // ---- BACK BUTTON HANDLING ----
        onBackPressedDispatcher.addCallback(this) {
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                finish()
            }
        }

        setContentView(webView)
        webView.loadUrl("$allowedScheme://$allowedHost")
    }

    private fun configureWebView() {
        val settings: WebSettings = webView.settings

        // ---- REQUIRED FUNCTIONALITY ----
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true

        // ---- SECURITY HARDENING ----
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.setSupportMultipleWindows(false)
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.mediaPlaybackRequiresUserGesture = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.safeBrowsingEnabled = true
        }

        // ---- FORCE WEBVIEW THEME TO MATCH SYSTEM ----
        val isSystemDark =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            settings.forceDark =
                if (isSystemDark)
                    WebSettings.FORCE_DARK_ON
                else
                    WebSettings.FORCE_DARK_OFF
        }

        // ---- URL + SSL RESTRICTION ----
        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val uri: Uri = request.url
                return uri.scheme != allowedScheme || uri.host != allowedHost
            }

            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                // NEVER ignore SSL errors
                handler.cancel()
            }

            // ---- OFFLINE DETECTION FIX ----
            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    // Trigger website's existing offline popup
                    view.evaluateJavascript(
                        "window.dispatchEvent(new Event('offline'));",
                        null
                    )
                }
            }
        }

        // ---- BASIC CHROME ----
        webView.webChromeClient = WebChromeClient()
    }

    override fun onDestroy() {
        webView.apply {
            stopLoading()
            clearHistory()
            removeAllViews()
            destroy()
        }
        super.onDestroy()
    }
}
