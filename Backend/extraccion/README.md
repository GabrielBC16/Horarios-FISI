# Extractor de PDF

## Requisitos para crear el .venv
- Python 3
- pip
- venv

## Instalación

Desde este directorio:

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -r requirements.txt
```

## Ejecución

```bash
python script_Pdf.py --input "archivo.pdf"
```

El extractor devuelve los registros extraídos en formato JSON por la salida estándar.