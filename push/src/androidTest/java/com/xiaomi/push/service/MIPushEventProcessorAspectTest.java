package com.xiaomi.push.service;

import static com.xiaomi.push.service.MIPushEventProcessor.buildIntent;
import static com.xiaomi.push.service.PullAllApplicationDataFromServerJob.getPullAction;
import static org.junit.Assert.assertFalse;

import android.content.Intent;

import com.xiaomi.push.service.clientReport.ReportConstants;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;

import org.junit.Test;

public class MIPushEventProcessorAspectTest {

    @Test
    public void avoidTrackingByMessageIdAndMessageType() {
        XmPushActionContainer container = XmPushActionOperator.packToContainer(getPullAction("appId"), "");
        Intent intent = buildIntent(XmPushActionOperator.packToBytes(container), 0);

        intent.putExtra("messageId", "123");
        intent.putExtra(ReportConstants.EVENT_MESSAGE_TYPE, ReportConstants.REGISTER_TYPE);

        assertFalse(intent.hasExtra("messageId"));
        assertFalse(intent.hasExtra(ReportConstants.EVENT_MESSAGE_TYPE));
    }
}