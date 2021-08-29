package sk.brehy;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Kontakt extends FirebaseMain {

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
        setContentView(R.layout.activity_kontakt);

        setBottomMenu();

        LayoutInflater myInflator = getLayoutInflater();
        View toastLayout = myInflator.inflate(R.layout.toast, (ViewGroup) findViewById(R.id.toast_layout));
        TextView toastMessage = (TextView) toastLayout.findViewById(R.id.toast_text);

        RelativeLayout adress = findViewById(R.id.adress);
        adress.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("adresa", getResources().getString(R.string.adresa));
                clipboard.setPrimaryClip(clip);
                toastMessage.setText("Adresa bola skopírovaná.");
                Toast myToast=new Toast(getApplicationContext());
                myToast.setDuration(Toast.LENGTH_LONG);
                myToast.setView(toastLayout);
                myToast.show();
            }
        });

        RelativeLayout call = findViewById(R.id.call);
        call.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Uri number = Uri.parse("tel:+421455322451");
                Intent callIntent = new Intent(Intent.ACTION_DIAL, number);
                startActivity(callIntent);
            }
        });

        RelativeLayout email = findViewById(R.id.email);
        email.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Uri mail = Uri.parse("mailto:"+getResources().getString(R.string.email));
                Intent sendIntent = new Intent(Intent.ACTION_SENDTO, mail);
                startActivity(sendIntent);
            }
        });

        RelativeLayout iban = findViewById(R.id.iban);
        iban.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("iban", "SK8709000000000074337685");
                clipboard.setPrimaryClip(clip);
                toastMessage.setText("IBAN bol skopírovaný.");
                Toast myToast=new Toast(getApplicationContext());
                myToast.setDuration(Toast.LENGTH_LONG);
                myToast.setView(toastLayout);
                myToast.show();
            }
        });

        RelativeLayout bic = findViewById(R.id.bic);
        bic.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("bic", "GIBASKBX");
                clipboard.setPrimaryClip(clip);
                toastMessage.setText("BIC bol skopírovaný.");
                Toast myToast=new Toast(getApplicationContext());
                myToast.setDuration(Toast.LENGTH_LONG);
                myToast.setView(toastLayout);
                myToast.show();
            }
        });
    }
}