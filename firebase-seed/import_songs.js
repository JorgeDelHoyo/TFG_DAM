const fs = require("fs");
const path = require("path");
const admin = require("firebase-admin");

const seedDir = __dirname;
const serviceAccountPath = path.join(seedDir, "serviceAccountKey.json");
const songsPath = path.join(seedDir, "songs.json");

function readJson(filePath, label) {
  if (!fs.existsSync(filePath)) {
    throw new Error(`No existe ${label}: ${filePath}`);
  }

  return JSON.parse(fs.readFileSync(filePath, "utf8"));
}

function validateSong(song, index) {
  const location = song.id ? `cancion '${song.id}'` : `cancion en posicion ${index}`;
  const requiredStringFields = ["id", "title", "artist", "youtubeVideoId", "difficulty"];

  for (const field of requiredStringFields) {
    if (typeof song[field] !== "string" || song[field].trim() === "") {
      throw new Error(`${location}: falta el campo string obligatorio '${field}'`);
    }
  }

  if (!song.tabs || typeof song.tabs !== "object" || Array.isArray(song.tabs)) {
    throw new Error(`${location}: 'tabs' debe ser un mapa, por ejemplo {"guitar":"archivo.gp3"}`);
  }

  if (Object.keys(song.tabs).length === 0) {
    throw new Error(`${location}: 'tabs' no puede estar vacio`);
  }

  if (!Array.isArray(song.tags)) {
    throw new Error(`${location}: 'tags' debe ser un array`);
  }
}

async function main() {
  const serviceAccount = readJson(serviceAccountPath, "serviceAccountKey.json");
  const songs = readJson(songsPath, "songs.json");

  if (!Array.isArray(songs)) {
    throw new Error("songs.json debe contener un array de canciones");
  }

  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });

  const db = admin.firestore();
  const batch = db.batch();

  songs.forEach((song, index) => {
    validateSong(song, index);

    const { id, ...data } = song;
    const docRef = db.collection("canciones").doc(id.trim());

    batch.set(
      docRef,
      {
        ...data,
        title: data.title.trim(),
        artist: data.artist.trim(),
        youtubeVideoId: data.youtubeVideoId.trim(),
        difficulty: data.difficulty.trim(),
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      },
      { merge: true }
    );
  });

  await batch.commit();
  console.log(`Importadas/actualizadas ${songs.length} canciones en la coleccion 'canciones'.`);
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});

