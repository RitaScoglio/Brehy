package sk.brehy.massInformation

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import sk.brehy.MainActivity
import sk.brehy.MainViewModel
import sk.brehy.R
import sk.brehy.databinding.FragmentMassInformationBinding
import java.io.File

class MassInformationFragment : Fragment() {

    private val viewModel: MassInformationViewModel by activityViewModels()
    private lateinit var binding: FragmentMassInformationBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMassInformationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.imgView.visibility = View.INVISIBLE

        showDownloadInfo()
        showImage()
        downloadOption()
        //viewModel.retrieveFilePath(requireContext(), download.filePath)
    }


    private fun showImage() {
        viewModel.filePath.observe(viewLifecycleOwner) { path ->
            if (File(path).exists()) {
                binding.imgView.setImageBitmap(BitmapFactory.decodeFile(path))
                binding.imgView.visibility = View.VISIBLE
                binding.downloadInfo.visibility = View.INVISIBLE
                binding.downloadButton.visibility = View.INVISIBLE
            } else {
                binding.downloadInfo.text = "Oznamy na tento týždeň nie sú k dispozícií."
            }
        }
    }

    private fun downloadOption() {
        binding.downloadButton.setOnClickListener {
            if (MainViewModel().isConnectedToInternet(requireContext()))
                viewModel.getAvailableMassInformation(requireContext())
            else
                (activity as MainActivity).showToast(
                    "Nie ste pripojený na internet.",
                    R.drawable.network_background,
                    R.color.brown_light
                )
        }
    }

    private fun showDownloadInfo() {
        viewModel.status.observe(viewLifecycleOwner) { status ->
            binding.downloadInfo.text = status
        }
    }

}