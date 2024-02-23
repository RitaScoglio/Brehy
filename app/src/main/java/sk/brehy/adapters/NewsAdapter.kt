package sk.brehy.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import sk.brehy.R


data class News(val node: Long, val title: String, val date: String, val content_list: String, val content: String) {

    companion object {
        val nodeComparator: Comparator<News> =
            Comparator<News> { n0, n1 -> n0.node.compareTo(n1.node) }
    }
}

class NewsAdapter internal constructor(context: Context, words: ArrayList<News>) :
    ArrayAdapter<News>(
        context, 0, words
    ) {
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
        val content_list = listItemView.findViewById<WebView>(R.id.content_list)
        content_list.setBackgroundColor(ContextCompat.getColor(context, R.color.brown_superlight))
        //content_list.loadData(current.content_list, "text/html; charset=utf-8", "utf-8")
        content_list.loadData(current.content, "text/html; charset=utf-8", "utf-8")
        val webSettings = content_list.settings
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