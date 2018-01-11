package com.maven.quizbrowsertest

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceActivity
import android.preference.PreferenceManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val jsInterface: JSInterface = JSInterface()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        WebView.setWebContentsDebuggingEnabled(true)
        webView.webViewClient = webViewClient
        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(jsInterface,"APP_DATA")
        jsInterface.setKey(readApiKeyFromPrefs())
        webView.loadUrl("file:///android_asset/read_api_key.html")
    }


    class JSInterface {
        private var _key: String=""

        fun setKey(key: String) { _key=key}

        @JavascriptInterface
        fun getKeyJson(): String = "{\"api_key\": \"_key\"}"

        @JavascriptInterface
        fun getKey(): String = _key
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

    private fun readApiKeyFromPrefs(): String{
        return PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.pref_key_api_key), "missing-key")
    }
}
