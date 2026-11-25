package io.github.matt2868.lifeplanner

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebViewAssetLoader

/** Hosts the planner page (assets/www/index.html) full screen and links it to the widget. */
class MainActivity : ComponentActivity() {
    private lateinit var web: WebView
    private var pageReady = false
    private var pendingAction: String? = null

    private val assetLoader by lazy {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val background = if (night) 0xFF111317.toInt() else 0xFFFFFFFF.toInt()

        val root = FrameLayout(this)
        root.setBackgroundColor(background)
        web = WebView(this)
        web.setBackgroundColor(background)
        root.addView(
            web,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        setContentView(root)

        WindowInsetsControllerCompat(window, root).apply {
            isAppearanceLightStatusBars = !night
            isAppearanceLightNavigationBars = !night
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout() or
                    WindowInsetsCompat.Type.ime()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
        }
        web.addJavascriptInterface(Bridge(applicationContext), "PlannerApp")
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                assetLoader.shouldInterceptRequest(request.url)

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (request.url.host == HOST) return false
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, request.url))
                } catch (_: Exception) {
                }
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                pageReady = true
                pendingAction?.let { runAction(it) }
                pendingAction = null
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                web.evaluateJavascript("window.__back?String(window.__back()):'false'") { result ->
                    if (result?.trim('"') != "true") {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        })

        pendingAction = intent?.getStringExtra(EXTRA_ACTION)
        web.loadUrl("https://$HOST/assets/www/index.html")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val action = intent.getStringExtra(EXTRA_ACTION) ?: return
        if (pageReady) runAction(action) else pendingAction = action
    }

    private fun runAction(action: String) {
        if (action == "add") web.evaluateJavascript("window.__openAdd&&window.__openAdd()", null)
    }

    override fun onResume() {
        super.onResume()
        if (pageReady) web.evaluateJavascript("window.__resume&&window.__resume()", null)
    }

    override fun onPause() {
        if (pageReady) web.evaluateJavascript("window.__pause&&window.__pause()", null)
        super.onPause()
    }

    companion object {
        const val HOST = "appassets.androidplatform.net"
        const val EXTRA_ACTION = "planner_action"
    }
}
