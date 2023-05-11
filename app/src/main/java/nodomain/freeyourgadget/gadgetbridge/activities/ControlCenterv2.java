/*  Copyright (C) 2016-2024 Andreas Shimokawa, Andrzej Surowiec, Arjan
    Schrijver, Carsten Pfeiffer, Daniel Dakhno, Daniele Gobbetti, Ganblejs,
    gfwilliams, Gordon Williams, Johannes Tysiak, José Rebelo, marco.altomonte,
    Petr Vaněk, Taavi Eomäe

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities;

import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_CONNECT;
import static nodomain.freeyourgadget.gadgetbridge.util.GB.toast;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.TypedValue;
import android.view.MenuItem;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.view.menu.MenuItemImpl;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.discovery.DiscoveryActivityV2;
import nodomain.freeyourgadget.gadgetbridge.adapter.GBDeviceAdapterv2;
import nodomain.freeyourgadget.gadgetbridge.database.DBAccess;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DailyTotals;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GBChangeLog;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class ControlCenterv2 extends Fragment {

    private DeviceManager deviceManager;
    private GBDeviceAdapterv2 mGBDeviceAdapter;
    private RecyclerView deviceListView;
    private FloatingActionButton fab;
    List<GBDevice> deviceList;
    private  HashMap<String,long[]> deviceActivityHashMap = new HashMap();

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View currentView = inflater.inflate(R.layout.activity_controlcenterv2_content_main, container, false);

        deviceManager = ((GBApplication) getActivity().getApplication()).getDeviceManager();

        deviceListView = currentView.findViewById(R.id.deviceListView);
        deviceListView.setHasFixedSize(true);
        deviceListView.setLayoutManager(new LinearLayoutManager(currentView.getContext()));

        deviceList = deviceManager.getDevices();
        mGBDeviceAdapter = new GBDeviceAdapterv2(currentView.getContext(), deviceList, deviceActivityHashMap);
        mGBDeviceAdapter.setHasStableIds(true);

        deviceListView.setAdapter(this.mGBDeviceAdapter);

        // get activity data asynchronously, this fills the deviceActivityHashMap
        // and calls refreshPairedDevices() → notifyDataSetChanged
        createRefreshTask("get activity data", getActivity().getApplication()).execute();

        fab = currentView.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchDiscoveryActivity();
            }
        });

        showFabIfNeccessary();

        /* uncomment to enable fixed-swipe to reveal more actions

        ItemTouchHelper swipeToDismissTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.LEFT , ItemTouchHelper.RIGHT) {
            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if(dX>50)
                    dX = 50;
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                GB.toast(getBaseContext(), "onMove", Toast.LENGTH_LONG, GB.ERROR);

                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                GB.toast(getBaseContext(), "onSwiped", Toast.LENGTH_LONG, GB.ERROR);

            }

            @Override
            public void onChildDrawOver(Canvas c, RecyclerView recyclerView,
                                        RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                        int actionState, boolean isCurrentlyActive) {
            }
        });

        swipeToDismissTouchHelper.attachToRecyclerView(deviceListView);
        */

        registerForContextMenu(deviceListView);

        refreshPairedDevices();

        if (GB.isBluetoothEnabled() && deviceList.isEmpty() && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            startActivity(new Intent(getActivity(), DiscoveryActivity.class));
        } else {
            GBApplication.deviceService().requestDeviceInfo();
        }

        return currentView;
    }

    private void launchDiscoveryActivity() {
        startActivity(new Intent(getActivity(), DiscoveryActivity.class));
    }

    private void showFabIfNeccessary() {
        if (GBApplication.getPrefs().getBoolean("display_add_device_fab", true)) {
            fab.show();
        } else {
            if (deviceManager.getDevices().size() < 1) {
                fab.show();
            } else {
                fab.hide();
            }
        }
    }

    @Override
    public void onDestroy() {
        unregisterForContextMenu(deviceListView);
        super.onDestroy();
    }

    private long[] getSteps(GBDevice device, DBHandler db) {
        Calendar day = GregorianCalendar.getInstance();

        DailyTotals ds = new DailyTotals();
        return ds.getDailyTotalsForDevice(device, day, db);
    }

    public void refreshPairedDevices() {
        mGBDeviceAdapter.notifyDataSetChanged();
        mGBDeviceAdapter.rebuildFolders();
    }

    public RefreshTask createRefreshTask(String task, Context context) {
        return new RefreshTask(task, context);
    }

    private void handleShortcut(Intent intent) {
        if(ACTION_CONNECT.equals(intent.getAction())) {
            String btDeviceAddress = intent.getStringExtra("device");
            if(btDeviceAddress!=null){
                GBDevice candidate = DeviceHelper.getInstance().findAvailableDevice(btDeviceAddress, this);
                if (candidate != null && !candidate.isConnected()) {
                    GBApplication.deviceService(candidate).connect();
                }
            }
        }
    }
    public class RefreshTask extends DBAccess {
        public RefreshTask(String task, Context context) {
            super(task, context);
        }

        @Override
        protected void doInBackground(DBHandler db) {
            for (GBDevice gbDevice : deviceList) {
                final DeviceCoordinator coordinator = gbDevice.getDeviceCoordinator();
                if (coordinator.supportsActivityTracking()) {
                    long[] stepsAndSleepData = getSteps(gbDevice, db);
                    deviceActivityHashMap.put(gbDevice.getAddress(), stepsAndSleepData);
                }
            }
        }

        @Override
        protected void onPostExecute(Object o) {
            refreshPairedDevices();
        }

    }
}
