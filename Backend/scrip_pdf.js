
import * as pdfjsLib from "pdfjs-dist/legacy/build/pdf.mjs";
import fs from "node:fs/promises";
import path from "node:path";

const INPUT = "./sistemas.pdf";
const OUTPUT = "./asignaturas.json";
const TOLERANCIA_Y = 3;

function numeroONull(valor) {
  const texto = String(valor ?? "").trim();
  return /^\d+$/.test(texto) ? Number(texto) : null;
}

// Leer todos los elementos de texto de una página.
async function leerPagina(pagina) {
  const contenido = await pagina.getTextContent();

  return contenido.items
    .filter(item => typeof item.str === "string")
    .filter(item => item.str.trim() !== "")
    .map(item => ({
      texto: item.str.trim(),
      x: item.transform[4],
      y: item.transform[5],
      ancho: item.width ?? 0
    }));
}

// Agrupar elementos por coordenada Y.
function agruparFilas(elementos) {
  const ordenados = [...elementos].sort(
    (a, b) => b.y - a.y || a.x - b.x
  );

  const filas = [];

  for (const elemento of ordenados) {
    const fila = filas.find(
      f => Math.abs(f.y - elemento.y) <= TOLERANCIA_Y
    );

    if (fila) {
      fila.elementos.push(elemento);
    } else {
      filas.push({
        y: elemento.y,
        elementos: [elemento]
      });
    }
  }

  return filas.map(fila => ({
    y: fila.y,
    elementos: fila.elementos.sort((a, b) => a.x - b.x)
  }));
}

// Encontrar las posiciones X de las seis columnas.
// Se obtiene a partir de la cabecera real del PDF.
function detectarColumnas(filas) {
  for (const fila of filas) {
    const elementos = fila.elementos;

    const buscar = (regex) =>
      elementos.find(e => regex.test(e.texto));

    const numero = buscar(/^N[°º.]?$/i);
    const codigo = buscar(/^C[oó]digo$/i);
    const asignatura = buscar(/^Asignatura$/i);
    const p2018 = buscar(/^2018$/);
    const p2023 = buscar(/^2023$/);
    const p2015 = buscar(/^2015$/);

    if (
      numero && codigo && asignatura &&
      p2018 && p2023 && p2015
    ) {
      return [
        numero.x,
        codigo.x,
        asignatura.x,
        p2018.x,
        p2023.x,
        p2015.x
      ];
    }
  }

  return null;
}

// Convertir posiciones X en límites entre columnas.
function obtenerLimites(columnas) {
  const limites = [-Infinity];

  for (let i = 0; i < columnas.length - 1; i++) {
    limites.push((columnas[i] + columnas[i + 1]) / 2);
  }

  limites.push(Infinity);
  return limites;
}

// Identificar a qué columna pertenece cada elemento.
function obtenerIndiceColumna(x, limites) {
  for (let i = 0; i < limites.length - 1; i++) {
    if (x >= limites[i] && x < limites[i + 1]) {
      return i;
    }
  }
  return -1;
}

// Convertir una fila visual en seis celdas.
function convertirFilaACeldas(fila, limites) {
  const celdas = Array.from({ length: 6 }, () => []);

  for (const elemento of fila.elementos) {
    const indice = obtenerIndiceColumna(elemento.x, limites);

    if (indice >= 0) {
      celdas[indice].push(elemento.texto);
    }
  }

  return celdas.map(partes => partes.join(" ").trim());
}

// Comprobar que se trata de una fila de asignatura.
function esRegistroValido(celdas) {
  const [numero, codigo, asignatura] = celdas;

  return (
    /^\d+$/.test(numero) &&
    /^[A-Z0-9]+$/i.test(codigo) &&
    asignatura.length > 0
  );
}

function crearRegistro(celdas) {
  const [
    numero,
    codigo,
    asignatura,
    p2018,
    p2023,
    p2015
  ] = celdas;

  return {
    numero: Number(numero),
    codigo,
    asignatura: asignatura.replace(/\s+/g, " "),
    planes: {
      "2018": numeroONull(p2018),
      "2023": numeroONull(p2023),
      "2015": numeroONull(p2015)
    }
  };
}

async function extraerPDF() {
  const buffer = await fs.readFile(INPUT);

  const pdf = await pdfjsLib.getDocument({
    data: new Uint8Array(buffer),
    useSystemFonts: true
  }).promise;

  const registros = [];
  let columnas = null;

  console.log(`Páginas encontradas: ${pdf.numPages}`);

  for (let paginaNum = 1; paginaNum <= pdf.numPages; paginaNum++) {
    const pagina = await pdf.getPage(paginaNum);

    const elementos = await leerPagina(pagina);
    const filas = agruparFilas(elementos);

    // En algunas páginas no se repite la cabecera.
    const detectadas = detectarColumnas(filas);
    if (detectadas) columnas = detectadas;

    if (!columnas) {
      console.warn(`Página ${paginaNum}: columnas no detectadas`);
      continue;
    }

    const limites = obtenerLimites(columnas);
    let cantidad = 0;

    for (const fila of filas) {
      const celdas = convertirFilaACeldas(fila, limites);

      if (!esRegistroValido(celdas)) continue;

      registros.push(crearRegistro(celdas));
      cantidad++;
    }

    console.log(`Página ${paginaNum}: ${cantidad} registros`);
  }

  // Eliminar posibles duplicados por número correlativo.
  const unicos = [
    ...new Map(
      registros.map(registro => [registro.numero, registro])
    ).values()
  ].sort((a, b) => a.numero - b.numero);

  await fs.mkdir(path.dirname(OUTPUT), {
    recursive: true
  });

  await fs.writeFile(
    OUTPUT,
    JSON.stringify(unicos, null, 2),
    "utf8"
  );

  console.log(`Total de registros: ${unicos.length}`);
  console.log(`Archivo generado: ${OUTPUT}`);
}

extraerPDF().catch(console.error);
