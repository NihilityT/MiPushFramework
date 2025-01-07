package top.trumeet.mipushframework.wizard;

public interface PermissionOperator {
    boolean isPermissionGranted();

    void requestPermissionSilently();

    void requestPermission();
}
