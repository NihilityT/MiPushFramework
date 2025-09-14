package top.trumeet.mipushframework.main.subpage;

import static top.trumeet.mipushframework.main.subpage.EventListPageKt.toEventInfoForDisplay;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;

import top.trumeet.mipush.provider.entities.Event;

public class EventListPageTest {
    @Test
    public void checkEventInfoToDisplayInfo() {
        Event event = new Event();
        event.setId(123123L);
        event.setPkg("test");
        Context context = ApplicationProvider.getApplicationContext();
        toEventInfoForDisplay(event, context, new EventListPageUtils(context));
    }
}