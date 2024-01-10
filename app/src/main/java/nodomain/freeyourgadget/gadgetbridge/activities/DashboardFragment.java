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

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.gridlayout.widget.GridLayout;

import com.google.android.material.card.MaterialCardView;

import java.util.Calendar;
import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.AbstractDashboardWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardActiveTimeWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardDistanceWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardSettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardSleepWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardStepsWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardTodayWidget;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class DashboardFragment extends Fragment {
    private Calendar day = GregorianCalendar.getInstance();
    private TextView textViewDate;
    private TextView arrowLeft;
    private TextView arrowRight;
    private GridLayout gridLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View dashboardView = inflater.inflate(R.layout.fragment_dashboard, container, false);
        setHasOptionsMenu(true);
        textViewDate = dashboardView.findViewById(R.id.dashboard_date);
        gridLayout = dashboardView.findViewById(R.id.dashboard_gridlayout);

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
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.dashboard_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.dashboard_show_calendar:
                // TODO: implement calendar activity
                GB.toast("The calendar view is not implemented yet", Toast.LENGTH_SHORT, GB.INFO);
                return false;
            case R.id.dashboard_settings:
                Intent intent = new Intent(requireActivity(), DashboardSettingsActivity.class);
                startActivityForResult(intent, 0);
                return false;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        refresh();
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

        gridLayout.removeAllViews();
        Prefs prefs = GBApplication.getPrefs();
        boolean cardsEnabled = prefs.getBoolean("dashboard_cards_enabled", true);

        if (prefs.getBoolean("dashboard_widget_today_enabled", true)) {
            createWidget(DashboardTodayWidget.newInstance(timeFrom, timeTo), cardsEnabled, 2);
        }
        if (prefs.getBoolean("dashboard_widget_steps_enabled", true)) {
            createWidget(DashboardStepsWidget.newInstance(timeFrom, timeTo), cardsEnabled, 1);
        }
        if (prefs.getBoolean("dashboard_widget_distance_enabled", true)) {
            createWidget(DashboardDistanceWidget.newInstance(timeFrom, timeTo), cardsEnabled, 1);
        }
        if (prefs.getBoolean("dashboard_widget_active_time_enabled", true)) {
            createWidget(DashboardActiveTimeWidget.newInstance(timeFrom, timeTo), cardsEnabled, 1);
        }
        if (prefs.getBoolean("dashboard_widget_sleep_enabled", true)) {
            createWidget(DashboardSleepWidget.newInstance(timeFrom, timeTo), cardsEnabled, 1);
        }
    }

    private void createWidget(AbstractDashboardWidget widgetObj, boolean cardsEnabled, int columnSpan) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        FragmentContainerView fragment = new FragmentContainerView(getActivity());
        int fragmentId = View.generateViewId();
        fragment.setId(fragmentId);
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(fragmentId, widgetObj)
                .commit();

        GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL,1f),
                GridLayout.spec(GridLayout.UNDEFINED, columnSpan, GridLayout.FILL,1f)
        );
        layoutParams.width = 0;
        int pixels_8dp = (int) (8 * scale + 0.5f);
        layoutParams.setMargins(pixels_8dp, pixels_8dp, pixels_8dp, pixels_8dp);

        if (cardsEnabled) {
            MaterialCardView card = new MaterialCardView(getActivity());
            int pixels_4dp = (int) (4 * scale + 0.5f);
            card.setRadius(pixels_4dp);
            card.setCardElevation(pixels_4dp);
            card.setContentPadding(pixels_4dp, pixels_4dp, pixels_4dp, pixels_4dp);
            card.setLayoutParams(layoutParams);
            card.addView(fragment);
            gridLayout.addView(card);
        } else {
            fragment.setLayoutParams(layoutParams);
            gridLayout.addView(fragment);
        }
    }
}