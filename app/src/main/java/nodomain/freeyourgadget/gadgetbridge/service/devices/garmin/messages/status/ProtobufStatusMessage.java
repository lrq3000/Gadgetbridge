package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MessageReader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MessageWriter;

public class ProtobufStatusMessage extends GFDIStatusMessage {

    private final Status status;
    private final int requestId;
    private final int dataOffset;
    private final ProtobufChunkStatus protobufChunkStatus;
    private final ProtobufStatusCode protobufStatusCode;
    private final int messageType;
    private final boolean sendOutgoing;

    public ProtobufStatusMessage(int messageType, Status status, int requestId, int dataOffset, ProtobufChunkStatus protobufChunkStatus, ProtobufStatusCode protobufStatusCode) {
        this(messageType, status, requestId, dataOffset, protobufChunkStatus, protobufStatusCode, true);
    }

    public ProtobufStatusMessage(int messageType, Status status, int requestId, int dataOffset, ProtobufChunkStatus protobufChunkStatus, ProtobufStatusCode protobufStatusCode, boolean sendOutgoing) {
        this.messageType = messageType;
        this.status = status;
        this.requestId = requestId;
        this.dataOffset = dataOffset;
        this.protobufChunkStatus = protobufChunkStatus;
        this.protobufStatusCode = protobufStatusCode;
        this.sendOutgoing = sendOutgoing;
    }

    public static ProtobufStatusMessage parseIncoming(MessageReader reader, int messageType) {
        final Status status = Status.fromCode(reader.readByte());
        final int requestID = reader.readShort();
        final int dataOffset = reader.readInt();
        final ProtobufChunkStatus protobufStatus = ProtobufChunkStatus.fromCode(reader.readByte());
        final ProtobufStatusCode error = ProtobufStatusCode.fromCode(reader.readByte());

        reader.warnIfLeftover();
        return new ProtobufStatusMessage(messageType, status, requestID, dataOffset, protobufStatus, error, false);
    }

    public int getDataOffset() {
        return dataOffset;
    }

    public ProtobufChunkStatus getProtobufChunkStatus() {
        return protobufChunkStatus;
    }

    public ProtobufStatusCode getProtobufStatusCode() {
        return protobufStatusCode;
    }

    public int getMessageType() {
        return messageType;
    }

    public int getRequestId() {
        return requestId;
    }

    public boolean isOK() {
        return this.status.equals(Status.ACK) &&
                this.protobufChunkStatus.equals(ProtobufChunkStatus.KEPT) &&
                this.protobufStatusCode.equals(ProtobufStatusCode.NO_ERROR);
    }

    @Override
    protected boolean generateOutgoing() {
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(GarminMessage.RESPONSE.getId());
        writer.writeShort(messageType);
        writer.writeByte(status.ordinal());
        writer.writeShort(requestId);
        writer.writeInt(dataOffset);
        writer.writeByte(protobufChunkStatus.ordinal());
        writer.writeByte(protobufStatusCode.code);
        return sendOutgoing;
    }

    protected Status getStatus() {
        return status;
    }

    public enum ProtobufChunkStatus { //based on the observations of the combination with the StatusCode
        KEPT,
        DISCARDED,
        ;

        @Nullable
        public static ProtobufChunkStatus fromCode(final int code) {
            for (final ProtobufChunkStatus status : ProtobufChunkStatus.values()) {
                if (status.ordinal() == code) {
                    return status;
                }
            }

            return null;
        }
    }

    public enum ProtobufStatusCode {
        NO_ERROR(0),
        UNKNOWN_REQUEST_ID(100),
        DUPLICATE_PACKET(101),
        MISSING_PACKET(102),
        EXCEEDED_TOTAL_PROTOBUF_LENGTH(103),
        PROTOBUF_PARSE_ERROR(200),
        ;

        private final int code;

        ProtobufStatusCode(final int code) {
            this.code = code;
        }

        @Nullable
        public static ProtobufStatusCode fromCode(final int code) {
            for (final ProtobufStatusCode protobufStatusCode : ProtobufStatusCode.values()) {
                if (protobufStatusCode.getCode() == code) {
                    return protobufStatusCode;
                }
            }
            return null;
        }

        public int getCode() {
            return code;
        }
    }


}
