package sk.brehy.news

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import sk.brehy.MainViewModel
import sk.brehy.MainActivity
import sk.brehy.R
import sk.brehy.adapters.News
import sk.brehy.adapters.NewsAdapter
import sk.brehy.databinding.FragmentNewsBinding
import sk.brehy.exception.BrehyException

class NewsFragment : Fragment() {

    private val newsModel: NewsViewModel by activityViewModels()
    private val databaseModel: MainViewModel by activityViewModels()
    private lateinit var binding: FragmentNewsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        try {
            binding = FragmentNewsBinding.inflate(inflater, container, false)
            return binding.root
        } catch (e: Exception) {
            throw BrehyException("Error inflating NewsFragment layout", e)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            newsModel.newsDatabase = databaseModel.newsDatabase

            newsModel.getSavedData()

            newsModel.newsList.observe(viewLifecycleOwner) { list ->
                try {
                    val adapter = NewsAdapter(requireContext(), list)
                    binding.listview.adapter = adapter
                    binding.listview.onItemClickListener =
                        AdapterView.OnItemClickListener { _, _, position, _ ->
                            try {
                                val news: News = list[position]
                                if (databaseModel.isConnectedToInternet(requireContext())) {
                                    newsModel.openedContent = news
                                    (activity as MainActivity).changeFragment(
                                        NewsContentFragment(),
                                        "news"
                                    )
                                } else {
                                    (activity as MainActivity).showToast(
                                        "You are not connected to the internet.",
                                        R.drawable.network_background,
                                        R.color.brown_light
                                    )
                                }
                            } catch (e: Exception) {
                                throw BrehyException("Error handling news item click", e)
                            }
                        }
                } catch (e: Exception) {
                    throw BrehyException("Error setting up news list adapter", e)
                }
            }
        } catch (e: Exception) {
            throw BrehyException("Error initializing NewsFragment view components", e)
        }
    }

    override fun onStart() {
        try {
            if (databaseModel.isConnectedToInternet(requireContext()))
                newsModel.getNewData(requireActivity())
            super.onStart()
        } catch (e: Exception) {
            throw BrehyException("Error during NewsFragment onStart operation", e)
        }
    }
}
