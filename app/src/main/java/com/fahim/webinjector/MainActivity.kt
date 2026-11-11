package com.fahim.webinjector

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.fahim.webinjector.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var scriptManager: ScriptManager

    // Configurable home URL – change here if needed
    private val homeUrl = "https://aincradmods.com/getkey"

    private var isPageLoading = false
    private var isEditingScript = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        scriptManager = ScriptManager(this)

        setupToolbar()
        setupScriptPanel()
        setupWebView()
        setupNavigationControls()
        setupApplyButton()
        setupBackPressHandler()

        // Load home page
        if (isNetworkAvailable()) {
            binding.webView.loadUrl(homeUrl)
        } else {
            showErrorSnackbar(getString(R.string.network_error), true)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.subtitle = getString(R.string.developer)
        binding.toolbar.setSubtitleTextColor(getColor(R.color.text_secondary))
    }

    private fun setupScriptPanel() {
        // Load saved (or default) script
        binding.scriptEditor.setText(scriptManager.getScript())
        binding.scriptEditor.isEnabled = false
        binding.btnSave.isEnabled = false

        binding.btnEdit.setOnClickListener {
            isEditingScript = true
            binding.scriptEditor.isEnabled = true
            binding.scriptEditor.requestFocus()
            binding.btnSave.isEnabled = true
            binding.btnEdit.isEnabled = false
            Toast.makeText(this, "Editing enabled", Toast.LENGTH_SHORT).show()
        }

        binding.btnSave.setOnClickListener {
            val script = binding.scriptEditor.text?.toString()?.trim().orEmpty()
            if (script.isEmpty()) {
                Toast.makeText(this, getString(R.string.empty_script), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            scriptManager.saveScript(script)
            isEditingScript = false
            binding.scriptEditor.isEnabled = false
            binding.btnSave.isEnabled = false
            binding.btnEdit.isEnabled = true
            Toast.makeText(this, getString(R.string.script_saved), Toast.LENGTH_SHORT).show()
        }

        binding.btnReset.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.confirm_reset_title)
                .setMessage(R.string.confirm_reset_message)
                .setPositiveButton(R.string.yes) { _, _ ->
                    scriptManager.resetToDefault()
                    binding.scriptEditor.setText(scriptManager.getScript())
                    isEditingScript = false
                    binding.scriptEditor.isEnabled = false
                    binding.btnSave.isEnabled = false
                    binding.btnEdit.isEnabled = true
                    Toast.makeText(this, getString(R.string.script_reset), Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webView = binding.webView
        val settings = webView.settings

        // Core WebView settings required by the spec
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.allowFileAccess = false
        settings.allowContentAccess = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.mediaPlaybackRequiresUserGesture = false
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(false)

        // Cookies & session
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                // Let WebView handle all navigation (redirects, external links, Cloudflare, etc.)
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                isPageLoading = true
                binding.progressBar.isVisible = true
                binding.progressBar.progress = 0
                updateNavButtons()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isPageLoading = false
                binding.progressBar.isVisible = false
                updateNavButtons()
                // Persist cookies
                CookieManager.getInstance().flush()
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    isPageLoading = false
                    binding.progressBar.isVisible = false
                    showErrorSnackbar(getString(R.string.load_error), true)
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: android.net.http.SslError?
            ) {
                // Secure handling – do NOT automatically proceed
                // Show a dialog so the user can decide (common for development / self-signed)
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("SSL Certificate Error")
                    .setMessage("The site has an untrusted certificate. Proceed anyway? (Not recommended for production)")
                    .setPositiveButton("Proceed") { _, _ -> handler?.proceed() }
                    .setNegativeButton("Cancel") { _, _ -> handler?.cancel() }
                    .setCancelable(false)
                    .show()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                binding.progressBar.progress = newProgress
                if (newProgress >= 100) {
                    binding.progressBar.isVisible = false
                } else {
                    binding.progressBar.isVisible = true
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                // Optional: could show title in toolbar
            }
        }
    }

    private fun setupNavigationControls() {
        binding.btnBack.setOnClickListener {
            if (binding.webView.canGoBack()) {
                binding.webView.goBack()
            }
        }
        binding.btnForward.setOnClickListener {
            if (binding.webView.canGoForward()) {
                binding.webView.goForward()
            }
        }
        binding.btnReload.setOnClickListener {
            if (isNetworkAvailable()) {
                binding.webView.reload()
            } else {
                showErrorSnackbar(getString(R.string.network_error), true)
            }
        }
        binding.btnHome.setOnClickListener {
            if (isNetworkAvailable()) {
                binding.webView.loadUrl(homeUrl)
            } else {
                showErrorSnackbar(getString(R.string.network_error), true)
            }
        }
        updateNavButtons()
    }

    private fun updateNavButtons() {
        binding.btnBack.isEnabled = binding.webView.canGoBack()
        binding.btnForward.isEnabled = binding.webView.canGoForward()
        binding.btnBack.alpha = if (binding.webView.canGoBack()) 1f else 0.4f
        binding.btnForward.alpha = if (binding.webView.canGoForward()) 1f else 0.4f
    }

    private fun setupApplyButton() {
        binding.btnApply.setOnClickListener {
            executeJavaScript()
        }
    }

    private fun executeJavaScript() {
        if (isPageLoading) {
            Toast.makeText(this, getString(R.string.page_loading), Toast.LENGTH_SHORT).show()
            return
        }

        val currentUrl = binding.webView.url
        if (currentUrl.isNullOrBlank() || currentUrl == "about:blank") {
            Toast.makeText(this, getString(R.string.no_page), Toast.LENGTH_SHORT).show()
            return
        }

        // Always use the currently saved script (not just the editor text)
        var script = scriptManager.getScript().trim()
        if (script.isEmpty()) {
            Toast.makeText(this, getString(R.string.empty_script), Toast.LENGTH_SHORT).show()
            return
        }

        // Strip the "javascript:" prefix if present – evaluateJavascript expects pure JS
        if (script.startsWith("javascript:", ignoreCase = true)) {
            script = script.substring("javascript:".length)
        }

        // Execute in the context of the currently loaded page
        binding.webView.evaluateJavascript(script) { result ->
            // result is a JSON-encoded string or "null"
            runOnUiThread {
                if (result == null || result == "null") {
                    // Many bookmarklets return undefined → "null". Still considered success unless an exception occurred.
                    // We show a gentle success toast; real failures are usually surfaced via alert() inside the script.
                    Toast.makeText(this, getString(R.string.js_success), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getString(R.string.js_success), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webView.canGoBack()) {
                    binding.webView.goBack()
                } else {
                    // Exit only when no history left
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun showErrorSnackbar(message: String, showRetry: Boolean) {
        val snack = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        if (showRetry) {
            snack.setAction(R.string.retry) {
                if (isNetworkAvailable()) {
                    binding.webView.loadUrl(homeUrl)
                } else {
                    Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
        snack.show()
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
        binding.webView.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.webView.onResume()
    }

    override fun onDestroy() {
        binding.webView.stopLoading()
        binding.webView.destroy()
        super.onDestroy()
    }
}
