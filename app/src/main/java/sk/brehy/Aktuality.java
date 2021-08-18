package sk.brehy;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;

public class Aktuality extends FirebaseMain {

    public void setBottomMenu() {
        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Intent intent;
                switch (item.getItemId()) {
                    case R.id.menu_aktuality:
                        intent = new Intent(getApplicationContext(), Aktuality.class);
                        break;
                    case R.id.menu_oznamy:
                        intent = new Intent(getApplicationContext(), Oznamy.class);
                        break;
                    case R.id.menu_stranka:
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.farabrehy.sk"));
                        break;
                    case R.id.menu_lektori:
                        intent = new Intent(getApplicationContext(), Lektori.class);
                        break;
                    case R.id.menu_kontakt:
                        intent = new Intent(getApplicationContext(), Kontakt.class);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + item.getItemId());
                }
                startActivity(intent);
                finish();
                return true;
            }
        });
    }

    ListView listView;
    TextView toastMessage;
    View toastLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aktuality);
        setBottomMenu();

        listView = findViewById(R.id.listview_aktuality);
        LayoutInflater myInflator = getLayoutInflater();
        toastLayout = myInflator.inflate(R.layout.toast, (ViewGroup) findViewById(R.id.toast_layout));
        toastMessage = (TextView) toastLayout.findViewById(R.id.toast_text);

        if (isNetworkAvailable()) {
            new GetData().execute();
        } else {
            getSavedData();
        }
    }

    private void getSavedData() {
        aktuality_reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<News> list = new ArrayList<>();
                Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                for (DataSnapshot child : children) {
                    Iterable<DataSnapshot> node = child.getChildren();
                    String title = null, href = null, date = null, content_list = null, content = null;
                    for (DataSnapshot news_info : node) {
                        switch (news_info.getKey()) {
                            case "title":
                                title = (String) news_info.getValue();
                                break;
                            case "href":
                                href = (String) news_info.getValue();
                                break;
                            case "date":
                                date = (String) news_info.getValue();
                                break;
                            case "content-list":
                                content_list = (String) news_info.getValue();
                                break;
                            case "content":
                                content = (String) news_info.getValue();
                                break;
                        }
                    }
                    list.add(new News(child.getKey(), title, href, date, content_list, content));
                }
                Collections.sort(list, News.nodeComparator);
                writeToListView(list);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.d("APP_data", "Failed to read value.", error.toException());
            }
        });
    }

    private void writeToDatabase(News n) {
        DatabaseReference ref = aktuality_reference.child(String.valueOf(n.getNode()));
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    aktuality_reference.child(String.valueOf(n.getNode())).child("title").setValue(n.getTitle());
                    aktuality_reference.child(String.valueOf(n.getNode())).child("href").setValue("https://farabrehy.sk" + n.getHref());
                    aktuality_reference.child(String.valueOf(n.getNode())).child("date").setValue(n.getDate());
                    aktuality_reference.child(String.valueOf(n.getNode())).child("content-list").setValue(n.getContent_list());
                } else if (!dataSnapshot.child("title").exists()) {
                    aktuality_reference.child(String.valueOf(n.getNode())).child("title").setValue(n.getTitle());
                } else if (!dataSnapshot.child("href").exists()) {
                    aktuality_reference.child(String.valueOf(n.getNode())).child("href").setValue("https://farabrehy.sk" + n.getHref());
                } else if (!dataSnapshot.child("date").exists()) {
                    aktuality_reference.child(String.valueOf(n.getNode())).child("date").setValue(n.getDate());
                } else if (!dataSnapshot.child("content-list").exists()) {
                    aktuality_reference.child(String.valueOf(n.getNode())).child("content-list").setValue(n.getContent_list());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private class GetData extends AsyncTask<Void, Void, ArrayList<News>> {

        @Override
        protected ArrayList<News> doInBackground(Void... params) {
            try {
                ArrayList<News> list = new ArrayList<>();
                Document doc = Jsoup.connect("https://farabrehy.sk").get();
                Element ele = doc.select("div.content").first();
                Elements child = ele.children();
                for (Element e : child) {
                    String node = e.attributes().get("id");
                    if (!node.equals("")) {
                        String href = "https://farabrehy.sk" + e.attributes().get("about");
                        String date = e.getElementsByClass("submitted").text().replace("Pridané používateľom admin dňa ", "");
                        String title = e.getElementsByClass("title").text();
                        String content_list = e.getElementsByClass("field-item even").html();


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
                                "margin: 0px;" +
                                "padding-bottom: 8px;" +
                                "text-align: justify;" +
                                "}" +
                                "</style><body>" + addToSrc(content_list) + "</body></html>";

                        list.add(new News(node, title, href, date, content_list, ""));
                    }
                }
                return list;
            } catch (Exception ignored) {
            }
            return null;
        }

        // This runs in UI when background thread finishes
        @Override
        protected void onPostExecute(ArrayList<News> result) {
            super.onPostExecute(result);
            for (News n : result) {
                writeToDatabase(n);
            }
            writeToListView(result);
        }
    }

    private void writeToListView(ArrayList<News> result) {
        NewsAdapter adapter = new NewsAdapter(this, result);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                News news = result.get(position);
                if (news.getContent() != null || isNetworkAvailable()) {
                    openedAktualityContent = news;
                    Intent intent = new Intent(getApplicationContext(), AktualityContent.class);
                    startActivity(intent);
                } else {
                    toastMessage.setText("Nie ste pripojený k internetu.");
                    Toast myToast = new Toast(getApplicationContext());
                    myToast.setDuration(Toast.LENGTH_LONG);
                    myToast.setView(toastLayout);
                    myToast.show();
                }
            }
        });
    }

}
