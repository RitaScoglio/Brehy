package sk.brehy.news

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import sk.brehy.R
import sk.brehy.databinding.FragmentNewsContentBinding

class NewsContentFragment : Fragment() {

    private val newsModel: NewsViewModel by activityViewModels()
    private lateinit var binding: FragmentNewsContentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNewsContentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showContent()
    }

    private fun showContent() {
        binding.title.text = newsModel.openedContent.title
        binding.date.text = newsModel.openedContent.date
        binding.content.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.brown_superlight
            )
        )
        binding.content.loadData(newsModel.openedContent.content, "text/html", "windows-1250")
        val webSettings = binding.content.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.defaultTextEncodingName = "utf-8"
    }
}