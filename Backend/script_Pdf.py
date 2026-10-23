import pdfplumber
import json

registros = []

with pdfplumber.open("sistemas.pdf") as pdf:
    for pagina in pdf.pages:
        tablas = pagina.extract_tables({
            "vertical_strategy": "lines",
            "horizontal_strategy": "lines"
        })
        for tabla in tablas:
            for fila in tabla:
                if not fila or len(fila) != 6:
                    continue
                numero, codigo, asignatura, p2018, p2023, p2015 = fila
                if not numero or not numero.strip().isdigit():
                    continue
                registro = {
                    "numero": int(numero),
                    "codigo": codigo,
                    "asignatura": asignatura,
                    "planes": {
                        "2018": int(p2018) if p2018 and p2018.strip().isdigit() else None,
                        "2023": int(p2023) if p2023 and p2023.strip().isdigit() else None,
                        "2015": int(p2015) if p2015 and p2015.strip().isdigit() else None
                    }
                }

                registros.append(registro)
with open("cursos-vacantes.json", "w", encoding="utf-8") as archivo:
    json.dump(registros, archivo, ensure_ascii=False, indent=2)