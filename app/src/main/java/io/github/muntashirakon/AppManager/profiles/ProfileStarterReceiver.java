// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.muntashirakon.AppManager.profiles;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;
import org.json.JSONException;
import java.io.IOException;
import io.github.muntashirakon.AppManager.profiles.struct.AppsProfile;
import io.github.muntashirakon.AppManager.utils.ThreadUtils;
import io.github.muntashirakon.io.Path;

public class ProfileStarterReceiver extends BroadcastReceiver {
    /**
     * @see ProfileApplierActivity#getAutomationIntent(Context, String, String)
     * for original logic to launch a profile from an intent.
     */
    @SuppressLint("UnsafeIntentLaunch")
    @Override
    public void onReceive(Context context, Intent intent) {
        String _profileId = intent.getStringExtra(ProfileApplierActivity.EXTRA_PROFILE_ID);
        String profileState = intent.getStringExtra(ProfileApplierActivity.EXTRA_STATE);
        // Not part of the original intent logic but allows notification configuration.
        // Default logic does not notify for automatic triggers (passes `false` directly to `info`).
        boolean notify = intent.getBooleanExtra("notify", false);

        if (_profileId == null || profileState == null) {
            return;
        }

        String profileId = ProfileManager.getProfileIdCompat(_profileId);

        ProfileApplierActivity.ProfileApplierInfo info = new ProfileApplierActivity.ProfileApplierInfo();
        info.profileId = profileId;
        info.state = profileState;
        info.notify = notify;

        PendingResult asyncResult = goAsync();

        ThreadUtils.postOnBackgroundThread(() -> {
            Path profilePath = ProfileManager.findProfilePathById(info.profileId);
            try {
                info.profile = AppsProfile.fromPath(profilePath);
                Intent serviceIntent = ProfileApplierService.getIntent(context,
                        ProfileQueueItem.fromProfiledApplierInfo(info), info.notify);
                ContextCompat.startForegroundService(context, serviceIntent);
            } catch (IOException | JSONException e) {
                //noinspection CallToPrintStackTrace
                e.printStackTrace();
            } finally {
                asyncResult.finish();
            }
        });
    }
}
