package sk.brehy.intro

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import sk.brehy.R
import sk.brehy.exception.BrehyException

class IntroFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            inflater.inflate(R.layout.fragment_uvod, container, false)
        } catch (e: Exception) {
            throw BrehyException("Failed to inflate IntroFragment layout.", e)
        }
    }
}