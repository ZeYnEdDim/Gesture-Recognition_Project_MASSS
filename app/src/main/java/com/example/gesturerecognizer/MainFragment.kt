package com.example.gesturerecognizer

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Address
import android.location.Geocoder
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.gesturerecognizer.databinding.FragmentMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale
import kotlin.math.*

class MainFragment : Fragment(), SensorEventListener, TextToSpeech.OnInitListener {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var tts: TextToSpeech
    
    private var isListening = false
    private val handler = Handler(Looper.getMainLooper())
    private val accelerometerData = mutableListOf<Triple<Float, Float, Float>>()
    private val resetRunnable = Runnable { if (!isListening) resetUI() }

    private val sharedPrefs: SharedPreferences by lazy {
        requireActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE)
    }

    private val scalerMean = doubleArrayOf(
        -9.007503584015109e-19, 2.2238117566965014e-17, 1.7774683804628012e-17, 2.2967970563043636,
        1.8110321892638641, 2.3848349586795345, 2.2714032036724148, 1.7906983205103049,
        2.358053181994596, 5.361969237487797, -3.949193277066673, -3.135984877535944,
        -3.6363774412661662, 3.9409396557198946, 2.9312460095894926, 3.948812247132448,
        410.60574345358344, 216.26020223682903, 349.13082547142665, 1.835428589872699,
        1.7218856391812793, 1.812367826071195
    )
    private val scalerStd = doubleArrayOf(
        2.6894818574333327e-16, 8.503897094039544e-16, 6.421840641925046e-16, 2.031087732667602,
        1.312333744559382, 1.4913756743224245, 2.0058059452242896, 1.2958160210245113,
        1.4731859337255002, 2.769297845877764, 3.254061138863485, 2.270844547086171,
        2.533484355332119, 3.605362259333522, 2.2670279314331503, 2.4207389474642227,
        802.7225312584188, 330.6294641190064, 425.11995063950627, 0.494650462228469,
        0.4430203680167262, 0.4235330024818836
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        tts = TextToSpeech(requireContext(), this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        binding.btnStartStop.setOnClickListener {
            if (!isListening) startListening()
        }
    }

    fun onVolumeKeyPressed() {
        if (sharedPrefs.getBoolean("volume_enabled", false) && !isListening) {
            startListening()
        }
    }

    private fun startListening() {
        if (isListening) return
        isListening = true
        accelerometerData.clear()
        
        binding.tvStatus.text = getString(R.string.status_listening)
        binding.tvResult.text = "---"
        binding.tvActivity.text = getString(R.string.status_waiting)
        binding.btnStartStop.isEnabled = false
        binding.btnStartStop.text = getString(R.string.recording_label)
        
        vibrate(50)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        handler.postDelayed({ stopListening() }, 1000)
    }

    private fun stopListening() {
        if (!isListening) return
        isListening = false
        sensorManager.unregisterListener(this)
        binding.tvStatus.text = getString(R.string.status_processing)
        binding.btnStartStop.text = getString(R.string.analyzing_label)
        vibrate(100)

        handler.postDelayed({
            if (accelerometerData.size < 10) {
                binding.tvStatus.text = getString(R.string.status_error)
                binding.tvResult.text = "!!!"
                binding.btnStartStop.isEnabled = true
                binding.btnStartStop.text = getString(R.string.start_listening_btn)
                startResetTimer()
                return@postDelayed
            }

            try {
                val features = extractFeatures()
                val scaledFeatures = scaleFeatures(features)
                val scores = GestureClassifier.score(scaledFeatures)
                val detectedGesture = predictGesture(scores)

                binding.tvStatus.text = getString(R.string.status_detected)
                binding.tvResult.text = detectedGesture
                binding.tvActivity.text = getGestureActionName(detectedGesture)
                binding.tvConfidence.text = getString(R.string.confidence_high)
                
                handleGestureAction(detectedGesture)

                if (detectedGesture != "DOWN") {
                    startResetTimer()
                }
            } catch (e: Exception) {
                binding.tvStatus.text = getString(R.string.status_error)
                binding.tvResult.text = "ERR"
                startResetTimer()
            }
            binding.btnStartStop.isEnabled = true
            binding.btnStartStop.text = getString(R.string.start_listening_btn)
        }, 100)
    }

    private fun startResetTimer() {
        handler.removeCallbacks(resetRunnable)
        handler.postDelayed(resetRunnable, 3000)
    }

    private fun resetUI() {
        if (_binding == null) return
        binding.tvStatus.text = getString(R.string.status_ready)
        binding.tvResult.text = "---"
        binding.tvActivity.text = getString(R.string.action_none)
        binding.tvConfidence.text = "---"
    }

    private fun getGestureActionName(gesture: String): String {
        return when (gesture) {
            "UP" -> getString(R.string.action_siren)
            "DOWN" -> "---"
            "LEFT" -> getString(R.string.action_emergency)
            "RIGHT" -> getString(R.string.action_safety)
            else -> getString(R.string.action_failed)
        }
    }

    private fun handleGestureAction(gesture: String) {
        val actionName = getGestureActionName(gesture)
        binding.tvActivity.text = actionName
        
        when (gesture) {
            "UP" -> {
                playSiren()
                startResetTimer()
            }
            "DOWN" -> {
                vibrateCustom(longArrayOf(0, 500))
                handleDownGesture()
                // startResetTimer is called inside processAddress for DOWN
            }
            "LEFT" -> {
                speak(actionName)
                vibrateCustom(longArrayOf(0, 300, 150, 300))
                startResetTimer()
            }
            "RIGHT" -> {
                speak(actionName)
                vibrateCustom(longArrayOf(0, 100))
                startResetTimer()
            }
            else -> {
                speak(getString(R.string.action_failed))
                startResetTimer()
            }
        }
    }

    private fun extractFeatures(): DoubleArray {
        val targetSize = 100
        val rawX = resample(accelerometerData.map { it.first.toDouble() }, targetSize)
        val rawY = resample(accelerometerData.map { it.second.toDouble() }, targetSize)
        val rawZ = resample(accelerometerData.map { it.third.toDouble() }, targetSize)

        val mX = rawX.average(); val mY = rawY.average(); val mZ = rawZ.average()
        val x = rawX.map { it - mX }; val y = rawY.map { it - mY }; val z = rawZ.map { it - mZ }

        fun std(v: List<Double>) = sqrt(v.sumOf { (it - v.average()).pow(2) } / (v.size - 1))
        fun rms(v: List<Double>) = sqrt(v.map { it.pow(2) }.average())

        fun computeFft(sig: List<Double>): Pair<Double, Double> {
            val n = sig.size
            val psd = DoubleArray(n)
            for (k in 0 until n) {
                var re = 0.0; var im = 0.0
                for (t in 0 until n) {
                    val angle = 2 * PI * k * t / n
                    re += sig[t] * cos(angle); im -= sig[t] * sin(angle)
                }
                psd[k] = re * re + im * im
            }
            val energy = psd.sum() / n
            val totalPsd = psd.sum() + 1e-9
            val entropy = psd.sumOf { p_val ->
                val p = p_val / totalPsd
                if (p > 1e-12) -p * ln(p) else 0.0
            }
            return Pair(energy, entropy)
        }

        val (ex, hx) = computeFft(x); val (ey, hy) = computeFft(y); val (ez, hz) = computeFft(z)

        return doubleArrayOf(
            x.average(), y.average(), z.average(),
            std(x), std(y), std(z),
            rms(x), rms(y), rms(z),
            (x.sumOf { abs(it) } + y.sumOf { abs(it) } + z.sumOf { abs(it) }) / targetSize,
            x.minOrNull() ?: 0.0, y.minOrNull() ?: 0.0, z.minOrNull() ?: 0.0,
            x.maxOrNull() ?: 0.0, y.maxOrNull() ?: 0.0, z.maxOrNull() ?: 0.0,
            ex, ey, ez, hx, hy, hz
        )
    }

    private fun resample(data: List<Double>, targetSize: Int): List<Double> {
        val resampled = DoubleArray(targetSize)
        for (i in 0 until targetSize) {
            val pos = i.toDouble() * (data.size - 1) / (targetSize - 1)
            val index = pos.toInt()
            val frac = pos - index
            resampled[i] = if (index >= data.size - 1) data.last()
            else data[index] * (1 - frac) + data[index + 1] * frac
        }
        return resampled.toList()
    }

    private fun scaleFeatures(f: DoubleArray) = DoubleArray(f.size) { (f[it] - scalerMean[it]) / (if (abs(scalerStd[it]) < 1e-9) 1.0 else scalerStd[it]) }

    private fun predictGesture(scores: DoubleArray): String {
        if (scores.size != 6) return "Error"
        val votes = IntArray(4)
        if (scores[0] > 0) votes[0]++ else votes[1]++
        if (scores[1] > 0) votes[0]++ else votes[2]++
        if (scores[2] > 0) votes[0]++ else votes[3]++
        if (scores[3] > 0) votes[1]++ else votes[2]++
        if (scores[4] > 0) votes[1]++ else votes[3]++
        if (scores[5] > 0) votes[2]++ else votes[3]++
        return when (votes.indices.maxByOrNull { votes[it] } ?: 0) {
            0 -> "DOWN"; 1 -> "LEFT"; 2 -> "RIGHT"; 3 -> "UP"; else -> "Unknown"
        }
    }

    private fun getVibrator(): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) (requireActivity().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else @Suppress("DEPRECATION") requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private fun vibrate(ms: Long) {
        if (!sharedPrefs.getBoolean("vibration_enabled", true)) return
        getVibrator().vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun vibrateCustom(pattern: LongArray) {
        if (!sharedPrefs.getBoolean("vibration_enabled", true)) return
        getVibrator().vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    private fun speak(text: String) {
        if (!sharedPrefs.getBoolean("voice_enabled", true)) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun playSiren() {
        try {
            ToneGenerator(AudioManager.STREAM_ALARM, 60).startTone(ToneGenerator.TONE_PROP_BEEP2, 1000)
        } catch (e: Exception) { }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) fetchLocation()
        else speak("Location permission denied.")
    }

    private fun handleDownGesture() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) fetchLocation()
        else requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    private fun fetchLocation() {
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location ->
                location?.let {
                    val geocoder = Geocoder(requireContext(), Locale.US)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocation(it.latitude, it.longitude, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) { processAddress(addresses.firstOrNull()) }
                            override fun onError(errorMessage: String?) { processAddress(null) }
                        })
                    } else {
                        @Suppress("DEPRECATION")
                        processAddress(geocoder.getFromLocation(it.latitude, it.longitude, 1)?.firstOrNull())
                    }
                } ?: speak("Location unavailable.")
            }
        } catch (e: SecurityException) { speak("Permission error.") }
    }

    private fun processAddress(address: Address?) {
        val msg = address?.let { getSimplifiedAddress(it) } ?: getString(R.string.action_failed)
        speak(msg)
        requireActivity().runOnUiThread { 
            binding.tvActivity.text = msg
            startResetTimer()
        }
    }

    private fun getSimplifiedAddress(addr: Address): String {
        return buildString {
            addr.thoroughfare?.let { append(it) }
            addr.subThoroughfare?.let { append(" $it") }
            addr.locality?.let { if (isNotEmpty()) append(", "); append(it) }
        }.ifBlank { "limited details" }
    }

    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) tts.language = Locale.US }

    override fun onSensorChanged(event: SensorEvent?) {
        if (isListening && event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            accelerometerData.add(Triple(event.values[0], event.values[1], event.values[2]))
        }
    }

    override fun onAccuracyChanged(s: Sensor?, a: Int) {}

    override fun onPause() {
        super.onPause()
        if (!isListening) sensorManager.unregisterListener(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        _binding = null
    }
}
