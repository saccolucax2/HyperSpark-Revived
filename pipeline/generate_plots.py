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
        print(f"CSV non trovato in: {csv_path}")
        return

    if not graphs_dir.exists():
        graphs_dir.mkdir(parents=True, exist_ok=True)

    df = pd.read_csv(csv_path)

    tier_order = ['Gateway', 'Mid', 'Low']
    instances = df['Instance_ID'].unique()

    print("Inizio Analisi Dati e Generazione Report!\n")

    # Lista unica per memorizzare tutti i risultati statistici
    anova_results = []

    # 1. Boxplot + Swarmplot e Calcolo ANOVA
    for inst in instances:
        subset = df[df['Instance_ID'] == inst]

        if len(subset) > 0:
            # --- 1. BOXPLOT TEMPI DI ESECUZIONE ---
            plt.figure(figsize=(10, 6))
            sns.boxplot(x='Tier', y='Execution_Time', hue='Tier', data=subset,
                        order=tier_order, palette="Set2", legend=False,
                        boxprops=dict(alpha=0.6))

            # Stripplot: i singoli punti delle run
            sns.stripplot(x='Tier', y='Execution_Time', data=subset,
                          order=tier_order, color="black", size=6, alpha=0.7, jitter=True)

            plt.title(f'Performance Distribution across Tiers - {inst}', fontsize=14)
            plt.ylabel('Execution Time (seconds)', fontsize=12)
            plt.xlabel('Edge Tier', fontsize=12)
            plt.grid(axis='y', linestyle='--', alpha=0.7)
            plt.tight_layout()

            plot_path = os.path.join(graphs_dir, f'boxplot_time_{inst}.png')
            plt.savefig(plot_path, dpi=300)
            plt.close()

            # --- 2. BOXPLOT FITNESS (QUALITÀ DELLA SOLUZIONE) ---
            plt.figure(figsize=(10, 6))
            sns.boxplot(x='Tier', y='Fitness', hue='Tier', data=subset,
                        order=tier_order, palette="Set1", legend=False,
                        boxprops=dict(alpha=0.6))

            sns.stripplot(x='Tier', y='Fitness', data=subset,
                          order=tier_order, color="black", size=6, alpha=0.7, jitter=True)

            plt.title(f'Solution Quality (Fitness) Distribution - {inst}', fontsize=14)
            plt.ylabel('Fitness (Objective Value)', fontsize=12)
            plt.xlabel('Edge Tier', fontsize=12)
            plt.grid(axis='y', linestyle='--', alpha=0.7)
            plt.tight_layout()

            fit_plot_path = os.path.join(graphs_dir, f'boxplot_fitness_{inst}.png')
            plt.savefig(fit_plot_path, dpi=300)
            plt.close()

            # --- 3. SCATTER PLOT TRADE-OFF (TIME vs FITNESS) ---
            plt.figure(figsize=(10, 6))
            sns.scatterplot(x='Execution_Time', y='Fitness', hue='Tier', style='Tier',
                            data=subset, hue_order=tier_order, palette="Set1", s=150, alpha=0.8)
            plt.title(f'Edge Trade-off: Latency vs Quality - {inst}', fontsize=14)
            plt.xlabel('Execution Time (seconds) -> Lower is better', fontsize=12)
            plt.ylabel('Fitness -> Higher is better', fontsize=12)
            plt.grid(True, linestyle='--', alpha=0.5)
            plt.tight_layout()

            scatter_path = os.path.join(graphs_dir, f'scatter_tradeoff_{inst}.png')
            plt.savefig(scatter_path, dpi=300)
            plt.close()

            # --- CALCOLO STATISTICO (ANOVA) PER TIME E FITNESS ---

            # ANOVA: Tempi di Esecuzione
            groups_time = [group['Execution_Time'].values for name, group in subset.groupby('Tier')]
            if len(groups_time) == 3 and all(len(g) > 1 for g in groups_time):
                f_val_time, p_val_time = stats.f_oneway(*groups_time)
                anova_results.append({
                    'Instance': inst,
                    'Type': 'Execution Time',
                    'F-Statistic': round(f_val_time, 4),
                    'P-Value': f"{p_val_time:.4e}",
                    'Significant (<0.05)': "Yes" if p_val_time < 0.05 else "No"
                })

            # ANOVA: Fitness
            groups_fit = [group['Fitness'].values for name, group in subset.groupby('Tier')]
            if len(groups_fit) == 3 and all(len(g) > 1 for g in groups_fit):
                f_val_fit, p_val_fit = stats.f_oneway(*groups_fit)
                anova_results.append({
                    'Instance': inst,
                    'Type': 'Fitness',
                    'F-Statistic': round(f_val_fit, 4),
                    'P-Value': f"{p_val_fit:.4e}",
                    'Significant (<0.05)': "Yes" if p_val_fit < 0.05 else "No"
                })

            print(f"Elaborazione e Statistica per {inst} completata.")

    # --- ESPORTAZIONE REPORT CSV ---
    if anova_results:
        anova_df = pd.DataFrame(anova_results)
        # Ordiniamo il dataframe prima per Istanza e poi per Type per una lettura più pulita
        anova_df = anova_df.sort_values(by=['Instance', 'Type'])
        anova_csv_path = os.path.join(graphs_dir, 'anova_statistical_report.csv')
        anova_df.to_csv(anova_csv_path, index=False)
        print(f"\n✅ Report Statistico ANOVA unificato salvato in: {anova_csv_path}")

    # --- 4. SCALABILITY LINEPLOT ---
    if len(instances) > 1:
        plt.figure(figsize=(10, 6))
        sns.lineplot(x='Instance_ID', y='Execution_Time', hue='Tier', data=df,
                     marker='o', hue_order=tier_order, palette="Set2", linewidth=2.5, markersize=10)
        plt.title('Scalability Analysis: Execution Time vs Problem Complexity', fontsize=14)
        plt.ylabel('Average Execution Time (seconds)', fontsize=12)
        plt.xlabel('NRP Instance', fontsize=12)
        plt.grid(True, linestyle='--', alpha=0.7)
        plt.tight_layout()

        scalability_path = os.path.join(graphs_dir, 'scalability_lineplot.png')
        plt.savefig(scalability_path, dpi=300)
        plt.close()
        print(f"Grafico Scalabilità salvato: {scalability_path}")

if __name__ == "__main__":
    generate_reports()