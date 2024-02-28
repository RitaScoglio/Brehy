package sk.brehy.contact

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import sk.brehy.MainActivity
import sk.brehy.R
import sk.brehy.databinding.FragmentContactBinding

class ContactFragment : Fragment() {

    private val viewModel: ContactViewModel by activityViewModels()
    private lateinit var binding: FragmentContactBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.adress.setOnClickListener {
            viewModel.textClipboard(
                "adresa",
                resources.getString(R.string.adresa),
                requireContext()
            )
            (activity as MainActivity).showToast(
                "Adresa bola skopírovaná.",
                R.drawable.toast_background,
                R.color.brown_dark
            )
        }

        binding.call.setOnClickListener {
            viewModel.newIntent("tel:+421455322451", Intent.ACTION_DIAL, requireContext())
        }

        binding.email.setOnClickListener {
            viewModel.newIntent(
                "mailto:${resources.getString(R.string.email)}",
                Intent.ACTION_SENDTO,
                requireContext()
            )
        }

        binding.ibanKostol.setOnClickListener {
            viewModel.textClipboard("iban", "SK8709000000000074337685", requireContext())
            (activity as MainActivity).showToast(
                "IBAN bol skopírovaný.",
                R.drawable.toast_background,
                R.color.brown_dark
            )
        }

        binding.ibanKnaz.setOnClickListener {
            viewModel.textClipboard("iban", "SK3402000000003638268157", requireContext())
            (activity as MainActivity).showToast(
                "IBAN bol skopírovaný.",
                R.drawable.toast_background,
                R.color.brown_dark
            )
        }
    }

}