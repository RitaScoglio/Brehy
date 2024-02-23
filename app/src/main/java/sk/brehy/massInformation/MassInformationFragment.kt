package sk.brehy.massInformation

import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import sk.brehy.MainActivity
import sk.brehy.R
import sk.brehy.databinding.FragmentMassInformationBinding
import java.io.File

class MassInformationFragment : Fragment() {

    companion object {
        fun newInstance() = MassInformationFragment()
    }
    private lateinit var viewModel: MassInformationViewModel
    private lateinit var binding: FragmentMassInformationBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMassInformationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.imgView.visibility = View.INVISIBLE

        viewModel = ViewModelProvider(requireActivity()).get(MassInformationViewModel::class.java)

        showDownloadInfo()
        showImage()
        downloadOption()
        //viewModel.retrieveFilePath(requireContext(), download.filePath)
    }


    private fun showImage(){
        viewModel.filePath.observe(viewLifecycleOwner, Observer { path ->
            if(File(path).exists()) {
                binding.imgView.setImageBitmap(BitmapFactory.decodeFile(path))
                binding.imgView.visibility = View.VISIBLE
                binding.downloadInfo.visibility = View.INVISIBLE
                binding.downloadButton.visibility = View.INVISIBLE
            } else {
                binding.downloadInfo.text = "Oznamy na tento týždeň nie sú k dispozícií."
            }
        })
    }

    private fun downloadOption() {
        binding.downloadButton.setOnClickListener {
            if (viewModel.isConnectedToInternet(requireContext()))
                viewModel.getAvailableMassInformation(requireContext())
            else
                (activity as MainActivity).showToast("Nie ste pripojený na internet.", R.drawable.network_background, R.color.brown_light)
        }
    }

    private fun showDownloadInfo(){
        viewModel.status.observe(viewLifecycleOwner, Observer { status ->
            binding.downloadInfo.text = status
        })
    }

}