package sk.brehy.news

import android.util.DisplayMetrics
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import sk.brehy.MainActivity
import sk.brehy.adapters.News
import sk.brehy.exception.BrehyException

class NewsViewModel : ViewModel() {
    lateinit var openedContent: News
    lateinit var newsDatabase: DatabaseReference

    var newsList = MutableLiveData<MutableList<News>>()

    fun getSavedData() {
        try {
            newsDatabase.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    try {
                        val list = mutableListOf<News>()
                        val children = dataSnapshot.children
                        for (child in children) {
                            val ID = child.children
                            var title = ""
                            var content = ""
                            for (news_info in ID) {
                                when (news_info.key) {
                                    "title" -> title = news_info.value as String
                                    "content" -> content = news_info.value as String
                                }
                            }
                            list.add(News(child.key!!, title, content))
                        }
                        newsList.value = mutableListOf()
                        newsList.value!!.addAll(list.sortedWith(compareByDescending<News> { it.id.toLong() }))
                    } catch (e: Exception) {
                        throw BrehyException("Error processing saved news data", e)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("APP_data", "Failed to read value.", error.toException())
                }
            })
        } catch (e: Exception) {
            throw BrehyException("Error adding database listener for saved news data", e)
        }
    }

    private fun updateDatabase(news: MutableList<News>) {
        try {
            newsDatabase.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    try {
                        val database = dataSnapshot.children
                        for (entry in news) {
                            newsDatabase.child(entry.id).child("title").setValue(entry.title)
                            newsDatabase.child(entry.id).child("content").setValue(entry.content)
                        }
                        val newsID = news.map { it.id }
                        database.filter { it.key !in newsID }.forEach { oldEntry ->
                            newsDatabase.child(oldEntry.key!!).removeValue()
                        }
                    } catch (e: Exception) {
                        throw BrehyException("Error updating news database", e)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("NewsViewModel", "Update database cancelled", error.toException())
                }
            })
        } catch (e: Exception) {
            throw BrehyException("Error adding database listener for updating news", e)
        }
    }

    fun getNewData(activity: FragmentActivity) {
        try {
            viewModelScope.async {
                val list = doInBackground(activity)
                onPostExecute(list)
            }
        } catch (e: Exception) {
            throw BrehyException("Error launching coroutine to get new data", e)
        }
    }

    private suspend fun doInBackground(activity: FragmentActivity): MutableList<News> =
        withContext(Dispatchers.IO) {
            val list = mutableListOf<News>()
            try {
                val doc = Jsoup.connect("https://farabrehy.sk/aktuality.php").timeout(5000).get()
                val children = doc.getElementsByAttribute("onmouseover")
                for (e in children) {
                    try {
                        val separate = e.attributes()["onclick"]
                        val indexStart =
                            separate.indexOf("getElementById") + "getElementById('".length
                        val expandID =
                            separate.substring(indexStart, separate.indexOf("'", indexStart + 1))
                        val expansion = doc.getElementById(expandID)
                        val title = e.child(0).text()
                        var content = removeSpecificColor(expansion?.children().toString())
                        content = "<html> <style>" +
                                "a, strong {" +
                                "  overflow-wrap: break-word;" +
                                "  word-wrap: break-word;" +
                                "  -ms-word-break: break-all;" +
                                "  word-break: break-all;" +
                                "  word-break: break-word;" +
                                "  -ms-hyphens: auto;" +
                                "  -moz-hyphens: auto;" +
                                "  -webkit-hyphens: auto;" +
                                "  hyphens: auto;" +
                                "}" +
                                "p, span {margin: 0px !important;" +
                                "padding-bottom: 8px !important;" +
                                "text-align: justify;" +
                                "font-size: 16px !important;" +
                                "}" +
                                "</style><body>" + changeImgSize(
                            content,
                            activity
                        ) + "</body></html>"

                        list.add(News(expandID, title, content))
                    } catch (exception: Exception) {
                        Log.d("NewsModel:AddList", exception.message.toString())
                    }
                }
            } catch (exception: Exception) {
                Log.d("NewsModel", exception.message.toString())
                getSavedData()
            }
            list
        }

    private fun removeSpecificColor(html: String): String {
        try {
            var newHTML = ""
            var lastIndex = 0
            Regex.fromLiteral("color: #").findAll(html).map { it.range.first }.toList()
                .map { index ->
                    newHTML += html.substring(lastIndex, index - 1)
                    lastIndex = html.indexOf(';', index)
                }
            newHTML += html.substring(lastIndex)
            return newHTML.replace("&nbsp;&nbsp;", "&nbsp;")
        } catch (e: Exception) {
            throw BrehyException("Error removing specific color from HTML", e)
        }
    }

    private fun onPostExecute(list: MutableList<News>) {
        try {
            if (list.isNotEmpty()) {
                updateDatabase(list)
                newsList.value = mutableListOf()
                newsList.value!!.addAll(list)
            }
        } catch (e: Exception) {
            throw BrehyException("Error updating news list after data fetch", e)
        }
    }

    fun changeImgSize(HTML: String, activity: FragmentActivity): String {
        return try {
            var html = HTML
            var alt = html.indexOf("alt=\"")
            if (alt != -1) {
                while (alt >= 0) {
                    val end = html.indexOf("\"", alt + 5) + 1
                    html = html.removeRange(alt, end)
                    alt = html.indexOf("alt=\"", alt + 1)
                }
            }
            var img = html.indexOf("<img")
            if (img != -1) {
                val list = mutableListOf<Int>()
                val newHtml = StringBuffer(html)
                while (img >= 0) {
                    list.add(img + 4)
                    img = html.indexOf("<img", img + 1)
                }
                val displayMetrics = DisplayMetrics()
                (activity as MainActivity).windowManager.defaultDisplay.getMetrics(displayMetrics)
                val width = displayMetrics.widthPixels / displayMetrics.density - 50.0
                for (index in list) {
                    val indexStartWidth = html.indexOf("width", index) + 7
                    val indexEndWidth = html.indexOf("\"", indexStartWidth + 1)
                    val sizeW = html.substring(indexStartWidth, indexEndWidth).toDouble()
                    if (sizeW > width) {
                        val ratio = sizeW / width
                        val indexStartHeight = html.indexOf("height", index) + 8
                        val indexEndHeight = html.indexOf("\"", indexStartHeight + 1)
                        val sizeH = html.substring(indexStartHeight, indexEndHeight).toDouble()
                        newHtml.delete(indexStartWidth, indexEndWidth)
                        newHtml.insert(indexStartWidth, width.toInt())
                        newHtml.delete(indexStartHeight, indexEndHeight)
                        newHtml.insert(indexStartHeight, (sizeH / ratio).toInt())
                    }
                }
                newHtml.toString()
            } else html
        } catch (e: Exception) {
            throw BrehyException("Error resizing images in HTML content", e)
        }
    }
}
