/*  Copyright (C) 2023 Arjan Schrijver

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities.dashboard;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DashboardTodayWidget#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DashboardTodayWidget extends Fragment {

    public DashboardTodayWidget() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View todayView = inflater.inflate(R.layout.dashboard_widget_today, container, false);

        PieChart chart = todayView.findViewById(R.id.dashboard_piechart_today);
        chart.getDescription().setEnabled(false);
        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.argb(0,0,0,0));
        chart.setHoleRadius(80f);
        chart.setRotationEnabled(false);
        chart.setDrawEntryLabels(false);

        Legend l = chart.getLegend();
        l.setTextColor(GBApplication.getTextColor(getContext()));
        ArrayList<LegendEntry> legendEntries = new ArrayList<>();
        legendEntries.add(new LegendEntry("Deep sleep", Legend.LegendForm.SQUARE, 10f, 10f, new DashPathEffect(new float[]{10f, 5f}, 0f), Color.rgb(0, 0, 255)));
        legendEntries.add(new LegendEntry("Light sleep", Legend.LegendForm.SQUARE, 10f, 10f, new DashPathEffect(new float[]{10f, 5f}, 0f), Color.rgb(150, 150, 255)));
        legendEntries.add(new LegendEntry("Inactive", Legend.LegendForm.SQUARE, 10f, 10f, new DashPathEffect(new float[]{10f, 5f}, 0f), Color.rgb(200, 200, 200)));
        legendEntries.add(new LegendEntry("Active", Legend.LegendForm.SQUARE, 10f, 10f, new DashPathEffect(new float[]{10f, 5f}, 0f), Color.rgb(0, 255, 0)));
        l.setCustom(legendEntries);

        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        entries.add(new PieEntry(120, "Deep sleep"));
        colors.add(Color.rgb(0, 0, 255));
        entries.add(new PieEntry(120, "Light sleep"));
        colors.add(Color.rgb(150, 150, 255));
        entries.add(new PieEntry(60, "Deep sleep"));
        colors.add(Color.rgb(0, 0, 255));
        entries.add(new PieEntry(120, "Light sleep"));
        colors.add(Color.rgb(150, 150, 255));
        entries.add(new PieEntry(180, "Inactive"));
        colors.add(Color.rgb(200, 200, 200));
        entries.add(new PieEntry(60, "Active"));
        colors.add(Color.rgb(0, 255, 0));
        entries.add(new PieEntry(60, "Inactive"));
        colors.add(Color.rgb(200, 200, 200));
        entries.add(new PieEntry(30, "Active"));
        colors.add(Color.rgb(0, 255, 0));
        entries.add(new PieEntry(150, "Inactive"));
        colors.add(Color.rgb(200, 200, 200));
        entries.add(new PieEntry(60, "Active"));
        colors.add(Color.rgb(0, 255, 0));
        entries.add(new PieEntry(60, "Inactive"));
        colors.add(Color.rgb(200, 200, 200));
        entries.add(new PieEntry(90, "Active"));
        colors.add(Color.rgb(0, 255, 0));
        entries.add(new PieEntry(150, "Inactive"));
        colors.add(Color.rgb(200, 200, 200));
        entries.add(new PieEntry(120, "Light sleep"));
        colors.add(Color.rgb(150, 150, 255));
        entries.add(new PieEntry(60, "Deep sleep"));
        colors.add(Color.rgb(0, 0, 255));

        PieDataSet dataSet = new PieDataSet(entries, "Today");
        dataSet.setSliceSpace(0f);
        dataSet.setSelectionShift(5f);
        dataSet.setDrawValues(false);
        dataSet.setColors(colors);

        PieData data = new PieData(dataSet);
        chart.setData(data);

        return todayView;
    }
}