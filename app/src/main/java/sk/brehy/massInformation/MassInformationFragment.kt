package sk.brehy.massInformation

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
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
        //binding.viewModel = viewModel

        showDownloadInfo()
        showImage()
        downloadOption()
        //viewModel.retrieveFilePath(requireContext(), download.filePath)
    }


    private fun showImage() {
        viewModel.filePath.observe(viewLifecycleOwner) { path ->
            Log.d("mass_fragment", "observed: ${path}")
            if (File(path).exists()) {
                Log.d("mass_fragment", "path exists")
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
       /* lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.status.collect { status ->
                    Log.d("mass_fragment", "observe status: ${status}")
                    binding.downloadInfo.text = status
                }
            }
        }*/
        binding.lifecycleOwner = this
        viewModel.status.observe(viewLifecycleOwner) { status ->
            Log.d("mass_fragment", "observe status: ${status}")
            binding.downloadInfo.text = status
            Log.d("mass_fragment", (binding.downloadInfo.text as String?).toString())
        }
    }

    fun showDownloadToast(text:String){
        val toast = Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT)
        toast.show()

    }

}