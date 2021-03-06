package com.example.medicationreminderapplication;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;

public class BySpecificDay extends Fragment {

    public BySpecificDay() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_by_specific_day, container, false);
        RecyclerView bySpecificDays = root.findViewById(R.id.recyclerBySpecificDay);
        ArrayList<String> days = new ArrayList<>();
        for (String day:
             getResources().getStringArray(R.array.DaysOfTheWeek)) {
                 days.add(day);
        }
        bySpecificDays.setAdapter(new TimesRecyclerViewAdapter(this.getContext(), new ArrayList<>(), days));
        bySpecificDays.setLayoutManager(new LinearLayoutManager(getContext()));
        return root;
    }
}