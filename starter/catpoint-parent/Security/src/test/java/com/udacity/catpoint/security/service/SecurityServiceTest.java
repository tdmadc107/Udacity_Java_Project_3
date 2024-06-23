package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.application.DisplayPanel;
import com.udacity.catpoint.security.data.*;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
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
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
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
        // There is no other way to code the logic of this requirement
        when(imageService.imageContainsCat(bufferedImage, 50.0f)).then(invocation -> true);
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(bufferedImage);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test case 8
    @Test
    public void whenImageNotContainCat_changeStatusNoAlarm_sensorsNotActive() {
        sensorWindow.setActive(false);
        sensorDoor.setActive(false);
        sensorMotion.setActive(false);
        when(imageService.imageContainsCat(bufferedImage, 50.0f)).then(invocation -> false);
        securityService.processImage(bufferedImage);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Test case 9
    @Test
    public void whenSystemDisarmed_setStatusNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Test case 10
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_AWAY", "ARMED_HOME"})
    public void whenSystemArmed_resetAllSensorsInactive(ArmingStatus status) {
        Set<Sensor> sensorSet = new HashSet<>();
        sensorSet.add(sensorWindow);
        sensorSet.add(sensorDoor);
        sensorSet.add(sensorMotion);
        sensorSet.forEach(sensor -> sensor.setActive(true));
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        when(securityService.getSensors()).thenReturn(sensorSet);
        securityService.setArmingStatus(status);
        securityService.getSensors().forEach(sensor -> assertFalse(sensor.getActive()));
    }

    // Test case 11
    @Test
    public void whenSystemArmedHome_whileCameraShowsCat_setAlarmStatusToAlarm() {
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
        when(imageService.imageContainsCat(bufferedImage, 50.0f)).then(invocation -> true);
        securityService.processImage(bufferedImage);
        Set<Sensor> sensorSet = new HashSet<>();
        when(securityService.getSensors()).thenReturn(sensorSet);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test case 12
    @Test
    public void whenPendingAlarmAndSensorActive_setStateToPendingAlarm() {
        when(securityService.getAlarmStatus()).then(invocation -> AlarmStatus.NO_ALARM);
        sensorWindow.setActive(true);
        securityService.changeSensorActivationStatus(sensorWindow, true);
        verify(securityRepository, atLeastOnce()).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    // Test case 13
    @Test
    public void whenSensorActiveAndStatusAlarm_AfterChangeSensorInactive_setStatusPending() {
        when(securityService.getAlarmStatus()).then(invocation -> AlarmStatus.ALARM);
        sensorWindow.setActive(true);
        securityService.changeSensorActivationStatus(sensorWindow, false);
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    // Test case 14
    @Test
    public void addTestCaseToAllCoverage() {
        when(securityService.getAlarmStatus()).then(invocation -> AlarmStatus.ALARM);
        securityService.addStatusListener(new DisplayPanel(securityService));
        securityService.removeStatusListener(new DisplayPanel(securityService));
        securityService.addSensor(sensorMotion);
        securityService.removeSensor(sensorMotion);
    }
}