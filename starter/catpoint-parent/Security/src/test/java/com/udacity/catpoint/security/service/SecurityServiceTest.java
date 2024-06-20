package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    public SecurityService securityService;
    @Mock
    public ImageService imageService;
    @Mock
    public SecurityRepository securityRepository;
    Sensor sensorDoor;
    Sensor sensorWindow;
    Sensor sensorMotion;
    @Mock
    BufferedImage bufferedImage;

    @BeforeEach
    void setUp() {
        securityService = new SecurityService(securityRepository, imageService);
        sensorDoor = new Sensor("door", SensorType.DOOR);
        sensorWindow = new Sensor("window", SensorType.WINDOW);
        sensorMotion = new Sensor("motion", SensorType.MOTION);
    }

    // Test case 1
    @Test
    public void whenAlarmAndSensorBecomesActivated_setSystemIntoPendingAlarmStatus() {
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensorWindow, true);
        verify(securityRepository, atLeastOnce()).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    // Test case 2
    @Test
    public void whenAlarmAndSensorBecomesActivated_systemIntoPendingAlarmStatus__setAlarmStatusToAlarm() {
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
        when(securityService.getAlarmStatus()).then(invocation -> AlarmStatus.PENDING_ALARM);
        sensorMotion.setActive(false);
        sensorDoor.setActive(false);
        sensorWindow.setActive(false);
        securityService.changeSensorActivationStatus(sensorWindow, true);
        verify(securityRepository, atLeastOnce()).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test case 3
    @Test
    public void whenPendingAlarmAndSensorInactive_setStateToNoAlarm() {
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensorWindow.setActive(true);
        securityService.changeSensorActivationStatus(sensorWindow, false);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensorDoor.setActive(true);
        securityService.changeSensorActivationStatus(sensorDoor, false);
        verify(securityRepository, times(2)).setAlarmStatus(AlarmStatus.NO_ALARM);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensorMotion.setActive(true);
        securityService.changeSensorActivationStatus(sensorMotion, false);
        verify(securityRepository, times(3)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Test case 4
    @Test
    public void whenAlarmActive_changeSensorState() {
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        sensorWindow.setActive(false);
        securityService.changeSensorActivationStatus(sensorWindow, true);
        verify(securityRepository, never()).setAlarmStatus(any());
    }

    // Test case 5
    @Test
    public void whenSensorActivated_alreadyActive_changeAlarmState() {
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensorDoor.setActive(true);
        securityService.changeSensorActivationStatus(sensorWindow, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test case 6
    @Test
    public void whenSensorDeactivatedAndAlreadyInactive_noChangeAlarmState() {
        securityService.changeSensorActivationStatus(sensorMotion, false);
        verify(securityRepository, never()).setAlarmStatus(any());
    }

    // Test case 7
    @Test
    public void whenCameraImageContainsCatAndSystemArmedHome_setSystemAlarmStatus() {
        // There is no other way to code the logic of this request
        when(imageService.imageContainsCat(bufferedImage, 50.0f)).then(invocation -> true);
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(bufferedImage);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test case 8
    @Test
    public void whenImageNotContainCat_changeStatusNoAlarm_sensorsNotActive() {

    }

    // Test case 9
    @Test
    public void whenSystemDisarmed_setStatusNoAlarm() {

    }

    // Test case 10
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_AWAY", "ARMED_HOME"})
    public void whenSystemArmed_resetAllSensorsInactive(ArmingStatus status) {

    }

    // Test case 11
    @Test
    public void whenSystemArmedHome_whileCameraShowsCat_setAlarmStatusToAlarm() {

    }

    // Test case 12
    @Test
    public void whenPendingAlarmAndSensorInactive_setStateToPendingAlarm() {

    }

    // Test case 13
    @Test
    public void whenSystemDisarmed_setStatusAlarm() {

    }
}