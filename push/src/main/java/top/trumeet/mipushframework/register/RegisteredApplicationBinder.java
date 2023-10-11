package top.trumeet.mipushframework.register;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.xiaomi.xmsf.R;

import top.trumeet.common.utils.Utils;
import top.trumeet.mipush.provider.register.RegisteredApplication;
import top.trumeet.mipushframework.permissions.ManagePermissionsActivity;
import top.trumeet.mipushframework.utils.BaseAppsBinder;
import top.trumeet.mipushframework.utils.ParseUtils;

/**
 * Created by Trumeet on 2017/8/26.
 * @author Trumeet
 */

public class RegisteredApplicationBinder extends BaseAppsBinder<RegisteredApplication> {
    static int ErrorColor = Color.parseColor("#FFF41804");
    static int GreenColor = Color.parseColor("#4caf50");
    static int YellowColor = Color.parseColor("#ff9800");

    RegisteredApplicationBinder() {
        super();
    }

    @NonNull
    @Override
    protected ViewHolder onCreateViewHolder(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
        return new ViewHolder(inflater.inflate(R.layout.app_item, parent, false));
    }

    @Override
    protected void onBindViewHolder(@NonNull final ViewHolder holder
            , @NonNull final RegisteredApplication item) {
        Context context = holder.itemView.getContext();
        fillData(item.getPackageName(), true,
                holder);
        //todo res color
        holder.summary.setText(null);
        if (item.lastReceiveTime.getTime() != 0) {
            holder.summary.setText(String.format("%s%s",
                    context.getString(R.string.last_receive),
                    ParseUtils.getFriendlyDateString(item.lastReceiveTime, Utils.getUTC(), context)));
        }
        holder.title.setTextColor(Utils.getColorAttr(context, android.R.attr.textColorPrimary));
        holder.status.setTextColor(Utils.getColorAttr(context, android.R.attr.textColorSecondary));
        switch (item.getRegisteredType()) {
            case 1: {
                holder.title.setTextColor(GreenColor);
                holder.status.setTextColor(GreenColor);
                holder.status.setText(R.string.app_registered);
                break;
            }
            case 2: {
                holder.title.setTextColor(YellowColor);
                holder.status.setTextColor(YellowColor);
                holder.status.setText(R.string.app_registered_error);
                break;
            }
            case 0: {
                holder.status.setText(R.string.status_app_not_registered);
                break;
            }
        }
        if (!item.existServices) {
            holder.status.setTextColor(ErrorColor);
            holder.status.setText(context.getText(R.string.mipush_services_not_found) + " - " + holder.status.getText());
        }
        holder.itemView.setOnClickListener(view -> context
                .startActivity(new Intent(context,
                        ManagePermissionsActivity.class)
                .putExtra(ManagePermissionsActivity.EXTRA_PACKAGE_NAME,
                        item.getPackageName())
                .putExtra(ManagePermissionsActivity.EXTRA_IGNORE_NOT_REGISTERED, true)));
    }
}
