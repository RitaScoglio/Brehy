package sk.brehy.news

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import sk.brehy.adapters.News
import java.util.Calendar

class NewsViewModel : ViewModel() {
    lateinit var openedContent: News
}