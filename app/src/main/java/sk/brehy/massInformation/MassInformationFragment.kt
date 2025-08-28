package sk.brehy.massInformation

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import sk.brehy.MainActivity
import sk.brehy.MainViewModel
import sk.brehy.R
import sk.brehy.databinding.FragmentMassInformationBinding
import sk.brehy.exception.BrehyException
import java.io.File

class MassInformationFragment : Fragment() {

    private val viewModel: MassInformationViewModel by activityViewModels()
    private lateinit var binding: FragmentMassInformationBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return try {
            binding = FragmentMassInformationBinding.inflate(inflater, container, false)
            binding.root
        } catch (e: Exception) {
            throw BrehyException("Failed to inflate view for MassInformationFragment.", e)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        try {
            super.onViewCreated(view, savedInstanceState)
            binding.imgView.visibility = View.INVISIBLE
            showDownloadInfo()
            showImage()
            downloadOption()
        } catch (e: Exception) {
            throw BrehyException("Error in onViewCreated of MassInformationFragment.", e)
        }
    }

    private fun showImage() {
        try {
            viewModel.filePath.observe(viewLifecycleOwner) { path ->
                try {
                    Log.d("mass_fragment", "observed: $path")
                    val file = File(path)
                    if (file.exists()) {
                        Log.d("mass_fragment", "path exists")
                        val bitmap = BitmapFactory.decodeFile(path)
                            ?: throw BrehyException("Failed to decode image from path: $path")
                        binding.imgView.setImageBitmap(bitmap)
                        binding.imgView.visibility = View.VISIBLE
                        binding.downloadInfo.visibility = View.INVISIBLE
                        binding.downloadButton.visibility = View.INVISIBLE
                    } else {
                        binding.downloadInfo.text = "Announcements for this week are not available."
                    }
                } catch (e: Exception) {
                    throw BrehyException("Failed while showing image for path: $path", e)
                }
            }
        } catch (e: Exception) {
            throw BrehyException("Failed to observe filePath LiveData.", e)
        }
    }

    private fun downloadOption() {
        binding.downloadButton.setOnClickListener {
            try {
                if (MainViewModel().isConnectedToInternet(requireContext())) {
                    viewModel.getAvailableMassInformation(requireContext())
                } else {
                    (activity as MainActivity).showToast(
                        "You are not connected to the internet.",
                        R.drawable.network_background,
                        R.color.brown_light
                    )
                }
            } catch (e: Exception) {
                throw BrehyException("Error during download button click action.", e)
            }
        }
    }

    private fun showDownloadInfo() {
        try {
            binding.lifecycleOwner = this
            viewModel.status.observe(viewLifecycleOwner) { status ->
                try {
                    Log.d("mass_fragment", "observe status: $status")
                    binding.downloadInfo.text = status
                    Log.d("mass_fragment", (binding.downloadInfo.text as String?).toString())
                } catch (e: Exception) {
                    throw BrehyException("Failed while updating download status text.", e)
                }
            }
        } catch (e: Exception) {
            throw BrehyException("Failed to observe status LiveData.", e)
        }
    }
}
