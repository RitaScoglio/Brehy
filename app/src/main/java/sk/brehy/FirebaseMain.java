package sk.brehy;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class FirebaseMain extends AppCompatActivity {

    static DatabaseReference oznamy_reference, aktuality_reference, kalendar_reference;
    static News openedAktualityContent;

    HashMap<String, ArrayList<People>> lektori_list = new HashMap<>();

    void setFirebase() {
        if(oznamy_reference == null) {
            FirebaseDatabase database = FirebaseDatabase.getInstance("https://brehy-458da-default-rtdb.europe-west1.firebasedatabase.app/");
            database.setPersistenceEnabled(true);
            oznamy_reference = database.getReference("oznamy");
            oznamy_reference.keepSynced(true);
            aktuality_reference = database.getReference("aktuality");
            aktuality_reference.keepSynced(true);
            kalendar_reference = database.getReference("kalendar");
            kalendar_reference.keepSynced(true);
        }
    }

    void getData(){
        kalendar_reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterable<DataSnapshot> years = dataSnapshot.getChildren();
                for (DataSnapshot year : years) {
                    String y = year.getKey();
                    Iterable<DataSnapshot> months = year.getChildren();
                    for (DataSnapshot month : months) {
                        String m = month.getKey();
                        Iterable<DataSnapshot> days = month.getChildren();
                        for (DataSnapshot day : days) {
                            String d = day.getKey();
                            Iterable<DataSnapshot> people = day.getChildren();
                            ArrayList<People> all = new ArrayList<>();
                            for (DataSnapshot human : people) {
                                all.add(new People(human.getKey(), (String) human.getValue()));
                            }
                            String date = y + "-" + m + "-" + d;
                            lektori_list.put(date, all);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.d("APP_data", "Failed to read value.", error.toException());
            }
        });
    }

    boolean isNetworkAvailable() {
        ConnectivityManager manager =
                (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()) {
            isAvailable = true;
        } else {
            LayoutInflater myInflator = getLayoutInflater();
            View toastLayout = myInflator.inflate(R.layout.network_toast, (ViewGroup) findViewById(R.id.toast_layout));
            Toast myToast = new Toast(getApplicationContext());
            myToast.setDuration(Toast.LENGTH_LONG);
            myToast.setView(toastLayout);
            myToast.show();
        }
        return isAvailable;
    }

    String addToSrc(String html) {
        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        StringBuffer q = new StringBuffer(html);
        String add = "https://farabrehy.sk";
        for (int index = html.indexOf("src=");
             index >= 0;
             index = html.indexOf("src=", index + 1)) {

            arrayList.add(index + 5);
        }

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        double width = displayMetrics.widthPixels / displayMetrics.density - 50.0;

        for (int indexStartWidth = html.indexOf("width:");
             indexStartWidth >= 0;
             indexStartWidth = html.indexOf("width:", indexStartWidth + 1)) {
            int indexEndWidth = html.indexOf(";", indexStartWidth + 1);
            double sizeW = Double.parseDouble(html.substring(indexStartWidth + 7, indexEndWidth - 2));
            if (sizeW > width) {
                double ratio = sizeW / width;
                int indexStartHeight = html.indexOf("height:", indexEndWidth + 1);
                int indexEndHeight = html.indexOf(";", indexStartHeight + 1);
                double sizeH = Double.parseDouble(html.substring(indexStartHeight + 8, indexEndHeight - 2));
                q.delete(indexStartWidth + 7, indexEndWidth - 2);
                q.insert(indexStartWidth + 7, (int) width);
                q.delete(indexStartHeight + 8, indexEndHeight - 2);
                q.insert(indexStartHeight + 8, (int) (sizeH/ratio));
            }
        }

        int prev = 0;
        for (int i = 0; i < arrayList.size(); i++) {
            q = q.insert(prev + arrayList.get(i), add);
            prev = (i + 1) * add.length();

        }
        Log.d("App_content", q.toString());
        return q.toString();
    }

}


