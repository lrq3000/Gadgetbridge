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
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.Calendar;
import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardActiveTimeWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardDistanceWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardSleepWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardStepsWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardTodayWidget;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public class DashboardFragment extends Fragment {
    private Calendar day = GregorianCalendar.getInstance();
    private TextView textViewDate;
    private TextView arrowLeft;
    private TextView arrowRight;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View dashboardView = inflater.inflate(R.layout.fragment_dashboard, container, false);
        setHasOptionsMenu(true);
        textViewDate = dashboardView.findViewById(R.id.dashboard_date);

        arrowLeft = dashboardView.findViewById(R.id.arrow_left);
        arrowLeft.setOnClickListener(v -> {
            day.add(Calendar.DAY_OF_MONTH, -1);
            refresh();
        });
        arrowRight = dashboardView.findViewById(R.id.arrow_right);
        arrowRight.setOnClickListener(v -> {
            Calendar today = GregorianCalendar.getInstance();
            if (!DateTimeUtils.isSameDay(today, day)) {
                day.add(Calendar.DAY_OF_MONTH, 1);
                refresh();
            }
        });

        refresh();

        return dashboardView;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.dashboard_menu, menu);
    }

    private void refresh() {
        day.set(Calendar.HOUR_OF_DAY, 23);
        day.set(Calendar.MINUTE, 59);
        day.set(Calendar.SECOND, 59);
        int timeTo = (int) (day.getTimeInMillis() / 1000);
        int timeFrom = DateTimeUtils.shiftDays(timeTo, -1);

        Calendar today = GregorianCalendar.getInstance();
        if (DateTimeUtils.isSameDay(today, day)) {
            textViewDate.setText(getContext().getString(R.string.activity_summary_today));
            arrowRight.setAlpha(0.5f);
        } else {
            textViewDate.setText(DateTimeUtils.formatDate(day.getTime()));
            arrowRight.setAlpha(1);
        }

        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_today, DashboardTodayWidget.newInstance(timeFrom, timeTo))
                .commit();
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_steps, DashboardStepsWidget.newInstance(timeFrom, timeTo))
                .commit();
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_distance, DashboardDistanceWidget.newInstance(timeFrom, timeTo))
                .commit();
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_active_time, DashboardActiveTimeWidget.newInstance(timeFrom, timeTo))
                .commit();
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_sleep, DashboardSleepWidget.newInstance(timeFrom, timeTo))
                .commit();
    }
}