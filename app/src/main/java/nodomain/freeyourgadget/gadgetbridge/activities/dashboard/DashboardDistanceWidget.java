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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.util.FormatUtils;

/**
 * A simple {@link AbstractDashboardWidget} subclass.
 * Use the {@link DashboardDistanceWidget#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DashboardDistanceWidget extends AbstractDashboardWidget {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardDistanceWidget.class);

    public DashboardDistanceWidget() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param timeFrom Start time in seconds since Unix epoch.
     * @param timeTo End time in seconds since Unix epoch.
     * @return A new instance of fragment DashboardDistanceWidget.
     */
    public static DashboardDistanceWidget newInstance(int timeFrom, int timeTo) {
        DashboardDistanceWidget fragment = new DashboardDistanceWidget();
        Bundle args = new Bundle();
        args.putInt(ARG_TIME_FROM, timeFrom);
        args.putInt(ARG_TIME_TO, timeTo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.dashboard_widget_distance, container, false);
        List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        long totalSteps = 0;
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            for (GBDevice dev : devices) {
                if (dev.getDeviceCoordinator().supportsActivityTracking()) {
                    totalSteps += getSteps(dev, dbHandler);
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not calculate total amount of steps: ", e);
        }
        ActivityUser activityUser = new ActivityUser();
        int stepLength = activityUser.getStepLengthCm();
        double distanceMeters = totalSteps * stepLength * 0.01;
        String distanceFormatted = FormatUtils.getFormattedDistanceLabel(distanceMeters);
        TextView distance = fragmentView.findViewById(R.id.distance);
        distance.setText(distanceFormatted);
        return fragmentView;
    }
}