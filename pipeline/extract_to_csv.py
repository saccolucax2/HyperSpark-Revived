import os
import re
import pandas as pd

def update_master_csv(log_folder='data/logs', master_csv='data/experiments_results.csv'):
    log_regex = r"\[(?P<ts>.*?)\] Instance: (?P<inst>.*?) \| Duration: (?P<dur>.*?)s \| Solution: .*?value:(?P<val>\d+)"

    results = []

    if not os.path.exists(log_folder):
        return

    for file in os.listdir(log_folder):
        if file.endswith(".log"):
            tier = "Gateway" if "Jetson" in file else ("Mid" if "RPi4" in file else "Low")
            node_id = file.replace(".log", "")

            with open(os.path.join(log_folder, file), 'r') as f:
                content = f.read()
                for match in re.finditer(log_regex, content):
                    results.append({
                        'Timestamp': match.group('ts'),
                        'Instance_ID': match.group('inst'), # Questo è il tuo "ID" (NRP1, NRP2...)
                        'Tier': tier,
                        'Node_ID': node_id,
                        'Execution_Time': float(match.group('dur')),
                        'Fitness': int(match.group('val'))
                    })

    if results:
        df_new = pd.DataFrame(results)
        if os.path.exists(master_csv):
            df_old = pd.read_csv(master_csv)
            df_master = pd.concat([df_old, df_new]).drop_duplicates(subset=['Timestamp', 'Node_ID'])
        else:
            df_master = df_new

        df_master.to_csv(master_csv, index=False)
        print(f"CSV Mater updated. Total records: {len(df_master)}")

if __name__ == "__main__":
    update_master_csv()