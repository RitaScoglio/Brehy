package sk.brehy.lector

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import sk.brehy.MainActivity
import sk.brehy.R
import sk.brehy.Secret
import sk.brehy.databinding.FragmentLectorLoginBinding
import sk.brehy.exception.BrehyException

class LectorLoginFragment : Fragment() {

    private val viewModel: LectorViewModel by activityViewModels()
    private lateinit var binding: FragmentLectorLoginBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return try {
            binding = FragmentLectorLoginBinding.inflate(inflater, container, false)
            binding.root
        } catch (e: Exception) {
            throw BrehyException("Failed to inflate LectorLoginFragment layout.", e)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            if (viewModel.checkLogIn(requireContext())) {
                val mainActivity = activity as? MainActivity
                    ?: throw BrehyException("Activity is not MainActivity during auto-login.")
                mainActivity.changeFragment(LectorFragment())
            }

            binding.loginButton.setOnClickListener {
                try {
                    val passwordInput = binding.loginEdit.text.toString()
                    val expectedPassword = Secret.lector.password
                        ?: throw BrehyException("Password in Secret.lector is null.")

                    if (passwordInput == expectedPassword) {
                        viewModel.saveLogIn(requireContext())
                        val mainActivity = activity as? MainActivity
                            ?: throw BrehyException("Activity is not MainActivity on successful login.")
                        mainActivity.changeFragment(LectorFragment())
                    } else {
                        val mainActivity = activity as? MainActivity
                            ?: throw BrehyException("Activity is not MainActivity on login failure.")
                        mainActivity.showToast(
                            "Nesprávne heslo.",
                            R.drawable.toast_background,
                            R.color.brown_dark
                        )
                    }
                } catch (e: Exception) {
                    throw BrehyException("Login button click failed in LectorLoginFragment.", e)
                }
            }
        } catch (e: Exception) {
            throw BrehyException("Failed in onViewCreated of LectorLoginFragment.", e)
        }
    }
}