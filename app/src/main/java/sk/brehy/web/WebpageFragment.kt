package sk.brehy.web

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import sk.brehy.databinding.FragmentWebpageBinding
import sk.brehy.exception.BrehyException

class WebpageFragment : Fragment() {

    private lateinit var binding: FragmentWebpageBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return try {
            binding = FragmentWebpageBinding.inflate(inflater, container, false)
            binding.root
        } catch (e: Exception) {
            throw BrehyException("Error inflating WebpageFragment layout", e)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            val webSettings = binding.webview.settings
            webSettings.javaScriptEnabled = true
            webSettings.domStorageEnabled = true
            webSettings.builtInZoomControls = true

            binding.webview.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    return try {
                        view.loadUrl(url)
                        true
                    } catch (e: Exception) {
                        throw BrehyException("Error loading URL: $url", e)
                    }
                }
            }

            binding.webview.loadUrl("https://farabrehy.sk")
        } catch (e: Exception) {
            throw BrehyException("Error setting up WebView", e)
        }
    }
}