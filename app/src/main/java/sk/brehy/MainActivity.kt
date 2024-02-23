package sk.brehy

//import sk.brehy.contact.ContactFragment
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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


//import sk.brehy.news.NewsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: MainActivityBinding
    private lateinit var massInfoModel: MassInformationViewModel
    private lateinit var mainModel: DatabaseMainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mainModel = ViewModelProvider(this).get(DatabaseMainViewModel::class.java)
        mainModel.initiateFirebase()
        mainModel.initiateFirebaseAuth(this)
        mainModel.initiateGoogleMessagingService(this)
        startDownloadingMassInformation()
        getLectorDatabaseData()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, IntroFragment())
                .commit()
        }
        setBottomNavigation()

        askNotificationPermission()
    }

    private fun startDownloadingMassInformation() {
        massInfoModel = ViewModelProvider(this).get(MassInformationViewModel::class.java)
        massInfoModel.getAvailableMassInformation(this)
    }

    private fun getLectorDatabaseData() {
        mainModel.getLectorData()
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
    }

    fun changeFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out)
            .replace(R.id.frame_layout, fragment)
            .setReorderingAllowed(true)
            .commit()
    }

    fun showToast(message: String, background_color: Int, text_color: Int){
        val layout = this.layoutInflater.inflate (R.layout.toast, this.findViewById(R.id.toast_layout))
        layout.background = ContextCompat.getDrawable(this, background_color)
        layout.setPadding(16,16,16,16)
        val toastMessage = layout.findViewById<TextView>(R.id.toast_text)
        toastMessage.text = message
        toastMessage.setTextColor(ContextCompat.getColor(this, text_color))
        val myToast = Toast(this)
        myToast.duration = Toast.LENGTH_LONG
        myToast.view = layout
        myToast.show()
    }

    // [START ask_post_notifications]
    // Declare the launcher at the top of your Activity/Fragment:
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        } else {
            // TODO: Inform user that that your app will not show notifications.
        }
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
    // [END ask_post_notifications]
}