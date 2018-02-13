package com.maven.quizbrowsertest

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceActivity
import android.preference.PreferenceManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.ListAdapter
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val jsInterface: JSInterface = JSInterface()
    private lateinit var urlAdapter: ListAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false)

        setContentView(R.layout.activity_main)
        setupUrlSpinner()
        setupWebView()
        setupProgressBar()
    }

    override fun onResume() {
        super.onResume()

        loadUrl("file:///android_asset/read_api_key.html") // temp
    }

    private fun setupUrlSpinner(){
        urlAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, resources.getStringArray(R.array.standard_urls))
        editSpinner.setAdapter(urlAdapter)

        editSpinner.setOnItemClickListener { parent, view, position, id ->
            loadUrl(urlAdapter.getItem(position) as String)
        }

        editSpinner.setOnEditorActionListener { v, actionId, event ->
            Log.i("editListener", "action=" + actionId)
            when (actionId) {
                EditorInfo.IME_ACTION_DONE,
                EditorInfo.IME_ACTION_SEND,
                EditorInfo.IME_ACTION_GO,
                EditorInfo.IME_ACTION_SEARCH -> {
                    Log.i("edit done", "url=" + editSpinner.text.toString())
                    loadUrl(editSpinner.text.toString())
                    true
                }
                else -> {
                    false
                }
            }
        }

        editSpinner.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus)
                loadUrl(editSpinner.text.toString())
        }
    }

    private fun loadUrl(url: String){
        // validate?
        closeKeyboard()
        jsInterface.setKey(readApiKeyFromPrefs())    // refresh key
        jsInterface.setMessageHandler { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
        webView.loadUrl(url, hashMapOf("api-key" to readApiKeyFromPrefs()))
        webView.requestFocus()
        editSpinner.setText(url)
        progressBar.progress = 0
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        WebView.setWebContentsDebuggingEnabled(true)
        webView.webViewClient = webViewClient
        webView.webChromeClient = webChromeClient
        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(jsInterface, "APP_INTERFACE")
    }

    private fun setupProgressBar(){
        progressBar.max = 100
    }

    class JSInterface {
        private var _key: String=""
        private var _messageHandler: ((String)->Unit)? = null

        fun setKey(key: String) { _key=key}
        fun setMessageHandler( handler: (String)->Unit) { _messageHandler=handler}

        @JavascriptInterface
        fun getKeyJson(): String = "{\"api_key\": \"_key\"}"

        @JavascriptInterface
        fun getKey(): String = _key

        @JavascriptInterface
        fun sendMessage(msg: String) {
            _messageHandler?.invoke(msg)
        }
    }

    override fun onBackPressed() {
        val bfList = webView.copyBackForwardList()
        if (bfList.currentIndex>0)
            webView.goBack()
        else
            super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.let{
            return when(it.itemId){
                R.id.action_push_key -> {
                    webView.evaluateJavascript("updateKey(\"pushed-api-key\")", null)
                    true
                }
                R.id.action_refresh -> {
                    webView.reload()
                    true
                }
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java).apply{
                        putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT,
                                SettingsActivity.GeneralPreferenceFragment::class.java.name)
                    })
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }

        return false
    }

    private val webViewClient = object: WebViewClient(){
        override fun onPageFinished(view: WebView?, url: String?) {
            Log.i("onPageFinished","page load complete")
            view?.apply{
                evaluateJavascript("window.mvnapikey='" + readApiKeyFromPrefs() + "'", null)

                evaluateJavascript("setKey('" + readApiKeyFromPrefs() + "')", null)
            }
            super.onPageFinished(view, url)
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            view?.apply{
                evaluateJavascript("window.mvnapikey='" + readApiKeyFromPrefs() + "'", null)
            }
            super.onPageStarted(view, url, favicon)
        }
    }

    private val webChromeClient = object: WebChromeClient(){
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            progressBar.progress = newProgress
        }
    }

    private fun readApiKeyFromPrefs(): String{
        return PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.pref_key_api_key), "missing-key")
    }

    private fun closeKeyboard(){
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(currentFocus?.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }
}
