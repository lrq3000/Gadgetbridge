package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import androidx.annotation.Nullable;

import java.nio.ByteOrder;
import java.util.List;

public enum LocalMessage {
    TODAY_WEATHER_CONDITIONS(6, GlobalFITMessage.WEATHER,
            new int[]{0, 253, 9, 1, 14, 13, 2, 3, 5, 4, 6, 7, 10, 11, 8}
    ),
    HOURLY_WEATHER_FORECAST(9, GlobalFITMessage.WEATHER,
            new int[]{0, 253, 1, 2, 3, 4, 5, 7, 15, 16, 17}
    ),
    DAILY_WEATHER_FORECAST(10, GlobalFITMessage.WEATHER,
            new int[]{0, 253, 14, 13, 2, 5, 12}
    );

    private final int type;
    private final GlobalFITMessage globalFITMessage;
    private final int[] globalDefinitionIds;

    LocalMessage(int type, GlobalFITMessage globalFITMessage, int[] globalDefinitionIds) {
        this.type = type;
        this.globalFITMessage = globalFITMessage;
        this.globalDefinitionIds = globalDefinitionIds;
    }

    @Nullable
    public static LocalMessage fromType(int type) {
        for (final LocalMessage localMessage : LocalMessage.values()) {
            if (localMessage.getType() == type) {
                return localMessage;
            }
        }
        return null;
    }

    public List<FieldDefinition> getLocalFieldDefinitions() {
        return globalFITMessage.getFieldDefinitions(globalDefinitionIds);
    }

    public RecordDefinition getRecordDefinition() {
        return new RecordDefinition(ByteOrder.BIG_ENDIAN, this);
    }

    public int getType() {
        return type;
    }

    public GlobalFITMessage getGlobalFITMessage() {
        return globalFITMessage;
    }
}
