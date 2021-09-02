package sk.brehy;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class PeopleAdapter extends ArrayAdapter<People> {

    PeopleAdapter(Context context, ArrayList<People> words) {
        super(context, 0, words);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View listItemView = convertView;
        // View view = super.getView(position, convertView, parent);
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.people_list, parent, false);
        }

        People current = getItem(position);

        TextView number = listItemView.findViewById(R.id.number);
        assert current != null;
        number.setText(current.getNumber());

        TextView name = listItemView.findViewById(R.id.name);
        name.setText(current.getName());

        return listItemView;
    }
}
