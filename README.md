# Gesture Recognizer & Safety Alert System 🛡️📱

This project is a comprehensive **AI-powered Android application** designed for personal safety. It uses real-time accelerometer data to recognize specific hand gestures and trigger emergency actions.

The project covers the entire pipeline: from **raw data collection** and **Machine Learning model training** to **mobile deployment**.

## 🚀 Key Features

- **Real-time Gesture Recognition**: Detects 4 distinct gestures (UP, DOWN, LEFT, RIGHT) using advanced signal processing.
- **Smart Actions**:
    - 🚨 **Siren Alert (UP)**: Triggers a loud alarm for immediate attention.
    - 📍 **Location Finder (DOWN)**: Automatically fetches and speaks your current street address.
    - 📞 **Emergency Contact (LEFT)**: Simulates emergency communication initialization.
    - 📡 **Safety Signal (RIGHT)**: Broadcasts safety status.
- **Modern UI/UX**: Implemented with **Material 3**, featuring a clean, card-based hierarchical design and a soft color palette.
- **Background Support**: Optimized for responsiveness with resource-based status updates.

## 🧠 Machine Learning & Logic

The recognition engine is built on sophisticated mathematical feature extraction from the 3-axis accelerometer:
- **Time Domain Features**: Mean, Standard Deviation, Root Mean Square (RMS), Max/Min.
- **Frequency Domain Features**: Fast Fourier Transform (FFT) Energy and Spectral Entropy.
- **Classifier**: A Decision Tree based model trained on custom datasets, exported as optimized code for Android.

## 📂 Project Structure

```text
├── app/                            # Android Studio Project (Kotlin, XML, Material 3)
│   ├── src/main/java/              # Recognition logic & UI controllers
│   └── src/main/res/               # Material 3 themes, layouts & animations
├── Training_scripts (Python)/      # ML Development Environment
│   ├── Scripts/                    # Core Python logic
│   │   ├── train_RF.py             # Random Forest model training
│   │   ├── train_SVM.py            # SVM model training
│   │   ├── preprocessing.py        # Feature engineering (RMS, FFT, Entropy)
│   │   └── visualize.py            # Data & Signal visualization
│   ├── Output/                     # Exported artifacts
│   │   ├── GestureClassifier.java  # Optimized Java class for Android deployment
│   │   ├── rf_model.pkl            # Trained Random Forest model
│   │   ├── svm_model.pkl           # Trained SVM model
│   │   ├── scaler_*.npy            # Mean and Std for feature scaling
│   │   └── Fig11_*.png             # Gesture signal analysis plots
│   ├── Train results/              # Performance metrics and logs
│   └── Data/                       # Internal dataset for training scripts
└── Collected data/                 # Raw sensor datasets
    ├── Gestures CSV User 1/        # Initial data collection (User 1)
    ├── Gesture CSV User 2/         # Validation data collection (User 2)
    ├── Gesture CSV Total/          # Merged dataset
    └── Gesture CSV Total no walking/ # Optimized dataset (noise filtered)
```

## 🛠️ Tech Stack

- **Mobile**: Kotlin, Jetpack Components, Google Play Services (Location), Material Design 3.
- **AI/ML**: Python, Scikit-learn, Numpy, Pandas.
- **Tools**: Android Studio, VS Code, Git/GitHub.

## 📖 How to Use

1. Launch the application.
2. Tap **START LISTENING** or use the **Volume Up** key (if enabled).
3. Perform a gesture (e.g., move the phone upwards for a Siren).
4. The system will process the data, display the result, and execute the corresponding safety action.

---
*Developed as a demonstration of combining Mobile Development with Applied Machine Learning for safety solutions.*
