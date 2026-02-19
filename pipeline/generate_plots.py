import os
import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
from scipy import stats
from pathlib import Path

def generate_reports():
    base_dir = Path(__file__).parent.parent

    csv_path = base_dir / 'data' / 'experiments_results.csv'
    graphs_dir = base_dir / 'data' / 'graphs'

    if not csv_path.exists():
        print(f"CSV not found at: {csv_path}")
        return

    if not graphs_dir.exists():
        graphs_dir.mkdir(parents=True, exist_ok=True)

    df = pd.read_csv(csv_path)

    tier_order = ['Gateway', 'Mid', 'Low']
    instances = df['Instance_ID'].unique()

    print("Starting data Analysis!\n")

    # 1. Boxplot and ANOVA
    for inst in instances:
        subset = df[df['Instance_ID'] == inst]

        if len(subset) > 0:
            plt.figure(figsize=(8, 6))
            sns.boxplot(x='Tier', y='Execution_Time', hue='Tier', data=subset, order=tier_order, palette="Set2", legend=False)
            plt.title(f'Performance Distribution across Tiers - {inst}')
            plt.ylabel('Execution Time (seconds)')
            plt.grid(axis='y', linestyle='--', alpha=0.7)

            plot_path = os.path.join(graphs_dir, f'boxplot_{inst}.png')
            plt.savefig(plot_path)
            plt.close()
            print(f"Graph created: {plot_path}")

            # ANOVA
            groups = [group['Execution_Time'].values for name, group in subset.groupby('Tier')]
            if len(groups) == 3 and all(len(g) > 1 for g in groups):
                f_val, p_val = stats.f_oneway(*groups)
                print(f"   ➤ Test ANOVA for {inst}: p-value = {p_val:.4e}")
                if p_val < 0.05:
                    print("     (Differenza statisticamente significativa!)\n")
                else:
                    print("     (Servono più run per validare la statistica)\n")
            else:
                print(f"   ➤ Salto ANOVA per {inst} (servono almeno 2 run per ogni Tier)\n")

    # 2. SCALABILITY GRAPH
    if len(instances) > 1:
        plt.figure(figsize=(10, 6))
        sns.lineplot(x='Instance_ID', y='Execution_Time', hue='Tier', data=df, marker='o', hue_order=tier_order)
        plt.title('Scalability Analysis: Execution Time vs Problem Complexity')
        plt.ylabel('Average Execution Time (seconds)')
        plt.xlabel('NRP Instance')
        plt.grid(True)

        scale_plot_path = os.path.join(graphs_dir, 'scalability_trend.png')
        plt.savefig(scale_plot_path)
        plt.close()
        print(f"Scalability graph created: {scale_plot_path}")

if __name__ == "__main__":
    generate_reports()