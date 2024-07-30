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

class NewsViewModel : ViewModel() {
    lateinit var openedContent: News
    lateinit var newsDatabase: DatabaseReference

    var newsList = MutableLiveData<MutableList<News>>()

    fun getSavedData() {
        newsDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
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
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("APP_data", "Failed to read value.", error.toException())
            }
        })
    }

    private fun updateDatabase(news: MutableList<News>) {
        newsDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val database = dataSnapshot.children
                val actualIDs = news.map { it.id }
                for (record in database) {
                    if (record.key !in actualIDs)
                        newsDatabase.child(record.key!!).removeValue()
                    else {
                        val entry = news.filter { it.id == record.key }.first()
                        if (record.child("title").value!! != entry.title)
                            newsDatabase.child(record.key!!).child("title")
                                .setValue(entry.title)
                        else if (record.child("content").value!! != entry.content)
                            newsDatabase.child(record.key!!).child("content")
                                .setValue(entry.content)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    fun getNewData(activity: FragmentActivity) {
        viewModelScope.async {
            val list = doInBackground(activity)
            onPostExecute(list)
        }
    }

    private suspend fun doInBackground(activity: FragmentActivity): MutableList<News> =
        withContext(Dispatchers.IO) { // to run code in Background Thread
            val list = mutableListOf<News>()
            try {
                val doc = Jsoup.connect("https://farabrehy.sk/aktuality.php").timeout(5000).get()
                val children = doc.getElementsByAttribute("onmouseover")
                for (e in children) {
                    val separate = e.attributes()["onclick"]
                    val indexStart = separate.indexOf("getElementById") + "getElementById('".length
                    val expandID =
                        separate.substring(indexStart, separate.indexOf("'", indexStart + 1))
                    val expansion = doc.getElementById(expandID)
                    val title = e.child(0).text()
                    var content = removeSpecificColor(expansion.children().toString())
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
                            "</style><body>" + changeImgSize(content, activity) + "</body></html>"
                    list.add(News(expandID, title, content))
                }
            } catch (exception: Exception) {
                Log.d("NewsViewModel", exception.message.toString())
                getSavedData()
            }
            return@withContext list
        }

    private fun removeSpecificColor(html: String): String {
        var newHTML = ""
        var lastIndex = 0
        Regex.fromLiteral("color: #").findAll(html).map { it.range.first }.toList()
            .map { index ->
                newHTML += html.substring(lastIndex, index - 1)
                lastIndex = html.indexOf(';', index)
            }
        newHTML += html.substring(lastIndex)
        return newHTML.replace("&nbsp;&nbsp;", "&nbsp;")
    }

    private fun onPostExecute(list: MutableList<News>) {
        if (list.isNotEmpty()) {
            updateDatabase(list)
            newsList.value = mutableListOf()
            newsList.value!!.addAll(list)
        }
    }

    fun changeImgSize(html: String, activity: FragmentActivity): String {
        var img = html.indexOf("img")
        if (img != -1) {
            val list = mutableListOf<Int>()
            val newHtml = StringBuffer(html)
            while (img >= 0) {
                list.add(img + 3)
                img = html.indexOf("img", img + 1)
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
                    val sizeH =
                        html.substring(indexStartHeight, indexEndHeight).toDouble()
                    newHtml.delete(indexStartWidth, indexEndWidth)
                    newHtml.insert(indexStartWidth, width.toInt())
                    newHtml.delete(indexStartHeight, indexEndHeight)
                    newHtml.insert(indexStartHeight, (sizeH / ratio).toInt())
                }
            }
            return newHtml.toString()
        } else return html
    }
}