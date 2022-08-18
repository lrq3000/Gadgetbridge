/*  Copyright (C) 2022 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.miband7;

import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.miband7.MiBand7FWHelper;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Support;

public class MiBand7Support extends Huami2021Support {
    private static final Logger LOG = LoggerFactory.getLogger(MiBand7Support.class);

    @Override
    public HuamiFWHelper createFWHelper(Uri uri, Context context) throws IOException {
        return new MiBand7FWHelper(uri, context);
    }

    @Override
    protected int getAllDisplayItems() {
        return R.array.pref_miband7_display_items_values;
    }

    @Override
    protected int getDefaultDisplayItems() {
        return R.array.pref_miband7_display_items_default;
    }

    @Override
    protected int getAllShortcutItems() {
        return R.array.pref_miband7_shortcuts_values;
    }

    @Override
    protected int getDefaultShortcutItems() {
        return R.array.pref_miband7_shortcuts_default;
    }
}
