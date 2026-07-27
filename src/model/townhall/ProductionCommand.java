package model.townhall;

import model.ResourceAmount;

//فقط قرارداد مشترک دستورها
public interface ProductionCommand {

    ResourceAmount getCost();

    //زمان کلی دستور
    int getTotalTurns();

    int getRemainingTurns();

    //یک نوبت از زمان باقی‌مانده کم می‌کنه
    void advanceOneTurn();

    boolean isCompleted();

    void complete();

    void cancel();

    void onStarted();
}

