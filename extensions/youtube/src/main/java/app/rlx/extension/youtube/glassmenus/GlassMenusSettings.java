/*
 * Copyright 2026 RLX Patches.
 * https://github.com/Ramik4/rlx-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.rlx.extension.youtube.glassmenus;

import static java.lang.Boolean.FALSE;
import static app.morphe.extension.shared.settings.Setting.parent;
import static app.morphe.extension.shared.settings.Setting.parentsAll;

import android.os.Build;

import app.morphe.extension.shared.settings.BooleanSetting;
import app.morphe.extension.shared.settings.IntegerSetting;
import app.morphe.extension.shared.settings.Setting;
import app.morphe.extension.shared.settings.preference.SeekBarPreference;
import app.morphe.extension.shared.settings.preference.SeekBarPreference.SeekBarConfig;

/**
 * Settings of the glass menus.
 */
public final class GlassMenusSettings {

    /**
     * The blur behind a window needs Android 12.
     */
    public static final class BlurAvailability implements Setting.Availability {
        @Override
        public boolean isAvailable() {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
        }
    }

    public static final BooleanSetting GLASS_MENUS =
            new BooleanSetting("morphe_glass_menus", FALSE, false, new BlurAvailability());

    /**
     * How opaque the tint of the menu is, in percent. Used while the blur is shown;
     * without the blur, the menu is at least {@link GlassMenus#MIN_OPACITY_WITHOUT_BLUR} opaque.
     */
    public static final IntegerSetting GLASS_MENUS_OPACITY =
            new IntegerSetting("morphe_glass_menus_opacity", 55, false,
                    parentsAll(parent(GLASS_MENUS), new BlurAvailability()));

    /**
     * How much the screen behind the menu is blurred, in dp.
     */
    public static final IntegerSetting GLASS_MENUS_BLUR =
            new IntegerSetting("morphe_glass_menus_blur", 24, false,
                    parentsAll(parent(GLASS_MENUS), new BlurAvailability()));

    /**
     * How much the screen behind the menu is darkened, in percent.
     */
    public static final IntegerSetting GLASS_MENUS_DIM =
            new IntegerSetting("morphe_glass_menus_dim", 20, false,
                    parentsAll(parent(GLASS_MENUS), new BlurAvailability()));

    static {
        SeekBarPreference.register(new SeekBarConfig(GLASS_MENUS_OPACITY, 0, 100, 5, "%"));
        SeekBarPreference.register(new SeekBarConfig(GLASS_MENUS_BLUR, 4, 60, 2, "dp"));
        SeekBarPreference.register(new SeekBarConfig(GLASS_MENUS_DIM, 0, 80, 5, "%"));
    }

    /**
     * Loads this class, so the settings are known to the settings screen, the search and the import and export.
     */
    public static void load() {
    }

    private GlassMenusSettings() {
    }
}
