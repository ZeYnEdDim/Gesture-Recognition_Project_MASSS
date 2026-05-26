import numpy as np
import os
import m2cgen as m2c
import joblib
from sklearn.model_selection import StratifiedKFold, cross_val_score, train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import classification_report, confusion_matrix
from scipy.stats import entropy
from scipy.fftpack import fft

def extract_features(data):
    features = []
    for df in data:
        mean = df[['x', 'y', 'z']].mean().values
        std = df[['x', 'y', 'z']].std().values
        rms = np.sqrt((df[['x', 'y', 'z']]**2).mean().values)
        sma = (abs(df['x']).sum() + abs(df['y']).sum() + abs(df['z']).sum()) / len(df)
        min_v, max_v = df[['x', 'y', 'z']].min().values, df[['x', 'y', 'z']].max().values

        energy, spec_entropy = [], []
        for col in ['x', 'y', 'z']:
            sig = df[col].values
            sig_fft = fft(sig)
            energy.append(np.sum(np.abs(sig_fft)**2) / len(sig))
            psd = np.abs(sig_fft)**2
            psd_norm = psd / (np.sum(psd) + 1e-9)
            spec_entropy.append(entropy(psd_norm))

        v = np.concatenate([mean, std, rms, [sma], min_v, max_v, energy, spec_entropy])
        features.append(v)
    return np.array(features)

if __name__ == "__main__":
    IN = 'Output/preprocessed_data.npz'
    OUT_DIR = 'Output'

    if not os.path.exists(IN):
        exit("Error: Run preprocess.py first!")

    print("1. Extracting Features...")
    d = np.load(IN, allow_pickle=True)
    X, y = extract_features(d['data']), d['labels']

    print("\n2. Starting K-Fold Cross-Validation (Random Forest)...")
    rf_model = RandomForestClassifier(n_estimators=100, random_state=42)
    kfold = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
    cv_scores = cross_val_score(rf_model, X, y, cv=kfold)
    print(f"K-Fold Accuracy: {cv_scores.mean():.2f} (+/- {cv_scores.std()*2:.2f})")

    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)
    rf_model.fit(X_train, y_train)
    print("\n--- 80/20 SPLIT REPORT ---")
    print(classification_report(y_test, rf_model.predict(X_test)))
    print("Confusion Matrix:\n", confusion_matrix(y_test, rf_model.predict(X_test)))

    print("\n3. Training Final Random Forest Model...")
    rf_model.fit(X, y)

    print("4. Exporting to Java...")
    java_code = m2c.export_to_java(rf_model)
    with open(os.path.join(OUT_DIR, 'GestureClassifier.java'), 'w') as f:
        f.write(java_code)
    
    joblib.dump(rf_model, os.path.join(OUT_DIR, 'rf_model.pkl'))
    print("DONE! Random Forest version is ready.")