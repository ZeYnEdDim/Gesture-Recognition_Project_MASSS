package com.example.gesturerecognizer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gesturerecognizer.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPrefs = requireActivity().getSharedPreferences("Settings", android.content.Context.MODE_PRIVATE)
        binding.tvUserName.text = sharedPrefs.getString("user_name", "User Name")

        binding.cardAccount.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_accountSettingsFragment)
        }

        binding.cardNotifications.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_notificationSettingsFragment)
        }

        binding.cardAddGesture.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_dataCollectorFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
