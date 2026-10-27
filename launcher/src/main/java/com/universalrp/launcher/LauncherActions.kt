package com.universalrp.launcher

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings

/**
 * Everything a button does, in one place and without any Android UI types.
 *
 * Each function returns a plain result rather than throwing, because the caller is a screen that
 * has to keep working when one button fails. A launcher that crashes when the camera app is missing
 * is worse than no launcher at all: the phone becomes unusable.
 */
object LauncherActions {

    /** What happened, so the screen can say something honest. */
    sealed class Result {
        object Done : Result()
        data class Failed(val reasonRes: Int) : Result()
    }

    /** The phone's installed apps, launchable ones only, sorted by their label. */
    fun installedApps(context: Context): List<AppEntry> {
        val manager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        return try {
            manager.queryIntentActivities(intent, 0)
                .asSequence()
                .mapNotNull { resolve ->
                    val info = resolve.activityInfo?.applicationInfo ?: return@mapNotNull null
                    // The launcher's own icon is not a button on its own screen.
                    if (info.packageName == context.packageName) return@mapNotNull null
                    val label = runCatching { manager.getApplicationLabel(info).toString() }
                        .getOrNull()
                        ?: return@mapNotNull null
                    AppEntry(
                        label = label,
                        packageName = info.packageName,
                        isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    )
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase() }
                .toList()
        } catch (error: Exception) {
            emptyList()
        }
    }

    fun launchApp(context: Context, packageName: String): Result {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return Result.Failed(R.string.no_apps_found)
        return try {
            context.startActivity(intent)
            Result.Done
        } catch (error: ActivityNotFoundException) {
            Result.Failed(R.string.no_apps_found)
        } catch (error: Exception) {
            Result.Failed(R.string.no_apps_found)
        }
    }

    /** Opens the dialler. Does not dial anything: the owner picks the number. */
    fun openDialer(context: Context): Result = start(
        context,
        Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        R.string.big_phone,
    )

    fun openMessages(context: Context): Result {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (context.packageManager.queryIntentActivities(intent, 0).isNotEmpty()) {
            return start(context, intent, R.string.big_messages)
        }
        // Some phones ship no messaging app that answers that category; the generic view is the
        // fallback that exists on every one of them.
        return start(
            context,
            Intent(Intent.ACTION_VIEW, Uri.parse("sms:")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            R.string.big_messages,
        )
    }

    fun openCamera(context: Context): Result = start(
        context,
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CAMERA)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        R.string.big_camera,
    )

    fun openContacts(context: Context): Result {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("content://contacts/people/"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (context.packageManager.queryIntentActivities(intent, 0).isNotEmpty()) {
            return start(context, intent, R.string.big_contacts)
        }
        return start(
            context,
            Intent(Intent.ACTION_VIEW, Uri.parse("content://contacts"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            R.string.big_contacts,
        )
    }

    fun openSettings(context: Context): Result = start(
        context,
        Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        R.string.big_settings,
    )

    /** Toggles the camera flash. Returns the new state. */
    fun toggleTorch(context: Context, currentlyOn: Boolean): Pair<Result, Boolean> {
        val manager = context.getSystemService(CameraManager::class.java)
            ?: return Result.Failed(R.string.torch_unavailable) to currentlyOn
        val cameraId = try {
            manager.cameraIdList.firstOrNull { id ->
                manager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (error: Exception) {
            null
        } ?: return Result.Failed(R.string.torch_unavailable) to currentlyOn
        return try {
            manager.setTorchMode(cameraId, !currentlyOn)
            Result.Done to !currentlyOn
        } catch (error: Exception) {
            // A torch that is already in use by another app cannot be taken; that is not a crash.
            Result.Failed(R.string.torch_unavailable) to currentlyOn
        }
    }

    fun nudgeVolume(context: Context, up: Boolean): Result {
        val audio = context.getSystemService(AudioManager::class.java)
            ?: return Result.Failed(R.string.volume_up)
        return try {
            val direction = if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
            Result.Done
        } catch (error: Exception) {
            Result.Failed(R.string.volume_up)
        }
    }

    /**
     * Dials the emergency number.
     *
     * ACTION_CALL places the call directly, which needs CALL_PHONE and is what an emergency button
     * is for. If the permission has not been granted the dialler is opened with the number filled
     * in instead, so the button still does something useful rather than nothing.
     */
    fun placeEmergencyCall(context: Context, number: String): Result {
        val call = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val granted = context.checkSelfPermission(android.Manifest.permission.CALL_PHONE) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) {
            return try {
                context.startActivity(call)
                Result.Done
            } catch (error: Exception) {
                openDialerWithNumber(context, number)
            }
        }
        return openDialerWithNumber(context, number)
    }

    private fun openDialerWithNumber(context: Context, number: String): Result = start(
        context,
        Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        R.string.emergency_call,
    )

    private fun start(context: Context, intent: Intent, reasonRes: Int): Result = try {
        context.startActivity(intent)
        Result.Done
    } catch (error: ActivityNotFoundException) {
        Result.Failed(reasonRes)
    } catch (error: Exception) {
        Result.Failed(reasonRes)
    }
}

/** One app on the phone, as the launcher's list shows it. */
data class AppEntry(
    val label: String,
    val packageName: String,
    val isSystem: Boolean,
)
