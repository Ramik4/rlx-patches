/*
 * Copyright (C) 2026 YaVoT maintainers (sashade8-ship-it)
 *
 * Entry point of the YaVoT add-on, https://github.com/sashade8-ship-it/morphe-patches-yavot
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Modified by RLX Patches (https://github.com/Ramik4/rlx-patches): built into the RLX bundle
 * instead of loaded as an add-on bundle.
 */

package app.rlx.extension.youtube.yandexvot;

import java.util.concurrent.atomic.AtomicBoolean;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.youtube.addon.AddOnApi;

/**
 * Subscribes Yandex voice over translation to the player hooks of Morphe Patches.
 * <p>
 * The hooks of {@link AddOnApi} are a public contract of Morphe Patches that does not change
 * between releases, so RLX uses them instead of hooking the player itself.
 */
@SuppressWarnings("unused")
public final class YandexVot {
    private static final AtomicBoolean registered = new AtomicBoolean(false);

    /**
     * Injection point. The patch adds a call to this method to {@code AddOnManager.registerAddOns()}.
     */
    public static void register() {
        if (!registered.compareAndSet(false, true)) return;

        try {
            Logger.printDebug(() -> "Registering Yandex voice over translation");

            // Load the settings class, so the settings are known to the settings search
            // and to import and export of the settings.
            YandexVotSettings.YANDEX_VOT_ENABLED.get();

            YandexVotCoordinator.register();
            AddOnApi.addPlayerOverlayButtonsListener(YandexVotButton::initializeButton);
            AddOnApi.addLegacyPlayerControlsListener(YandexVotButton::initializeLegacyButton);
            AddOnApi.addNewVideoStartedListener(YandexVoiceOverTranslationPatch::onNewVideoStarted);
            AddOnApi.addVideoIdListener(YandexVoiceOverTranslationPatch::onVideoIdChanged);
            AddOnApi.addVideoTimeListener(YandexVoiceOverTranslationPatch::setVideoTime);
            AddOnApi.addVideoStateListener(YandexVoiceOverTranslationPatch::videoStateChanged);
        } catch (Exception | LinkageError ex) {
            // Never break the player: without the hooks the feature is just not available.
            Logger.printException(() -> "Yandex voice over translation registration failure", ex);
        }
    }

    private YandexVot() {
    }
}
