use std::fs::{self, File};
use std::io::{BufWriter, Read, Write};
use regex::Regex;
use glob::glob;

#[derive(Debug, Clone)]
struct NodeResult {
    id: String,
    time_seconds: f64,
    is_gateway: bool,
}

fn main() -> std::io::Result<()> {
    println!("🚀 Starting HyperSpark Dashboard...");

    // ========================================================
    // 1. PTH CONFIGURATION
    // ========================================================
    let results_path = "../data/logs/*.log";
    let output_dot_path = "topology_live.dot";
    let temp_dot_path = "topology_live.tmp";

    let mut nodes: Vec<NodeResult> = Vec::new();
    let re_time = Regex::new(r"took\s+(\d+(\.\d+)?)\s*s").unwrap();

    // ========================================================
    // 2. LOG READING
    // ========================================================
    println!("📂 Scanning logs...");
    let paths = glob(results_path).expect("Failed to read glob pattern");

    for entry in paths {
        match entry {
            Ok(path) => {
                let filename = path.file_stem().unwrap().to_string_lossy().to_string();
                let mut content = String::new();

                if let Ok(mut file) = File::open(&path) {
                    if file.read_to_string(&mut content).is_ok() {
                        if let Some(caps) = re_time.captures(&content) {
                            let seconds: f64 = caps[1].parse().unwrap_or(0.0);
                            let is_gateway = filename.to_lowercase().contains("jetson") ||
                                filename.to_lowercase().contains("gateway");

                            nodes.push(NodeResult {
                                id: filename,
                                time_seconds: seconds,
                                is_gateway,
                            });
                        }
                    }
                }
            },
            Err(e) => println!("⚠️ Error accessing file: {:?}", e),
        }
    }

    if nodes.is_empty() {
        println!("⚠️ No logs found.");
        return Ok(());
    }

    // ========================================================
    // 3. BASELINE DEFINITION
    // ========================================================
    let gateway_time = nodes.iter()
        .find(|n| n.is_gateway)
        .map(|n| n.time_seconds)
        .unwrap_or_else(|| {
            nodes.iter().map(|n| n.time_seconds).fold(f64::INFINITY, |a, b| a.min(b))
        });

    println!("📊 Baseline (Gateway): {:.4}s", gateway_time);

    // ========================================================
    // 4. DOT GENERATION
    // ========================================================

    let file = File::create(temp_dot_path)?;
    let mut writer = BufWriter::new(file);

    writeln!(writer, "digraph HyperSparkTopology {{")?;
    writeln!(writer, "    graph [dpi=300, fontname=\"Arial\", rankdir=LR, splines=ortho, nodesep=0.6, ranksep=1.2, bgcolor=\"#ffffff\"];")?;
    writeln!(writer, "    node [shape=none, fontname=\"Arial\"];")?;
    writeln!(writer, "    edge [fontname=\"Arial\", penwidth=1.2, color=\"#888888\"];")?;
    writeln!(writer, "    labelloc=\"t\";")?;
    writeln!(writer, "    label=\"HyperSpark Dashboard: Real-Time Edge Performance\";")?;

    for node in &nodes {
        let degradation = ((node.time_seconds - gateway_time) / gateway_time) * 100.0;

        let (bgcolor, status_text, text_color) = if node.is_gateway {
            ("#DCEDC8", "BASELINE (Gateway)".to_string(), "black")
        } else if degradation < 10.0 {
            ("#E0F7FA", format!("Performance: Good (+{:.1}%)", degradation), "black")
        } else if degradation < 100.0 {
            ("#FFF9C4", format!("Degradation: Medium (+{:.1}%)", degradation), "black")
        } else {
            ("#FFCDD2", format!("CRITICAL LAG (+{:.1}%)", degradation), "black")
        };

        let label_html = format!(
            "<<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"6\" BGCOLOR=\"{}\">
            <TR><TD BORDER=\"0\"><B><FONT POINT-SIZE=\"14\">{}</FONT></B></TD></TR>
            <TR><TD BORDER=\"0\" ALIGN=\"LEFT\">Exec Time: {:.3}s</TD></TR>
            <TR><TD BORDER=\"0\" ALIGN=\"LEFT\"><FONT COLOR=\"{}\"><B>{}</B></FONT></TD></TR>
            </TABLE>>",
            bgcolor, node.id, node.time_seconds, text_color, status_text
        );

        writeln!(writer, "    \"{}\" [label={}, group=main];", node.id, label_html)?;

        if !node.is_gateway {
            let multiplier = node.time_seconds / gateway_time;
            writeln!(writer, "    \"{}\" -> \"Jetson_Nano\" [taillabel=\"{:.1}x slower\", labeldistance=3.8, labelangle=25, style=dashed];", node.id, multiplier)?;
        }
    }

    writeln!(writer, "}}")?;

    writer.flush()?;
    drop(writer);

    fs::rename(temp_dot_path, output_dot_path)?;

    println!("✅ Dashboard updated: {} ", output_dot_path);
    Ok(())
}