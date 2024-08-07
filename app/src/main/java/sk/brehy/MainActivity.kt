package sk.brehy

import android.Manifest
import android.annotation.TargetApi
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import sk.brehy.contact.ContactFragment
import sk.brehy.databinding.MainActivityBinding
import sk.brehy.intro.IntroFragment
import sk.brehy.lector.LectorLoginFragment
import sk.brehy.massInformation.MassInformationFragment
import sk.brehy.massInformation.MassInformationViewModel
import sk.brehy.news.NewsFragment
import sk.brehy.web.WebpageFragment


class MainActivity : AppCompatActivity() {

    private lateinit var binding: MainActivityBinding
    private lateinit var massInfoModel: MassInformationViewModel
    private lateinit var mainModel: MainViewModel

    /* private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            when (this@MainActivity.supportFragmentManager.backStackEntryCount) {
                0 -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        this@MainActivity.finishAndRemoveTask()
                    else
                        this@MainActivity.finish()
                    this@MainActivity.onDestroy()
                }
                1 -> {
                    showToast(
                        "Pre skončenie aplikácie stlačte tlačídlo \"Späť\" ešte raz.",
                        R.drawable.border_dark,
                        R.color.brown_light
                    )
                    this@MainActivity.supportFragmentManager.popBackStack()
                }
                else -> this@MainActivity.supportFragmentManager.popBackStack()
            }
        }
    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        mainModel = ViewModelProvider(this)[MainViewModel::class.java]
        mainModel.initiateFirebase()
        mainModel.initiateFirebaseAuth(this)
        mainModel.initiateGoogleMessagingService(this)

        checkDrawOverlayPermission()
        startDownloadingMassInformation()

        if (savedInstanceState == null)
            changeFragment(IntroFragment(), "intro")
        setBottomNavigation()
        askNotificationPermission()
    }

    val REQUEST_CODE = 10101

    fun checkDrawOverlayPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }
        return if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_CODE)
            false
        } else {
            true
        }
    }

    @Deprecated("Deprecated in Java")
    @TargetApi(Build.VERSION_CODES.M)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                //startService(Intent(this, PowerButtonService::class.java))
            }
        }
    }

    private fun startDownloadingMassInformation() {
        massInfoModel = ViewModelProvider(this)[MassInformationViewModel::class.java]
        massInfoModel.getAvailableMassInformation(this)
    }

    private fun setBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_news -> {
                    changeFragment(NewsFragment())
                    true
                }

                R.id.menu_mass_information -> {
                    changeFragment(MassInformationFragment())
                    true
                }

                R.id.menu_webpage -> {
                    changeFragment(WebpageFragment())
                    true
                }

                R.id.menu_lector -> {
                    changeFragment(LectorLoginFragment())
                    true
                }

                R.id.menu_contact -> {
                    changeFragment(ContactFragment())
                    true
                }

                else -> true
            }
        }
        binding.bottomNavigation.menu.setGroupCheckable(0, true, false)
        for (i in 0 until binding.bottomNavigation.menu.size()) {
            binding.bottomNavigation.menu.getItem(i).isChecked = false
        }
        binding.bottomNavigation.menu.setGroupCheckable(0, true, true)
    }

    fun changeFragment(fragment: Fragment, from: String = "") {
        if (from != "")
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in,
                    R.anim.fade_out,
                    R.anim.fade_in,
                    R.anim.slide_out
                )
                .replace(R.id.frame_layout, fragment)
                .addToBackStack(from)
                .setReorderingAllowed(true)
                .commit()
        else
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in,
                    R.anim.fade_out,
                    R.anim.fade_in,
                    R.anim.slide_out
                )
                .replace(R.id.frame_layout, fragment)
                .setReorderingAllowed(true)
                .commit()
    }

    fun showToast(message: String, backgroundColor: Int, textColor: Int) {
        val layout =
            this.layoutInflater.inflate(R.layout.toast, this.findViewById(R.id.toast_layout))
        layout.background = ContextCompat.getDrawable(this, backgroundColor)
        layout.setPadding(16, 16, 16, 16)
        val toastMessage = layout.findViewById<TextView>(R.id.toast_text)
        toastMessage.text = message
        toastMessage.setTextColor(ContextCompat.getColor(this, textColor))
        val myToast = Toast(this)
        myToast.duration = Toast.LENGTH_LONG
        myToast.view = layout
        myToast.show()
    }

    // [START ask_post_notifications]
    // Declare the launcher at the top of your Activity/Fragment:
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _: Boolean ->
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    // [END ask_post_notifications] }
}