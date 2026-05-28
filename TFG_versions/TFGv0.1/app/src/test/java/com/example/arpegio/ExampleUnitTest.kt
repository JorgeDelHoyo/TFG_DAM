package com.example.arpegio

import com.example.arpegio.data.local.Converters
import com.example.arpegio.data.model.Song
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

/**
 * Pruebas unitarias para las clases auxiliares y modelos del proyecto:
 * - [Song] (Modelo principal)
 * - [Converters] (Convertidores de tipo para Room)
 */
class ExampleUnitTest {

    // ────────────────────────────────────────────────────────────────────────
    // Pruebas para el Modelo Song
    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun song_getYouTubeUrl_isCorrect() {
        val song = Song(youtubeVideoId = "dQw4w9WgXcQ")
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", song.getYouTubeUrl())
    }

    @Test
    fun song_hasTabFor_isCorrect() {
        val song = Song(tabs = mapOf("Guitarra" to "guitar.gp3", "Bajo" to "bass.gp3"))
        
        assertTrue(song.hasTabFor("Guitarra"))
        assertTrue(song.hasTabFor("Bajo"))
        assertFalse(song.hasTabFor("Batería"))
    }

    @Test
    fun song_defaultValues_areCorrect() {
        val song = Song()
        assertEquals("", song.id)
        assertEquals("", song.title)
        assertEquals("", song.artist)
        assertEquals("", song.youtubeVideoId)
        assertTrue(song.tabs.isEmpty())
        assertEquals("intermediate", song.difficulty)
        assertTrue(song.tags.isEmpty())
        assertNull(song.createdAt)
        assertFalse(song.isLocal)
    }

    @Test
    fun song_customConstructor_isCorrect() {
        val date = Date()
        val song = Song(
            id = "test_id",
            title = "Test Title",
            artist = "Test Artist",
            youtubeVideoId = "vid_id",
            tabs = mapOf("Ukelele" to "uke.gp3"),
            difficulty = "advanced",
            tags = listOf("chill"),
            createdAt = date,
            isLocal = true
        )

        assertEquals("test_id", song.id)
        assertEquals("Test Title", song.title)
        assertEquals("Test Artist", song.artist)
        assertEquals("vid_id", song.youtubeVideoId)
        assertEquals("uke.gp3", song.tabs["Ukelele"])
        assertEquals("advanced", song.difficulty)
        assertEquals("chill", song.tags[0])
        assertEquals(date, song.createdAt)
        assertTrue(song.isLocal)
    }

    // ────────────────────────────────────────────────────────────────────────
    // Pruebas para Converters de Base de Datos Room
    // ────────────────────────────────────────────────────────────────────────

    private val converters = Converters()

    @Test
    fun converters_mapSerialization_isCorrect() {
        val map = mapOf("Guitarra" to "path/to/guitar.gp3", "Piano" to "path/to/piano.gp3")
        val json = converters.fromStringMap(map)
        
        assertNotNull(json)
        assertTrue(json.contains("Guitarra"))
        assertTrue(json.contains("Piano"))

        val deserialized = converters.toStringMap(json)
        assertEquals(2, deserialized.size)
        assertEquals("path/to/guitar.gp3", deserialized["Guitarra"])
        assertEquals("path/to/piano.gp3", deserialized["Piano"])
    }

    @Test
    fun converters_mapSerializationNull_isCorrect() {
        val json = converters.fromStringMap(null)
        assertEquals("null", json)

        val deserialized = converters.toStringMap("null")
        assertTrue(deserialized.isEmpty())
    }

    @Test
    fun converters_listSerialization_isCorrect() {
        val list = listOf("metal", "hardcore", "instrumental")
        val json = converters.fromStringList(list)

        assertNotNull(json)
        assertTrue(json.contains("metal"))

        val deserialized = converters.toStringList(json)
        assertEquals(3, deserialized.size)
        assertEquals("metal", deserialized[0])
        assertEquals("hardcore", deserialized[1])
        assertEquals("instrumental", deserialized[2])
    }

    @Test
    fun converters_listSerializationNull_isCorrect() {
        val json = converters.fromStringList(null)
        assertEquals("null", json)

        val deserialized = converters.toStringList("null")
        assertTrue(deserialized.isEmpty())
    }

    @Test
    fun converters_dateSerialization_isCorrect() {
        val now = Date()
        val timestamp = converters.dateToTimestamp(now)

        assertNotNull(timestamp)
        assertEquals(now.time, timestamp)

        val deserialized = converters.fromTimestamp(timestamp)
        assertNotNull(deserialized)
        assertEquals(now, deserialized)
    }

    @Test
    fun converters_dateSerializationNull_isCorrect() {
        assertNull(converters.dateToTimestamp(null))
        assertNull(converters.fromTimestamp(null))
    }
}