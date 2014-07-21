/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ex.camera2.portability;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.OnZoomChangeListener;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceHolder;

import com.android.ex.camera2.portability.debug.Log;

/**
 * An interface which provides possible camera device operations.
 *
 * The client should call {@code CameraAgent.openCamera} to get an instance
 * of {@link CameraAgent.CameraProxy} to control the camera. Classes
 * implementing this interface should have its own one unique {@code Thread}
 * other than the main thread for camera operations. Camera device callbacks
 * are wrapped since the client should not deal with
 * {@code android.hardware.Camera} directly.
 *
 * TODO: provide callback interfaces for:
 * {@code android.hardware.Camera.ErrorCallback},
 * {@code android.hardware.Camera.OnZoomChangeListener}, and
 */
public abstract class CameraAgent {
    public static final long CAMERA_OPERATION_TIMEOUT_MS = 2500;

    private static final Log.Tag TAG = new Log.Tag("CamAgnt");

    public static class CameraStartPreviewCallbackForward
            implements CameraStartPreviewCallback {
        private final Handler mHandler;
        private final CameraStartPreviewCallback mCallback;

        public static CameraStartPreviewCallbackForward getNewInstance(
                Handler handler, CameraStartPreviewCallback cb) {
            if (handler == null || cb == null) {
                return null;
            }
            return new CameraStartPreviewCallbackForward(handler, cb);
        }

        private CameraStartPreviewCallbackForward(Handler h,
                CameraStartPreviewCallback cb) {
            mHandler = h;
            mCallback = cb;
        }

        @Override
        public void onPreviewStarted() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onPreviewStarted();
                }});
        }
    }

    /**
     * A callback helps to invoke the original callback on another
     * {@link android.os.Handler}.
     */
    public static class CameraOpenCallbackForward implements CameraOpenCallback {
        private final Handler mHandler;
        private final CameraOpenCallback mCallback;

        /**
         * Returns a new instance of {@link FaceDetectionCallbackForward}.
         *
         * @param handler The handler in which the callback will be invoked in.
         * @param cb The callback to be invoked.
         * @return The instance of the {@link FaceDetectionCallbackForward}, or
         *         null if any parameter is null.
         */
        public static CameraOpenCallbackForward getNewInstance(
                Handler handler, CameraOpenCallback cb) {
            if (handler == null || cb == null) {
                return null;
            }
            return new CameraOpenCallbackForward(handler, cb);
        }

        private CameraOpenCallbackForward(Handler h, CameraOpenCallback cb) {
            // Given that we are using the main thread handler, we can create it
            // here instead of holding onto the PhotoModule objects. In this
            // way, we can avoid memory leak.
            mHandler = new Handler(Looper.getMainLooper());
            mCallback = cb;
        }

        @Override
        public void onCameraOpened(final CameraProxy camera) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onCameraOpened(camera);
                }});
        }

        @Override
        public void onCameraDisabled(final int cameraId) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onCameraDisabled(cameraId);
                }});
        }

        @Override
        public void onDeviceOpenFailure(final int cameraId, final String info) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onDeviceOpenFailure(cameraId, info);
                }});
        }

        @Override
        public void onDeviceOpenedAlready(final int cameraId, final String info) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onDeviceOpenedAlready(cameraId, info);
                }});
        }

        @Override
        public void onReconnectionFailure(final CameraAgent mgr, final String info) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onReconnectionFailure(mgr, info);
                }});
        }
    }

    /**
     * A handler for all camera api runtime exceptions.
     * The default behavior is to throw the runtime exception.
     */
    public static interface CameraExceptionCallback {
        public void onCameraException(RuntimeException e);
    }

    /**
     * An interface which wraps
     * {@link android.hardware.Camera.ErrorCallback}
     */
    public static interface CameraErrorCallback {
        public void onError(int error, CameraProxy camera);
    }

    /**
     * An interface which wraps
     * {@link android.hardware.Camera.AutoFocusCallback}.
     */
    public static interface CameraAFCallback {
        public void onAutoFocus(boolean focused, CameraProxy camera);
    }

    /**
     * An interface which wraps
     * {@link android.hardware.Camera.AutoFocusMoveCallback}.
     */
    public static interface CameraAFMoveCallback {
        public void onAutoFocusMoving(boolean moving, CameraProxy camera);
    }

    /**
     * An interface which wraps
     * {@link android.hardware.Camera.ShutterCallback}.
     */
    public static interface CameraShutterCallback {
        public void onShutter(CameraProxy camera);
    }

    /**
     * An interface which wraps
     * {@link android.hardware.Camera.PictureCallback}.
     */
    public static interface CameraPictureCallback {
        public void onPictureTaken(byte[] data, CameraProxy camera);
    }

    /**
     * An interface which wraps
     * {@link android.hardware.Camera.PreviewCallback}.
     */
    public static interface CameraPreviewDataCallback {
        public void onPreviewFrame(byte[] data, CameraProxy camera);
    }

    /**
     * An interface which wraps
     * {@link android.hardware.Camera.FaceDetectionListener}.
     */
    public static interface CameraFaceDetectionCallback {
        /**
         * Callback for face detection.
         *
         * @param faces   Recognized face in the preview.
         * @param camera  The camera which the preview image comes from.
         */
        public void onFaceDetection(Camera.Face[] faces, CameraProxy camera);
    }

    /**
     * An interface to be called when the camera preview has started.
     */
    public static interface CameraStartPreviewCallback {
        /**
         * Callback when the preview starts.
         */
        public void onPreviewStarted();
    }

    /**
     * An interface to be called for any events when opening or closing the
     * camera device. This error callback is different from the one defined
     * in the framework, {@link android.hardware.Camera.ErrorCallback}, which
     * is used after the camera is opened.
     */
    public static interface CameraOpenCallback {
        /**
         * Callback when camera open succeeds.
         */
        public void onCameraOpened(CameraProxy camera);

        /**
         * Callback when {@link com.android.camera.CameraDisabledException} is
         * caught.
         *
         * @param cameraId The disabled camera.
         */
        public void onCameraDisabled(int cameraId);

        /**
         * Callback when {@link com.android.camera.CameraHardwareException} is
         * caught.
         *
         * @param cameraId The camera with the hardware failure.
         * @param info The extra info regarding this failure.
         */
        public void onDeviceOpenFailure(int cameraId, String info);

        /**
         * Callback when trying to open the camera which is already opened.
         *
         * @param cameraId The camera which is causing the open error.
         */
        public void onDeviceOpenedAlready(int cameraId, String info);

        /**
         * Callback when {@link java.io.IOException} is caught during
         * {@link android.hardware.Camera#reconnect()}.
         *
         * @param mgr The {@link CameraAgent}
         *            with the reconnect failure.
         */
        public void onReconnectionFailure(CameraAgent mgr, String info);
    }

    /**
     * Opens the camera of the specified ID asynchronously. The camera device
     * will be opened in the camera handler thread and will be returned through
     * the {@link CameraAgent.CameraOpenCallback#
     * onCameraOpened(com.android.camera.cameradevice.CameraAgent.CameraProxy)}.
     *
     * @param handler The {@link android.os.Handler} in which the callback
     *                was handled.
     * @param callback The callback for the result.
     * @param cameraId The camera ID to open.
     */
    public void openCamera(final Handler handler, final int cameraId,
                           final CameraOpenCallback callback) {
        getDispatchThread().runJob(new Runnable() {
            @Override
            public void run() {
                getCameraHandler().obtainMessage(CameraActions.OPEN_CAMERA, cameraId, 0,
                        CameraOpenCallbackForward.getNewInstance(handler, callback)).sendToTarget();
            }});
    }

    /**
     * Closes the camera device.
     *
     * @param camera The camera to close. {@code null} means all.
     * @param synced Whether this call should be synchronous.
     */
    public void closeCamera(CameraProxy camera, boolean synced) {
        if (synced) {
            final WaitDoneBundle bundle = new WaitDoneBundle();

            getDispatchThread().runJobSync(new Runnable() {
                @Override
                public void run() {
                    getCameraHandler().obtainMessage(CameraActions.RELEASE).sendToTarget();
                    getCameraHandler().post(bundle.mUnlockRunnable);
                }}, bundle.mWaitLock, CAMERA_OPERATION_TIMEOUT_MS, "camera release");
        } else {
            getDispatchThread().runJob(new Runnable() {
                @Override
                public void run() {
                    getCameraHandler().removeCallbacksAndMessages(null);
                    getCameraHandler().obtainMessage(CameraActions.RELEASE).sendToTarget();
                }});
        }
    }

    /**
     * Sets a callback for handling camera api runtime exceptions on
     * a handler.
     */
    public abstract void setCameraDefaultExceptionCallback(CameraExceptionCallback callback,
            Handler handler);

    /**
     * Recycles the resources used by this instance. CameraAgent will be in
     * an unusable state after calling this.
     */
    public abstract void recycle();

    /**
     * @return The camera devices info.
     */
    public abstract CameraDeviceInfo getCameraDeviceInfo();

    /**
     * @return The handler to which camera tasks should be posted.
     */
    protected abstract Handler getCameraHandler();

    /**
     * @return The thread used on which client callbacks are served.
     */
    protected abstract DispatchThread getDispatchThread();

    /**
     * An interface that takes camera operation requests and post messages to the
     * camera handler thread. All camera operations made through this interface is
     * asynchronous by default except those mentioned specifically.
     */
    public static abstract class CameraProxy {

        /**
         * Returns the underlying {@link android.hardware.Camera} object used
         * by this proxy. This method should only be used when handing the
         * camera device over to {@link android.media.MediaRecorder} for
         * recording.
         */
        @Deprecated
        public abstract android.hardware.Camera getCamera();

        /**
         * @return The camera ID associated to by this
         * {@link CameraAgent.CameraProxy}.
         */
        public abstract int getCameraId();

        /**
         * @return The camera characteristics.
         */
        public abstract CameraDeviceInfo.Characteristics getCharacteristics();

        /**
         * @return The camera capabilities.
         */
        public abstract CameraCapabilities getCapabilities();

        /**
         * Reconnects to the camera device. On success, the camera device will
         * be returned through {@link CameraAgent
         * .CameraOpenCallback#onCameraOpened(com.android.camera.cameradevice.CameraAgent
         * .CameraProxy)}.
         * @see android.hardware.Camera#reconnect()
         *
         * @param handler The {@link android.os.Handler} in which the callback
         *                was handled.
         * @param cb The callback when any error happens.
         */
        public void reconnect(final Handler handler, final CameraOpenCallback cb) {
            getDispatchThread().runJob(new Runnable() {
                @Override
                public void run() {
                    getCameraHandler().obtainMessage(CameraActions.RECONNECT, getCameraId(), 0,
                            CameraOpenCallbackForward.getNewInstance(handler, cb)).sendToTarget();
                }});
        }

        /**
         * Unlocks the camera device.
         *
         * @see android.hardware.Camera#unlock()
         */
        public void unlock() {
            final WaitDoneBundle bundle = new WaitDoneBundle();
            getDispatchThread().runJobSync(new Runnable() {
                @Override
                public void run() {
                    getCameraHandler().sendEmptyMessage(CameraActions.UNLOCK);
                    getCameraHandler().post(bundle.mUnlockRunnable);
                }}, bundle.mWaitLock, CAMERA_OPERATION_TIMEOUT_MS, "camera unlock");
        }

        /**
         * Locks the camera device.
         * @see android.hardware.Camera#lock()
         */
        public void lock() {
            getDispatchThread().runJob(new Runnable() {
                @Override
                public void run() {
                    getCameraHandler().sendEmptyMessage(CameraActions.LOCK);
                }});
        }

        /**
         * Sets the {@link android.graphics.SurfaceTexture} for preview.
         *
         * @param surfaceTexture The {@link SurfaceTexture} for preview.
         */
        public void setPreviewTexture(final SurfaceTexture surfaceTexture) {
            getDispatchThread().runJob(new Runnable() {
                @Override
                public void run() {
                    getCameraHandler()
                            .obtainMessage(CameraActions.SET_PREVIEW_TEXTURE_ASYNC, surfaceTexture)
                            .sendToTarget();
                }});
        }

        /**
         * Blocks until a {@link android.graphics.SurfaceTexture} has been set
         * for preview.
         *
         * @param surfaceTexture The {@link SurfaceTexture} for preview.
         */
        public void setPreviewTextureSync(final SurfaceTexture surfaceTexture) {
            final WaitDoneBundle bundle = new WaitDoneBundle();
            getDispatchThread().runJobSync(new Runnable() {
                @Override
                public void run() {
                    getCameraHandler()
                            .obtainMessage(CameraActions.SET_PREVIEW_TEXTURE_ASYNC, surfaceTexture)
                            .sendToTarget();
                    getCameraHandler().post(bundle.mUnlockRunnable);
                }}, bundle.mWaitLock, CAMERA_OPERATION_TIMEOUT_MS, "set preview texture");
        }

        /**
         * Sets the {@link android.view.SurfaceHolder} for preview.
         *
         * @param surfaceHolder The {@link SurfaceHolder} for preview.
         */
        public void setPreviewDisplay(final SurfaceHolder surfaceHolder) {
            getDispatchThread().runJob(new Runnable() {
                @Override
                public void run() {
                    getCameraHandler()
                            .obtainMessage(CameraActions.SET_PREVIEW_DISPLAY_ASYNC, surfaceHolder)
                            .sendToTarget();
                }});
        }

        /**
         * Starts the camera preview.
         */
        public void startPreview() {
            getDispatchThread().runJob(new Runnable() {
                @Override
                public void run() {
                    getCameraHandler()
                            .obtainMessage(CameraActions.START_PREVIEW_ASYNC, null).sendToTarget();
                }});
        }

        /**
         * Starts the camera preview and executes a callback on a handler once
         * the preview starts.
         */
        public void startPreviewWithCallback(final Handler h, final CameraStartPreviewCallback cb) {
            getDispatchThread().runJob(new Runnable() {
                @Override
                public void run() {
                    getCameraHandler().obtainMessage(CameraActions.START_PREVIEW_ASYNC,
                            CameraStartPreviewCallbackForward.getNewInstance(h, cb))
                                    .sendToTarget();
                }});
        }

        /**
         * Stops the camera preview synchronously.
         * {@code stopPreview()} must be synchronous to ensure that the caller can
         * continues to release resources related to camera preview.
         */
        public void stopPreview() {
            final WaitDoneBundle bundle = new WaitDoneBundle();
            getDispatchThread().runJobSync(new Runnable() {
                @Override
                public void run() {
                    getCameraHandler().sendEmptyMessage(CameraActions.STOP_PREVIEW);
                    getCameraHandler().post(bundle.mUnlockRunnable);
                }}, bundle.mWaitLock, CAMERA_OPERATION_TIMEOUT_MS, "stop preview");
        }

        /**
         * Sets the callback for preview data.
         *
         * @param handler    The {@link android.os.Handler} in which the callback was handled.
         * @param cb         The callback to be invoked when the preview data is available.
         * @see  android.hardware.Camera#setPreviewCallback(android.hardware.Camera.PreviewCallback)
         */
        public abstract void setPreviewDataCallback(Handler handler, CameraPreviewDataCallback cb);

        /**
         * Sets the one-time callback for preview data.
         *
         * @param handler    The {@link android.os.Handler} in which the callback was handled.
         * @param cb         The callback to be invoked when the preview data for
         *                   next frame is available.
         * @see  android.hardware.Camera#setPreviewCallback(android.hardware.Camera.PreviewCallback)
         */
        public abstract void setOneShotPreviewCallback(Handler handler,
                                                       CameraPreviewDataCallback cb);

        /**
         * Sets the callback for preview data.
         *
         * @param handler The handler in which the callback will be invoked.
         * @param cb      The callback to be invoked when the preview data is available.
         * @see android.hardware.Camera#setPreviewCallbackWithBuffer(android.hardware.Camera.PreviewCallback)
         */
        public abstract void setPreviewDataCallbackWithBuffer(Handler handler,
                                                              CameraPreviewDataCallback cb);

        /**
         * Adds buffer for the preview callback.
         *
         * @param callbackBuffer The buffer allocated for the preview data.
         */
        public void addCallbackBuffer(final byte[] callbackBuffer) {
            getDispatchThread().runJob(new Runnable() {
                @Override
                public void run() {
                    getCameraHandler()
                            .obtainMessage(CameraActions.ADD_CALLBACK_BUFFER, callbackBuffer)
                            .sendToTarget();
                }
            });
        }

        /**
         * Starts the auto-focus process. The result will be returned through the callback.
         *
         * @param handler The handler in which the callback will be invoked.
         * @param cb      The auto-focus callback.
         */
        public abstract void autoFocus(Handler handler, CameraAFCallback cb);

        /**
         * Cancels the auto-focus process.
         */
        public void cancelAutoFocus() {
            getDispatchThread().runJob(new Runnable() {
                @Override
                public void run() {
                    getCameraHandler().removeMessages(CameraActions.AUTO_FOCUS);
                    getCameraHandler().sendEmptyMessage(CameraActions.CANCEL_AUTO_FOCUS);
                }});
        }

        /**
         * Sets the auto-focus callback
         *
         * @param handler The handler in which the callback will be invoked.
         * @param cb      The callback to be invoked when the preview data is available.
         */
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        public abstract void setAutoFocusMoveCallback(Handler handler, CameraAFMoveCallback cb);

        /**
         * Instrument the camera to take a picture.
         *
         * @param handler   The handler in which the callback will be invoked.
         * @param shutter   The callback for shutter action, may be null.
         * @param raw       The callback for uncompressed data, may be null.
         * @param postview  The callback for postview image data, may be null.
         * @param jpeg      The callback for jpeg image data, may be null.
         * @see android.hardware.Camera#takePicture(
         *         android.hardware.Camera.ShutterCallback,
         *         android.hardware.Camera.PictureCallback,
         *         android.hardware.Camera.PictureCallback)
         */
        public abstract void takePicture(
                Handler handler,
                CameraShutterCallback shutter,
                CameraPictureCallback raw,
                CameraPictureCallback postview,
                CameraPictureCallback jpeg);

        /**
         * Sets the display orientation for camera to adjust the preview orientation.
         *
         * @param degrees The rotation in degrees. Should be 0, 90, 180 or 270.
         */
        public void setDisplayOrientation(final int degrees) {
            getDispatchThread().runJob(new Runnable() {
                @Override
                public void run() {
                    getCameraHandler()
                            .obtainMessage(CameraActions.SET_DISPLAY_ORIENTATION, degrees, 0)
                            .sendToTarget();
                }});
        }

        /**
         * Sets the listener for zoom change.
         *
         * @param listener The listener.
         */
        public abstract void setZoomChangeListener(OnZoomChangeListener listener);

        /**
         * Sets the face detection listener.
         *
         * @param handler  The handler in which the callback will be invoked.
         * @param callback The callback for face detection results.
         */
        public abstract void setFaceDetectionCallback(Handler handler,
                                                      CameraFaceDetectionCallback callback);

        /**
         * Starts the face detection.
         */
        public void startFaceDetection() {
            getDispatchThread().runJob(new Runnable() {
                @Override
                public void run() {
                    getCameraHandler().sendEmptyMessage(CameraActions.START_FACE_DETECTION);
                }});
        }

        /**
         * Stops the face detection.
         */
        public void stopFaceDetection() {
            getDispatchThread().runJob(new Runnable() {
                @Override
                public void run() {
                    getCameraHandler().sendEmptyMessage(CameraActions.STOP_FACE_DETECTION);
                }});
        }

        /**
         * Registers an error callback.
         *
         * @param handler  The handler on which the callback will be invoked.
         * @param cb The error callback.
         * @see android.hardware.Camera#setErrorCallback(android.hardware.Camera.ErrorCallback)
         */
        public abstract void setErrorCallback(Handler handler, CameraErrorCallback cb);

        /**
         * Sets the camera parameters.
         *
         * @param params The camera parameters to use.
         */
        @Deprecated
        public abstract void setParameters(Camera.Parameters params);

        /**
         * Gets the current camera parameters synchronously. This method is
         * synchronous since the caller has to wait for the camera to return
         * the parameters. If the parameters are already cached, it returns
         * immediately.
         */
        @Deprecated
        public abstract Camera.Parameters getParameters();

        /**
         * Gets the current camera settings synchronously.
         * <p>This method is synchronous since the caller has to wait for the
         * camera to return the parameters. If the parameters are already
         * cached, it returns immediately.</p>
         */
        public abstract CameraSettings getSettings();

        /**
         * Default implementation of {@link #applySettings(CameraSettings)}
         * that is only missing the set of states it needs to wait for
         * before applying the settings.
         *
         * @param settings The settings to use on the device.
         * @param statesToAwait Bitwise OR of the required camera states.
         * @return Whether the settings can be applied.
         */
        protected boolean applySettingsHelper(final CameraSettings settings,
                                              final int statesToAwait) {
            if (settings == null) {
                Log.v(TAG, "null parameters in applySettings()");
                return false;
            }
            if (!getCapabilities().supports(settings)) {
                return false;
            }

            final CameraSettings copyOfSettings = new CameraSettings(settings);
            getDispatchThread().runJob(new Runnable() {
                @Override
                public void run() {
                    getCameraState().waitForStates(statesToAwait);
                    getCameraHandler().obtainMessage(CameraActions.APPLY_SETTINGS, copyOfSettings)
                            .sendToTarget();
                }
            });
            return true;
        }

        /**
         * Applies the settings to the camera device.
         *
         * @param settings The settings to use on the device.
         * @return Whether the settings can be applied.
         */
        public abstract boolean applySettings(CameraSettings settings);

        /**
         * Forces {@code CameraProxy} to update the cached version of the camera
         * settings regardless of the dirty bit.
         */
        public void refreshSettings() {
            getDispatchThread().runJob(new Runnable() {
                @Override
                public void run() {
                    getCameraHandler().sendEmptyMessage(CameraActions.REFRESH_PARAMETERS);
                }});
        }

        /**
         * Enables/Disables the camera shutter sound.
         *
         * @param enable   {@code true} to enable the shutter sound,
         *                 {@code false} to disable it.
         */
        public void enableShutterSound(final boolean enable) {
            getDispatchThread().runJob(new Runnable() {
                @Override
                public void run() {
                    getCameraHandler()
                            .obtainMessage(CameraActions.ENABLE_SHUTTER_SOUND, (enable ? 1 : 0), 0)
                            .sendToTarget();
                }});
        }

        /**
         * Dumps the current settings of the camera device.
         *
         * <p>The content varies based on the underlying camera API settings
         * implementation.</p>
         *
         * @return The content of the device settings represented by a string.
         */
        public abstract String dumpDeviceSettings();

        /**
         * @return The handler to which camera tasks should be posted.
         */
        public abstract Handler getCameraHandler();

        /**
         * @return The thread used on which client callbacks are served.
         */
        public abstract DispatchThread getDispatchThread();

        /**
         * @return The state machine tracking the camera API's current mode.
         */
        public abstract CameraStateHolder getCameraState();
    }

    public static class WaitDoneBundle {
        public final Runnable mUnlockRunnable;
        public final Object mWaitLock;

        WaitDoneBundle() {
            mWaitLock = new Object();
            mUnlockRunnable = new Runnable() {
                @Override
                public void run() {
                    synchronized (mWaitLock) {
                        mWaitLock.notifyAll();
                    }
                }};
        }
    }
}