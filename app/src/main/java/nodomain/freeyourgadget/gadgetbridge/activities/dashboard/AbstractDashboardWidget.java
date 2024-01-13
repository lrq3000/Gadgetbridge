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
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;

import androidx.annotation.ColorInt;
import androidx.fragment.app.Fragment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.StepAnalysis;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySession;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.DailyTotals;

public abstract class AbstractDashboardWidget extends Fragment {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDashboardWidget.class);

    protected static String ARG_TIME_FROM = "dashboard_widget_argument_time_from";
    protected static String ARG_TIME_TO = "dashboard_widget_argument_time_to";

    protected int timeFrom;
    protected int timeTo;

    protected @ColorInt int color_not_worn = Color.argb(128, 0, 0, 0);
    protected @ColorInt int color_worn = Color.argb(128, 128, 128, 128);
    protected @ColorInt int color_activity = Color.GREEN;
    protected @ColorInt int color_deep_sleep = Color.BLUE;
    protected @ColorInt int color_light_sleep = Color.rgb(150, 150, 255);
    protected @ColorInt int color_distance = Color.BLUE;
    protected @ColorInt int color_active_time = Color.rgb(170, 0, 255);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            timeFrom = getArguments().getInt(ARG_TIME_FROM);
            timeTo = getArguments().getInt(ARG_TIME_TO);
        }
    }


    public void setTimespan(int timeFrom, int timeTo) {
        this.timeFrom = timeFrom;
        this.timeTo = timeTo;
        fillData();
    }

    protected abstract void fillData();

    protected long getSteps(GBDevice device, DBHandler db) {
        Calendar day = GregorianCalendar.getInstance();
        day.setTimeInMillis(timeTo * 1000L);
        DailyTotals ds = new DailyTotals();
        return ds.getDailyTotalsForDevice(device, day, db)[0];
    }

    protected long getSleep(GBDevice device, DBHandler db) {
        Calendar day = GregorianCalendar.getInstance();
        day.setTimeInMillis(timeTo * 1000L);
        DailyTotals ds = new DailyTotals();
        return ds.getDailyTotalsForDevice(device, day, db)[1];
    }

    protected long getActiveMinutes(GBDevice gbDevice, DBHandler db, int timeFrom, int timeTo) {
        ActivitySession stepSessionsSummary = new ActivitySession();
        List<ActivitySession> stepSessions;
        List<? extends ActivitySample> activitySamples = getAllSamples(db, gbDevice, timeFrom, timeTo);
        StepAnalysis stepAnalysis = new StepAnalysis();

        boolean isEmptySummary = false;
        if (activitySamples != null) {
            stepSessions = stepAnalysis.calculateStepSessions(activitySamples);
            if (stepSessions.toArray().length == 0) {
                isEmptySummary = true;
            }
            stepSessionsSummary = stepAnalysis.calculateSummary(stepSessions, isEmptySummary);
        }
        long duration = stepSessionsSummary.getEndTime().getTime() - stepSessionsSummary.getStartTime().getTime();
        return duration / 1000 / 60;
    }

    protected List<? extends ActivitySample> getAllSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        SampleProvider<? extends ActivitySample> provider = getProvider(db, device);
        return provider.getAllActivitySamples(tsFrom, tsTo);
    }

    SampleProvider<? extends AbstractActivitySample> getProvider(DBHandler db, GBDevice device) {
        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        return coordinator.getSampleProvider(device, db.getDaoSession());
    }

    /**
     * @param width Bitmap width in pixels
     * @param barWidth Gauge bar width in pixels
     * @param filledColor Color of the filled part of the gauge
     * @param filledFactor Factor between 0 and 1 that determines the amount of the gauge that should be filled
     * @return Bitmap containing the gauge
     */
    Bitmap drawGauge(int width, int barWidth, @ColorInt int filledColor, float filledFactor) {
        int height = width / 2;
        int barMargin = (int) Math.ceil(barWidth / 2f);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(barWidth);
        paint.setColor(Color.argb(75, 150, 150, 150));
        canvas.drawArc(barMargin, barMargin, width - barMargin, width - barMargin, 180 + 180 * filledFactor, 180 - 180 * filledFactor, false, paint);
        paint.setColor(filledColor);
        canvas.drawArc(barMargin, barMargin, width - barMargin, width - barMargin, 180, 180 * filledFactor, false, paint);

        return bitmap;
    }

    protected int getStepsTotal() {
        List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        int totalSteps = 0;
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            for (GBDevice dev : devices) {
                if (dev.getDeviceCoordinator().supportsActivityTracking()) {
                    totalSteps += getSteps(dev, dbHandler);
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not calculate total amount of steps: ", e);
        }
        return totalSteps;
    }

    protected float getStepsGoalFactor() {
        ActivityUser activityUser = new ActivityUser();
        float stepsGoal = activityUser.getStepsGoal();
        float goalFactor = getStepsTotal() / stepsGoal;
        if (goalFactor > 1) goalFactor = 1;

        return goalFactor;
    }

    protected float getDistanceTotal() {
        List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        long totalSteps = 0;
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            for (GBDevice dev : devices) {
                if (dev.getDeviceCoordinator().supportsActivityTracking()) {
                    totalSteps += getSteps(dev, dbHandler);
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not calculate total distance: ", e);
        }
        ActivityUser activityUser = new ActivityUser();
        int stepLength = activityUser.getStepLengthCm();
        return totalSteps * stepLength * 0.01f;
    }

    protected float getDistanceGoalFactor() {
        ActivityUser activityUser = new ActivityUser();
        int distanceGoal = activityUser.getDistanceGoalMeters();
        float goalFactor = getDistanceTotal() / distanceGoal;
        if (goalFactor > 1) goalFactor = 1;

        return goalFactor;
    }

    protected long getActiveMinutesTotal() {
        List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        long totalActiveMinutes = 0;
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            for (GBDevice dev : devices) {
                if (dev.getDeviceCoordinator().supportsActivityTracking()) {
                    totalActiveMinutes += getActiveMinutes(dev, dbHandler, timeFrom, timeTo);
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not calculate total amount of activity: ", e);
        }
        return totalActiveMinutes;
    }

    protected float getActiveMinutesGoalFactor() {
        ActivityUser activityUser = new ActivityUser();
        int activeTimeGoal = activityUser.getActiveTimeGoalMinutes();
        float goalFactor = (float) getActiveMinutesTotal() / activeTimeGoal;
        if (goalFactor > 1) goalFactor = 1;

        return goalFactor;
    }

    protected long getSleepMinutesTotal() {
        List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        long totalSleepMinutes = 0;
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            for (GBDevice dev : devices) {
                if (dev.getDeviceCoordinator().supportsActivityTracking()) {
                    totalSleepMinutes += getSleep(dev, dbHandler);
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not calculate total amount of sleep: ", e);
        }
        return totalSleepMinutes;
    }

    protected float getSleepMinutesGoalFactor() {
        ActivityUser activityUser = new ActivityUser();
        int sleepMinutesGoal = activityUser.getSleepDurationGoal() * 60;
        float goalFactor = (float) getSleepMinutesTotal() / sleepMinutesGoal;
        if (goalFactor > 1) goalFactor = 1;

        return goalFactor;
    }
}
