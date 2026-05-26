import os
import pandas as pd
import matplotlib.pyplot as plt
from scipy.signal import savgol_filter

# Yolları terminalın olduğu ana qovluğa görə düzəltdik
DATA_DIR = "Data/Gestures CSV"
OUTPUT_DIR = "Output"

def visualize():
    gestures = ['LEFT', 'RIGHT', 'UP', 'DOWN']
    print("2. Fig. 11 stilində qrafiklər yaradılır...")

    # Output qovluğu yoxdursa yarat
    if not os.path.exists(OUTPUT_DIR):
        os.makedirs(OUTPUT_DIR)

    for gesture in gestures:
        g_dir = os.path.join(DATA_DIR, gesture)
        
        # Qovluğun mövcudluğunu yoxla
        if not os.path.exists(g_dir):
            print(f"Xəbərdarlıq: {g_dir} tapılmadı.")
            continue
            
        files = [f for f in os.listdir(g_dir) if f.endswith('.csv')]
        if not files: 
            continue

        # Müqayisə üçün ilk faylı götürürük
        raw = pd.read_csv(os.path.join(g_dir, files[0]))
        proc = raw.copy()
        
        for col in ['x', 'y', 'z']:
            # Savitzky-Golay (9, 3)
            proc[col] = savgol_filter(proc[col], 9, 3)
            # Gravity Elimination
            proc[col] = proc[col] - proc[col].mean()

        fig, axes = plt.subplots(3, 2, figsize=(14, 10), sharex='col')
        fig.suptitle(f'Fig 11. {gesture}: (a) Before vs (b) After Preprocessing', fontsize=16)
        
        cols = ['x', 'y', 'z']
        for i in range(3):
            # (a) Before - Raw data
            axes[i, 0].plot(raw[cols[i]].values, color='#1f77b4')
            axes[i, 0].set_title(f"{cols[i].upper()} Axis (Raw)")
            
            # (b) After - Processed data
            axes[i, 1].plot(proc[cols[i]].values, color='#1f77b4')
            axes[i, 1].set_title(f"{cols[i].upper()} Axis (Processed)")
            axes[i, 1].axhline(0, color='red', linestyle='--', alpha=0.5)

        plt.tight_layout(rect=[0, 0.03, 1, 0.95])
        
        save_path = os.path.join(OUTPUT_DIR, f'Fig11_{gesture}.png')
        plt.savefig(save_path)
        plt.close()
        print(f"Yarandı: {save_path}")

if __name__ == "__main__":
    visualize()