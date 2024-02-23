package sk.brehy.news

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import sk.brehy.DatabaseMainViewModel
import sk.brehy.R
import sk.brehy.databinding.FragmentNewsBinding
import sk.brehy.databinding.FragmentNewsContentBinding

class NewsContentFragment : Fragment() {
    companion object {
        fun newInstance() = NewsFragment()
    }

    private lateinit var newsModel: NewsViewModel
    private lateinit var databaseModel: DatabaseMainViewModel
    private lateinit var binding: FragmentNewsContentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNewsContentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        newsModel = ViewModelProvider(requireActivity()).get(NewsViewModel::class.java)
        databaseModel = ViewModelProvider(requireActivity()).get(DatabaseMainViewModel::class.java)
        showContent()
    }

    private fun showContent() {
        binding.title.setText(newsModel.openedContent.title)
        binding.date.setText(newsModel.openedContent.date)
        binding.content.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.brown_superlight))
        binding.content.loadData(newsModel.openedContent.content, "text/html", "windows-1250")
        val webSettings = binding.content.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.defaultTextEncodingName = "utf-8"
    }
}