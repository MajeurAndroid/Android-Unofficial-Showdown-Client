package com.majeur.psclient.service

import android.os.Handler
import android.os.Looper
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class ReplayManager(private val showdownService: ShowdownService) {

    var isPaused = false
        private set

    private val battleObserver get() = showdownService.battleMessageObserver

    private var uiHandler = Handler(Looper.getMainLooper())
    private val isWaitingForReplayData = AtomicBoolean(false)

    private var replay: ReplayData? = null

    fun startReplay(replayId : String) {
        if (isWaitingForReplayData.get() || replay != null) return

        val id = replayId.removePrefix("replay-")
        Timber.tag("ReplayManager[LOAD]").i("Loading replay content for $id")
        val url = HttpUrl.Builder().run {
            scheme("https")
            host("replay.pokemonshowdown.com")
            addPathSegment("$id.json")
            build()
        }

        val request = Request.Builder()
                .url(url)
                .build()

        isWaitingForReplayData.set(true)
        showdownService.okHttpClient.newCall(request).enqueue(object : Callback {

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                val rawResponse = response.body()?.string()
                if (rawResponse?.isEmpty() != false) {
                    Timber.e("Replay download request responded with an empty body.")
                    uiHandler.post(this@ReplayManager::notifyReplayDownloadFailure)
                    isWaitingForReplayData.set(false)
                    return
                }
                try {
                    val replayData = ReplayData(JSONObject(rawResponse))
                    uiHandler.post { initReplayRoom(replayData) }
                    isWaitingForReplayData.set(false)
                } catch (e: JSONException) {
                    Timber.e(e,"Error while parsing replay json.")
                    uiHandler.post(this@ReplayManager::notifyReplayDownloadFailure)
                    isWaitingForReplayData.set(false)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Timber.e(e,"Replay download Call failed.")
                uiHandler.post(this@ReplayManager::notifyReplayDownloadFailure)
                isWaitingForReplayData.set(false)
            }
        })
    }

    private fun initReplayRoom(replayData: ReplayData) {
        replay = replayData
        isPaused = false

        val initMessage = MSG_INIT_ROOM.format("replay-${replayData.id}")
        processData(initMessage)

        val logMessage = MSG_BATTLE_LOG.format("replay-${replayData.id}", replayData.log)
        processData(logMessage)
    }

    private fun deinitReplayRoom() {
        if (replay == null) return

        val deinitMessage = MSG_DEINIT_ROOM.format("replay-${replay!!.id}")
        processData(deinitMessage)

        replay = null
    }

    private fun notifyReplayDownloadFailure() {
        processData(MSG_POPUP_REPLAY_DL_FAIL)
    }

    private fun processData(data: String) {
        Timber.tag("ReplayManager[DATA]").i(data)
        showdownService.processServerData(data)
    }

    fun goToNextTurn() {
        if (replay == null) return
        battleObserver.actionQueue.skipToNextTurn()
    }

    fun goToStart() {
        if (replay == null) return
        // TODO This is a bit hacky
        battleObserver.onRoomDeInit()
        battleObserver.onRoomInit()
        val logMessage = MSG_BATTLE_LOG.format("replay-${replay!!.id}", replay!!.log)
        processData(logMessage)
    }

    fun pause() {
        if (replay == null) return
        isPaused = true
        battleObserver.actionQueue.stopLoop()
    }

    fun play() {
        if (replay == null) return
        isPaused = false
        battleObserver.actionQueue.startLoop()
    }

    fun closeReplay() {
        if (replay == null) return
        deinitReplayRoom()
    }

    private class ReplayData(replayData : JSONObject) {
        val id: String = replayData.getString("id")
        val p1: String = replayData.getString("p1")
        val p2: String = replayData.getString("p2")
        val format: String = replayData.getString("format")
        val log: String = replayData.getString("log")
        //  val inputLog: String = replayData.getString("inputlog")
        val uploadTime: Long = replayData.getLong("uploadtime")
        val views: Long = replayData.getLong("views")
        val p1Id: String = replayData.getString("p1id")
        val p2Id: String = replayData.getString("p2id")
        val formatId: String = replayData.getString("formatid")
        // rating
        // private
        // password
    }

    companion object {
        private const val MSG_INIT_ROOM = ">%s\n|init|battle"
        private const val MSG_DEINIT_ROOM = ">%s\n|deinit"
        private const val MSG_BATTLE_LOG = ">%s\n%s"
        private const val MSG_POPUP_REPLAY_DL_FAIL = "|popup|An error occurred when trying to retrieve the replay"
    }
}

