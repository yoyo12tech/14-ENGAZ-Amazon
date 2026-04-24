package com.team14.amazon.user.security;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JwtConfigurationManager Singleton pattern.
 */
public class JwtConfigurationManagerTest {

    @Test
    public void testSingletonProperties() throws Exception {
        Class<JwtConfigurationManager> clazz = JwtConfigurationManager.class;

        // a) Assert exactly one constructor and it is private
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        assertEquals(1, constructors.length, "Should have exactly one constructor");
        assertTrue(Modifier.isPrivate(constructors[0].getModifiers()), "Constructor should be private");

        // b) Assert getInstance() is public static and returns JwtConfigurationManager
        var getInstanceMethod = clazz.getDeclaredMethod("getInstance");
        assertTrue(Modifier.isPublic(getInstanceMethod.getModifiers()), "getInstance should be public");
        assertTrue(Modifier.isStatic(getInstanceMethod.getModifiers()), "getInstance should be static");
        assertEquals(clazz, getInstanceMethod.getReturnType(), "getInstance should return JwtConfigurationManager");

        // c) Assert ref1 == ref2
        JwtConfigurationManager ref1 = JwtConfigurationManager.getInstance();
        JwtConfigurationManager ref2 = JwtConfigurationManager.getInstance();
        assertSame(ref1, ref2, "Should return the same instance");
    }

    @Test
    public void testThreadSafety() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        Set<JwtConfigurationManager> instances = Collections.synchronizedSet(new HashSet<>());

        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                instances.add(JwtConfigurationManager.getInstance());
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(1, instances.size(), "All threads should get the same instance");
    }

    @Test
    public void testNoSpringAnnotations() {
        Class<JwtConfigurationManager> clazz = JwtConfigurationManager.class;
        assertFalse(clazz.isAnnotationPresent(Component.class), "Should not have @Component");
        assertFalse(clazz.isAnnotationPresent(Service.class), "Should not have @Service");
        assertFalse(clazz.isAnnotationPresent(Configuration.class), "Should not have @Configuration");
    }
}
