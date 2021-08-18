package sk.brehy;

import androidx.annotation.NonNull;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Uvod extends FirebaseMain {

    public void setBottomMenu(){
        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Intent intent;
                switch (item.getItemId()) {
                    case R.id.menu_aktuality:
                        intent = new Intent(Uvod.this, Aktuality.class);
                        break;
                    case R.id.menu_oznamy:
                        intent = new Intent(Uvod.this, Oznamy.class);
                        break;
                    case R.id.menu_stranka:
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.farabrehy.sk"));
                        break;
                    case R.id.menu_lektori:
                        intent = new Intent(Uvod.this, Lektori.class);
                        break;
                    case R.id.menu_kontakt:
                        intent = new Intent(Uvod.this, Kontakt.class);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + item.getItemId());
                }
                startActivity(intent);
                return true;
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uvod);
        setBottomMenu();
        setFirebase();
    }
}