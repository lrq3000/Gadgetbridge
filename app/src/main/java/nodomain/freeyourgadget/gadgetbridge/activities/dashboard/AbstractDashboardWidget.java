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

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.activities.charts.StepAnalysis;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySession;
import nodomain.freeyourgadget.gadgetbridge.model.DailyTotals;

public abstract class AbstractDashboardWidget extends Fragment {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDashboardWidget.class);

    protected static String ARG_TIME_FROM = "dashboard_widget_argument_time_from";
    protected static String ARG_TIME_TO = "dashboard_widget_argument_time_to";

    protected int timeFrom;
    protected int timeTo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            timeFrom = getArguments().getInt(ARG_TIME_FROM);
            timeTo = getArguments().getInt(ARG_TIME_TO);
        }
    }

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
}
