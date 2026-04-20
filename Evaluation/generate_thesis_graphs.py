import os
import matplotlib.pyplot as plt
import numpy as np
import seaborn as sns

# TASK 1: SETUP & MOCK DATA
hardware_metrics = {
    'Flagship': {'TTP': 800, 'TTV': 40},
    'Mid-Range': {'TTP': 1500, 'TTV': 65},
    'Budget': {'TTP': 3200, 'TTV': 110}
}

payload_bytes = {
    'ZK Proof': 215,
    'Public Signals': 64,
    'Nonce': 32,
    'Signature': 72,
    'Public Key': 64
}

scan_success = {
    'Low': {10: 100, 20: 98, 30: 85, 40: 60, 50: 30},
    'High': {10: 100, 20: 100, 30: 95, 40: 85, 50: 65}
}

# Apply clean academic style
try:
    plt.style.use('seaborn-v0_8-whitegrid')
except OSError:
    try:
        plt.style.use('seaborn-whitegrid')
    except OSError:
        pass # Fallback to default if style is not found

# Output directory for saving the figures
output_dir = os.path.dirname(os.path.abspath(__file__))

def add_value_labels(ax, spacing=5):
    """Add labels to the end of each bar in a bar chart."""
    for rect in ax.patches:
        y_value = rect.get_height()
        x_value = rect.get_x() + rect.get_width() / 2
        space = spacing
        va = 'bottom'
        if y_value < 0:
            space *= -1
            va = 'top'
        if y_value > 0:
            label = "{:.0f}".format(y_value)
            ax.annotate(
                label,                      
                (x_value, y_value),         
                xytext=(0, space),          
                textcoords="offset points", 
                ha='center',                
                va=va,
                fontsize=10,
                fontweight='bold')

# TASK 2: GRAPH 1 - CRYPTOGRAPHIC ASYMMETRY (Bar Chart)
def generate_asymmetry_chart():
    labels = list(hardware_metrics.keys())
    ttp_means = [hardware_metrics[l]['TTP'] for l in labels]
    ttv_means = [hardware_metrics[l]['TTV'] for l in labels]

    x = np.arange(len(labels))  # the label locations
    width = 0.35  # the width of the bars

    fig, ax = plt.subplots(figsize=(9, 6))
    
    # Use dark blue for proving, light blue/grey for verification
    rects1 = ax.bar(x - width/2, ttp_means, width, label='Proving (TTP)', color='#1f497d') 
    rects2 = ax.bar(x + width/2, ttv_means, width, label='Verification (TTV)', color='#c6d9f1')

    ax.set_ylabel('Time (ms)', fontsize=12)
    ax.set_title('Client-Side Proving vs. Verification Asymmetry', pad=20, fontsize=14, fontweight='bold')
    ax.set_xticks(x)
    ax.set_xticklabels(labels, fontsize=11)
    ax.legend(fontsize=11)

    add_value_labels(ax)

    plt.tight_layout()
    plt.savefig(os.path.join(output_dir, 'fig1_asymmetry.png'), dpi=300)
    plt.close()

# TASK 3: GRAPH 2 - PAYLOAD BYTE BREAKDOWN (Donut Chart)
def generate_payload_chart():
    labels = list(payload_bytes.keys())
    sizes = list(payload_bytes.values())
    total_bytes = sum(sizes)
    
    # Define a custom color palette
    colors = sns.color_palette('pastel')[0:len(labels)]

    fig, ax = plt.subplots(figsize=(8, 6))
    
    # Create donut chart
    wedges, texts, autotexts = ax.pie(
        sizes, 
        labels=labels, 
        autopct=lambda pct: f"{int(round(pct * total_bytes / 100.0))} B",
        startangle=140, 
        colors=colors, 
        pctdistance=0.75,
        textprops=dict(color="black", fontweight="bold")
    )
    
    # Draw circle for donut shape to leave center empty
    centre_circle = plt.Circle((0,0),0.55,fc='white')
    fig.gca().add_artist(centre_circle)
    
    # Add a text label in the center about the limit
    ax.text(0, 0, f"Total Size:\n{total_bytes} B\n\n(Limit: ~2000 B)", 
            ha='center', va='center', fontsize=12, fontweight='bold')
    
    ax.axis('equal')  
    plt.title('ZK-Credential Payload Structure (Bytes)', pad=20, fontsize=14, fontweight='bold')
    plt.tight_layout()
    plt.savefig(os.path.join(output_dir, 'fig2_payload.png'), dpi=300)
    plt.close()

# TASK 4: GRAPH 3 - DISTANCE DECAY CURVE (Line Chart)
def generate_reliability_chart():
    distances_low = list(scan_success['Low'].keys())
    success_low = list(scan_success['Low'].values())
    
    distances_high = list(scan_success['High'].keys())
    success_high = list(scan_success['High'].values())

    fig, ax = plt.subplots(figsize=(9, 6))
    
    ax.plot(distances_low, success_low, marker='o', linestyle='-', color='#d73027', 
            label='Low Error Correction (Level.L)', markersize=8, linewidth=2)
    ax.plot(distances_high, success_high, marker='s', linestyle='-', color='#1a9850', 
            label='High Error Correction (Level.H)', markersize=8, linewidth=2)

    ax.set_xlabel('Physical Distance (cm)', fontsize=12)
    ax.set_ylabel('Scan Success Rate (%)', fontsize=12)
    ax.set_title('QR Transport Reliability by Distance and Density', pad=20, fontsize=14, fontweight='bold')
    ax.set_ylim(0, 110)
    ax.set_xticks(distances_low)
    
    # Ensure grid is visible (may be redundant with seaborn style, but added to be safe)
    ax.yaxis.grid(True, linestyle='--', which='major', color='grey', alpha=0.3)
    ax.xaxis.grid(True, linestyle='--', which='major', color='grey', alpha=0.3)
    
    ax.legend(fontsize=11)
    plt.tight_layout()
    plt.savefig(os.path.join(output_dir, 'fig3_reliability.png'), dpi=300)
    plt.close()

if __name__ == '__main__':
    generate_asymmetry_chart()
    generate_payload_chart()
    generate_reliability_chart()
    print("Graphs generated successfully in:", output_dir)
