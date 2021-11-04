package sk.brehy;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.amulyakhare.textdrawable.TextDrawable;
import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.DatePicker;
import com.applandeo.materialcalendarview.EventDay;
import com.applandeo.materialcalendarview.builders.DatePickerBuilder;
import com.applandeo.materialcalendarview.exceptions.OutOfDateRangeException;
import com.applandeo.materialcalendarview.listeners.OnCalendarPageChangeListener;
import com.applandeo.materialcalendarview.listeners.OnDayClickListener;
import com.applandeo.materialcalendarview.listeners.OnSelectDateListener;
import com.applandeo.materialcalendarview.utils.CalendarProperties;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Lektori extends FirebaseMain {

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

    CalendarView calendarView;
    FloatingActionButton floatingActionButton;
    ListView listView;
    HashMap<String, ArrayList<People>> list = new HashMap<>();
    CalendarProperties p;
    Field f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lektori);
        getSavedData();
        setBottomMenu();

        calendarView = (CalendarView) findViewById(R.id.calendarView);
        f = null;
        try {
            f = CalendarView.class.getDeclaredField("mCalendarProperties");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        assert f != null;
        f.setAccessible(true);
        p = null;
        try {
            p = (CalendarProperties) f.get(calendarView);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        assert p != null;
        p.setSelectionColor(getResources().getColor(R.color.red));
        calendarView.requestLayout();


        calendarView.setOnPreviousPageChangeListener(new OnCalendarPageChangeListener() {
            @Override
            public void onChange() {
                highlightedWeekends();
            }
        });
        calendarView.setOnForwardPageChangeListener(new OnCalendarPageChangeListener() {
            @Override
            public void onChange() {
                highlightedWeekends();
            }
        });
        calendarView.setOnDayClickListener(new OnDayClickListener() {
            @Override
            public void onDayClick(EventDay eventDay) {
                Calendar today = Calendar.getInstance();
                Calendar clickedDay = eventDay.getCalendar();
                boolean sameDay = today.get(Calendar.DAY_OF_YEAR) == clickedDay.get(Calendar.DAY_OF_YEAR) &&
                        today.get(Calendar.YEAR) == clickedDay.get(Calendar.YEAR);
                if (!sameDay) {
                    p.setSelectionColor(getResources().getColor(R.color.brown_dark));
                    calendarView.requestLayout();
                } else {
                    p.setSelectionColor(getResources().getColor(R.color.red));
                    calendarView.requestLayout();
                }
                writeToListView(clickedDay);
            }
        });
        highlightedWeekends();

        floatingActionButton = findViewById(R.id.floatButton);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeToDatabase(calendarView.getSelectedDate());
            }
        });

        listView = findViewById(R.id.listview_kalendar);

        /*OnSelectDateListener listener = new OnSelectDateListener() {
            @Override
            public void onSelect(List<Calendar> calendars) {
                Toast.makeText(getApplicationContext(), "selected", Toast.LENGTH_SHORT).show();
            }
        };
        
        DatePickerBuilder builder = new DatePickerBuilder(this, listener)
                .pickerType(CalendarView.ONE_DAY_PICKER);

        DatePicker datePicker = builder.build();
        datePicker.show();
        */

    }

    private void highlightedWeekends() {
        List<Calendar> calendars = new ArrayList<>();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        int month = c.get(Calendar.MONTH);
        int i = 0;
        do {
            int day = c.get(Calendar.DAY_OF_WEEK);
            if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.add(Calendar.DATE, i);
                calendars.add(cal);
            }
            i++;
            c.add(Calendar.DAY_OF_YEAR, 1);
        } while (c.get(Calendar.MONTH) == month);
        calendarView.setHighlightedDays(calendars);
    }

    private void writeToDatabase(Calendar cal) {
        String date = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH);
        ArrayList<People> current = list.get(date);
        int num;
        if (current == null)
            num = 1;
        else
            num = current.size() + 1;
        if (isNetworkAvailable())
            openDialog(Integer.toString(num), date, "", "Pridať lektora");
    }

    private void getSavedData() {
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
                            list.put(date, all);
                        }
                    }
                }
                refreshScreenData();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.d("APP_data", "Failed to read value.", error.toException());
            }
        });
    }

    private void refreshScreenData() {
        List<EventDay> events = new ArrayList<>();
        for (Map.Entry<String, ArrayList<People>> entry : list.entrySet()) {
            String[] key = entry.getKey().split("-");
            int value = entry.getValue().size();
            Calendar calendar = Calendar.getInstance();
            calendar.set(Integer.parseInt(key[0]), Integer.parseInt(key[1]) - 1, Integer.parseInt(key[2]));
            TextDrawable d = TextDrawable.builder()
                    .beginConfig()
                    .textColor(getResources().getColor(R.color.brown_superlight))
                    .bold()
                    .endConfig()
                    .buildRound(Integer.toString(value), getResources().getColor(R.color.brown_dark));

            events.add(new EventDay(calendar, d));
        }
        calendarView.setEvents(events);
        Calendar a = calendarView.getSelectedDate();
        writeToListView(a);
    }

    private void writeToListView(Calendar calendar) {
        String date = calendar.get(Calendar.YEAR) + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.DAY_OF_MONTH);
        ArrayList<People> current = list.get(date);
        if (current != null) {
            PeopleAdapter adapter = new PeopleAdapter(this, current);
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                    People people = current.get(position);
                    if (isNetworkAvailable())
                        openDialog(people.getNumber(), date, people.getName(), "Upraviť lektora");
                }
            });
        } else {
            listView.setAdapter(null);
        }
    }

    public void openDialog(String num, String date, String name, String headText) {
        final Dialog dialog = new Dialog(Lektori.this);
        dialog.setContentView(R.layout.lektor_dialog);
        String[] keys = date.split("-");

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;

        TextView head = dialog.findViewById(R.id.head);
        head.setText(headText);

        TextView datum = dialog.findViewById(R.id.date);
        String nazov = keys[2] + ". "+getResources().getStringArray(R.array.material_calendar_months_array)[Integer.parseInt(keys[1])-1] + " "+ keys[0];
        datum.setText(nazov);

        TextView number = dialog.findViewById(R.id.number);
        number.setText("Poradie: " + num);

        EditText edit = (EditText) dialog.findViewById(R.id.name);
        edit.setText(name);

        Button positive = (Button) dialog.findViewById(R.id.positive);
        positive.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String name = edit.getText().toString();

                if (!name.equals("")) {
                    kalendar_reference.child(keys[0]).child(keys[1]).child(keys[2]).child(String.valueOf(num)).setValue(name);
                } else {
                    ArrayList<People> people = list.get(date);
                    if (people != null) {
                        if (people.size() > (Integer.parseInt(num) - 1)) {
                            kalendar_reference.child(keys[0]).child(keys[1]).child(keys[2]).removeValue();
                            people.remove(Integer.parseInt(num) - 1);
                            if (people.isEmpty())
                                list.remove(date);
                            else {
                                int n = 1;
                                ArrayList<People> newPeople = new ArrayList<>();
                                for (People human : people) {
                                    newPeople.add(new People(String.valueOf(n), human.getName()));
                                    kalendar_reference.child(keys[0]).child(keys[1]).child(keys[2]).child(String.valueOf(n)).setValue(human.getName());
                                    n++;
                                }
                                list.put(date, newPeople);
                            }
                            refreshScreenData();
                        }
                    }
                }
                dialog.dismiss();
            }
        });
        Button negative = (Button) dialog.findViewById(R.id.negative);
        negative.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
        dialog.getWindow().setAttributes(lp);
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

}
