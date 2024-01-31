/*  Copyright (C) 2023-2024 Arjan Schrijver

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
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.StepAnalysis;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySession;
import nodomain.freeyourgadget.gadgetbridge.util.HealthUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

/**
 * A simple {@link AbstractDashboardWidget} subclass.
 * Use the {@link DashboardTodayWidget#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DashboardTodayWidget extends AbstractDashboardWidget {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardTodayWidget.class);

    private View todayView;

    private boolean mode_24h;

    private PieChart chart_0_12;
    private PieChart chart_12_24;

    public DashboardTodayWidget() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param timeFrom Start time in seconds since Unix epoch.
     * @param timeTo End time in seconds since Unix epoch.
     * @return A new instance of fragment DashboardTodayWidget.
     */
    public static DashboardTodayWidget newInstance(int timeFrom, int timeTo) {
        DashboardTodayWidget fragment = new DashboardTodayWidget();
        Bundle args = new Bundle();
        args.putInt(ARG_TIME_FROM, timeFrom);
        args.putInt(ARG_TIME_TO, timeTo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        todayView = inflater.inflate(R.layout.dashboard_widget_today, container, false);

        // Determine whether to draw a single or a double chart. In case 24h mode is selected,
        // use just the outer chart (chart_12_24) for all data.
        Prefs prefs = GBApplication.getPrefs();
        mode_24h = prefs.getBoolean("dashboard_widget_today_24h", false);

        // Initialize outer chart
        chart_12_24 = todayView.findViewById(R.id.dashboard_piechart_today_12_24);
        chart_12_24.getDescription().setEnabled(false);
        chart_12_24.getLegend().setEnabled(false);
        chart_12_24.setDrawHoleEnabled(true);
        chart_12_24.setHoleColor(Color.TRANSPARENT);
        chart_12_24.setRotationEnabled(false);
        chart_12_24.setDrawEntryLabels(false);
        chart_12_24.setHighlightPerTapEnabled(false);
        if (mode_24h) {
            chart_12_24.setHoleRadius(80f);
            chart_12_24.setTransparentCircleRadius(81f);
            chart_12_24.setTransparentCircleColor(GBApplication.getTextColor(getContext()));
        } else {
            chart_12_24.setHoleRadius(91f);
            chart_12_24.setTransparentCircleRadius(91f);
            chart_12_24.setTransparentCircleColor(Color.TRANSPARENT);
        }

        // Initialize inner chart
        chart_0_12 = todayView.findViewById(R.id.dashboard_piechart_today_0_12);
        if (mode_24h) {
            chart_0_12.setVisibility(View.INVISIBLE);
        } else {
            chart_0_12.getDescription().setEnabled(false);
            chart_0_12.getLegend().setEnabled(false);
            chart_0_12.setDrawHoleEnabled(true);
            chart_0_12.setHoleColor(Color.TRANSPARENT);
            chart_0_12.setHoleRadius(90f);
            chart_0_12.setTransparentCircleRadius(91f);
            chart_0_12.setTransparentCircleColor(GBApplication.getTextColor(getContext()));
            chart_0_12.setRotationEnabled(false);
            chart_0_12.setDrawEntryLabels(false);
            chart_0_12.setHighlightPerTapEnabled(false);
        }

        // Initialize legend
        TextView legend = todayView.findViewById(R.id.dashboard_piechart_legend);
        SpannableString l_not_worn = new SpannableString("■ " + getString(R.string.abstract_chart_fragment_kind_not_worn));
        l_not_worn.setSpan(new ForegroundColorSpan(color_not_worn), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString l_worn = new SpannableString("■ " + "Worn");
        l_worn.setSpan(new ForegroundColorSpan(color_worn), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString l_activity = new SpannableString("■ " + getString(R.string.activity_type_activity));
        l_activity.setSpan(new ForegroundColorSpan(color_activity), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString l_deep_sleep = new SpannableString("■ " + getString(R.string.activity_type_deep_sleep));
        l_deep_sleep.setSpan(new ForegroundColorSpan(color_deep_sleep), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString l_light_sleep = new SpannableString("■ " + getString(R.string.activity_type_light_sleep));
        l_light_sleep.setSpan(new ForegroundColorSpan(color_light_sleep), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableStringBuilder legendBuilder = new SpannableStringBuilder();
        legend.setText(legendBuilder.append(l_not_worn).append(" ").append(l_worn).append(" ").append(l_activity).append("\n").append(l_light_sleep).append(" ").append(l_deep_sleep));

        // Initialize scale chart
        PieChart scale = todayView.findViewById(R.id.dashboard_piechart_scale);
        scale.getDescription().setEnabled(false);
        scale.getLegend().setEnabled(false);
        scale.setDrawHoleEnabled(true);
        scale.setHoleColor(Color.TRANSPARENT);
        scale.setHoleRadius(75f);
        scale.setTransparentCircleRadius(75f);
        scale.setRotationEnabled(false);
        scale.setHighlightPerTapEnabled(false);
        scale.setEntryLabelColor(GBApplication.getTextColor(getContext()));
        ArrayList<PieEntry> scaleEntries = new ArrayList<>();
        if (mode_24h) {
            scale.setRotationAngle(285f);
            for (int i = 2; i <= 24; i+= 2) {
                scaleEntries.add(new PieEntry(1, String.valueOf(i)));
            }
        } else {
            scale.setRotationAngle(315f);
            for (int i = 3; i <= 12; i += 3) {
                scaleEntries.add(new PieEntry(1, String.format("%d / %d", i, i + 12)));
            }
        }
        PieDataSet scaleDataSet = new PieDataSet(scaleEntries, "Time scale");
        scaleDataSet.setSliceSpace(0f);
        scaleDataSet.setDrawValues(false);
        scaleDataSet.setColor(Color.TRANSPARENT);
        PieData scaleData = new PieData(scaleDataSet);
        scale.setData(scaleData);

        fillData();

        return todayView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (chart_0_12 != null) fillData();
    }

    protected void fillData() {
        todayView.post(new Runnable() {
            @Override
            public void run() {
                FillDataAsyncTask myAsyncTask = new FillDataAsyncTask();
                myAsyncTask.execute();
            }
        });
    }

    private class FillDataAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            // Retrieve activity data
            List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
            List<ActivitySample> allActivitySamples = new ArrayList<>();
            List<ActivitySession> stepSessions = new ArrayList<>();
            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                for (GBDevice dev : devices) {
                    if (dev.getDeviceCoordinator().supportsActivityTracking()) {
                        List<? extends ActivitySample> activitySamples = HealthUtils.getAllSamples(dbHandler, dev, timeFrom, timeTo);
                        allActivitySamples.addAll(activitySamples);
                        StepAnalysis stepAnalysis = new StepAnalysis();
                        stepSessions.addAll(stepAnalysis.calculateStepSessions(activitySamples));
                    }
                }
            } catch (Exception e) {
                LOG.warn("Could not retrieve activity amounts: ", e);
            }

            // Integrate and chronologically order various data from multiple devices
            List<GeneralizedActivity> generalizedActivities = new ArrayList<>();
            long midDaySecond = timeFrom + (12 * 60 * 60);
            for (ActivitySample sample : allActivitySamples) {
                // Handle only TYPE_NOT_WORN and TYPE_SLEEP (including variants) here
                if (sample.getKind() != ActivityKind.TYPE_NOT_WORN && (sample.getKind() == ActivityKind.TYPE_NOT_MEASURED || (sample.getKind() & ActivityKind.TYPE_SLEEP) == 0))
                    continue;
                if (generalizedActivities.size() > 0) {
                    GeneralizedActivity previous = generalizedActivities.get(generalizedActivities.size() - 1);
                    // Merge samples if the type is the same, and they are within a minute of each other
                    if (previous.activityKind == sample.getKind() && previous.timeTo > sample.getTimestamp() - 60) {
                        // But only if the resulting activity doesn't cross the midday boundary in 12h mode
                        if (mode_24h || sample.getTimestamp() + 60 < midDaySecond || previous.timeFrom > midDaySecond) {
                            generalizedActivities.get(generalizedActivities.size() - 1).timeTo = sample.getTimestamp() + 60;
                            continue;
                        }
                    }
                }
                generalizedActivities.add(new GeneralizedActivity(
                        sample.getKind(),
                        sample.getTimestamp(),
                        sample.getTimestamp() + 60
                ));
            }
            for (ActivitySession session : stepSessions) {
                if (!mode_24h && session.getStartTime().getTime() / 1000 < midDaySecond && session.getEndTime().getTime() / 1000 > midDaySecond) {
                    generalizedActivities.add(new GeneralizedActivity(
                            session.getActivityKind(),
                            session.getStartTime().getTime() / 1000,
                            midDaySecond
                    ));
                    generalizedActivities.add(new GeneralizedActivity(
                            session.getActivityKind(),
                            midDaySecond,
                            session.getEndTime().getTime() / 1000
                    ));
                } else {
                    generalizedActivities.add(new GeneralizedActivity(
                            session.getActivityKind(),
                            session.getStartTime().getTime() / 1000,
                            session.getEndTime().getTime() / 1000
                    ));
                }
            }
            Collections.sort(generalizedActivities, (o1, o2) -> (int) (o1.timeFrom - o2.timeFrom));

            // Add pie slice entries
            ArrayList<PieEntry> entries_0_12 = new ArrayList<>();
            ArrayList<Integer> colors_0_12 = new ArrayList<>();
            ArrayList<PieEntry> entries_12_24 = new ArrayList<>();
            ArrayList<Integer> colors_12_24 = new ArrayList<>();
            long secondIndex = timeFrom;
            for (GeneralizedActivity activity : generalizedActivities) {
                // FIXME: correctly merge parallel activities from multiple devices
                // Skip earlier sessions
                if (activity.timeFrom < secondIndex) continue;
                // Use correct entries list for this part of the day
                ArrayList<PieEntry> entries = entries_0_12;
                ArrayList<Integer> colors = colors_0_12;
                if (mode_24h || activity.timeFrom >= midDaySecond) {
                    entries = entries_12_24;
                    colors = colors_12_24;
                }
                // Draw inactive slice
                if (activity.timeFrom > secondIndex) {
                    entries.add(new PieEntry(activity.timeFrom - secondIndex, "Inactive"));
                    colors.add(color_worn);
                }
                // Draw activity slices
                if (activity.activityKind == ActivityKind.TYPE_NOT_WORN) {
                    entries.add(new PieEntry(activity.timeTo - activity.timeFrom, "Not worn"));
                    colors.add(color_not_worn);
                    secondIndex = activity.timeTo;
                } else if (activity.activityKind == ActivityKind.TYPE_LIGHT_SLEEP || activity.activityKind == ActivityKind.TYPE_SLEEP) {
                    entries.add(new PieEntry(activity.timeTo - activity.timeFrom, "Light sleep"));
                    colors.add(color_light_sleep);
                    secondIndex = activity.timeTo;
                } else if (activity.activityKind == ActivityKind.TYPE_DEEP_SLEEP) {
                    entries.add(new PieEntry(activity.timeTo - activity.timeFrom, "Deep sleep"));
                    colors.add(color_deep_sleep);
                    secondIndex = activity.timeTo;
                } else {
                    entries.add(new PieEntry(activity.timeTo - activity.timeFrom, "Active"));
                    colors.add(color_activity);
                    secondIndex = activity.timeTo;
                }
            }
            // Fill remaining time until midnight
            long currentTime = Calendar.getInstance().getTimeInMillis() / 1000;
            if (!mode_24h && currentTime > timeFrom && currentTime < midDaySecond) {
                // Fill with unknown slice up until current time
                entries_0_12.add(new PieEntry(currentTime - secondIndex, "Unknown"));
                colors_0_12.add(color_worn);
                // Draw transparent slice for remaining time until midday
                entries_0_12.add(new PieEntry(midDaySecond - currentTime, "Empty"));
                colors_0_12.add(Color.TRANSPARENT);
            }
            if ((mode_24h || currentTime >= midDaySecond) && currentTime < timeTo) {
                // Fill with unknown slice up until current time
                entries_12_24.add(new PieEntry(currentTime - secondIndex, "Unknown"));
                colors_12_24.add(color_worn);
                // Draw transparent slice for remaining time until midnight
                entries_12_24.add(new PieEntry(timeTo - currentTime, "Empty"));
                colors_12_24.add(Color.TRANSPARENT);
            }

            // Draw charts
            if (!mode_24h) {
                PieDataSet dataSet_0_12 = new PieDataSet(entries_0_12, "Today 0-12h");
                dataSet_0_12.setSliceSpace(0f);
                dataSet_0_12.setDrawValues(false);
                dataSet_0_12.setColors(colors_0_12);
                chart_0_12.setData(new PieData(dataSet_0_12));
                chart_0_12.invalidate();
            }
            PieDataSet dataSet_12_24 = new PieDataSet(entries_12_24, "Today 12-24h");
            dataSet_12_24.setSliceSpace(0f);
            dataSet_12_24.setDrawValues(false);
            dataSet_12_24.setColors(colors_12_24);
            chart_12_24.setData(new PieData(dataSet_12_24));
            chart_12_24.invalidate();
            return null;
        }
    }

    private class GeneralizedActivity {
        public int activityKind;
        public long timeFrom;
        public long timeTo;

        private GeneralizedActivity(int activityKind, long timeFrom, long timeTo) {
            this.activityKind = activityKind;
            this.timeFrom = timeFrom;
            this.timeTo = timeTo;
        }
    }
}