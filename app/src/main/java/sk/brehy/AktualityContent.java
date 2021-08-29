package sk.brehy;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class AktualityContent extends FirebaseMain {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aktuality_content);
        setBottomMenu();

        if (isNetworkAvailable()) {
            new GetData().execute();
        } else {
            showContent(null);
        }
    }

    private class GetData extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            try {
                Document doc = Jsoup.connect(openedAktualityContent.getHref()).get();
                String html = doc.select("div.field-item.even").html();
                String content = "<html> <style>" +
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
                                "p {margin: 0px;" +
                                "padding-bottom: 8px;" +
                                "text-align: justify;" +
                                "}" +
                                "</style><body>" + addToSrc(html)+ "</body></html>";
                return content;
            } catch (Exception ignored) {
                Exception e = ignored;
            }
            return null;
        }

        // This runs in UI when background thread finishes
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            writeToDatabase(result);
            showContent(result);
        }
    }

    private void showContent(String result) {
        TextView title = findViewById(R.id.title);
        title.setText(openedAktualityContent.getTitle());
        TextView date = findViewById(R.id.date);
        date.setText(openedAktualityContent.getDate());
        WebView content = findViewById(R.id.content);
        content.setBackgroundColor(ContextCompat.getColor(this, R.color.brown_superlight));
        if(result != null){
            content.loadData(result, "text/html", "utf-8");
        } else {
            content.loadData(openedAktualityContent.getContent(), "text/html", "utf-8");
        }
    }

    private void writeToDatabase(String n) {
        DatabaseReference ref = aktuality_reference.child(String.valueOf(openedAktualityContent.getNode()));
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.child("content").exists()) {
                    aktuality_reference.child(String.valueOf(openedAktualityContent.getNode())).child("content").setValue(n);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }
}