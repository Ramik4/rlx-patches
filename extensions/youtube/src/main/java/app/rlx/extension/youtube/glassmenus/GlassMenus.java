/*
 * Copyright 2026 RLX Patches.
 * https://github.com/Ramik4/rlx-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.rlx.extension.youtube.glassmenus;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.ResourceType;
import app.morphe.extension.shared.ResourceUtils;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.shared.settings.preference.SeekBarPreference;
import app.morphe.extension.shared.theme.ThemeUtils;
import app.morphe.extension.shared.ui.Dim;

/**
 * Draws the menus of the app (the bottom sheets, such as the menu of a video, the player settings and the share
 * sheet, and the popup menus of large screens) as frosted glass: the screen behind the menu is blurred, and the menu
 * is a translucent tint of the app background with a light rim, so the blurred screen shows through it.
 * <p>
 * A menu is a window of its own, so the blur is the blur behind a window of Android 12 and later
 * ({@link WindowManager.LayoutParams#FLAG_BLUR_BEHIND}), which the system draws. The system turns it off when the
 * device does not support it or while the battery saver is on; the menu is then nearly opaque, so it stays readable.
 * <p>
 * The menu content keeps its own views. Only the background of the sheet, and opaque backgrounds that fill the
 * width of the sheet, are replaced, so the items keep their ripples and their selected state.
 */
@SuppressWarnings("unused")
public final class GlassMenus {

    /**
     * How opaque the menu is at least, in percent, while the system does not blur the screen behind it.
     */
    static final int MIN_OPACITY_WITHOUT_BLUR = 92;

    /**
     * Views that hold the content of a bottom sheet, the first one found is used.
     */
    private static final int[] SHEET_IDS = {
            ResourceUtils.getIdentifier(ResourceType.ID, "design_bottom_sheet"),
            ResourceUtils.getIdentifier(ResourceType.ID, "bottom_sheet"),
    };

    /**
     * Corner radius of a menu if the background of the app does not give one.
     */
    private static final float DEFAULT_CORNER_RADIUS = Dim.dp(12);

    /**
     * A background of a view in the menu is a fill of the menu, and not of an item,
     * if the view is at least this part of the width of the menu.
     */
    private static final float FILL_WIDTH_FRACTION = 0.9f;

    /**
     * Windows and popups that are already glass. A menu can be created again for the same window.
     */
    private static final Set<Object> styled = Collections.newSetFromMap(new WeakHashMap<>());

