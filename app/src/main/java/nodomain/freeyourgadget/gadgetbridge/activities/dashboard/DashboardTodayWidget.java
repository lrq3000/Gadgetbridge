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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.DashboardFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.StepAnalysis;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySession;
import nodomain.freeyourgadget.gadgetbridge.util.DashboardUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

/**
 * A simple {@link AbstractDashboardWidget} subclass.
 * Use the {@link DashboardTodayWidget#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DashboardTodayWidget extends AbstractDashboardWidget {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardTodayWidget.class);

    private View todayView;
    private ImageView todayChart;

    private boolean mode_24h;

    public DashboardTodayWidget() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param dashboardData An instance of DashboardFragment.DashboardData.
     * @return A new instance of fragment DashboardTodayWidget.
     */
    public static DashboardTodayWidget newInstance(DashboardFragment.DashboardData dashboardData) {
        DashboardTodayWidget fragment = new DashboardTodayWidget();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DASHBOARD_DATA, dashboardData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        todayView = inflater.inflate(R.layout.dashboard_widget_today, container, false);
        todayChart = todayView.findViewById(R.id.dashboard_today_chart);

        // Determine whether to draw a single or a double chart. In case 24h mode is selected,
        // use just the outer chart (chart_12_24) for all data.
        Prefs prefs = GBApplication.getPrefs();
        mode_24h = prefs.getBoolean("dashboard_widget_today_24h", false);

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

        legend.setVisibility(prefs.getBoolean("dashboard_widget_today_legend", true) ? View.VISIBLE : View.GONE);

        fillData();

        return todayView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (todayChart != null) fillData();
    }

    protected void fillData() {
        if (todayView == null) return;
        todayView.post(new Runnable() {
            @Override
            public void run() {
                FillDataAsyncTask myAsyncTask = new FillDataAsyncTask();
                myAsyncTask.execute();
            }
        });
    }

    private class FillDataAsyncTask extends AsyncTask<Void, Void, Void> {
        private final TreeMap<Long, Integer> activityTimestamps = new TreeMap<>();
        private final List<GeneralizedActivity> generalizedActivities = new ArrayList<>();
        Bitmap todayBitmap;

        private void addActivity(long timeFrom, long timeTo, int activityKind) {
            for (long i = timeFrom; i<=timeTo; i++) {
                // If the current timestamp isn't saved yet, do so immediately
                if (activityTimestamps.get(i) == null) {
                    activityTimestamps.put(i, activityKind);
                    continue;
                }
                // If the current timestamp is already saved, compare the activity kinds and
                // keep the most 'important' one
                switch (activityTimestamps.get(i)) {
                    case ActivityKind.TYPE_ACTIVITY:
                        break;
                    case ActivityKind.TYPE_DEEP_SLEEP:
                        if (activityKind == ActivityKind.TYPE_ACTIVITY)
                            activityTimestamps.put(i, activityKind);
                        break;
                    case ActivityKind.TYPE_LIGHT_SLEEP:
                        if (activityKind == ActivityKind.TYPE_ACTIVITY ||
                                activityKind == ActivityKind.TYPE_DEEP_SLEEP)
                            activityTimestamps.put(i, activityKind);
                        break;
                    case ActivityKind.TYPE_REM_SLEEP:
                        if (activityKind == ActivityKind.TYPE_ACTIVITY ||
                                activityKind == ActivityKind.TYPE_DEEP_SLEEP ||
                                activityKind == ActivityKind.TYPE_LIGHT_SLEEP)
                            activityTimestamps.put(i, activityKind);
                        break;
                    case ActivityKind.TYPE_SLEEP:
                        if (activityKind == ActivityKind.TYPE_ACTIVITY ||
                                activityKind == ActivityKind.TYPE_DEEP_SLEEP ||
                                activityKind == ActivityKind.TYPE_LIGHT_SLEEP ||
                                activityKind == ActivityKind.TYPE_REM_SLEEP)
                            activityTimestamps.put(i, activityKind);
                        break;
                    default:
                        activityTimestamps.put(i, activityKind);
                        break;
                }
            }
        }

        private void createGeneralizedActivities() {
            GeneralizedActivity previous = null;
            long midDaySecond = dashboardData.timeFrom + (12 * 60 * 60);
            for (Map.Entry<Long, Integer> activity : activityTimestamps.entrySet()) {
                long timestamp = activity.getKey();
                int activityKind = activity.getValue();
                if (previous == null || previous.activityKind != activityKind || (!mode_24h && timestamp == midDaySecond) || previous.timeTo < timestamp - 60) {
                    previous = new GeneralizedActivity(activityKind, timestamp, timestamp);
                    generalizedActivities.add(previous);
                } else {
                    previous.timeTo = timestamp;
                }
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            // Retrieve activity data
            List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
            List<ActivitySample> allActivitySamples = new ArrayList<>();
            List<ActivitySession> stepSessions = new ArrayList<>();
            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                for (GBDevice dev : devices) {
                    if ((dashboardData.showAllDevices || dashboardData.showDeviceList.contains(dev.getAddress())) && dev.getDeviceCoordinator().supportsActivityTracking()) {
                        List<? extends ActivitySample> activitySamples = DashboardUtils.getAllSamples(dbHandler, dev, dashboardData);
                        allActivitySamples.addAll(activitySamples);
                        StepAnalysis stepAnalysis = new StepAnalysis();
                        stepSessions.addAll(stepAnalysis.calculateStepSessions(activitySamples));
                    }
                }
            } catch (Exception e) {
                LOG.warn("Could not retrieve activity amounts: ", e);
            }

            // Integrate and chronologically order various data from multiple devices
            long midDaySecond = dashboardData.timeFrom + (12 * 60 * 60);
            for (ActivitySample sample : allActivitySamples) {
                // Handle only TYPE_NOT_WORN and TYPE_SLEEP (including variants) here
                if (sample.getKind() != ActivityKind.TYPE_NOT_WORN && (sample.getKind() == ActivityKind.TYPE_NOT_MEASURED || (sample.getKind() & ActivityKind.TYPE_SLEEP) == 0))
                    continue;
                // Add to day results
                addActivity(sample.getTimestamp(), sample.getTimestamp() + 60, sample.getKind());
            }
            for (ActivitySession session : stepSessions) {
                addActivity(session.getStartTime().getTime() / 1000, session.getEndTime().getTime() / 1000, ActivityKind.TYPE_ACTIVITY);
            }
            createGeneralizedActivities();

            // Prepare circular chart
            int width = 500;
            int height = 500;
            int barWidth = 40;
            int hourTextSp = 12;
            float hourTextPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, hourTextSp, requireContext().getResources().getDisplayMetrics());
            float outerCircleMargin = mode_24h ? barWidth / 2f : barWidth / 2f + hourTextPixels * 1.3f;
            float innerCircleMargin = outerCircleMargin + barWidth * 1.3f;
            int degreeFactor = mode_24h ? 240 : 120;
            todayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(todayBitmap);
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.STROKE);

            // Draw clock stripes
            float clockMargin = outerCircleMargin + (mode_24h ? barWidth : barWidth*2.3f);
            int clockStripesInterval = mode_24h ? 15 : 30;
            float clockStripesWidth = barWidth / 3f;
            paint.setStrokeWidth(clockStripesWidth);
            paint.setColor(color_worn);
            for (int i=0; i<360; i+=clockStripesInterval) {
                canvas.drawArc(clockMargin, clockMargin, width - clockMargin, height - clockMargin, i, 1, false, paint);
            }

            // Draw hours
            boolean normalClock = DateFormat.is24HourFormat(getContext());
            Map<Integer, String> hours = new HashMap<Integer, String>() {
                {
                    put(3, "3");
                    put(6, normalClock ? "6" : "6am");
                    put(9, "9");
                    put(12, normalClock ? "12" : "12pm");
                    put(15, normalClock ? "15" : "3");
                    put(18, normalClock ? "18" : "6pm");
                    put(21, normalClock ? "21" : "9");
                    put(24, normalClock ? "24" : "12am");
                }
            };
            Paint textPaint = new Paint();
            textPaint.setAntiAlias(true);
            textPaint.setColor(color_worn);
            textPaint.setTextSize(hourTextPixels);
            textPaint.setTextAlign(Paint.Align.CENTER);
            Rect textBounds = new Rect();
            if (mode_24h) {
                textPaint.getTextBounds(hours.get(6), 0, hours.get(6).length(), textBounds);
                canvas.drawText(hours.get(6), width - (clockMargin + clockStripesWidth + textBounds.width()), height / 2f + textBounds.height() / 2f, textPaint);
                textPaint.getTextBounds(hours.get(12), 0, hours.get(12).length(), textBounds);
                canvas.drawText(hours.get(12), width / 2f, height - (clockMargin + clockStripesWidth), textPaint);
                textPaint.getTextBounds(hours.get(18), 0, hours.get(18).length(), textBounds);
                canvas.drawText(hours.get(18), clockMargin + clockStripesWidth + textBounds.width() / 2f, height / 2f + textBounds.height() / 2f, textPaint);
                textPaint.getTextBounds(hours.get(24), 0, hours.get(24).length(), textBounds);
                canvas.drawText(hours.get(24), width / 2f, clockMargin + clockStripesWidth + textBounds.height(), textPaint);
            } else {
                textPaint.getTextBounds(hours.get(3), 0, hours.get(3).length(), textBounds);
                canvas.drawText(hours.get(3), width - (clockMargin + clockStripesWidth + textBounds.width()), height / 2f + textBounds.height() / 2f, textPaint);
                textPaint.getTextBounds(hours.get(6), 0, hours.get(6).length(), textBounds);
                canvas.drawText(hours.get(6), width / 2f, height - (clockMargin + clockStripesWidth), textPaint);
                textPaint.getTextBounds(hours.get(9), 0, hours.get(9).length(), textBounds);
                canvas.drawText(hours.get(9), clockMargin + clockStripesWidth + textBounds.width() / 2f, height / 2f + textBounds.height() / 2f, textPaint);
                textPaint.getTextBounds(hours.get(12), 0, hours.get(12).length(), textBounds);
                canvas.drawText(hours.get(12), width / 2f, clockMargin + clockStripesWidth + textBounds.height(), textPaint);
                textPaint.getTextBounds(hours.get(15), 0, hours.get(15).length(), textBounds);
                canvas.drawText(hours.get(15), width - textBounds.width() / 2f, height / 2f + textBounds.height() / 2f, textPaint);
                textPaint.getTextBounds(hours.get(18), 0, hours.get(18).length(), textBounds);
                canvas.drawText(hours.get(18), width / 2f, height - textBounds.height() / 2f, textPaint);
                textPaint.getTextBounds(hours.get(21), 0, hours.get(21).length(), textBounds);
                canvas.drawText(hours.get(21), textBounds.width() / 2f, height / 2f + textBounds.height() / 2f, textPaint);
                textPaint.getTextBounds(hours.get(24), 0, hours.get(24).length(), textBounds);
                canvas.drawText(hours.get(24), width / 2f, textBounds.height(), textPaint);
            }

            // Draw generalized activities on circular chart
            long secondIndex = dashboardData.timeFrom;
            long currentTime = Calendar.getInstance().getTimeInMillis() / 1000;
            for (GeneralizedActivity activity : generalizedActivities) {
                // Determine margin depending on 24h/12h mode
                float margin = (mode_24h || activity.timeFrom >= midDaySecond) ? outerCircleMargin : innerCircleMargin;
                // Draw inactive slices
                if (!mode_24h && secondIndex < midDaySecond && activity.timeFrom >= midDaySecond) {
                    paint.setStrokeWidth(barWidth / 3f);
                    paint.setColor(color_worn);
                    canvas.drawArc(innerCircleMargin, innerCircleMargin, width - innerCircleMargin, height - innerCircleMargin, 270 + (secondIndex - dashboardData.timeFrom) / degreeFactor, (midDaySecond - secondIndex) / degreeFactor, false, paint);
                    secondIndex = midDaySecond;
                }
                if (activity.timeFrom > secondIndex) {
                    paint.setStrokeWidth(barWidth / 3f);
                    paint.setColor(color_worn);
                    canvas.drawArc(margin, margin, width - margin, height - margin, 270 + (secondIndex - dashboardData.timeFrom) / degreeFactor, (activity.timeFrom - secondIndex) / degreeFactor, false, paint);
                }
                long start_angle = 270 + (activity.timeFrom - dashboardData.timeFrom) / degreeFactor;
                long sweep_angle = (activity.timeTo - activity.timeFrom) / degreeFactor;
                if (activity.activityKind == ActivityKind.TYPE_NOT_WORN) {
                    paint.setStrokeWidth(barWidth / 3f);
                    paint.setColor(color_not_worn);
                    canvas.drawArc(margin, margin, width - margin, height - margin, start_angle, sweep_angle, false, paint);
                } else if (activity.activityKind == ActivityKind.TYPE_REM_SLEEP || activity.activityKind == ActivityKind.TYPE_LIGHT_SLEEP || activity.activityKind == ActivityKind.TYPE_SLEEP) {
                    paint.setStrokeWidth(barWidth);
                    paint.setColor(color_light_sleep);
                    canvas.drawArc(margin, margin, width - margin, height - margin, start_angle, sweep_angle, false, paint);
                } else if (activity.activityKind == ActivityKind.TYPE_DEEP_SLEEP) {
                    paint.setStrokeWidth(barWidth);
                    paint.setColor(color_deep_sleep);
                    canvas.drawArc(margin, margin, width - margin, height - margin, start_angle, sweep_angle, false, paint);
                } else {
                    paint.setStrokeWidth(barWidth);
                    paint.setColor(color_activity);
                    canvas.drawArc(margin, margin, width - margin, height - margin, start_angle, sweep_angle, false, paint);
                }
                secondIndex = activity.timeTo;
            }
            // Fill remaining time until current time
            if (!mode_24h && currentTime > dashboardData.timeFrom && currentTime < midDaySecond) {
                // Fill inner bar up until current time
                paint.setStrokeWidth(barWidth / 3f);
                paint.setColor(color_worn);
                canvas.drawArc(innerCircleMargin, innerCircleMargin, width - innerCircleMargin, height - innerCircleMargin, 270 + (secondIndex - dashboardData.timeFrom) / degreeFactor, (currentTime - secondIndex) / degreeFactor, false, paint);
                // Fill inner bar up until midday
                paint.setStrokeWidth(barWidth / 3f);
                paint.setColor(color_unknown);
                canvas.drawArc(innerCircleMargin, innerCircleMargin, width - innerCircleMargin, height - innerCircleMargin, 270 + (currentTime - dashboardData.timeFrom) / degreeFactor, (midDaySecond - currentTime) / degreeFactor, false, paint);
                // Fill outer bar up until midnight
                paint.setStrokeWidth(barWidth / 3f);
                paint.setColor(color_unknown);
                canvas.drawArc(outerCircleMargin, outerCircleMargin, width - outerCircleMargin, height - outerCircleMargin, 0, 360, false, paint);
            }
            if ((mode_24h || currentTime >= midDaySecond) && currentTime < dashboardData.timeTo) {
                // Fill outer bar up until current time
                paint.setStrokeWidth(barWidth / 3f);
                paint.setColor(color_worn);
                canvas.drawArc(outerCircleMargin, outerCircleMargin, width - outerCircleMargin, height - outerCircleMargin, 270 + (secondIndex - dashboardData.timeFrom) / degreeFactor, (currentTime - secondIndex) / degreeFactor, false, paint);
                // Fill outer bar up until midnight
                paint.setStrokeWidth(barWidth / 3f);
                paint.setColor(color_unknown);
                canvas.drawArc(outerCircleMargin, outerCircleMargin, width - outerCircleMargin, height - outerCircleMargin, 270 + (currentTime - dashboardData.timeFrom) / degreeFactor, (dashboardData.timeTo - currentTime) / degreeFactor, false, paint);
            }
            if (secondIndex < dashboardData.timeTo && currentTime > dashboardData.timeTo) {
                // Fill outer bar up until midnight
                paint.setStrokeWidth(barWidth / 3f);
                paint.setColor(color_worn);
                canvas.drawArc(outerCircleMargin, outerCircleMargin, width - outerCircleMargin, height - outerCircleMargin, 270 + (secondIndex - dashboardData.timeFrom) / degreeFactor, (dashboardData.timeTo - secondIndex) / degreeFactor, false, paint);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            todayChart.setImageBitmap(todayBitmap);
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

        @NonNull
        @Override
        public String toString() {
            return "Generalized activity: timeFrom=" + timeFrom + ", timeTo=" + timeTo + ", activityKind=" + activityKind + ", calculated duration: " + (timeTo - timeFrom) + " seconds";
        }
    }
}