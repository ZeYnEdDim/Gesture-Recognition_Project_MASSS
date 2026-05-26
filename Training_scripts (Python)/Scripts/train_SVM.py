import numpy as np
import os
import m2cgen as m2c
import joblib
from sklearn.model_selection import train_test_split, StratifiedKFold, cross_val_score
from sklearn.svm import SVC
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import classification_report, confusion_matrix
from scipy.stats import entropy
from scipy.fftpack import fft

def extract_features(data):
    """Sənin bütün zəngin feature-lərini (22 ədəd) saxlayan funksiya."""
    features = []
    for df in data:
        # Time-domain features
        mean = df[['x', 'y', 'z']].mean().values
        std = df[['x', 'y', 'z']].std().values
        rms = np.sqrt((df[['x', 'y', 'z']]**2).mean().values)
        sma = (abs(df['x']).sum() + abs(df['y']).sum() + abs(df['z']).sum()) / len(df)
        min_v = df[['x', 'y', 'z']].min().values
        max_v = df[['x', 'y', 'z']].max().values

        # Frequency-domain features (FFT & Entropy)
        energy, spec_entropy = [], []
        for col in ['x', 'y', 'z']:
            sig = df[col].values
            sig_fft = fft(sig)
            energy.append(np.sum(np.abs(sig_fft)**2) / len(sig))
            psd = np.abs(sig_fft)**2
            psd_norm = psd / (np.sum(psd) + 1e-9)
            spec_entropy.append(entropy(psd_norm))

        # 22 feature
        v = np.concatenate([mean, std, rms, [sma], min_v, max_v, energy, spec_entropy])
        features.append(v)
    return np.array(features)

if __name__ == "__main__":
    IN = 'Output/preprocessed_data.npz'
    OUT_DIR = 'Output'

    if not os.path.exists(IN):
        exit(f"XƏTA: {IN} faylı tapılmadı! Əvvəlcə Scripts/preprocess.py işlədin.")

    print("1. Datas are uploaded and Feature Extraction (22 əlamət) is starting...")
    d = np.load(IN, allow_pickle=True)
    X = extract_features(d['data'])
    y = d['labels']

    # --- STEP A: K-FOLD CROSS VALIDATION ---
    print(f"\n2. K-Fold Cross-Validation (SVM) is starting...")
    scaler_cv = StandardScaler()
    X_scaled_cv = scaler_cv.fit_transform(X)
    
    model_cv = SVC(
    kernel='rbf',
    C=1.0,
    gamma='scale',
    random_state=42
)
    
    kfold = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
    
    cv_scores = cross_val_score(model_cv, X_scaled_cv, y, cv=kfold)
    
    print(f"K-Fold Results: {cv_scores}")
    print(f"Average Accuracy: {cv_scores.mean():.2f} (+/- {cv_scores.std() * 2:.2f})")

    # --- STEP B: TRAIN-TEST SPLIT ---
    print(f"\n3. DETAILED Calculation (80/20 split) is prepared...")
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)
    
    scaler_split = StandardScaler()
    X_train_s = scaler_split.fit_transform(X_train)
    X_test_s = scaler_split.transform(X_test)
    
    model_split = SVC(
    kernel='rbf',
    C=1.0,
    random_state=42
).fit(X_train_s, y_train)

    y_pred = model_split.predict(X_test_s)
    
    print("\n--- 80/20 SPLIT DETAILS ---")
    print(classification_report(y_test, y_pred))
    print("Confusion Matrix:\n", confusion_matrix(y_test, y_pred))

    # --- STEP C: FINAL MODEL ---
    print(f"\n4. RESULT MODEL is prepared (All {len(X)} samples used)...")
    from sklearn.multiclass import OneVsOneClassifier

    final_scaler = StandardScaler()
    X_final = final_scaler.fit_transform(X)
    
    final_model = SVC(
    kernel='rbf',
    C=1.0,
    gamma='scale',
    probability=True,
    decision_function_shape='ovr',
    random_state=42
)
    final_model.fit(X_final, y)

    print("ANDROID_MEAN =", list(final_scaler.mean_))
    print("ANDROID_STD =", list(final_scaler.scale_))
    
    np.save(os.path.join(OUT_DIR, 'scaler_mean.npy'), final_scaler.mean_)
    np.save(os.path.join(OUT_DIR, 'scaler_std.npy'), final_scaler.scale_)

    
    

    # Java Export
    print("5. Converting to Java code (GestureClassifier.java)...")
    classes = ['DOWN', 'LEFT', 'RIGHT', 'UP']

    java_code = m2c.export_to_java(final_model)

    with open(
        os.path.join(OUT_DIR, 'GestureClassifier.java'),
        'w'
    ) as f:
        f.write(java_code)
    
    joblib.dump(final_model, os.path.join(OUT_DIR, 'svm_model.pkl'))