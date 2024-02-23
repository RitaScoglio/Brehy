package sk.brehy.lector

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import sk.brehy.MainActivity
import sk.brehy.R
import sk.brehy.Secret
import sk.brehy.databinding.FragmentLectorLoginBinding

class LectorLoginFragment : Fragment() {

    companion object {
        fun newInstance() = LectorLoginFragment()
    }

    private lateinit var viewModel: LectorViewModel
    private lateinit var binding: FragmentLectorLoginBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLectorLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(LectorViewModel::class.java)

        if (viewModel.checkLogIn(requireContext())) (activity as MainActivity).changeFragment(LectorFragment())

        binding.loginButton.setOnClickListener {
            if (binding.loginEdit.text.toString() == Secret.lector.password) {
                viewModel.saveLogIn(requireContext())
                (activity as MainActivity).changeFragment(LectorFragment())
            } else {
                (activity as MainActivity).showToast("Nesprávne heslo.", R.drawable.toast_background, R.color.brown_dark)
            }
        }
    }

}