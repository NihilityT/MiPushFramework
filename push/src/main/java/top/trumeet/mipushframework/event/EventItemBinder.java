package top.trumeet.mipushframework.event;

import static com.xiaomi.push.service.MIPushEventProcessor.buildContainer;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.xiaomi.xmpush.thrift.XmPushActionContainer;
import com.xiaomi.xmsf.R;
import com.xiaomi.xmsf.push.utils.Utils;

import top.trumeet.mipush.provider.event.Event;
import top.trumeet.mipush.provider.event.EventType;
import top.trumeet.mipush.provider.event.type.TypeFactory;
import top.trumeet.mipushframework.utils.BaseAppsBinder;

/**
 * Created by Trumeet on 2017/8/26.
 *
 * @author Trumeet
 * @see Event
 * @see EventFragment
 */

public class EventItemBinder extends BaseAppsBinder<Event> {
    private EventListPageUtils utils = null;

    EventItemBinder() {
        super();
    }

    @NonNull
    @Override
    protected ViewHolder onCreateViewHolder(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
        return new ViewHolder(inflater.inflate(R.layout.event_item, parent, false));
    }

    @Override
    protected void onBindViewHolder(final @NonNull ViewHolder holder, final @NonNull Event item) {
        Context context = holder.itemView.getContext();
        if (utils == null) {
            utils = new EventListPageUtils(context);
        }

        fillData(item.getPkg(), false, holder);
        final EventType type = TypeFactory.create(item, item.getPkg());
        holder.title.setText(type.getTitle(context));
        holder.summary.setText(type.getSummary(context));

        String status = utils.getStatusDescription(item);
        String receiveDate = utils.getReceiveDate(item);

        holder.text2.setText(receiveDate);
        holder.status.setText(status);
        fillWithEventContent(holder, item);

        holder.itemView.setOnClickListener(view -> {
            Dialog dialog = createInfoDialog(item, context); // "Developer info" dialog for event messages
            if (dialog != null) {
                dialog.show();
            } else {
                EventListPageUtils.startManagePermissions(context, type.getPkg());
            }
        });
    }

    private void fillWithEventContent(@NonNull ViewHolder holder, @NonNull Event item) {
        new ConfigurationWorkerTask(holder, Utils.getCustomContainer(item)).execute();
    }

    @Nullable
    private Dialog createInfoDialog(final Event event, final Context context) {
        XmPushActionContainer container = event.getPayload() == null ?
                null : buildContainer(event.getPayload());
        final CharSequence info = EventListPageUtils.containerToJson(container, event.getRegSec());
        if (info == null) {
            return null;
        }

        TextView showText = new TextView(context);
        showText.setText(info);
        showText.setTextSize(14);
        showText.setTextIsSelectable(true);
        showText.setTypeface(Typeface.MONOSPACE);

        final ScrollView scrollView = new ScrollView(context);
        scrollView.addView(showText);

        AlertDialog.Builder build = new AlertDialog.Builder(context)
                .setView(scrollView)
                .setTitle("Developer Info")
                .setNeutralButton(android.R.string.copy, (dialogInterface, i) -> {
                    EventListPageUtils.copyToClipboard(context, info);
                })
                .setNegativeButton(R.string.action_edit_permission, (dialogInterface, i) ->
                        EventListPageUtils.startManagePermissions(context, event.getPkg()));

        AlertDialog dialog;
        if (event.getPayload() != null) {
            XmPushActionContainer containerWithRegSec = Utils.getCustomContainer(event);

            build.setPositiveButton(R.string.action_notify, (dialogInterface, i) ->
                    EventListPageUtils.mockMessage(containerWithRegSec));
            build.setNeutralButton(R.string.action_configurate, null);

            dialog = build.create();
            dialog.setOnShowListener(dialogInterface -> {

                Button button = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                button.setOnClickListener(view -> {
                    showText.setText(EventListPageUtils.getContent(event, containerWithRegSec));
                });
            });
        } else {
            dialog = build.create();
        }
        return dialog;
    }

    private class ConfigurationWorkerTask extends AsyncTask<String, Void, String> {
        private final ViewHolder viewHolder;
        private final XmPushActionContainer container;
        private boolean stop = false;

        ConfigurationWorkerTask(ViewHolder viewHolder, XmPushActionContainer container) {
            this.viewHolder = viewHolder;
            this.container = container;
            if (container.metaInfo.passThrough == 0) {
                stop = true;
            }
        }

        @Override
        protected String doInBackground(String... params) {
            if (stop) return null;

            return utils.getDecoratedStatus(container);
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(String status) {
            if (stop) return;

            if (status != null) {
                viewHolder.status.setText(status);
            }
            viewHolder.summary.setText(
                    EventListPageUtils.getDecoratedSummary(
                            viewHolder.summary.getText().toString(), container));
        }
    }
}
