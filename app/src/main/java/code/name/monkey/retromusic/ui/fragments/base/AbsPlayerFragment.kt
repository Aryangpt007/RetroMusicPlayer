package code.name.monkey.retromusic.ui.fragments.base

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import code.name.monkey.appthemehelper.ThemeStore
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.dialogs.*
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.interfaces.PaletteColorHolder
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.ui.activities.tageditor.AbsTagEditorActivity
import code.name.monkey.retromusic.ui.activities.tageditor.SongTagEditorActivity
import code.name.monkey.retromusic.ui.fragments.player.PlayerAlbumCoverFragment
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.util.NavigationUtil
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.util.RetroUtil
import code.name.monkey.retromusic.views.FitSystemWindowsLayout

abstract class AbsPlayerFragment : AbsMusicServiceFragment(), Toolbar.OnMenuItemClickListener, PaletteColorHolder, PlayerAlbumCoverFragment.Callbacks {
    var callbacks: Callbacks? = null
        private set
    private var updateIsFavoriteTask: AsyncTask<*, *, *>? = null


    override fun onAttach(context: Context?) {
        super.onAttach(context)
        try {
            callbacks = context as Callbacks?
        } catch (e: ClassCastException) {
            throw RuntimeException(context!!.javaClass.simpleName + " must implement " + Callbacks::class.java.simpleName)
        }

    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        val song = MusicPlayerRemote.currentSong
        when (item.itemId) {
            R.id.action_toggle_favorite -> {
                toggleFavorite(song)
                return true
            }
            R.id.action_share -> {
                if (fragmentManager != null) {
                    SongShareDialog.create(song).show(fragmentManager!!, "SHARE_SONG")
                }
                return true
            }
            R.id.action_delete_from_device -> {
                DeleteSongsDialog.create(song)
                        .show(activity!!.supportFragmentManager, "DELETE_SONGS")
                return true
            }
            R.id.action_add_to_playlist -> {
                if (fragmentManager != null) {
                    AddToPlaylistDialog.create(song).show(fragmentManager!!, "ADD_PLAYLIST")
                }
                return true
            }
            R.id.action_clear_playing_queue -> {
                MusicPlayerRemote.clearQueue()
                return true
            }
            R.id.action_save_playing_queue -> {
                CreatePlaylistDialog.create(MusicPlayerRemote.playingQueue)
                        .show(activity!!.supportFragmentManager, "ADD_TO_PLAYLIST")
                return true
            }
            R.id.action_tag_editor -> {
                val intent = Intent(activity, SongTagEditorActivity::class.java)
                intent.putExtra(AbsTagEditorActivity.EXTRA_ID, song.id)
                startActivity(intent)
                return true
            }
            R.id.action_details -> {
                if (fragmentManager != null) {
                    SongDetailDialog.create(song).show(fragmentManager!!, "SONG_DETAIL")
                }
                return true
            }
            R.id.action_go_to_album -> {
                NavigationUtil.goToAlbum(activity!!, song.albumId)
                return true
            }
            R.id.action_go_to_artist -> {
                NavigationUtil.goToArtist(activity!!, song.artistId)
                return true
            }
            R.id.now_playing -> {
                NavigationUtil.goToPlayingQueue(activity!!)
                return true
            }
            R.id.action_show_lyrics -> {
                NavigationUtil.goToLyrics(activity!!)
                return true
            }
            R.id.action_equalizer -> {
                NavigationUtil.openEqualizer(activity!!)
                return true
            }
            R.id.action_sleep_timer -> {
                SleepTimerDialog().show(fragmentManager!!, TAG)
                return true
            }
            R.id.action_set_as_ringtone -> {
                MusicUtil.setRingtone(activity!!, song.id)
                return true
            }
            R.id.action_settings -> {
                NavigationUtil.goToSettings(activity!!)
                return true
            }
            R.id.action_go_to_genre -> {
                val retriever = MediaMetadataRetriever()
                val trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id.toLong())
                retriever.setDataSource(activity, trackUri)
                var genre: String? = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                if (genre == null) {
                    genre = "Not Specified"
                }
                Toast.makeText(context, genre, Toast.LENGTH_SHORT).show()
                return true
            }
        }
        return false
    }

    protected open fun toggleFavorite(song: Song) {
        MusicUtil.toggleFavorite(activity!!, song)
    }

    abstract fun toolbarGet(): Toolbar

    abstract fun onShow()

    abstract fun onHide()

    abstract fun onBackPressed(): Boolean

    abstract fun toolbarIconColor(): Int

    override fun onServiceConnected() {
        updateIsFavorite()
    }

    override fun onPlayingMetaChanged() {
        updateIsFavorite()
    }

    override fun onDestroyView() {
        if (updateIsFavoriteTask != null && !updateIsFavoriteTask!!.isCancelled) {
            updateIsFavoriteTask!!.cancel(true)
        }
        super.onDestroyView()
    }

    @SuppressLint("StaticFieldLeak")
    fun updateIsFavorite() {
        if (updateIsFavoriteTask != null) {
            updateIsFavoriteTask!!.cancel(false)
        }
        updateIsFavoriteTask = object : AsyncTask<Song, Void, Boolean>() {
            override fun doInBackground(vararg params: Song): Boolean? {
                val activity = activity
                if (activity != null) {
                    return MusicUtil.isFavorite(getActivity()!!, params[0])
                } else {
                    cancel(false)
                    return null
                }
            }

            override fun onPostExecute(isFavorite: Boolean?) {
                val activity = activity
                if (activity != null) {
                    val res = if (isFavorite!!)
                        R.drawable.ic_favorite_white_24dp
                    else
                        R.drawable.ic_favorite_border_white_24dp
                    val drawable = RetroUtil.getTintedVectorDrawable(activity, res, toolbarIconColor())
                    toolbarGet().menu.findItem(R.id.action_toggle_favorite).setIcon(drawable).title = if (isFavorite) getString(R.string.action_remove_from_favorites) else getString(R.string.action_add_to_favorites)
                }
            }
        }.execute(MusicPlayerRemote.currentSong)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(ThemeStore.primaryColor(activity!!))
        if (PreferenceUtil.getInstance().fullScreenMode) {
            if (view.findViewById<View>(R.id.status_bar) != null)
                view.findViewById<View>(R.id.status_bar).visibility = View.GONE
        }
    }

    fun setSafeArea(safeArea: View) {
        val layout = safeArea.findViewById<FitSystemWindowsLayout>(R.id.safeArea)
        if (layout != null) {
            layout.isFit = !PreferenceUtil.getInstance().fullScreenMode
        }
    }

    interface Callbacks {

        fun onPaletteColorChanged()
    }

    companion object {
        val TAG: String = AbsPlayerFragment::class.java.simpleName
    }

}
