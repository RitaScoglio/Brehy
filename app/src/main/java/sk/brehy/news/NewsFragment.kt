package sk.brehy.news

import android.os.AsyncTask
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import org.jsoup.Jsoup
import sk.brehy.DatabaseMainViewModel
import sk.brehy.MainActivity
import sk.brehy.R
import sk.brehy.adapters.News
import sk.brehy.adapters.NewsAdapter
import sk.brehy.databinding.FragmentNewsBinding
import java.util.Collections

class NewsFragment : Fragment() {

    companion object {
        fun newInstance() = NewsFragment()
    }

    private lateinit var newsModel: NewsViewModel
    private lateinit var databaseModel: DatabaseMainViewModel
    private lateinit var binding: FragmentNewsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        newsModel = ViewModelProvider(this).get(NewsViewModel::class.java)
        databaseModel = ViewModelProvider(requireActivity()).get(DatabaseMainViewModel::class.java)

        if (databaseModel.isConnectedToInternet(requireContext())) {
            GetData().execute()
        } else {
            savedData
        }
    }

    private val savedData: Unit
        private get() {
            databaseModel.newsDatabase.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val list: ArrayList<News> = ArrayList<News>()
                    val children = dataSnapshot.children
                    for (child in children) {
                        val node = child.children
                        var title: String = ""
                        var date: String = ""
                        var content_list: String = ""
                        var content: String = ""
                        for (news_info in node) {
                            when (news_info.key) {
                                "title" -> title = news_info.value as String
                                "date" -> date = news_info.value as String
                                "content-list" -> content_list = news_info.value as String
                                "content" -> content = news_info.value as String
                            }
                        }
                        list.add(News(child.key!!.toLong(), title, date, content_list, content))
                    }
                    Collections.sort(list, News.nodeComparator)
                    writeToListView(list)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("APP_data", "Failed to read value.", error.toException())
                }
            })
        }

    private fun writeToDatabase(n: News) {
        try {
            val ref = databaseModel.newsDatabase.child(java.lang.String.valueOf(n.node))
            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (!dataSnapshot.exists()) {
                        databaseModel.newsDatabase.child(java.lang.String.valueOf(n.node))
                            .child("title").setValue(n.title)
                        databaseModel.newsDatabase.child(java.lang.String.valueOf(n.node))
                            .child("date").setValue(n.date)
                        databaseModel.newsDatabase.child(java.lang.String.valueOf(n.node))
                            .child("content-list").setValue(n.content_list)
                        databaseModel.newsDatabase.child(java.lang.String.valueOf(n.node))
                            .child("content").setValue(n.content)
                    } else if (!dataSnapshot.child("title").exists()) {
                        databaseModel.newsDatabase.child(java.lang.String.valueOf(n.node))
                            .child("title").setValue(n.title)
                    } else if (!dataSnapshot.child("date").exists()) {
                        databaseModel.newsDatabase.child(java.lang.String.valueOf(n.node))
                            .child("date").setValue(n.date)
                    } else if (!dataSnapshot.child("content-list").exists()) {
                        databaseModel.newsDatabase.child(java.lang.String.valueOf(n.node))
                            .child("content-list").setValue(n.content_list)
                    } else if (!dataSnapshot.child("content").exists()) {
                        databaseModel.newsDatabase.child(java.lang.String.valueOf(n.node))
                            .child("content").setValue(n.content)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private inner class GetData : AsyncTask<Void?, Void?, ArrayList<News>?>() {
        override fun doInBackground(vararg p0: Void?): ArrayList<News>? {
            try {
                val list: ArrayList<News> = ArrayList<News>()
                val doc = Jsoup.connect("https://farabrehy.sk/aktuality.php").get()
                val children = doc.getElementsByAttribute("onmouseover")
                for (e in children) {
                    val separate = e.attributes()["onclick"]
                    val indexStart = separate.indexOf("getElementById") + "getElementById('".length
                    val expandID =
                        separate.substring(indexStart, separate.indexOf("'", indexStart + 1))
                    val expansion = doc.getElementById(expandID)
                    val date = e.child(1).text()
                    val title = e.child(0).text()
                    var content_list = e.child(2).text()
                    var content = expansion.children().toString()
                    content_list = "<html><style>" +
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
                            "p {" +
                            "margin: 0px !important;" +
                            "padding-bottom: 8px !important;" +
                            "text-align: justify;" +
                            "font-size: 16px !important;" +
                            "}" +
                            "</style><body>" + addToSrc(content_list!!) + "</body></html>"
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
                            "p {margin: 0px !important;" +
                            "padding-bottom: 8px !important;" +
                            "text-align: justify;" +
                            "font-size: 16px !important;" +
                            "}" +
                            "</style><body>" + addToSrc(content) + "</body></html>"
                    list.add(News(expandID.toLong(), title, date, content_list, content))
                }
                return list
            } catch (ignored: Exception) {
            }
            return null
        }

        // This runs in UI when background thread finishes
        override fun onPostExecute(result: ArrayList<News>?) {
            super.onPostExecute(result)
            if (result != null) {
                for (n in result) {
                    writeToDatabase(n)
                }
            }
            writeToListView(result)
        }

    }

    private fun writeToListView(result: ArrayList<News>?) {
        val adapter = NewsAdapter(requireContext(), result!!)
        binding.listview.adapter = adapter
        binding.listview.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, position, l ->
                val news: News = result[position]
                if (databaseModel.isConnectedToInternet(requireContext())) {
                    //newsModel.openedContent = news
                    //(activity as MainActivity).changeFragment(NewsContentFragment())
                } else {
                    (activity as MainActivity).showToast("Nie ste pripojený na internet.", R.drawable.network_background, R.color.brown_light)
                }
            }
    }

    fun addToSrc(html: String): String {
        val arrayList = ArrayList<Int>()
        var q = StringBuffer(html)
        val add = "https://farabrehy.sk"
        var index = html.indexOf("src=")
        while (index >= 0) {
            arrayList.add(index + 5)
            index = html.indexOf("src=", index + 1)
        }
        val displayMetrics = DisplayMetrics()
        (activity as MainActivity).windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = displayMetrics.widthPixels / displayMetrics.density - 50.0
        var indexStartWidth = html.indexOf("width:")
        while (indexStartWidth >= 0) {
            val indexEndWidth = html.indexOf(";", indexStartWidth + 1)
            val sizeW = html.substring(indexStartWidth + 7, indexEndWidth - 2).toDouble()
            if (sizeW > width) {
                val ratio = sizeW / width
                val indexStartHeight = html.indexOf("height:", indexEndWidth + 1)
                val indexEndHeight = html.indexOf(";", indexStartHeight + 1)
                val sizeH = html.substring(indexStartHeight + 8, indexEndHeight - 2).toDouble()
                q.delete(indexStartWidth + 7, indexEndWidth - 2)
                q.insert(indexStartWidth + 7, width.toInt())
                q.delete(indexStartHeight + 8, indexEndHeight - 2)
                q.insert(indexStartHeight + 8, (sizeH / ratio).toInt())
            }
            indexStartWidth = html.indexOf("width:", indexStartWidth + 1)
        }
        var prev = 0
        for (i in arrayList.indices) {
            q = q.insert(prev + arrayList[i], add)
            prev = (i + 1) * add.length
        }
        Log.d("App_content", q.toString())
        return q.toString()
    }

}