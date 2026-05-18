# Importador de canciones

Herramienta local para crear o actualizar documentos en la coleccion `canciones` de Firestore sin hacerlo campo a campo desde Firebase Console.

## Uso

1. Descarga la clave desde Firebase Console:
   Project settings -> Service accounts -> Generate new private key.

2. Copia esa clave en esta carpeta y renombrala a:

   ```text
   serviceAccountKey.json
   ```

3. Copia el ejemplo:

   ```powershell
   Copy-Item songs.example.json songs.json
   ```

4. Edita `songs.json` con tus canciones.

5. Instala dependencias:

   ```powershell
   npm install
   ```

6. Importa las canciones:

   ```powershell
   npm run import
   ```

## Formato

```json
{
  "id": "killer_queen",
  "title": "Killer Queen",
  "artist": "Queen",
  "youtubeVideoId": "2ZBtPf7FOoM",
  "tabs": {
    "guitar": "queen-killer_queen.gp3"
  },
  "difficulty": "intermediate",
  "tags": ["queen", "rock"]
}
```

El campo `id` sera el ID del documento en Firestore. Si vuelves a ejecutar el importador con el mismo `id`, actualiza ese documento manteniendo otros campos no indicados gracias a `merge: true`.

La clave `serviceAccountKey.json` esta ignorada por Git. No la subas nunca a GitHub ni la metas en la app Android.

