package top.trumeet.mipushframework.wizard.permission;

public interface PermissionOperator {
    boolean isPermissionGranted();

    void requestPermissionSilently();

    void requestPermission();
}
