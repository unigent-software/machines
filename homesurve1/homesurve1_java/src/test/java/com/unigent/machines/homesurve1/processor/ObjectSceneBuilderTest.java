package com.unigent.machines.homesurve1.processor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
public class ObjectSceneBuilderTest {

    private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    @Test
    public void testAdjustedBox() {
        Assert.assertArrayEquals(
                new int [] {15, 11, 153, 115},
                ObjectSceneBuilder.adjustedBox(Arrays.asList(10,10,100,100))
        );
    }

}
