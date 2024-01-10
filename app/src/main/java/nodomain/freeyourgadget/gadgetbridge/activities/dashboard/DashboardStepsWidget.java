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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;

/**
 * A simple {@link AbstractDashboardWidget} subclass.
 * Use the {@link DashboardStepsWidget#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DashboardStepsWidget extends AbstractDashboardWidget {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardStepsWidget.class);

    public DashboardStepsWidget() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param timeFrom Start time in seconds since Unix epoch.
     * @param timeTo End time in seconds since Unix epoch.
     * @return A new instance of fragment DashboardStepsWidget.
     */
    public static DashboardStepsWidget newInstance(int timeFrom, int timeTo) {
        DashboardStepsWidget fragment = new DashboardStepsWidget();
        Bundle args = new Bundle();
        args.putInt(ARG_TIME_FROM, timeFrom);
        args.putInt(ARG_TIME_TO, timeTo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.dashboard_widget_steps, container, false);
        TextView stepsCount = fragmentView.findViewById(R.id.steps_count);
        ImageView stepsGauge = fragmentView.findViewById(R.id.steps_gauge);

        // Update text representation
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
        stepsCount.setText(String.valueOf(totalSteps));

        // Draw gauge
        ActivityUser activityUser = new ActivityUser();
        float stepsGoal = activityUser.getStepsGoal();
        float goalFactor = totalSteps / stepsGoal;
        if (goalFactor > 1) goalFactor = 1;
        stepsGauge.setImageBitmap(drawGauge(200, 15, Color.BLUE, goalFactor));

        return fragmentView;
    }
}