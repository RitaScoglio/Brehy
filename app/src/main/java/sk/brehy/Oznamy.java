package sk.brehy;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.WebView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Oznamy extends FirebaseMain {

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
                        intent = new Intent(getApplicationContext(), Webstranka.class);
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

    private WebView WebView;
    final String mime = "text/html";
    final String encoding = "utf-8";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oznamy);
        setBottomMenu();


        WebView = (WebView) findViewById(R.id.webview_oznamy);
        WebView.setBackgroundColor(ContextCompat.getColor(this, R.color.brown_superlight));
        if (isNetworkAvailable()) {
            new GetData().execute();
        } else {
            getSavedData();
        }
    }

    private void getSavedData() {
        oznamy_reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String html = "";
                Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                for (DataSnapshot child : children) {
                    html = (String) child.getValue();
                }
                WebView.loadData(html, mime, encoding);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.d("APP_data", "Failed to read value.", error.toException());
            }
        });
    }

    private void writeToDatabase(String key, String value) {
        DatabaseReference ref = oznamy_reference.child(key);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    oznamy_reference.removeValue();
                    oznamy_reference.child(key).setValue(value);
                }
                WebView.loadData(value, mime, encoding);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private class GetData extends AsyncTask<Void, Void, String> {

        String nedela;

        @Override
        protected String doInBackground(Void... params) {
            try {
                Document doc = Jsoup.connect("https://farabrehy.sk/farske-oznamy").get();
                Element ele = doc.select("div.field-item.even").first();
                Elements child = ele.children();
                for (Element e : child) {
                    if (e.tagName().equals("p")) {
                        if (e.attributes().isEmpty())
                            e.addClass("divider");
                        else
                            e.clearAttributes().addClass("basic");
                    } else if (e.tagName().equals("h2")) {
                        nedela = e.text().replaceAll("\\.", "");
                    }
                }
                //String html = ele.outerHtml();
                String html = "<html> <style>" +
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
                        ".basic {margin: 0px;" +
                        "padding-bottom: 8px;" +
                        "text-align: justify;" +
                        "}" +
                        ".divider {font-size: 50%;" +
                        "}" +
                        "</style><body>" +
                        ele.outerHtml() + "</body></html>";
                return html;
            } catch (Exception ignored) {
            }
            return "";
        }

        // This runs in UI when background thread finishes
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            writeToDatabase(nedela, result);
        }
    }

}
