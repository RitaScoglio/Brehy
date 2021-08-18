package sk.brehy;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class NewsAdapter extends ArrayAdapter<News> {

    NewsAdapter(Context context, ArrayList<News> words) {
        super(context, 0, words);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View listItemView = convertView;
        // View view = super.getView(position, convertView, parent);
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.news_list, parent, false);
        }

        News current = getItem(position);

        TextView title = listItemView.findViewById(R.id.title);
        assert current != null;
        title.setText(current.getTitle());

        TextView date = listItemView.findViewById(R.id.date);
        date.setText(current.getDate());

        WebView content_list = listItemView.findViewById(R.id.content_list);
        content_list.loadData(current.getContent_list(), "text/html", "utf-8");
        content_list.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.brown_superlight));

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
        });*/

        return listItemView;
    }

    boolean isNetworkAvailable() {
        ConnectivityManager manager =
                (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()) {
            // Network is present and connected
            isAvailable = true;
        }
        return isAvailable;
    }
}