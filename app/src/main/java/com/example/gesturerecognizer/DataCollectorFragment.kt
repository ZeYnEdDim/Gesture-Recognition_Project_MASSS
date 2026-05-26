package com.example.gesturerecognizer

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gesturerecognizer.databinding.FragmentDataCollectorBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class DataCollectorFragment : Fragment(), SensorEventListener {

    private var _binding: FragmentDataCollectorBinding? = null
    private val binding get() = _binding!!

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var vibrator: Vibrator? = null

    private val sensorData = mutableListOf<String>()
    private var isRecording = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDataCollectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        vibrator = requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        val gestures = arrayOf("LEFT", "RIGHT", "UP", "DOWN")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, gestures)
        binding.actvGesture.setAdapter(adapter)

        // Dropdown-dan jest seçiləndə sayğacı yenilə
        binding.actvGesture.setOnItemClickListener { _, _, _, _ ->
            updateCounterDisplay()
        }

        // User ID dəyişəndə sayğacı avtomatik yenilə
        binding.etUserId.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateCounterDisplay()
            }
        })

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Bas-Saxla məntiqi
        binding.btnCollect.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    startRecording()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    if (isRecording) {
                        stopRecording()
                    }
                    true
                }
                else -> false
            }
        }
        
        // İlk açılışda sayğacı göstər
        updateCounterDisplay()
    }

    private fun startRecording() {
        val userId = binding.etUserId.text.toString().trim()
        val gesture = binding.actvGesture.text.toString()

        if (userId.isEmpty() || gesture.isEmpty()) {
            Toast.makeText(context, "Please enter User ID and select Gesture", Toast.LENGTH_SHORT).show()
            return
        }

        isRecording = true
        sensorData.clear()
        binding.tvDataLog.text = "Recording... Release to save."
        vibratePhone(100)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
    }

    private fun stopRecording() {
        val userId = binding.etUserId.text.toString().trim()
        val gesture = binding.actvGesture.text.toString()

        sensorManager.unregisterListener(this)
        isRecording = false
        vibratePhone(50)

        if (sensorData.size > 15) {
            saveDataToFile(userId, gesture)
            updateCounterDisplay() // Fayl yazıldıqdan sonra sayğacı yenidən hesabla
            binding.tvDataLog.text = "Success! Sample saved."
        } else {
            binding.tvDataLog.text = "Failed! Hold longer."
        }
    }

    // Faylları həm JEST-ə, həm də USER ID-yə görə filterləyib sayırıq
    private fun updateCounterDisplay() {
        val currentGesture = binding.actvGesture.text.toString()
        val currentUserId = binding.etUserId.text.toString().trim()

        if (currentGesture.isNotEmpty()) {
            val folder = File(requireContext().getExternalFilesDir(null), "GestureData/$currentGesture")
            val fileCount = if (folder.exists()) {
                folder.listFiles { file ->
                    val isCsv = file.extension == "csv"
                    val belongsToUser = if (currentUserId.isNotEmpty()) {
                        file.name.startsWith("${currentUserId}_")
                    } else {
                        true // User ID boşdursa, hamısını say (ümumi statistika üçün)
                    }
                    isCsv && belongsToUser
                }?.size ?: 0
            } else {
                0
            }
            binding.tvCounter.text = getString(R.string.gestures_count, fileCount)
        }
    }

    private fun saveDataToFile(userId: String, gesture: String) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${userId}_${gesture}_$timestamp.csv"
        
        val folder = File(requireContext().getExternalFilesDir(null), "GestureData/$gesture")
        if (!folder.exists()) folder.mkdirs()

        val file = File(folder, fileName)
        try {
            val fos = FileOutputStream(file)
            fos.write("timestamp,x,y,z\n".toByteArray())
            sensorData.forEach { fos.write("$it\n".toByteArray()) }
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun vibratePhone(duration: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(duration)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (isRecording && event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val time = System.currentTimeMillis()
            sensorData.add("$time,${event.values[0]},${event.values[1]},${event.values[2]}")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroyView() {
        super.onDestroyView()
        sensorManager.unregisterListener(this)
        _binding = null
    }
}
