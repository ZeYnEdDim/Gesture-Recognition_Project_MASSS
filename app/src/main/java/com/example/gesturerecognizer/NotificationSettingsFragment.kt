package com.example.gesturerecognizer

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gesturerecognizer.databinding.FragmentNotificationSettingsBinding

class NotificationSettingsFragment : Fragment() {

    private var _binding: FragmentNotificationSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val sharedPrefs = requireActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE)

        // Load saved states
        binding.switchVolume.isChecked = sharedPrefs.getBoolean("volume_enabled", false)
        binding.switchVibration.isChecked = sharedPrefs.getBoolean("vibration_enabled", true)
        binding.switchVoice.isChecked = sharedPrefs.getBoolean("voice_enabled", true)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.switchVolume.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("volume_enabled", isChecked).apply()
        }

        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("vibration_enabled", isChecked).apply()
        }

        binding.switchVoice.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("voice_enabled", isChecked).apply()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
