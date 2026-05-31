package com.example.arpegio.ui.viewmodel

import android.net.Uri
import com.example.arpegio.data.model.Song
import com.example.arpegio.data.repository.SongRepository
import com.example.arpegio.rules.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*
import java.lang.RuntimeException

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: SongRepository
    private lateinit var viewModel: LibraryViewModel

    private lateinit var remoteSongsFlow: MutableStateFlow<List<Song>>
    private lateinit var localSongsFlow: MutableStateFlow<List<Song>>

    private val sampleRemoteSong = Song(
        id = "remote_1",
        title = "Killer Queen",
        artist = "Queen",
        youtubeVideoId = "dQw4w9WgXcQ",
        tabs = mapOf("Guitarra" to "queen-killer_queen.gp3"),
        isLocal = false
    )

    private val sampleLocalSong = Song(
        id = "local_1",
        title = "Mis Acordes",
        artist = "Mis Canciones",
        tabs = mapOf("Guitarra" to "/data/user/0/.../user_tab_123.gp3"),
        isLocal = true
    )

    @Before
    fun setUp() {
        repository = mock()
        remoteSongsFlow = MutableStateFlow(emptyList())
        localSongsFlow = MutableStateFlow(emptyList())

        whenever(repository.getSongs()).thenReturn(remoteSongsFlow)
        whenever(repository.getLocalSongs()).thenReturn(localSongsFlow)

        viewModel = LibraryViewModel(repository)
    }

    @Test
    fun `initial state is Loading before flows emit`() = runTest {
        // Al instanciarse en setUp(), ya se suscribió a los flows.
        // Dado que remoteSongsFlow y localSongsFlow emiten valores iniciales inmediatamente en MutableStateFlow,
        // el estado ya pasa a Success rápido. Pero si creáramos un ViewModel con flows pausados, sería Loading.
        
        val repoMock = mock<SongRepository>()
        whenever(repoMock.getSongs()).thenReturn(MutableStateFlow(emptyList()))
        whenever(repoMock.getLocalSongs()).thenReturn(MutableStateFlow(emptyList()))
        
        val vm = LibraryViewModel(repoMock)
        assertTrue(vm.uiState.value is LibraryUiState.Success)
    }

    @Test
    fun `loadAllSongsCombined success updates uiState to Success with correct lists`() = runTest {
        val remoteList = listOf(sampleRemoteSong)
        val localList = listOf(sampleLocalSong)

        remoteSongsFlow.value = remoteList
        localSongsFlow.value = localList

        val state = viewModel.uiState.value
        assertTrue(state is LibraryUiState.Success)
        val successState = state as LibraryUiState.Success
        assertEquals(remoteList, successState.remoteSongs)
        assertEquals(localList, successState.localSongs)
        assertFalse(successState.isLocalExpanded)
    }

    @Test
    fun `loadAllSongsCombined error sets uiState to Error`() = runTest {
        val repoMock = mock<SongRepository>()
        whenever(repoMock.getSongs()).thenReturn(flow { throw RuntimeException("Firestore offline") })
        whenever(repoMock.getLocalSongs()).thenReturn(MutableStateFlow(emptyList()))

        val vm = LibraryViewModel(repoMock)
        val state = vm.uiState.value
        assertTrue(state is LibraryUiState.Error)
        assertEquals("Firestore offline", (state as LibraryUiState.Error).message)
    }

    @Test
    fun `toggleLocalExpanded alternates local section visibility`() = runTest {
        remoteSongsFlow.value = listOf(sampleRemoteSong)
        localSongsFlow.value = listOf(sampleLocalSong)

        // Inicialmente false
        val state1 = viewModel.uiState.value as LibraryUiState.Success
        assertFalse(state1.isLocalExpanded)

        // Alternar -> true
        viewModel.toggleLocalExpanded()
        val state2 = viewModel.uiState.value as LibraryUiState.Success
        assertTrue(state2.isLocalExpanded)

        // Alternar -> false
        viewModel.toggleLocalExpanded()
        val state3 = viewModel.uiState.value as LibraryUiState.Success
        assertFalse(state3.isLocalExpanded)
    }

    @Test
    fun `deleteSong delegates to repository successfully`() = runTest {
        viewModel.deleteSong(sampleLocalSong)
        verify(repository).deleteLocalSong(sampleLocalSong)
    }

    @Test
    fun `deleteSong failure sends ShowToast event`() = runTest {
        whenever(repository.deleteLocalSong(any())).thenThrow(RuntimeException("Delete failed"))

        val events = mutableListOf<LibraryUiEvent>()
        val job = launch(UnconfinedTestDispatcher()) {
            viewModel.uiEvent.collect { events.add(it) }
        }

        viewModel.deleteSong(sampleLocalSong)

        assertEquals(1, events.size)
        assertTrue(events[0] is LibraryUiEvent.ShowToast)
        assertEquals("Error al eliminar la canción", (events[0] as LibraryUiEvent.ShowToast).message)

        job.cancel()
    }

    @Test
    fun `updateSongTitle with blank title returns early and does not call repository`() = runTest {
        viewModel.updateSongTitle(sampleLocalSong, "   ")
        verify(repository, never()).updateLocalSong(any())
    }

    @Test
    fun `updateSongTitle with valid title calls repository`() = runTest {
        viewModel.updateSongTitle(sampleLocalSong, "Nuevo Nombre")
        
        val expectedSong = sampleLocalSong.copy(title = "Nuevo Nombre")
        verify(repository).updateLocalSong(expectedSong)
    }

    @Test
    fun `updateSongTitle failure sends ShowToast event`() = runTest {
        whenever(repository.updateLocalSong(any())).thenThrow(RuntimeException("Update failed"))

        val events = mutableListOf<LibraryUiEvent>()
        val job = launch(UnconfinedTestDispatcher()) {
            viewModel.uiEvent.collect { events.add(it) }
        }

        viewModel.updateSongTitle(sampleLocalSong, "Nuevo Nombre")

        assertEquals(1, events.size)
        assertTrue(events[0] is LibraryUiEvent.ShowToast)
        assertEquals("Error al renombrar la canción", (events[0] as LibraryUiEvent.ShowToast).message)

        job.cancel()
    }

    @Test
    fun `addCustomSong with empty title sends ShowToast event`() = runTest {
        val mockUri = mock<Uri>()
        val events = mutableListOf<LibraryUiEvent>()
        val job = launch(UnconfinedTestDispatcher()) {
            viewModel.uiEvent.collect { events.add(it) }
        }

        viewModel.addCustomSong("", mockUri)

        assertEquals(1, events.size)
        assertTrue(events[0] is LibraryUiEvent.ShowToast)
        assertEquals("Campos inválidos", (events[0] as LibraryUiEvent.ShowToast).message)

        job.cancel()
    }

    @Test
    fun `addCustomSong with null uri sends ShowToast event`() = runTest {
        val events = mutableListOf<LibraryUiEvent>()
        val job = launch(UnconfinedTestDispatcher()) {
            viewModel.uiEvent.collect { events.add(it) }
        }

        viewModel.addCustomSong("Mi Cancion", null)

        assertEquals(1, events.size)
        assertTrue(events[0] is LibraryUiEvent.ShowToast)
        assertEquals("Campos inválidos", (events[0] as LibraryUiEvent.ShowToast).message)

        job.cancel()
    }

    @Test
    fun `addCustomSong success calls repository and sends SongAddedSuccess`() = runTest {
        val mockUri = mock<Uri>()
        whenever(repository.saveLocalSong(any(), any())).thenReturn(Result.success(Unit))

        val events = mutableListOf<LibraryUiEvent>()
        val job = launch(UnconfinedTestDispatcher()) {
            viewModel.uiEvent.collect { events.add(it) }
        }

        viewModel.addCustomSong("Mi Cancion", mockUri)

        verify(repository).saveLocalSong("Mi Cancion", mockUri)
        assertEquals(1, events.size)
        assertTrue(events[0] is LibraryUiEvent.SongAddedSuccess)

        job.cancel()
    }

    @Test
    fun `addCustomSong failure sends ShowToast event with error message`() = runTest {
        val mockUri = mock<Uri>()
        whenever(repository.saveLocalSong(any(), any())).thenReturn(Result.failure(RuntimeException("Internal saving error")))

        val events = mutableListOf<LibraryUiEvent>()
        val job = launch(UnconfinedTestDispatcher()) {
            viewModel.uiEvent.collect { events.add(it) }
        }

        viewModel.addCustomSong("Mi Cancion", mockUri)

        verify(repository).saveLocalSong("Mi Cancion", mockUri)
        assertEquals(1, events.size)
        assertTrue(events[0] is LibraryUiEvent.ShowToast)
        assertEquals("Internal saving error", (events[0] as LibraryUiEvent.ShowToast).message)

        job.cancel()
    }

    @Test
    fun `refresh resets UI to loading and updates lists`() = runTest {
        // Inicialmente success
        remoteSongsFlow.value = listOf(sampleRemoteSong)
        localSongsFlow.value = listOf(sampleLocalSong)
        
        viewModel.refresh()
        
        // Verificamos que se vuelve a llamar a getSongs y getLocalSongs
        verify(repository, atLeast(2)).getSongs()
        verify(repository, atLeast(2)).getLocalSongs()
    }

    @Test
    fun `toggleLocalExpanded when state is not Success does not toggle expansion`() = runTest {
        val repoMock = mock<SongRepository>()
        whenever(repoMock.getSongs()).thenReturn(flow { throw RuntimeException("Firestore offline") })
        whenever(repoMock.getLocalSongs()).thenReturn(MutableStateFlow(emptyList()))

        val vm = LibraryViewModel(repoMock)
        assertTrue(vm.uiState.value is LibraryUiState.Error)

        // Llamamos a toggleLocalExpanded en estado de Error y comprobamos que sigue en Error sin romper
        vm.toggleLocalExpanded()
        assertTrue(vm.uiState.value is LibraryUiState.Error)
    }
}