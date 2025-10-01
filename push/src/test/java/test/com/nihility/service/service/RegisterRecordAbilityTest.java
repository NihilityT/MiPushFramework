package test.com.nihility.service.service;

import static org.mockito.Mockito.verify;

import android.content.Intent;

import com.nihility.service.RegisterRecordAbility;
import com.nihility.service.RegisterRecorder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RegisterRecordAbilityTest {
    @Mock
    RegisterRecorder registerRecorder;
    @InjectMocks
    RegisterRecordAbility listener;

    @Test
    public void recordRegisterRequestAtStart() {
        Intent intent = new Intent();
        listener.start(intent);

        verify(registerRecorder).recordRegisterRequest(intent);
    }

}
