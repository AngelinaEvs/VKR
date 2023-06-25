package com.nefrit.app.app

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnMultiChoiceClickListener
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.Image
import android.opengl.*
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.util.SizeF
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.*
import com.google.ar.core.ArCoreApk.InstallStatus
import com.google.ar.core.Config.InstantPlacementMode
import com.google.ar.core.exceptions.*
import com.nefrit.app.R
import com.nefrit.common.depth.OpenCvUtils
import com.nefrit.common.depth.common.helpers.*
import com.nefrit.common.depth.common.samplerender.*
import com.nefrit.common.depth.common.samplerender.Mesh
import com.nefrit.common.depth.common.samplerender.Mesh.PrimitiveMode
import com.nefrit.common.depth.common.samplerender.Texture.ColorFormat
import com.nefrit.common.depth.common.samplerender.Texture.WrapMode
import com.nefrit.common.depth.common.samplerender.arcore.BackgroundRenderer
import com.nefrit.common.depth.common.samplerender.arcore.PlaneRenderer
import com.nefrit.common.depth.common.samplerender.arcore.SpecularCubemapFilter
import com.nefrit.common.utils.DrawingView
import kotlinx.android.synthetic.main.alert_dialog.view.*
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class HelloArActivity : AppCompatActivity(), SampleRender.Renderer {

    private var surfaceView: GLSurfaceView? = null
    private var imageView: ImageView? = null
    private var takeButton: ImageButton? = null
    private var getSquare: Button? = null
    private var installRequested: Boolean = false
    private var session: Session? = null
    private val messageSnackbarHelper: SnackbarHelper = SnackbarHelper()
    private var displayRotationHelper: DisplayRotationHelper? = null
    private val trackingStateHelper: TrackingStateHelper = TrackingStateHelper(this)
    private var tapHelper: TapHelper? = null
    private var render: SampleRender? = null
    private var planeRenderer: PlaneRenderer? = null
    private var backgroundRenderer: BackgroundRenderer? = null
    private var virtualSceneFramebuffer: Framebuffer? = null
    private var hasSetTextureNames: Boolean = false
    private val depthSettings: DepthSettings = DepthSettings()
    private val depthSettingsMenuDialogCheckboxes: BooleanArray = BooleanArray(2)
    private val instantPlacementSettings: InstantPlacementSettings = InstantPlacementSettings()
    private val instantPlacementSettingsMenuDialogCheckboxes: BooleanArray = BooleanArray(1)

    // Point Cloud
    private var pointCloudVertexBuffer: VertexBuffer? = null
    private var pointCloudMesh: Mesh? = null
    private var pointCloudShader: Shader? = null

    // Keep track of the last point cloud rendered to avoid updating the VBO if point cloud
    // was not changed.  Do this using the timestamp since we can't compare PointCloud objects.
    private var lastPointCloudTimestamp: Long = 0

    // Virtual object (ARCore pawn)
    private var virtualObjectMesh: Mesh? = null
    private var virtualObjectShader: Shader? = null
    private var virtualObjectAlbedoTexture: Texture? = null
    private var virtualObjectAlbedoInstantPlacementTexture: Texture? = null
    private val wrappedAnchors: MutableList<WrappedAnchor> = ArrayList()

    // Environmental HDR
    private var dfgTexture: Texture? = null
    private var cubemapFilter: SpecularCubemapFilter? = null

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private val modelMatrix: FloatArray = FloatArray(16)
    private val viewMatrix: FloatArray = FloatArray(16)
    private val projectionMatrix: FloatArray = FloatArray(16)
    private val modelViewMatrix: FloatArray = FloatArray(16) // view x model
    private val modelViewProjectionMatrix: FloatArray = FloatArray(16) // projection x view x model
    private val sphericalHarmonicsCoefficients: FloatArray = FloatArray(9 * 3)
    private val viewInverseMatrix: FloatArray = FloatArray(16)
    private val worldLightDirection: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f)
    private val viewLightDirection: FloatArray = FloatArray(4) // view x world light direction
    private var dist = 1.0

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_depth)
        surfaceView = findViewById(R.id.surfaceview)
        imageView = findViewById(R.id.imageView)
        getSquare = findViewById(R.id.getSquare)
        displayRotationHelper = DisplayRotationHelper( /*context=*/this)
        tapHelper = TapHelper( /*context=*/this)
        render = SampleRender(surfaceView, this, assets)
        installRequested = false
        depthSettings.onCreate(this)
        instantPlacementSettings.onCreate(this)
        takeButton = findViewById(R.id.take_button)
        var bitmapImage: Bitmap? = null
        takeButton?.setOnClickListener {
            bitmapImage = surfaceView?.width?.let {
                surfaceView?.height?.let { it1 ->
                    Bitmap.createBitmap(
                        it,
                        it1,
                        Bitmap.Config.ARGB_8888
                    )
                }
            }
            runOnUiThread {
                imageView?.setImageBitmap(bitmapImage)
                takeButton?.visibility = View.GONE
                getSquare?.visibility = View.VISIBLE
                surfaceView?.onPause()

                bitmapImage?.let {
                    val dv = DrawingView(applicationContext, bitmapImage)
                    setContentView(dv)
                }
            }
        }
        OpenCvUtils.findContour(bitmapImage)
        val resS = getSquareInSm().toString()
        getSquare?.setOnClickListener {
            surfaceView?.setVisibility(View.GONE)
            getSquare?.setText("Вычислить площадь")
            getSquare?.setOnClickListener {
                onCreateDialog(resS).show()
            }
        }
    }

    private fun onCreateDialog(square: String): Dialog {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        val inflater: LayoutInflater = getLayoutInflater()
        val view: View = inflater.inflate(R.layout.alert_dialog, null)
        view.square.text = "Площадь равняется ${square} см²"
        builder.setView(view)
        builder.setPositiveButton(
            "Сохранить"
        ) { _, _ -> openActivity() }
        return builder.create()
    }

    private fun openActivity() {
        val intent = Intent(this@HelloArActivity, MainActivity::class.java)
        this@HelloArActivity.startActivity(intent)
    }

    private fun getSquareInSm(): Double {
        val data = getCameraResolution()
        val w = data.first.width / 10.0
        val pixelsW = data.second.width
        val pixelsH = data.second.height
        val focusDist = data.third / 10.0
        val dpi = pixelsW / w
        val s = (pixelsW * pixelsH) / (dpi * dpi)
        return (s * dist * dist) / (focusDist * focusDist)
    }

    private fun getMillimetersDepth(depthImage: Image, x: Int, y: Int): Int {
        // The depth image has a single plane, which stores depth for each
        val plane: Image.Plane = depthImage.planes[0]
        val byteIndex: Int = x * plane.pixelStride + y * plane.rowStride
        val buffer: ByteBuffer = plane.buffer.order(ByteOrder.nativeOrder())
        return buffer.getShort(byteIndex).toInt()
    }

    private fun getCameraResolution(camNum: Int = 0): Triple<SizeF, Size, Float> {
        var size = SizeF(1f, 1f)
        var pixelSize = Size(1, 1)
        var focusDist = 1.0F
        val manager: CameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            val cameraIds: Array<String> = manager.cameraIdList
            if (cameraIds.size > camNum) {
                val character: CameraCharacteristics =
                    manager.getCameraCharacteristics(cameraIds[camNum])
                size = character.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) as SizeF
                pixelSize =
                    character.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE) as Size
                focusDist =
                    character.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)[0]
            }
        } catch (_: CameraAccessException) {
        }
        return Triple(size, pixelSize, focusDist)
    }

    override fun onDestroy() {
        if (session != null) {
            session!!.close()
            session = null
        }
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        if (session == null) {
            var exception: Exception? = null
            var message: String? = null
            try {
                when (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    InstallStatus.INSTALL_REQUESTED -> {
                        installRequested = true
                        return
                    }
                    InstallStatus.INSTALLED -> {}
                }

                if (!CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this)
                    return
                }

                session = Session(this)
            } catch (e: UnavailableArcoreNotInstalledException) {
                message = "Please install ARCore"
                exception = e
            } catch (e: UnavailableUserDeclinedInstallationException) {
                message = "Please install ARCore"
                exception = e
            } catch (e: UnavailableApkTooOldException) {
                message = "Please update ARCore"
                exception = e
            } catch (e: UnavailableSdkTooOldException) {
                message = "Please update this app"
                exception = e
            } catch (e: UnavailableDeviceNotCompatibleException) {
                message = "This device does not support AR"
                exception = e
            } catch (e: Exception) {
                message = "Failed to create AR session"
                exception = e
            }
            if (message != null) {
                messageSnackbarHelper.showError(this, message)
                Log.e(TAG, "Exception creating session", exception)
                return
            }
        }

        try {
            configureSession()
            session!!.resume()
        } catch (e: CameraNotAvailableException) {
            messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.")
            session = null
            return
        }
        surfaceView!!.onResume()
        displayRotationHelper!!.onResume()
    }

    public override fun onPause() {
        super.onPause()
        if (session != null) {
            displayRotationHelper!!.onPause()
            surfaceView!!.onPause()
            session!!.pause()
        }
    }

    public override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        results: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(
                this,
                "Camera permission is needed to run this application",
                Toast.LENGTH_LONG
            )
                .show()
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                CameraPermissionHelper.launchPermissionSettings(this)
            }
            finish()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus)
    }

    override fun onSurfaceCreated(render: SampleRender) {
        try {
            planeRenderer = PlaneRenderer(render)
            backgroundRenderer = BackgroundRenderer(render)
            virtualSceneFramebuffer = Framebuffer(render,  /*width=*/1,  /*height=*/1)
            cubemapFilter = SpecularCubemapFilter(
                render, CUBEMAP_RESOLUTION, CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES
            )

            dfgTexture = Texture(
                render,
                Texture.Target.TEXTURE_2D,
                WrapMode.CLAMP_TO_EDGE,  /*useMipmaps=*/
                false
            )

            val dfgResolution = 64
            val dfgChannels = 2
            val halfFloatSize = 2
            val buffer: ByteBuffer =
                ByteBuffer.allocateDirect(dfgResolution * dfgResolution * dfgChannels * halfFloatSize)
            assets.open("models/dfg.raw").use { `is` -> `is`.read(buffer.array()) }

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, dfgTexture!!.getTextureId())
            GLError.maybeThrowGLException("Failed to bind DFG texture", "glBindTexture")
            GLES30.glTexImage2D(
                GLES30.GL_TEXTURE_2D,  /*level=*/
                0,
                GLES30.GL_RG16F,  /*width=*/
                dfgResolution,  /*height=*/
                dfgResolution,  /*border=*/
                0,
                GLES30.GL_RG,
                GLES30.GL_HALF_FLOAT,
                buffer
            )
            GLError.maybeThrowGLException("Failed to populate DFG texture", "glTexImage2D")

            pointCloudShader = Shader.Companion.createFromAssets(
                render, "shaders/point_cloud.vert", "shaders/point_cloud.frag",  /*defines=*/null
            )
                .setVec4(
                    "u_Color", floatArrayOf(31.0f / 255.0f, 188.0f / 255.0f, 210.0f / 255.0f, 1.0f)
                )
                .setFloat("u_PointSize", 5.0f)

            pointCloudVertexBuffer =
                VertexBuffer(render, 4,  /*entries=*/null)
            val pointCloudVertexBuffers: Array<VertexBuffer> = arrayOf(
                pointCloudVertexBuffer!!
            )
            pointCloudMesh = Mesh(
                render, PrimitiveMode.POINTS,  /*indexBuffer=*/null, pointCloudVertexBuffers
            )

            virtualObjectAlbedoTexture = Texture.Companion.createFromAsset(
                render,
                "models/pawn_albedo.png",
                WrapMode.CLAMP_TO_EDGE,
                ColorFormat.SRGB
            )
            virtualObjectAlbedoInstantPlacementTexture = Texture.Companion.createFromAsset(
                render,
                "models/pawn_albedo_instant_placement.png",
                WrapMode.CLAMP_TO_EDGE,
                ColorFormat.SRGB
            )
            val virtualObjectPbrTexture: Texture = Texture.Companion.createFromAsset(
                render,
                "models/pawn_roughness_metallic_ao.png",
                WrapMode.CLAMP_TO_EDGE,
                ColorFormat.LINEAR
            )
            virtualObjectMesh = Mesh.Companion.createFromAsset(render, "models/pawn.obj")
            val m = mutableMapOf<String, String>()
            m["NUMBER_OF_MIPMAP_LEVELS"] = cubemapFilter!!.numberOfMipmapLevels.toString()
            virtualObjectShader = Shader.Companion.createFromAssets(
                render,
                "shaders/environmental_hdr.vert",
                "shaders/environmental_hdr.frag",  /*defines=*/
                m
            )
                .setTexture("u_AlbedoTexture", virtualObjectAlbedoTexture)
                .setTexture("u_RoughnessMetallicAmbientOcclusionTexture", virtualObjectPbrTexture)
                .setTexture("u_Cubemap", cubemapFilter!!.filteredCubemapTexture)
                .setTexture("u_DfgTexture", dfgTexture)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read a required asset file", e)
            messageSnackbarHelper.showError(this, "Failed to read a required asset file: " + e)
        }
    }

    override fun onSurfaceChanged(render: SampleRender?, width: Int, height: Int) {
        displayRotationHelper!!.onSurfaceChanged(width, height)
        virtualSceneFramebuffer!!.resize(width, height)
    }

    override fun onDrawFrame(render: SampleRender) {
        if (session == null) {
            return
        }

        if (!hasSetTextureNames) {
            session!!.setCameraTextureNames(
                intArrayOf(
                    backgroundRenderer?.cameraColorTexture?.getTextureId() ?: 0
                )
            )
            hasSetTextureNames = true
        }

        displayRotationHelper!!.updateSessionIfNeeded(session!!)


        val frame: Frame
        try {
            frame = session!!.update()
        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "Camera not available during onDrawFrame", e)
            messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.")
            return
        }
        val camera: Camera = frame.camera

        try {
            backgroundRenderer!!.setUseDepthVisualization(
                render, depthSettings.depthColorVisualizationEnabled()
            )
            backgroundRenderer!!.setUseOcclusion(render, depthSettings.useDepthForOcclusion())
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read a required asset file", e)
            messageSnackbarHelper.showError(this, "Failed to read a required asset file: " + e)
            return
        }
        backgroundRenderer!!.updateDisplayGeometry(frame)
        if ((camera.trackingState == TrackingState.TRACKING
                    && (depthSettings.useDepthForOcclusion()
                    || depthSettings.depthColorVisualizationEnabled()))
        ) {
            try {
                frame.acquireDepthImage16Bits().use { depthImage ->
                    dist = getMillimetersDepth(
                        depthImage,
                        depthImage.width / 2,
                        depthImage.height / 2
                    ) / 10.0
                    backgroundRenderer!!.updateCameraDepthTexture(depthImage)
                }
            } catch (e: NotYetAvailableException) {

            }
        }


        handleTap(frame, camera)

        trackingStateHelper.updateKeepScreenOnFlag(camera.trackingState)

        if (frame.timestamp != 0L) {
            backgroundRenderer!!.drawBackground(render)
        }

        if (camera.trackingState == TrackingState.PAUSED) {
            return
        }

        camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR)

        camera.getViewMatrix(viewMatrix, 0)
        frame.acquirePointCloud().use { pointCloud ->
            if (pointCloud.timestamp > lastPointCloudTimestamp) {
                pointCloudVertexBuffer!!.set(pointCloud.points)
                lastPointCloudTimestamp = pointCloud.timestamp
            }
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
            pointCloudShader!!.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
            render.draw(pointCloudMesh, pointCloudShader)
        }

        planeRenderer!!.drawPlanes(
            render,
            session!!.getAllTrackables(Plane::class.java),
            camera.displayOrientedPose,
            projectionMatrix
        )

        updateLightEstimation(frame.lightEstimate, viewMatrix)

        render.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 0f)
        for (wrappedAnchor: WrappedAnchor in wrappedAnchors) {
            val anchor: Anchor = wrappedAnchor.anchor
            val trackable: Trackable = wrappedAnchor.trackable
            if (anchor.trackingState != TrackingState.TRACKING) {
                continue
            }

            anchor.pose.toMatrix(modelMatrix, 0)

            Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)

            virtualObjectShader!!.setMat4("u_ModelView", modelViewMatrix)
            virtualObjectShader!!.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
            if ((trackable is InstantPlacementPoint
                        && trackable.getTrackingMethod()
                        == InstantPlacementPoint.TrackingMethod.SCREENSPACE_WITH_APPROXIMATE_DISTANCE)
            ) {
                virtualObjectShader!!.setTexture(
                    "u_AlbedoTexture", virtualObjectAlbedoInstantPlacementTexture
                )
            } else {
                virtualObjectShader!!.setTexture("u_AlbedoTexture", virtualObjectAlbedoTexture)
            }
            render.draw(virtualObjectMesh, virtualObjectShader, virtualSceneFramebuffer)
        }

        backgroundRenderer!!.drawVirtualScene(render, virtualSceneFramebuffer, Z_NEAR, Z_FAR)
    }

    private fun handleTap(frame: Frame, camera: Camera) {
        val tap: MotionEvent? = tapHelper!!.poll()
        if (tap != null && camera.trackingState == TrackingState.TRACKING) {
            val hitResultList: List<HitResult> =
                if (instantPlacementSettings.isInstantPlacementEnabled()) {
                    frame.hitTestInstantPlacement(
                        tap.x,
                        tap.y,
                        APPROXIMATE_DISTANCE_METERS
                    )
                } else {
                    frame.hitTest(tap)
                }
            for (hit: HitResult in hitResultList) {
                val trackable: Trackable = hit.getTrackable()
                if ((((trackable is Plane
                            && trackable.isPoseInPolygon(hit.getHitPose())
                            && (PlaneRenderer.Companion.calculateDistanceToPlane(
                        hit.getHitPose(),
                        camera.getPose()
                    ) > 0)))
                            || ((trackable is Point
                            && trackable.getOrientationMode()
                            == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL))
                            || (trackable is InstantPlacementPoint)
                            || (trackable is DepthPoint))
                ) {
                    if (wrappedAnchors.size >= 20) {
                        wrappedAnchors.get(0).anchor.detach()
                        wrappedAnchors.removeAt(0)
                    }

                    wrappedAnchors.add(WrappedAnchor(hit.createAnchor(), trackable))
                    runOnUiThread { showOcclusionDialogIfNeeded() }

                    break
                }
            }
        }
    }

    private fun showOcclusionDialogIfNeeded() {
        val isDepthSupported: Boolean = session!!.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
        if (!depthSettings.shouldShowDepthEnableDialog() || !isDepthSupported) {
            return  // Don't need to show dialog.
        }

        // Asks the user whether they want to use depth-based occlusion.
        AlertDialog.Builder(this)
            .setTitle(R.string.options_title_with_depth)
            .setMessage(R.string.depth_use_explanation)
            .setPositiveButton(
                R.string.button_text_enable_depth,
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                    depthSettings.setUseDepthForOcclusion(
                        true
                    )
                }
            )
            .setNegativeButton(
                R.string.button_text_disable_depth,
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                    depthSettings.setUseDepthForOcclusion(
                        false
                    )
                }
            )
            .show()
    }

    private fun launchInstantPlacementSettingsMenuDialog() {
        resetSettingsMenuDialogCheckboxes()
        val resources: Resources = resources
        AlertDialog.Builder(this)
            .setTitle(R.string.options_title_instant_placement)
            .setMultiChoiceItems(
                resources.getStringArray(R.array.instant_placement_options_array),
                instantPlacementSettingsMenuDialogCheckboxes,
                OnMultiChoiceClickListener { _: DialogInterface?, which: Int, isChecked: Boolean ->
                    instantPlacementSettingsMenuDialogCheckboxes[which] = isChecked
                }
            )
            .setPositiveButton(
                R.string.done
            ) { _: DialogInterface?, _: Int -> applySettingsMenuDialogCheckboxes() }
            .setNegativeButton(
                android.R.string.cancel
            ) { _: DialogInterface?, _: Int -> resetSettingsMenuDialogCheckboxes() }
            .show()
    }

    private fun launchDepthSettingsMenuDialog() {
        // Retrieves the current settings to show in the checkboxes.
        resetSettingsMenuDialogCheckboxes()

        // Shows the dialog to the user.
        val resources: Resources = getResources()
        if (session!!.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
            // With depth support, the user can select visualization options.
            AlertDialog.Builder(this)
                .setTitle(R.string.options_title_with_depth)
                .setMultiChoiceItems(
                    resources.getStringArray(R.array.depth_options_array),
                    depthSettingsMenuDialogCheckboxes
                ) { _: DialogInterface?, which: Int, isChecked: Boolean ->
                    depthSettingsMenuDialogCheckboxes[which] = isChecked
                }
                .setPositiveButton(
                    R.string.done
                ) { _: DialogInterface?, _: Int -> applySettingsMenuDialogCheckboxes() }
                .setNegativeButton(
                    android.R.string.cancel
                ) { _: DialogInterface?, _: Int -> resetSettingsMenuDialogCheckboxes() }
                .show()
        } else {
            // Without depth support, no settings are available.
            AlertDialog.Builder(this)
                .setTitle(R.string.options_title_without_depth)
                .setPositiveButton(
                    R.string.done,
                    { dialogInterface: DialogInterface?, which: Int -> applySettingsMenuDialogCheckboxes() }
                )
                .show()
        }
    }

    private fun applySettingsMenuDialogCheckboxes() {
        depthSettings.setUseDepthForOcclusion(depthSettingsMenuDialogCheckboxes.get(0))
        depthSettings.setDepthColorVisualizationEnabled(depthSettingsMenuDialogCheckboxes.get(1))
        instantPlacementSettings.setInstantPlacementEnabled(
            instantPlacementSettingsMenuDialogCheckboxes.get(0)
        )
        configureSession()
    }

    private fun resetSettingsMenuDialogCheckboxes() {
        depthSettingsMenuDialogCheckboxes[0] = depthSettings.useDepthForOcclusion()
        depthSettingsMenuDialogCheckboxes[1] = depthSettings.depthColorVisualizationEnabled()
        instantPlacementSettingsMenuDialogCheckboxes[0] =
            instantPlacementSettings.isInstantPlacementEnabled()
    }

    private fun hasTrackingPlane(): Boolean {
        for (plane: Plane in session!!.getAllTrackables(
            Plane::class.java
        )) {
            if (plane.getTrackingState() == TrackingState.TRACKING) {
                return true
            }
        }
        return false
    }

    private fun updateLightEstimation(lightEstimate: LightEstimate, viewMatrix: FloatArray) {
        if (lightEstimate.getState() != LightEstimate.State.VALID) {
            virtualObjectShader!!.setBool("u_LightEstimateIsValid", false)
            return
        }
        virtualObjectShader!!.setBool("u_LightEstimateIsValid", true)
        Matrix.invertM(viewInverseMatrix, 0, viewMatrix, 0)
        virtualObjectShader!!.setMat4("u_ViewInverse", viewInverseMatrix)
        updateMainLight(
            lightEstimate.getEnvironmentalHdrMainLightDirection(),
            lightEstimate.getEnvironmentalHdrMainLightIntensity(),
            viewMatrix
        )
        updateSphericalHarmonicsCoefficients(
            lightEstimate.getEnvironmentalHdrAmbientSphericalHarmonics()
        )
        cubemapFilter!!.update(lightEstimate.acquireEnvironmentalHdrCubeMap())
    }

    private fun updateMainLight(
        direction: FloatArray,
        intensity: FloatArray,
        viewMatrix: FloatArray
    ) {
        // We need the direction in a vec4 with 0.0 as the final component to transform it to view space
        worldLightDirection[0] = direction[0]
        worldLightDirection[1] = direction[1]
        worldLightDirection[2] = direction[2]
        Matrix.multiplyMV(viewLightDirection, 0, viewMatrix, 0, worldLightDirection, 0)
        virtualObjectShader!!.setVec4("u_ViewLightDirection", viewLightDirection)
        virtualObjectShader!!.setVec3("u_LightIntensity", intensity)
    }

    private fun updateSphericalHarmonicsCoefficients(coefficients: FloatArray) {
        if (coefficients.size != 9 * 3) {
            throw IllegalArgumentException(
                "The given coefficients array must be of length 27 (3 components per 9 coefficients"
            )
        }

        for (i in 0 until (9 * 3)) {
            sphericalHarmonicsCoefficients[i] =
                coefficients[i] * sphericalHarmonicFactors[i / 3]
        }
        virtualObjectShader!!.setVec3Array(
            "u_SphericalHarmonicsCoefficients", sphericalHarmonicsCoefficients
        )
    }

    private fun configureSession() {
        val config: Config = session!!.getConfig()
        config.setLightEstimationMode(Config.LightEstimationMode.ENVIRONMENTAL_HDR)
        if (session!!.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
            config.setDepthMode(Config.DepthMode.AUTOMATIC)
        } else {
            config.setDepthMode(Config.DepthMode.DISABLED)
        }
        if (instantPlacementSettings.isInstantPlacementEnabled()) {
            config.setInstantPlacementMode(InstantPlacementMode.LOCAL_Y_UP)
        } else {
            config.setInstantPlacementMode(InstantPlacementMode.DISABLED)
        }
        session!!.configure(config)
    }

    companion object {
        private val TAG: String = HelloArActivity::class.java.simpleName

        private val sphericalHarmonicFactors: FloatArray = floatArrayOf(
            0.282095f,
            -0.325735f,
            0.325735f,
            -0.325735f,
            0.273137f,
            -0.273137f,
            0.078848f,
            -0.273137f,
            0.136569f
        )
        private const val Z_NEAR: Float = 0.1f
        private const val Z_FAR: Float = 100f
        private const val CUBEMAP_RESOLUTION: Int = 16
        private const val CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES: Int = 32

        private const val APPROXIMATE_DISTANCE_METERS: Float = 2.0f

        fun start(context: Context) {
            val intent = Intent(context, HelloArActivity::class.java)
            context.startActivity(intent)
        }
    }
}

internal class WrappedAnchor constructor(val anchor: Anchor, val trackable: Trackable)