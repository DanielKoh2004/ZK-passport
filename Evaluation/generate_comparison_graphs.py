import os
import matplotlib.pyplot as plt
import numpy as np

# Theoretical Proof Sizes in BYTES
protocols = {
    'zk-SNARK\n(Groth16)': 256,       # ~256 Bytes (Constant)
    'zk-SNARK\n(PLONK)': 1024,        # ~1 KB (Constant)
    'Bulletproofs': 1500,             # ~1.5 KB (Logarithmic scale for small circuits)
    'zk-STARK': 45000                 # ~45 KB to 100+ KB (Logarithmic)
}

# The absolute maximum bytes a single QR Code can hold
QR_CODE_MAX_LIMIT = 2953 

output_dir = os.path.dirname(os.path.abspath(__file__))

def generate_comparison_graph():
    # Apply clean academic style
    try:
        plt.style.use('seaborn-v0_8-whitegrid')
    except OSError:
        pass 

    labels = list(protocols.keys())
    sizes = list(protocols.values())

    fig, ax = plt.subplots(figsize=(9, 6))

    # Define colors based on viability (Green for safe, Orange for close, Red for impossible)
    colors = ['#1a9850', '#fdae61', '#fdae61', '#d73027']

    bars = ax.bar(labels, sizes, color=colors, width=0.5)

    # Add a bold RED line for the QR Code Limit
    ax.axhline(y=QR_CODE_MAX_LIMIT, color='red', linestyle='--', linewidth=2.5, label='Maximum QR Code Capacity (~2.9 KB)')

    # Because STARKs are 45,000 bytes and SNARKs are 256 bytes, a linear scale hides the SNARK bar completely. 
    # Use a Logarithmic Y-Axis so all bars are visible and the difference is obvious.
    ax.set_yscale('log')

    ax.set_ylabel('Proof Size on Logarithmic Scale (Bytes)', fontsize=12)
    ax.set_title('Zero-Knowledge Protocol Comparison by Payload Size', pad=20, fontsize=14, fontweight='bold')

    # Data Labels above bars
    for bar in bars:
        yval = bar.get_height()
        # Format commas for thousands (e.g., 45,000)
        ax.text(bar.get_x() + bar.get_width()/2.0, yval * 1.15, f"{yval:,} B", ha='center', va='bottom', fontweight='bold')

    ax.legend(loc='upper left', fontsize=11)

    plt.tight_layout()
    # Save the output
    plt.savefig(os.path.join(output_dir, 'fig4_protocol_comparison.png'), dpi=300)
    plt.close()

if __name__ == '__main__':
    generate_comparison_graph()
    print("Justification Graph generated successfully in:", output_dir)
