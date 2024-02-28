package sk.brehy.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import sk.brehy.R


data class News(val id: String, val title: String, val content: String) {
    var date: String

    init {
        val pom = id.chunked(2)
        date = "Zverejnené: ${pom[0]}. ${pom[1]}. ${pom[2]}${pom[3]} ${pom[4]}:${pom[5]}"
    }
}

class NewsAdapter internal constructor(context: Context, words: MutableList<News>) :
    ArrayAdapter<News>(
        context, 0, words
    ) {
    @SuppressLint("SetJavaScriptEnabled")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var listItemView = convertView
        // View view = super.getView(position, convertView, parent);
        if (listItemView == null) {
            listItemView = LayoutInflater.from(context).inflate(
                R.layout.news_list, parent, false
            )
        }
        val current = getItem(position)
        val title = listItemView!!.findViewById<TextView>(R.id.title)
        assert(current != null)
        title.text = current!!.title
        val date = listItemView.findViewById<TextView>(R.id.date)
        date.text = current.date
        val contentList = listItemView.findViewById<WebView>(R.id.content_list)
        contentList.setBackgroundColor(ContextCompat.getColor(context, R.color.brown_superlight))
        //content_list.loadData(current.content_list, "text/html; charset=utf-8", "utf-8")
        contentList.loadData(current.content, "text/html; charset=utf-8", "utf-8")
        val webSettings = contentList.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.defaultTextEncodingName = "utf-8"

        /*content_list.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (v.getId() == R.id.content_list && event.getAction() == MotionEvent.ACTION_DOWN)  {
                    Log.d("APP-hjkl", "here");
                    if (current.getContent() != null || isNetworkAvailable()) {
                        FirebaseMain.openedAktualityContent = current;
                        Intent intent = new Intent(getContext(), AktualityContent.class);
                        getContext().startActivity(intent);
                    }
                }
                return true;
            }
        });*/return listItemView
    }
}