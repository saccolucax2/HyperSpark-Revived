use floria::{
    Directory, InMemoryStore, InMemoryStoreBackend, Store, Vertex, VertexTemplate
};
use std::sync::Arc;
use std::fs;
use std::fs::File;
use std::io::{Read, Write};

struct NodeData {
    id: String,
    role: String,
    time_str: String,
    seconds: f64,
}

fn main() {
    println!("🚀 Avvio HyperSpark AUTOMATED Dashboard...");

    // 1. SETUP STORE
    let backend = Arc::new(InMemoryStoreBackend::default());
    let store = InMemoryStore::new(backend);

    // ==========================================
    // 2. SCANSIONE AUTOMATICA DEI LOG
    // ==========================================
    let results_path = "../edge_results";
    println!("📂 Scansione cartella: {}", results_path);

    let mut nodes_found: Vec<NodeData> = Vec::new();

    match fs::read_dir(results_path) {
        Ok(entries) => {
            for entry in entries {
                let entry = entry.expect("Errore lettura file");
                let path = entry.path();

                if path.extension().and_then(|s| s.to_str()) == Some("log") {
                    let filename = path.file_stem().unwrap().to_str().unwrap().to_string();

                    let mut file = File::open(&path).expect("Impossibile aprire file log");
                    let mut contents = String::new();
                    file.read_to_string(&mut contents).expect("Impossibile leggere log");

                    let mut time_val = 0.0;
                    let mut time_display = "Running...".to_string();

                    if let Some(idx) = contents.rfind("took ") {
                        let substr = &contents[idx + 5..];
                        let end_idx = substr.find(" s").unwrap_or(substr.len());
                        let num_str = &substr[..end_idx].trim();

                        if let Ok(val) = num_str.parse::<f64>() {
                            time_val = val;
                            time_display = format!("{:.2}s", val);
                        }
                    }

                    let role = if filename.to_lowercase().contains("jetson") || filename.to_lowercase().contains("gateway") {
                        "HighEndGateway"
                    } else if filename.to_lowercase().contains("rpi4") || filename.to_lowercase().contains("mid") {
                        "MidEndDevice"
                    } else {
                        "LowEndSensor"
                    };

                    println!("   Found -> ID: {} | Role: {} | Time: {}", filename, role, time_display);

                    nodes_found.push(NodeData {
                        id: filename,
                        role: role.to_string(),
                        time_str: time_display,
                        seconds: time_val,
                    });
                }
            }
        }
        Err(e) => println!("❌ ERRORE: Non trovo la cartella edge_results! ({})", e),
    }

    if nodes_found.is_empty() {
        println!("⚠️ Nessun dato trovato. Assicurati di aver lanciato Docker prima!");
        return;
    }

    // ==========================================
    // 3. GENERAZIONE TOPOLOGIA FLORIA (Logica)
    // ==========================================
    let t_gw = VertexTemplate::new(Directory::default(), &store).expect("E");
    let id_t_gw = t_gw.template.id.clone();
    store.add_vertex_template(t_gw).expect("E");

    let t_mid = VertexTemplate::new(Directory::default(), &store).expect("E");
    let id_t_mid = t_mid.template.id.clone();
    store.add_vertex_template(t_mid).expect("E");

    let t_low = VertexTemplate::new(Directory::default(), &store).expect("E");
    let id_t_low = t_low.template.id.clone();
    store.add_vertex_template(t_low).expect("E");

    for node in &nodes_found {
        let template_id = match node.role.as_str() {
            "HighEndGateway" => id_t_gw.clone(),
            "MidEndDevice" => id_t_mid.clone(),
            _ => id_t_low.clone(),
        };
        let v = Vertex::new(Directory::default(), template_id, &store).expect("Err V");
        store.add_vertex(v).expect("Err Add V");
    }

    let gateway_time = nodes_found.iter()
        .find(|n| n.role == "HighEndGateway")
        .map(|n| n.seconds)
        .unwrap_or(1.0); // Evitiamo divisione per zero se manca il gateway

    println!("📊 Gateway Baseline: {:.2}s", gateway_time);

    // ==========================================
    // 4. GENERAZIONE FILE DOT (FIX STRINGS)
    // ==========================================

    // Invece di una macro gigante, costruiamo la stringa pezzo per pezzo. È più sicuro.
    let mut dot_output = String::new();

    // HEADER
    dot_output.push_str("digraph HyperSparkLive {\n");
    dot_output.push_str("    rankdir=BT;\n");
    dot_output.push_str("    dpi=300;\n");
    dot_output.push_str("    bgcolor=\"#2b2b2b\";\n");
    dot_output.push_str("    node [shape=note, style=filled, fontname=\"Segoe UI\", fontcolor=\"white\", fontsize=12];\n");
    dot_output.push_str("    edge [color=\"white\", fontcolor=\"#aaaaaa\", fontsize=10];\n\n");

    // INTESTAZIONE GRAFICO
    dot_output.push_str("    subgraph cluster_header {\n");
    dot_output.push_str("        style=invis;\n");
    dot_output.push_str("        Header [label=\"HyperSpark Live Dashboard\\nGenerated by Floria Engine\", shape=plaintext, fillcolor=\"none\", fontsize=16];\n");
    dot_output.push_str("    }\n\n");

    // NODI
    let gateway_id = nodes_found.iter().find(|n| n.role == "HighEndGateway")
        .map(|n| n.id.clone()).unwrap_or("UnknownGW".to_string());

    for node in &nodes_found {
        let label_text: String;
        let color: &str;
        let font_color = "white"; // Default

        if node.role == "HighEndGateway" {
            // IL GATEWAY È LA BASELINE
            color = "#2e7d32"; // Verde Scuro Professionale
            label_text = format!("{}\\nTime: {}\\n(BASELINE)", node.id, node.time_str);
        } else {
            // CALCOLO DEGRADAZIONE PRECISA
            // Formula: ((NodeTime - GatewayTime) / GatewayTime) * 100
            let degradation_pct = ((node.seconds - gateway_time) / gateway_time) * 100.0;

            // Logica colori dinamica basata sulla percentuale reale
            if degradation_pct > 300.0 {
                color = "#c62828"; // Rosso Scuro (Critico)
            } else if degradation_pct > 100.0 {
                color = "#ef6c00"; // Arancione (Warning)
            } else {
                color = "#f9a825"; // Giallo (Low degradation)
            }

            label_text = format!("{}\\nTime: {}\\nDegradation: +{:.1}%", node.id, node.time_str, degradation_pct);
        }

        // Scrittura nodo
        let node_line = format!(
            "    \"{}\" [label=\"{}\", fillcolor=\"{}\", fontcolor=\"{}\"];\n",
            node.id, label_text, color, font_color
        );
        dot_output.push_str(&node_line);

        // Scrittura arco (se non è il gateway stesso)
        if node.id != gateway_id {
            dot_output.push_str(&format!("    \"{}\" -> \"{}\" [color=\"#777777\"];\n", node.id, gateway_id));
        }
    }

    dot_output.push_str("}\n");

    // SCRITTURA FILE SICURA
    let path_out = "topology_live.dot";
    let mut file = File::create(path_out).expect("Err create file");
    file.write_all(dot_output.as_bytes()).expect("Err write file");

    // Forza il flush per assicurarsi che il file sia chiuso bene
    file.flush().expect("Err flush");

    println!("✅ GRAFO SCIENTIFICO GENERATO: {}", path_out);
    println!("👉 Apri il file con il plugin 'DOT/Graphviz' (NON Syncfusion!)");
}