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

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
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
import nodomain.freeyourgadget.gadgetbridge.activities.charts.SleepAnalysis;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.StepAnalysis;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySession;

/**
 * A simple {@link AbstractDashboardWidget} subclass.
 * Use the {@link DashboardTodayWidget#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DashboardTodayWidget extends AbstractDashboardWidget {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardTodayWidget.class);

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View todayView = inflater.inflate(R.layout.dashboard_widget_today, container, false);

        // Initialize chart
        PieChart chart = todayView.findViewById(R.id.dashboard_piechart_today);
        chart.getDescription().setEnabled(false);
        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.argb(0,0,0,0));
        chart.setHoleRadius(80f);
        chart.setTransparentCircleRadius(81);
        chart.setTransparentCircleColor(GBApplication.getTextColor(getContext()));
        chart.setRotationEnabled(false);
        chart.setDrawEntryLabels(false);
        chart.setHighlightPerTapEnabled(false);

        // Initialize legend
        Legend l = chart.getLegend();
        l.setTextColor(GBApplication.getTextColor(getContext()));
        ArrayList<LegendEntry> legendEntries = new ArrayList<>();
        legendEntries.add(new LegendEntry(getContext().getString(R.string.activity_type_deep_sleep), Legend.LegendForm.SQUARE, 10f, 10f, new DashPathEffect(new float[]{10f, 5f}, 0f), Color.rgb(0, 0, 255)));
        legendEntries.add(new LegendEntry(getContext().getString(R.string.activity_type_light_sleep), Legend.LegendForm.SQUARE, 10f, 10f, new DashPathEffect(new float[]{10f, 5f}, 0f), Color.rgb(150, 150, 255)));
        legendEntries.add(new LegendEntry(getContext().getString(R.string.activity_type_activity), Legend.LegendForm.SQUARE, 10f, 10f, new DashPathEffect(new float[]{10f, 5f}, 0f), Color.rgb(0, 255, 0)));
        legendEntries.add(new LegendEntry(getContext().getString(R.string.abstract_chart_fragment_kind_not_worn), Legend.LegendForm.SQUARE, 10f, 10f, new DashPathEffect(new float[]{10f, 5f}, 0f), Color.rgb(0, 0, 0)));
        l.setCustom(legendEntries);
        l.setEnabled(false);

        // Retrieve activity data
        List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        List<ActivitySample> allActivitySamples = new ArrayList<>();
        List<ActivitySession> stepSessions = new ArrayList<>();
        List<SleepAnalysis.SleepSession> sleepSessions = new ArrayList<>();
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            for (GBDevice dev : devices) {
                if (dev.getDeviceCoordinator().supportsActivityTracking()) {
                    List<? extends ActivitySample> activitySamples = getAllSamples(dbHandler, dev, timeFrom, timeTo);
                    allActivitySamples.addAll(activitySamples);
                    StepAnalysis stepAnalysis = new StepAnalysis();
                    SleepAnalysis sleepAnalysis = new SleepAnalysis();
                    if (activitySamples != null) {
                        stepSessions.addAll(stepAnalysis.calculateStepSessions(activitySamples));
                        sleepSessions.addAll(sleepAnalysis.calculateSleepSessions(activitySamples));
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not retrieve activity amounts: ", e);
        }

        // Integrate and chronologically order various data from multiple devices
        List<GeneralizedActivity> generalizedActivities = new ArrayList<>();
        for (ActivitySample sample : allActivitySamples) {
            if (sample.getKind() != ActivityKind.TYPE_NOT_WORN) continue;
            if (generalizedActivities.size() > 0) {
                GeneralizedActivity previous = generalizedActivities.get(generalizedActivities.size() - 1);
                if (previous.activityKind == ActivityKind.TYPE_NOT_WORN && previous.timeTo > sample.getTimestamp() - 60) {
                    // If the current sample starts within a minute after the end of the previous not worn session, merge them
                    generalizedActivities.get(generalizedActivities.size() - 1).timeTo = sample.getTimestamp() + 60;
                    continue;
                }
            }
            generalizedActivities.add(new GeneralizedActivity(
                    sample.getKind(),
                    sample.getTimestamp(),
                    sample.getTimestamp() + 60
            ));
        }
        for (ActivitySession session : stepSessions) {
            generalizedActivities.add(new GeneralizedActivity(
                    session.getActivityKind(),
                    session.getStartTime().getTime() / 1000,
                    session.getEndTime().getTime() / 1000
            ));
        }
        for (SleepAnalysis.SleepSession session : sleepSessions) {
            generalizedActivities.add(new GeneralizedActivity(
                    ActivityKind.TYPE_SLEEP,
                    session.getSleepStart().getTime() / 1000,
                    session.getSleepEnd().getTime() / 1000
            ));
        }
        Collections.sort(generalizedActivities, (o1, o2) -> (int) (o1.timeFrom - o2.timeFrom));

        // Add pie slice entries
        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();
        long secondIndex = timeFrom;
        for (GeneralizedActivity activity : generalizedActivities) {
            // FIXME: correctly merge parallel activities from multiple devices
            // Skip earlier sessions
            if (activity.timeFrom < secondIndex) continue;
            // Draw inactive slice
            if (activity.timeFrom > secondIndex) {
                entries.add(new PieEntry(activity.timeFrom - secondIndex, "Inactive"));
                colors.add(Color.rgb(128, 128, 128));
            }
            // Draw activity slices
            if (activity.activityKind == ActivityKind.TYPE_NOT_WORN) {
                entries.add(new PieEntry(activity.timeTo - activity.timeFrom, "Not worn"));
                colors.add(Color.rgb(0, 0, 0));
                secondIndex = activity.timeTo;
            } else if (activity.activityKind == ActivityKind.TYPE_LIGHT_SLEEP || activity.activityKind == ActivityKind.TYPE_SLEEP) {
                entries.add(new PieEntry(activity.timeTo - activity.timeFrom, "Light sleep"));
                colors.add(Color.rgb(150, 150, 255));
                secondIndex = activity.timeTo;
            } else if (activity.activityKind == ActivityKind.TYPE_DEEP_SLEEP) {
                entries.add(new PieEntry(activity.timeTo - activity.timeFrom, "Deep sleep"));
                colors.add(Color.rgb(0, 0, 255));
                secondIndex = activity.timeTo;
            //} else if (activity.activityKind == ActivityKind.TYPE_WALKING || activity.activityKind == ActivityKind.TYPE_RUNNING || activity.activityKind == ActivityKind.TYPE_EXERCISE){
            } else {
                entries.add(new PieEntry(activity.timeTo - activity.timeFrom, "Active"));
                colors.add(Color.rgb(0, 255, 0));
                secondIndex = activity.timeTo;
            }
        }
        // Fill remaining time until midnight
        if (Calendar.getInstance().getTimeInMillis() / 1000 > timeFrom && Calendar.getInstance().getTimeInMillis() / 1000 < timeTo) {
            // Fill with unknown slice up until current time
            entries.add(new PieEntry(Calendar.getInstance().getTimeInMillis() / 1000 - secondIndex, "Unknown"));
            colors.add(Color.rgb(128, 128, 128));
            // Draw transparent slice for remaining time until midnight
            entries.add(new PieEntry(timeTo - Calendar.getInstance().getTimeInMillis() / 1000, ""));
            colors.add(Color.argb(0, 0, 0, 0));
        }

        // Draw chart
        PieDataSet dataSet = new PieDataSet(entries, "Today");
        dataSet.setSliceSpace(0f);
        dataSet.setSelectionShift(5f);
        dataSet.setDrawValues(false);
        dataSet.setColors(colors);
        PieData data = new PieData(dataSet);
        chart.setData(data);

        // Initialize scale chart
        PieChart scale = todayView.findViewById(R.id.dashboard_piechart_scale);
        scale.getDescription().setEnabled(false);
        scale.getLegend().setEnabled(false);
        scale.setDrawHoleEnabled(true);
        scale.setHoleColor(Color.argb(0,0,0,0));
        scale.setHoleRadius(99f);
        scale.setRotationEnabled(false);
        scale.setHighlightPerTapEnabled(false);
        scale.setRotationAngle(278f);
        scale.setEntryLabelColor(GBApplication.getTextColor(getContext()));
        ArrayList<PieEntry> scaleEntries = new ArrayList<>();
        for (int i = 1; i <= 24; i++) {
            scaleEntries.add(new PieEntry(1, String.valueOf(i)));
        }
        PieDataSet scaleDataSet = new PieDataSet(scaleEntries, "Time scale");
        scaleDataSet.setSliceSpace(0f);
        scaleDataSet.setDrawValues(false);
        scaleDataSet.setColor(Color.argb(0,0,0,0));
        PieData scaleData = new PieData(scaleDataSet);
        scale.setData(scaleData);

        return todayView;
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