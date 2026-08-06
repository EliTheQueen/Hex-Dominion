package model.disaster;

import model.HexCoordinate;

import java.util.UUID;

//چون Disaster عمومی به‌تنهایی قابل اجرا نیست.
//باید subclass مشخص داشته باشیم
//pas abstract
public abstract class DisasterEvent {

    private final String id;
    private final DisasterType type;
    private final HexCoordinate origin;

    private DisasterStatus status;

    protected DisasterEvent(DisasterType type, HexCoordinate origin) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }

        if (origin == null) {
            throw new IllegalArgumentException("origin must not be null");
        }

        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.origin = origin;
        this.status = DisasterStatus.CREATED;
    }

    public final void start() {
        if (status != DisasterStatus.CREATED) {
            throw new IllegalStateException("only created disasters can start");
        }

        status = DisasterStatus.ACTIVE;
        onStarted();
    }

    public final void complete() {
        if (status != DisasterStatus.ACTIVE) {
            throw new IllegalStateException("only active disasters can complete");
        }

        onCompleted();
        status = DisasterStatus.COMPLETED;
    }

    public final void cancel() {
        if (status == DisasterStatus.COMPLETED || status == DisasterStatus.CANCELLED) {
            return;
        }

        status = DisasterStatus.CANCELLED;
        onCancelled();
    }

    protected void onStarted() {
    }

    protected void onCompleted() {
    }

    protected void onCancelled() {
    }

    public String getId() {
        return id;
    }

    public DisasterType getType() {
        return type;
    }

    public HexCoordinate getOrigin() {
        return origin;
    }

    public DisasterStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return status == DisasterStatus.ACTIVE;
    }
}