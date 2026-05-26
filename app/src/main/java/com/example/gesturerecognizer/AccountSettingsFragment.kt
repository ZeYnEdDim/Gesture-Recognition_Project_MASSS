package com.example.gesturerecognizer

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gesturerecognizer.databinding.FragmentAccountSettingsBinding

class AccountSettingsFragment : Fragment() {

    private var _binding: FragmentAccountSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val sharedPrefs = requireActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE)

        // Load saved data
        binding.etName.setText(sharedPrefs.getString("user_name", ""))
        binding.etPhone.setText(sharedPrefs.getString("emergency_contact", ""))
        binding.etAddress.setText(sharedPrefs.getString("home_address", ""))

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSave.setOnClickListener {
            sharedPrefs.edit().apply {
                putString("user_name", binding.etName.text.toString())
                putString("emergency_contact", binding.etPhone.text.toString())
                putString("home_address", binding.etAddress.text.toString())
                apply()
            }
            Toast.makeText(requireContext(), getString(R.string.changes_saved), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
