package com.rehman.utilities

import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.location.Address
import android.location.Geocoder
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Vibrator
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.annotation.ColorRes
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.textfield.TextInputLayout
import com.rehman.utilities.databinding.NoInternetDialogBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object Utils {
    private lateinit var vb: Vibrator
    // View's min height
    private var minHeight = 0

    fun vibrate(context: Context, milliseconds: Long) {
        vb = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vb.vibrate(milliseconds)
    }

    fun hideKeyboard(activity: Activity) {
        val view: View? = activity.currentFocus
        if (view != null) {
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    fun hideSystemUiVisibility(context: Activity) {
        ViewCompat.getWindowInsetsController(context.window.decorView)?.let { controller ->
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun animateFragmentStatusBarColor(
        activity: Activity, context: Context, @ColorRes newColorResId:
        Int, animationDuration: Long
    ) {
        val currentColor = activity.window?.statusBarColor ?: return
        val newColor = ContextCompat.getColor(context, newColorResId)

        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), currentColor, newColor)
        colorAnimation.duration =
            animationDuration // Set the duration of the fade animation (in milliseconds)
        colorAnimation.addUpdateListener { animator ->
            activity.window?.statusBarColor = animator.animatedValue as Int
        }
        colorAnimation.start()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun changeStatusBarAndNavBarColors(
        activity: Activity,
        statusBarColor: Int,
        navBarColor: Int,
        isLightMode: Boolean
    ) {
        val window = activity.window

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            val appearance = if (isLightMode) {
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            } else {
                0
            }
            controller?.setSystemBarsAppearance(appearance, appearance)
            window.navigationBarColor = navBarColor
            window.statusBarColor = statusBarColor
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            @Suppress("DEPRECATION")
            window.statusBarColor = statusBarColor
            @Suppress("DEPRECATION")
            window.navigationBarColor = navBarColor
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val view = activity.window.decorView
                val flags = view.systemUiVisibility
                if (isLightMode) {
                    view.systemUiVisibility =
                        flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                } else {
                    view.systemUiVisibility =
                        flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                }
            }
        }
    }

    fun setStatusBarTransparent(activity: Activity) {
        val window = activity.window
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        val decorView = window.decorView
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        window.statusBarColor = Color.TRANSPARENT
    }

    fun enableFullScreenWithoutNavigation(activity: Activity) {
        activity.window.apply {
            requestFeature(Window.FEATURE_NO_TITLE)
            addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }
    }

    class ImageSaver(private val context: Context) {

        fun saveBitmapToStorage(
            bitmap: Bitmap,
            fileName: String,
            folderName: String
        ) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveImageToMediaStore(bitmap, fileName, folderName)
            } else {
                saveImageToExternalStorage(bitmap, fileName, folderName)
            }


        }

        @RequiresApi(Build.VERSION_CODES.Q)
        private fun saveImageToMediaStore(bitmap: Bitmap, fileName: String, folderName: String) {


            val contentResolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(
                    MediaStore.MediaColumns.DISPLAY_NAME, "$fileName _ ${
                        getCurrentDateAndTime
                            ("dd-MM-yy hh:mm:ss a")
                    }"
                )
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/$folderName/"
                )
            }

            val uri =
                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    outputStream.close()
                }
            }
        }

        private fun saveImageToExternalStorage(
            bitmap: Bitmap,
            fileName: String,
            folderName: String
        ) {
            val imageDirectory =
                File(Environment.getExternalStorageDirectory().path + "/$folderName/")
            if (!imageDirectory.exists()) {
                imageDirectory.mkdirs()
            }

            val imageFile =
                File(imageDirectory, "$fileName _ ${getCurrentDateAndTime("dd-MM-yy hh:mm:ss a")}")
            val outputStream: OutputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()

            // Refresh the MediaScanner to make the image available in the gallery
            MediaScannerConnection.scanFile(
                context,
                arrayOf(imageFile.absolutePath),
                null,
                null
            )
        }
    }

    //Pattern : dd-MM-yy hh:mm:ss a
    fun getCurrentDateAndTime(pattern: String): String {
        val format = SimpleDateFormat(pattern, Locale.getDefault())
        return format.format(Calendar.getInstance().time)
    }

    fun isFieldValid(inputLayout: TextInputLayout): Boolean {
        val valid = inputLayout.editText?.text.toString().trim()

        return if (valid.isEmpty()) {
            inputLayout.error = "*Field can't be empty"
            false
        } else {
            inputLayout.error = null
            inputLayout.isErrorEnabled = false
            true
        }
    }

    fun isEmailFieldValid(inputLayout: TextInputLayout): Boolean {
        val valid = inputLayout.editText?.text.toString().trim()
        val checkEmail = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+".toRegex()

        return if (valid.isEmpty()) {
            inputLayout.error = "*Field can't be empty"
            false
        } else if (!valid.matches(checkEmail)) {
            inputLayout.error = "*Invalid Email !"
            false
        } else {
            inputLayout.error = null
            inputLayout.isErrorEnabled = false
            true
        }
    }

    fun isPasswordFieldValid(inputLayout: TextInputLayout, passwordLenth: Int): Boolean {
        val valid = inputLayout.editText?.text.toString().trim()
        val checkPassword = "^(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{6,}$".toRegex()

        return if (valid.isEmpty()) {
            inputLayout.error = "*Field can't be empty"
            false
        } else if (valid.length < passwordLenth) {
            inputLayout.error = "*Password must be greater than $passwordLenth characters !"
            false
        } else if (!valid.matches(checkPassword)) {
            inputLayout.error =
                "Password must contain at least 1 digit, 1 lower and upper case letter, and a special character !"
            false
        } else {
            inputLayout.error = null
            inputLayout.isErrorEnabled = false
            true
        }
    }

    fun String.fadeTextAnimation(textView: TextView, fadeDuration: Long) {
        val fadeOut = ObjectAnimator.ofFloat(textView, "alpha", 1f, 0f)
        fadeOut.duration = fadeDuration // 500

        fadeOut.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                textView.text = this@fadeTextAnimation

                val fadeIn = ObjectAnimator.ofFloat(textView, "alpha", 0f, 1f)
                fadeIn.duration = fadeDuration //500
                fadeIn.start()
            }

            override fun onAnimationCancel(animation: Animator) {}

            override fun onAnimationRepeat(animation: Animator) {}
        })

        fadeOut.start()
    }

    fun setUpVideo(context: Context, videoView: VideoView, fileName: Int) {
        val uri = "android.resource://${context.packageName}/$fileName"
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, Uri.parse(uri))
        val width =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
        val height =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt()
                ?: 0
        val params = videoView.layoutParams
        params.width = context.resources.displayMetrics.widthPixels
        params.height = (params.width * height) / width
        videoView.layoutParams = params

        videoView.setVideoURI(uri.toUri())
        videoView.requestFocus()
        videoView.setOnPreparedListener { mp ->
            mp.isLooping = true
            mp.start()
        }
    }

    fun isAppInstalled(context: Context, packageName: String): Boolean {
        val packageManager = context.packageManager
        var isInstalled: Boolean
        try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            isInstalled = true
        } catch (e: PackageManager.NameNotFoundException) {
            isInstalled = false
            e.printStackTrace()
        }
        return isInstalled
    }

    fun Context.showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun Fragment.showToast(message: String) {
        context?.showToast(message)
    }

    fun getAddressFromLatLng(context: Context, latitude: Double, longitude: Double): Address {
        var address: Address? = null

            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses!!.size > 0) {

                address = addresses[0]

            }

        return address!!
    }


    // Expandable View Methods
    fun toggleCardViewHeight(
        expandedHeight: Int,
        invisibleView: ViewGroup,
        mainView: ViewGroup,
        arrowImage: ImageView
    ) {
        TransitionManager.beginDelayedTransition(invisibleView, AutoTransition())
        if (mainView.height == minHeight && invisibleView.visibility == View.GONE) {
            // expand
            invisibleView.visibility = View.VISIBLE
            arrowImage.animate().rotation(180f).setDuration(500).start()
            expandView(expandedHeight, mainView) //'height' is the height of screen which we have
            // measured already.
        } else {
            // collapse
            collapseView(mainView)
            invisibleView.visibility = View.GONE
            arrowImage.animate().rotation(0f).setDuration(500).start()
        }
    }

    private fun collapseView(mainView: ViewGroup) {
        val anim = ValueAnimator.ofInt(
            mainView.measuredHeightAndState,
            minHeight
        )
        anim.addUpdateListener { valueAnimator: ValueAnimator ->
            val `val` = valueAnimator.animatedValue as Int
            val layoutParams = mainView.layoutParams
            layoutParams.height = `val`
            mainView.layoutParams = layoutParams
        }
        anim.start()
    }

    private fun expandView(height: Int, mainView: ViewGroup) {
        val anim = ValueAnimator.ofInt(
            mainView.measuredHeightAndState,
            height
        )
        anim.addUpdateListener { valueAnimator: ValueAnimator ->
            val `val` = valueAnimator.animatedValue as Int
            val layoutParams = mainView.layoutParams
            layoutParams.height = `val`
            mainView.layoutParams = layoutParams
        }
        anim.start()
    }

    fun measureExpandableViewHeight(mainView: ViewGroup) {
        mainView.viewTreeObserver.addOnPreDrawListener(object :
            ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                mainView.viewTreeObserver.removeOnPreDrawListener(this)
                minHeight = mainView.height
                val layoutParams = mainView.layoutParams
                layoutParams.height = minHeight
                mainView.layoutParams = layoutParams
                return true
            }
        })
    }

    //

    class NoInternetDialog(private val context: Context) {
        private val dialog: Dialog = Dialog(context, R.style.FullScreenDialog)
        private val binding: NoInternetDialogBinding
        private var vb: Vibrator? = null

        init {
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.setCancelable(false)
            binding = NoInternetDialogBinding.inflate(LayoutInflater.from(context))
            dialog.setContentView(binding.root)

            //

            vb = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        fun setBackgroundColor(hexColorCode: String) {
            binding.mainLayout.setBackgroundColor(Color.parseColor(hexColorCode))
        }

        fun setTitleTextColor(hexColorCode: String) {
            binding.textView.setTextColor(Color.parseColor(hexColorCode))
        }

        fun setDescriptionTextColor(hexColorCode: String) {
            binding.textView2.setTextColor(Color.parseColor(hexColorCode))
        }

        fun setButtonBackgroundColor(hexColorCode: String) {
            binding.connect.setBackgroundColor(Color.parseColor(hexColorCode))
        }

        fun setButtonTextColor(hexColorCode: String) {
            binding.connect.setTextColor(Color.parseColor(hexColorCode))
        }

        fun checkInternetConnection() {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo
            val isConnected = networkInfo?.isConnectedOrConnecting == true
            if (!isConnected) {
                showDialog()
                Log.wtf("INTERNET", "No Internet available")
            } else {
                dismissDialog()
                Log.wtf("INTERNET", "Internet available")
            }
        }

        private fun showDialog() {
            binding.connect.setOnClickListener {
                vb!!.vibrate(30)
                checkInternetConnection()
            }

            dialog.show()
        }

        private fun dismissDialog() {
            dialog.dismiss()
        }
    }

    fun loadImage(
        context: Context,
        load: Any,
        placeHolder: Drawable,
        error: Drawable,
        imageView: ImageView
    ) {

        Glide.with(context)
            .load(load)
            .transition(DrawableTransitionOptions.withCrossFade())
            .placeholder(placeHolder)
            .error(error)
            .into(imageView)

    }

}