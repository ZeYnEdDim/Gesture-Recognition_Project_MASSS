import os
import pandas as pd
import numpy as np
from scipy.signal import savgol_filter

DATA_DIR = "Data/Gestures CSV"
OUTPUT_DIR = "Output"
OUTPUT_FILE = os.path.join(OUTPUT_DIR, "preprocessed_data.npz")

def process():
    gestures = ['LEFT', 'RIGHT', 'UP', 'DOWN']
    processed_samples = []
    labels = []

    if not os.path.exists(OUTPUT_DIR):
        os.makedirs(OUTPUT_DIR)

    print("1. Starting Preprocessing...")
    
    for gesture in gestures:
        g_dir = os.path.join(DATA_DIR, gesture)
        if not os.path.exists(g_dir):
            print(f"Warning: {g_dir} not found, skipping...")
            continue
        
        files = [f for f in os.listdir(g_dir) if f.endswith('.csv')]
        for f_name in files:
            file_path = os.path.join(g_dir, f_name)
            try:
                df = pd.read_csv(file_path)
                
                # Minimum length check for Savitzky-Golay filter
                if len(df) < 10:
                    continue

                # 1. Savitzky-Golay Smoothing
                for col in ['x', 'y', 'z']:
                    df[col] = savgol_filter(df[col], 9, 3)
                
                # 2. Gravity Elimination (Mean subtraction)
                for col in ['x', 'y', 'z']:
                    df[col] = df[col] - df[col].mean()
                
                processed_samples.append(df)
                labels.append(gesture)
                
            except Exception as e:
                print(f"Error processing {f_name}: {e}")

    # Save processed data
    np.savez(OUTPUT_FILE, 
             data=np.array(processed_samples, dtype=object), 
             labels=labels)
    
    print(f"\nSUCCESSFUL! {len(processed_samples)} files processed.")
    print(f"Output file saved at: {OUTPUT_FILE}")

if __name__ == "__main__":
    process()