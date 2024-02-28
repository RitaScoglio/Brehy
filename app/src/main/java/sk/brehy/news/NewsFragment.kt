package sk.brehy.news

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.activityViewModels
import sk.brehy.MainViewModel
import sk.brehy.MainActivity
import sk.brehy.R
import sk.brehy.adapters.News
import sk.brehy.adapters.NewsAdapter
import sk.brehy.databinding.FragmentNewsBinding

class NewsFragment : Fragment() {

    private val newsModel: NewsViewModel by activityViewModels()
    private val databaseModel: MainViewModel by activityViewModels()
    private lateinit var binding: FragmentNewsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        newsModel.newsList.observe(viewLifecycleOwner) { list ->
            val adapter = NewsAdapter(requireContext(), list)
            binding.listview.adapter = adapter
            binding.listview.onItemClickListener =
                AdapterView.OnItemClickListener { _, _, position, _ ->
                    val news: News = list[position]
                    if (databaseModel.isConnectedToInternet(requireContext())) {
                        newsModel.openedContent = news
                        (activity as MainActivity).changeFragment(NewsContentFragment(), "news")
                    } else {
                        (activity as MainActivity).showToast(
                            "Nie ste pripojený na internet.",
                            R.drawable.network_background,
                            R.color.brown_light
                        )
                    }
                }
        }
    }

}