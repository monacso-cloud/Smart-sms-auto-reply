package com.smartreply.beta;

import org.junit.Test;

import java.time.LocalTime;

import static org.junit.Assert.*;

public class AutomationSchedulePolicyTest {

    @Test
    public void daytimeWindowIncludesOnlySelectedHours() {
        LocalTime start = LocalTime.of(8, 0);
        LocalTime end = LocalTime.of(20, 0);

        assertTrue(AutomationSchedulePolicy.isInsideWindow(LocalTime.of(8, 0), start, end));
        assertTrue(AutomationSchedulePolicy.isInsideWindow(LocalTime.of(12, 0), start, end));
        assertFalse(AutomationSchedulePolicy.isInsideWindow(LocalTime.of(20, 0), start, end));
        assertFalse(AutomationSchedulePolicy.isInsideWindow(LocalTime.of(6, 0), start, end));
    }

    @Test
    public void overnightWindowWorksFrom6pmTo6am() {
        LocalTime start = LocalTime.of(18, 0);
        LocalTime end = LocalTime.of(6, 0);

        assertTrue(AutomationSchedulePolicy.isInsideWindow(LocalTime.of(18, 0), start, end));
        assertTrue(AutomationSchedulePolicy.isInsideWindow(LocalTime.of(23, 30), start, end));
        assertTrue(AutomationSchedulePolicy.isInsideWindow(LocalTime.of(5, 59), start, end));
        assertFalse(AutomationSchedulePolicy.isInsideWindow(LocalTime.of(6, 0), start, end));
        assertFalse(AutomationSchedulePolicy.isInsideWindow(LocalTime.of(12, 0), start, end));
    }

    @Test
    public void parsesCommonBusinessTimeFormats() {
        assertEquals(LocalTime.of(18, 0),
                AutomationSchedulePolicy.parseTime("6:00 PM", LocalTime.MIDNIGHT));
        assertEquals(LocalTime.of(6, 0),
                AutomationSchedulePolicy.parseTime("6 AM", LocalTime.MIDNIGHT));
        assertEquals(LocalTime.of(20, 30),
                AutomationSchedulePolicy.parseTime("20:30", LocalTime.MIDNIGHT));
    }
}
