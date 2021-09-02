package sk.brehy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class LektoriLogin extends FirebaseMain {

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
                        intent = new Intent(getApplicationContext(), LektoriLogin.class);
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
        setContentView(R.layout.activity_lektori_login);

        setBottomMenu();

        SharedPreferences settings = getApplicationContext().getSharedPreferences("FarnostBrehy", 0);
        boolean logedIn = settings.getBoolean("logedIn", false);
        if (logedIn)
            goToLektori();

        LayoutInflater myInflator = getLayoutInflater();
        View toastLayout = myInflator.inflate(R.layout.toast, (ViewGroup) findViewById(R.id.toast_layout));
        TextView toastMessage = (TextView) toastLayout.findViewById(R.id.toast_text);

        EditText loginEdit = findViewById(R.id.loginEdit);
        Button loginButton = findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String login = loginEdit.getText().toString();
                if(login.equals("Cit12")){
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("logedIn", true).apply();
                    goToLektori();
                } else {
                    toastMessage.setText("Nesprávne heslo.");
                    Toast myToast = new Toast(getApplicationContext());
                    myToast.setDuration(Toast.LENGTH_LONG);
                    myToast.setView(toastLayout);
                    myToast.show();
                }
            }
        });
    }

    public void goToLektori(){
        Intent intent = new Intent(getApplicationContext(), Lektori.class);
        startActivity(intent);
        finish();
    }
}