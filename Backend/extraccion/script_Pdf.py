import argparse
import json
import sys
from pathlib import Path

import pdfplumber


def entero(valor):
    if valor is None:
        return None
    texto = str(valor).strip()
    return int(texto) if texto.isdigit() else None


def extraer(archivo_pdf: Path):
    registros = []
    with pdfplumber.open(archivo_pdf) as pdf:
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
                    numero_entero = entero(numero)
                    if numero_entero is None:
                        continue
                    registros.append({
                        "numero": numero_entero,
                        "codigo": (codigo or "").strip(),
                        "asignatura": (asignatura or "").strip(),
                        "planes": {
                            "2018": entero(p2018),
                            "2023": entero(p2023),
                            "2015": entero(p2015)
                        }
                    })
    return registros


def main():
    parser = argparse.ArgumentParser(description="Extrae demanda por curso desde un PDF.")
    parser.add_argument("--input", required=True, help="Ruta del PDF de entrada")
    args = parser.parse_args()

    archivo = Path(args.input)
    if not archivo.is_file():
        raise FileNotFoundError(f"No se encontró el archivo: {archivo}")

    json.dump(extraer(archivo), sys.stdout, ensure_ascii=False)


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
