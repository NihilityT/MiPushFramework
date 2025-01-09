package top.trumeet.mipushframework.wizard;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import top.trumeet.mipushframework.wizard.permission.PermissionInfo;
import top.trumeet.mipushframework.wizard.permission.PermissionOperator;

public abstract class DisplayOnlyPhonyPermissionInfo implements PermissionInfo, PermissionOperator {
    protected final Context context;

    public DisplayOnlyPhonyPermissionInfo(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public PermissionOperator getPermissionOperator() {
        return this;
    }

    @NonNull
    @Override
    public Intent nextPageIntent() {
        return null;
    }

    @Override
    public boolean isPermissionGranted() {
        return true;
    }

    @Override
    public void requestPermissionSilently() {

    }

    @Override
    public void requestPermission() {

    }
}
