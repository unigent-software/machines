package com.unigent.machines.homesurve1.processor.controller;

import com.unigent.machines.homesurve1.processor.dynamics.MapAction;
import com.unigent.machines.homesurve1.processor.dynamics.MapDynamicsMemory;
import com.unigent.machines.homesurve1.processor.dynamics.MapState;
import com.unigent.machines.homesurve1.processor.dynamics.MapStateTransition;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
public class UserTaskExecutorTest {

    @Test
    public void testFindPath() {
        MapState.MapObject mo1_1 = new MapState.MapObject("obj-1", 45, 100);
        MapState.MapObject mo1_2 = new MapState.MapObject("obj-1", 45, 150);
        MapState.MapObject mo2_1 = new MapState.MapObject("obj-2", 0, 400);
        MapState.MapObject mo3_1 = new MapState.MapObject("obj-3", 270, 340);
        MapState.MapObject mo3_2 = new MapState.MapObject("obj-3", 200, 340);
        MapState.MapObject mo3_3 = new MapState.MapObject("obj-3", 180, 340);
        MapState.MapObject mo4_1 = new MapState.MapObject("obj-4", 90, 120);

        MapState s1 = new MapState(mo1_1);
        MapState s2 = new MapState(mo1_2);
        MapState s3 = new MapState(mo2_1);
        MapState s4 = new MapState(mo3_1);
        MapState s5 = new MapState(mo3_2);
        MapState s6 = new MapState(mo3_3);
        MapState s7 = new MapState(mo4_1);

        MapStateTransition t_1_2 = new MapStateTransition(s1, new MapAction(1, 2), s2);
        MapStateTransition t_1_3 = new MapStateTransition(s1, new MapAction(1, 3), s3);
        MapStateTransition t_2_4 = new MapStateTransition(s2, new MapAction(2, 4), s4);
        MapStateTransition t_2_5 = new MapStateTransition(s2, new MapAction(2, 5), s5);
        MapStateTransition t_3_6 = new MapStateTransition(s3, new MapAction(3, 6), s6);
        MapStateTransition t_3_7 = new MapStateTransition(s3, new MapAction(3, 7), s7);

        MapDynamicsMemory mapDynamicsMemory = Mockito.mock(MapDynamicsMemory.class);
        when(mapDynamicsMemory.findByTransitionsFromState(s1)).thenReturn(List.of(t_1_2, t_1_3));
        when(mapDynamicsMemory.findByTransitionsFromState(s2)).thenReturn(List.of(t_2_4, t_2_5));
        when(mapDynamicsMemory.findByTransitionsFromState(s3)).thenReturn(List.of(t_3_6, t_3_7));
        when(mapDynamicsMemory.findByTransitionsFromState(s4)).thenReturn(emptyList());
        when(mapDynamicsMemory.findByTransitionsFromState(s5)).thenReturn(emptyList());
        when(mapDynamicsMemory.findByTransitionsFromState(s6)).thenReturn(emptyList());
        when(mapDynamicsMemory.findByTransitionsFromState(s7)).thenReturn(emptyList());

        assertNull(UserTaskExecutor.findPath(s2, s7, mapDynamicsMemory));
        assertEquals(List.of(new MapAction(1, 3), new MapAction(3, 7)), UserTaskExecutor.findPath(s1, s7, mapDynamicsMemory));
    }
}




