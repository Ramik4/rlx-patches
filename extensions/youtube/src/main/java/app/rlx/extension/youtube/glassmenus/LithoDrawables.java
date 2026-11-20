/*
 * Copyright 2026 RLX Patches.
 * https://github.com/Ramik4/rlx-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.rlx.extension.youtube.glassmenus;

import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.morphe.extension.shared.Logger;

/**
 * Finds the drawables a Litho host view draws itself, such as the background of a component that is not a view.
 * <p>
 * A Litho host ({@code com.facebook.litho.ComponentHost}) keeps its mounted content in sparse arrays of mount items,
 * and draws the drawables among them in {@code dispatchDraw}. These are not the background of a view, so they are
 * found in the fields of the host: a field holds a sparse array, which holds an array of mount items, which hold
 * the content. The names of these classes and fields are obfuscated, so they are found by their types only.
 */
final class LithoDrawables {

    private static final String COMPONENT_HOST = "com.facebook.litho.ComponentHost";

    /**
     * How deep the fields of the host are searched: host field, sparse array, its array, mount item, content.
     */
    private static final int MAX_DEPTH = 4;

    @Nullable
    private static Class<?> componentHostClass;
    private static boolean componentHostLoaded;

    private static final Map<Class<?>, List<Field>> fieldsByClass = new IdentityHashMap<>();

    @Nullable
    private static Class<?> componentHostClass(View view) {
        if (!componentHostLoaded) {
            componentHostLoaded = true;
            try {
                componentHostClass = Class.forName(COMPONENT_HOST, false, view.getClass().getClassLoader());
            } catch (Throwable ex) {
                Logger.printDebug(() -> "Litho host not found: " + ex);
            }
        }
        return componentHostClass;
    }

    static boolean isHost(View view) {
        Class<?> host = componentHostClass(view);
        return host != null && host.isInstance(view);
    }

    /**
     * @return The drawables the host draws itself.
     */
    static List<Drawable> drawablesOf(View host) {
        List<Drawable> drawables = new ArrayList<>();
        Class<?> hostClass = componentHostClass(host);
        if (hostClass == null || !hostClass.isInstance(host)) return drawables;

        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        try {
            for (Field field : fieldsOf(hostClass)) {
                collect(field.get(host), MAX_DEPTH, drawables, visited);
            }
        } catch (Throwable ex) {
            Logger.printDebug(() -> "Litho drawables not found: " + ex);
        }
        // Only those the host draws.
        drawables.removeIf(drawable -> drawable.getCallback() != host);
        return drawables;
    }

    private static void collect(@Nullable Object value, int depth, List<Drawable> out, Set<Object> visited)
            throws IllegalAccessException {
        if (value == null || depth < 0 || !visited.add(value)) return;

        if (value instanceof Drawable drawable) {
            out.add(drawable);
            return;
        }
        if (value instanceof View || value instanceof CharSequence || value instanceof Class) return;

        if (value instanceof Object[] array) {
            for (Object element : array) {
                collect(element, depth - 1, out, visited);
            }
            return;
        }
        if (value instanceof Collection<?> collection) {
            for (Object element : collection) {
                collect(element, depth - 1, out, visited);
            }
            return;
        }
        if (value instanceof android.util.SparseArray<?> sparseArray) {
            for (int i = 0, size = sparseArray.size(); i < size; i++) {
                collect(sparseArray.valueAt(i), depth - 1, out, visited);
            }
            return;
        }
        if (isPlatformClass(value.getClass())) return;

        for (Field field : fieldsOf(value.getClass())) {
            collect(field.get(value), depth - 1, out, visited);
        }
    }

    private static boolean isPlatformClass(Class<?> type) {
        final String name = type.getName();
        return name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("android.")
                || name.startsWith("kotlin.") || name.startsWith("dalvik.") || type.isPrimitive();
    }

    /**
     * @return The object fields declared by the class and its superclasses of the app.
     */
    private static List<Field> fieldsOf(Class<?> type) {
        List<Field> fields = fieldsByClass.get(type);
        if (fields != null) return fields;

        fields = new ArrayList<>();
        for (Class<?> current = type; current != null && !isPlatformClass(current);
             current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
                try {
                    field.setAccessible(true);
                    fields.add(field);
                } catch (Throwable ignored) {
                    // Not accessible.
                }
            }
        }
        fieldsByClass.put(type, fields);
        return fields;
    }

    private LithoDrawables() {
    }
}
