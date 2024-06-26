package org.thoughtcrime.securesms.mediasend.camerax

import android.content.Context
import android.os.Build
import androidx.camera.view.CameraController
import org.signal.core.util.asListContains
import org.thoughtcrime.securesms.mms.MediaConstraints
import org.thoughtcrime.securesms.util.FeatureFlags
import org.thoughtcrime.securesms.video.VideoUtil

/**
 * Describes device capabilities
 */
sealed class CameraXModePolicy {

  abstract val isVideoSupported: Boolean

  abstract fun initialize(cameraController: SignalCameraController)

  open fun setToImage(cameraController: SignalCameraController) = Unit

  open fun setToVideo(cameraController: SignalCameraController) = Unit

  /**
   * The device supports having Image and Video enabled at the same time
   */
  object Mixed : CameraXModePolicy() {

    override val isVideoSupported: Boolean = true

    override fun initialize(cameraController: SignalCameraController) {
      cameraController.setEnabledUseCases(CameraController.IMAGE_CAPTURE or CameraController.VIDEO_CAPTURE)
    }
  }

  /**
   * The device supports image and video, but only one mode at a time.
   */
  object Single : CameraXModePolicy() {

    override val isVideoSupported: Boolean = true

    override fun initialize(cameraController: SignalCameraController) {
      setToImage(cameraController)
    }

    override fun setToImage(cameraController: SignalCameraController) {
      cameraController.setEnabledUseCases(CameraController.IMAGE_CAPTURE)
    }

    override fun setToVideo(cameraController: SignalCameraController) {
      cameraController.setEnabledUseCases(CameraController.VIDEO_CAPTURE)
    }
  }

  /**
   * The device supports taking images only.
   */
  object ImageOnly : CameraXModePolicy() {

    override val isVideoSupported: Boolean = false

    override fun initialize(cameraController: SignalCameraController) {
      cameraController.setEnabledUseCases(CameraController.IMAGE_CAPTURE)
    }
  }

  companion object {
    @JvmStatic
    fun acquire(context: Context, mediaConstraints: MediaConstraints, isVideoEnabled: Boolean): CameraXModePolicy {
      val isVideoSupported = Build.VERSION.SDK_INT >= 26 &&
        isVideoEnabled &&
        MediaConstraints.isVideoTranscodeAvailable() &&
        VideoUtil.getMaxVideoRecordDurationInSeconds(context, mediaConstraints) > 0

      val isMixedModeSupported = isVideoSupported &&
        Build.VERSION.SDK_INT >= 26 &&
        CameraXUtil.isMixedModeSupported(context) &&
        !FeatureFlags.cameraXMixedModelBlocklist().asListContains(Build.MODEL)

      return when {
        isMixedModeSupported -> Mixed
        isVideoSupported -> Single
        else -> ImageOnly
      }
    }
  }
}
