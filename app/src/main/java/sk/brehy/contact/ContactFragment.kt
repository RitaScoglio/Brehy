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
import sk.brehy.exception.BrehyException

class ContactFragment : Fragment() {

    private val viewModel: ContactViewModel by activityViewModels()
    private lateinit var binding: FragmentContactBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return try {
            binding = FragmentContactBinding.inflate(inflater, container, false)
            binding.root
        } catch (e: Exception) {
            throw BrehyException("Failed to inflate ContactFragment binding.", e)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            binding.adress.setOnClickListener {
                try {
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
                } catch (e: Exception) {
                    throw BrehyException("Failed to copy address to clipboard or show toast.", e)
                }
            }

            binding.call.setOnClickListener {
                try {
                    viewModel.newIntent("tel:+421455322451", Intent.ACTION_DIAL, requireContext())
                } catch (e: Exception) {
                    throw BrehyException("Failed to initiate phone call intent.", e)
                }
            }

            binding.email.setOnClickListener {
                try {
                    viewModel.newIntent(
                        "mailto:${resources.getString(R.string.email)}",
                        Intent.ACTION_SENDTO,
                        requireContext()
                    )
                } catch (e: Exception) {
                    throw BrehyException("Failed to initiate email intent.", e)
                }
            }

            binding.ibanKostol.setOnClickListener {
                try {
                    viewModel.textClipboard("iban", "SK8709000000000074337685", requireContext())
                    (activity as MainActivity).showToast(
                        "IBAN bol skopírovaný.",
                        R.drawable.toast_background,
                        R.color.brown_dark
                    )
                } catch (e: Exception) {
                    throw BrehyException("Failed to copy Kostol IBAN or show toast.", e)
                }
            }

            binding.ibanKnaz.setOnClickListener {
                try {
                    viewModel.textClipboard("iban", "SK3402000000003638268157", requireContext())
                    (activity as MainActivity).showToast(
                        "IBAN bol skopírovaný.",
                        R.drawable.toast_background,
                        R.color.brown_dark
                    )
                } catch (e: Exception) {
                    throw BrehyException("Failed to copy Knaz IBAN or show toast.", e)
                }
            }
        } catch (e: Exception) {
            throw BrehyException("Failed to set click listeners in ContactFragment.", e)
        }
    }
}