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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DashboardActiveTimeWidget#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DashboardActiveTimeWidget extends AbstractDashboardWidget {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardActiveTimeWidget.class);

    public DashboardActiveTimeWidget() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.dashboard_widget_active_time, container, false);
        Calendar day = Calendar.getInstance();
        day.set(Calendar.HOUR_OF_DAY, 23);
        day.set(Calendar.MINUTE, 59);
        day.set(Calendar.SECOND, 59);
        int timeTo = (int) (day.getTimeInMillis() / 1000);
        int timeFrom = DateTimeUtils.shiftDays(timeTo, -1);
        List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        long totalActiveMinutes = 0;
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            for (GBDevice dev : devices) {
                if (dev.getDeviceCoordinator().supportsActivityTracking()) {
                    totalActiveMinutes += getActiveMinutes(dev, dbHandler, timeFrom, timeTo);
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not calculate total amount of sleep: ", e);
        }
        TextView activeTime = fragmentView.findViewById(R.id.active_time);
        activeTime.setText(DateTimeUtils.formatDurationHoursMinutes(totalActiveMinutes, TimeUnit.MINUTES));
        return fragmentView;
    }
}