    private static boolean isEnabled() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && GlassMenusSettings.GLASS_MENUS.get();
    }

    private static int opacityPercent() {
        return SeekBarPreference.clampToRange(GlassMenusSettings.GLASS_MENUS_OPACITY);
    }

    private static int blurRadiusPx() {
        return Dim.dp(SeekBarPreference.clampToRange(GlassMenusSettings.GLASS_MENUS_BLUR));
    }

    private static float dimAmount() {
        return SeekBarPreference.clampToRange(GlassMenusSettings.GLASS_MENUS_DIM) / 100f;
    }

    /**
     * Injection point: the dialog of a bottom sheet, right after the app creates it and before it is shown.
     */
    public static void onBottomSheetDialog(@Nullable Dialog dialog) {
        if (dialog == null || !isEnabled()) return;

        try {
            Window window = dialog.getWindow();
            if (window == null || !styled.add(window)) return;

            new SheetGlass(window).attach();
        } catch (Exception ex) {
            Logger.printException(() -> "onBottomSheetDialog failure", ex);
        }
    }

    /**
     * Injection point: a popup menu, before it is shown. The app can set its content view after this call.
     */
    public static void onPopupWindow(@Nullable PopupWindow popup) {
        if (popup == null || !isEnabled()) return;

        Utils.runOnMainThreadDelayed(() -> {
            try {
                View content = popup.getContentView();
                if (content == null || !styled.add(popup)) return;

                whenAttached(content, () -> stylePopup(popup, content));
            } catch (Exception ex) {
                Logger.printException(() -> "onPopupWindow failure", ex);
            }
        }, 0);
    }

    private static void whenAttached(View view, Runnable action) {
        if (view.isAttachedToWindow()) {
            view.post(action);
            return;
        }
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(@NonNull View attached) {
                attached.removeOnAttachStateChangeListener(this);
                attached.post(action);
            }

            @Override
            public void onViewDetachedFromWindow(@NonNull View detached) {
            }
        });
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private static void stylePopup(PopupWindow popup, View content) {
        try {
            if (!popup.isShowing()) return;

            View root = content.getRootView();
            if (!(root.getLayoutParams() instanceof WindowManager.LayoutParams params)) return;

            WindowManager windowManager = (WindowManager) content.getContext().getSystemService(Context.WINDOW_SERVICE);
            final boolean blur = windowManager.isCrossWindowBlurEnabled();

            // The view that draws the background of the popup: the background view the popup adds around the
            // content if the popup has a background, otherwise the content itself.
            View backgroundView = content.getParent() instanceof View parent && parent.getBackground() != null
                    ? parent
                    : content;
            GlassDrawable glass = new GlassDrawable(cornerRadiusOf(backgroundView.getBackground()), true);
            glass.setOpacityPercent(blur ? opacityPercent() : Math.max(opacityPercent(), MIN_OPACITY_WITHOUT_BLUR));
            backgroundView.setBackground(glass);
            clearFillBackgrounds(backgroundView, backgroundView.getWidth(), null);

            params.flags |= WindowManager.LayoutParams.FLAG_BLUR_BEHIND | WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            params.setBlurBehindRadius(blurRadiusPx());
            params.dimAmount = dimAmount();
            windowManager.updateViewLayout(root, params);
        } catch (Exception ex) {
            Logger.printException(() -> "stylePopup failure", ex);
        }
    }

    /**
     * @return The corner radius of the background the app gave the menu, or the default radius.
     */
    private static float cornerRadiusOf(@Nullable Drawable background) {
        if (background != null) {
            Rect bounds = background.getBounds();
            if (!bounds.isEmpty()) {
                Outline outline = new Outline();
                background.getOutline(outline);
                final float radius = outline.getRadius();
                if (radius > 0) return radius;
            }
        }
        return DEFAULT_CORNER_RADIUS;
    }

    /**
     * Removes the opaque backgrounds that fill the width of the menu, which would hide the glass.
     *
     * @param sameKind Backgrounds of this class are removed too, such as the class of the original background
     *                 of the sheet, which the app can also use for the views inside it.
     */
    private static void clearFillBackgrounds(View view, int menuWidth,
                                             @Nullable Class<? extends Drawable> sameKind) {
        if (!(view instanceof ViewGroup group) || menuWidth <= 0) return;

        for (int i = 0, count = group.getChildCount(); i < count; i++) {
            View child = group.getChildAt(i);
            Drawable background = child.getBackground();
            if (background != null
                    && child.getWidth() >= menuWidth * FILL_WIDTH_FRACTION
                    && (isOpaqueFill(background) || background.getClass() == sameKind)) {
                child.setBackground(null);
            }
            clearFillBackgrounds(child, menuWidth, sameKind);
        }
    }

    private static boolean isOpaqueFill(Drawable background) {
        if (background instanceof ColorDrawable color) {
            return Color.alpha(color.getColor()) == 255;
        }
        if (background instanceof GradientDrawable gradient) {
            return gradient.getColor() != null && Color.alpha(gradient.getColor().getDefaultColor()) == 255;
        }
        return false;
    }

    /**
     * The glass of a bottom sheet, from when its window is shown until it is removed.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private static final class SheetGlass {
        private final Window window;
        private final View decor;
        private final float defaultDimAmount;
        private final Consumer<Boolean> blurListener = this::applyBlur;
        private final ViewTreeObserver.OnGlobalLayoutListener layoutListener = this::styleSheet;

        @Nullable
        private GlassDrawable glass;
        @Nullable
        private Class<? extends Drawable> originalBackgroundKind;
        private boolean blurEnabled;
        private boolean shown;

        SheetGlass(Window window) {
            this.window = window;
            this.decor = window.getDecorView();
            this.defaultDimAmount = window.getAttributes().dimAmount;
        }

        void attach() {
            decor.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(@NonNull View view) {
                    onShown();
                }

                @Override
                public void onViewDetachedFromWindow(@NonNull View view) {
                    onHidden();
                }
            });
            if (decor.isAttachedToWindow()) {
                onShown();
            }
        }

        private void onShown() {
            if (shown) return;
            shown = true;
            try {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                        | WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                // Called right away with the current state, and again when the system turns the blur on or off.
                window.getWindowManager().addCrossWindowBlurEnabledListener(blurListener);
                decor.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
                decor.post(this::styleSheet);
            } catch (Exception ex) {
                Logger.printException(() -> "SheetGlass onShown failure", ex);
            }
        }

        private void onHidden() {
            if (!shown) return;
            shown = false;
            try {
                window.getWindowManager().removeCrossWindowBlurEnabledListener(blurListener);
                decor.getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener);
            } catch (Exception ex) {
                Logger.printException(() -> "SheetGlass onHidden failure", ex);
            }
        }

        private void applyBlur(boolean enabled) {
            blurEnabled = enabled;
            WindowManager.LayoutParams params = window.getAttributes();
            params.setBlurBehindRadius(enabled ? blurRadiusPx() : 0);
            params.dimAmount = enabled ? dimAmount() : defaultDimAmount;
            window.setAttributes(params);
            updateOpacity();
        }

        private void updateOpacity() {
            if (glass != null) {
                glass.setOpacityPercent(blurEnabled
                        ? opacityPercent()
                        : Math.max(opacityPercent(), MIN_OPACITY_WITHOUT_BLUR));
            }
        }

        /**
         * Gives the sheet the glass background. Called again on each layout, because the app binds
         * the items of the menu, and can change the content, after the sheet is shown.
         */
        private void styleSheet() {
            View sheet = findSheet();
            if (sheet == null) return;

            if (glass == null || sheet.getBackground() != glass) {
                Drawable original = sheet.getBackground();
                if (original != null && !(original instanceof GlassDrawable)) {
                    originalBackgroundKind = original.getClass();
                }
                glass = new GlassDrawable(cornerRadiusOf(original), false);
                updateOpacity();
                sheet.setBackground(glass);
                sheet.setClipToOutline(true);
            }
            clearFillBackgrounds(sheet, sheet.getWidth(), originalBackgroundKind);
        }

        @Nullable
        private View findSheet() {
            for (int id : SHEET_IDS) {
                if (id == 0) continue;
                View sheet = decor.findViewById(id);
                if (sheet != null) return sheet;
            }
            return null;
        }
    }

    /**
     * A translucent tint of the app background with rounded corners and a light rim.
     */
    private static final class GlassDrawable extends Drawable {
        private static final float RIM_WIDTH = Dim.dp(1);

        private final float radius;
        private final boolean roundBottom;
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final RectF rect = new RectF();
        private int opacityPercent = 100;

        GlassDrawable(float radius, boolean roundBottom) {
            this.radius = radius;
            this.roundBottom = roundBottom;

            final boolean dark = Utils.isDarkModeEnabled();
            final int background = ThemeUtils.getAppBackgroundColor();
            // A sheet is raised above the page, so it is a little lighter than the page, as in the app.
            final int tint = blend(background, Color.WHITE, dark ? 0.08f : 0.6f);
            fill.setColor(tint);

            rim.setStyle(Paint.Style.STROKE);
            rim.setStrokeWidth(RIM_WIDTH);
            rim.setColor(dark ? Color.WHITE : Color.BLACK);
            rim.setAlpha(dark ? 0x30 : 0x18);
        }

        void setOpacityPercent(int percent) {
            opacityPercent = Math.max(0, Math.min(100, percent));
            fill.setAlpha(255 * opacityPercent / 100);
            invalidateSelf();
        }

        private static int blend(int color, int with, float amount) {
            final float keep = 1 - amount;
            return Color.rgb(
                    (int) (Color.red(color) * keep + Color.red(with) * amount),
                    (int) (Color.green(color) * keep + Color.green(with) * amount),
                    (int) (Color.blue(color) * keep + Color.blue(with) * amount));
        }

        @Override
        protected void onBoundsChange(@NonNull Rect bounds) {
            final float bottom = roundBottom ? radius : 0;
            rect.set(bounds);
            path.reset();
            path.addRoundRect(rect, new float[]{radius, radius, radius, radius, bottom, bottom, bottom, bottom},
                    Path.Direction.CW);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            canvas.drawPath(path, fill);
            // The rim is drawn inside the menu, so it is not cut by the outline.
            final float inset = RIM_WIDTH / 2;
            final float rimRadius = Math.max(0, radius - inset);
            rect.inset(inset, inset);
            canvas.drawRoundRect(rect, rimRadius, rimRadius, rim);
            rect.inset(-inset, -inset);
        }

        @Override
        public void getOutline(@NonNull Outline outline) {
            Rect bounds = getBounds();
            // A sheet has round top corners only: the outline reaches below the bottom of the sheet.
            outline.setRoundRect(bounds.left, bounds.top, bounds.right,
                    roundBottom ? bounds.bottom : bounds.bottom + (int) Math.ceil(radius), radius);
            outline.setAlpha(opacityPercent / 100f);
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    private GlassMenus() {
    }
}
