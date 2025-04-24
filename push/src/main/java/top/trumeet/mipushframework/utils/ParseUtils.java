package top.trumeet.mipushframework.utils;

import android.content.Context;

import com.xiaomi.xmsf.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by Trumeet on 2017/7/18.
 *
 * @author Trumeet
 */

public class ParseUtils {
    public static Date parseDate(String dateString) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"
                , Locale.getDefault());
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        try {
            return format.parse(dateString);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static String getFriendlyDateString(Date fromServer, Date current,
                                               Context context) {

        Calendar calendarCurrent = Calendar.getInstance();
        Calendar calendarServer = Calendar.getInstance();
        calendarServer.setTime(fromServer);
        calendarCurrent.setTime(current);

        long time1 = calendarCurrent.getTimeInMillis();
        long time2 = calendarServer.getTimeInMillis();

        // Calculate difference in milliseconds
        long diff = time1 - time2;
        long sec = diff / 1000;
        long min = diff / (60 * 1000);
        long hour = diff / (60 * 60 * 1000);
        long day = diff / (24 * 60 * 60 * 1000);


        if (day < 1) {
            if (hour < 1) {
                if (min < 1) {
                    return context.getString(R.string.date_format_just,
                            context.getString(R.string.date_just));
                } else {
                    return context.getString(R.string.date_format_normal,
                            String.valueOf(min),
                            context.getResources()
                                    .getQuantityString(R.plurals.date_minutes,
                                            (int) min));
                }
            } else if (hour < 24) {
                return context.getString(R.string.date_format_normal,
                        String.valueOf(hour),
                        context.getResources()
                                .getQuantityString(R.plurals.date_hours,
                                        (int) hour));
            } else {
                return parseDate(calendarServer.getTime());

            }
        } else if (day < 30) {
            return context.getString(R.string.date_format_normal,
                    String.valueOf(day),
                    context.getResources()
                            .getQuantityString(R.plurals.date_days,
                                    (int) day));
        } else {
            return context.getString(R.string.date_format_long,
                    parseDate(calendarServer.getTime()));
        }
    }

    public static String parseDate(Date date) {
        DateFormat formatter = DateFormat.getDateTimeInstance();
        return formatter.format(date);

    }
}
