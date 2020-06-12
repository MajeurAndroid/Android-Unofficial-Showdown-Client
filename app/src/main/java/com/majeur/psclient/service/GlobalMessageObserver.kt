package com.majeur.psclient.service

import android.util.Log
import com.majeur.psclient.model.AvailableBattleRoomsInfo
import com.majeur.psclient.model.BattleFormat
import com.majeur.psclient.model.RoomInfo
import com.majeur.psclient.util.Utils
import org.json.JSONException
import org.json.JSONObject
import java.util.*

abstract class GlobalMessageObserver : AbsMessageObserver() {

    val TAG = GlobalMessageObserver::class.java.simpleName

    override var observedRoomId: String? = "lobby"

    init {
        interceptCommandBefore = setOf("init", "noinit")
        interceptCommandAfter = setOf("deinit")
    }

    var isUserGuest: Boolean = true
        private set

    private var requestServerCountsOnly = false
    private val privateMessages = mutableMapOf<String, MutableList<String>>()

    public override fun onMessage(message: ServerMessage) {
        message.newArgsIteration()
        when (message.command) {
            "connected" -> onConnectedToServer()
            "challstr" -> processChallengeString(message)
            "updateuser" -> processUpdateUser(message)
            "queryresponse" -> processQueryResponse(message)
            "formats" -> processAvailableFormats(message)
            "popup" -> handlePopup(message)
            "updatesearch" -> handleUpdateSearch(message)
            "pm" -> handlePm(message)
            "updatechallenges" -> handleChallenges(message)
            "networkerror" -> onNetworkError()
            "init" -> onRoomInit(message.roomId, message.nextArg)
            "deinit" -> onRoomDeinit(message.roomId)
            "noinit" -> {
                if (message.hasNextArg && "nonexistent" == message.nextArg && message.hasNextArg)
                    onShowPopup(message.nextArg)
            }
            "nametaken" -> {
                message.nextArg // Skipping name
                onShowPopup(message.nextArg)
            }
            "usercount" -> {
            }
        }
    }

    private fun processChallengeString(msg: ServerMessage) {
        service?.putSharedData("challenge", msg.remainingArgsRaw)
        service?.tryCookieSignIn()
    }

    private fun processUpdateUser(msg: ServerMessage) {
        var username = msg.nextArg
        service?.putSharedData("myusername", username)
        val userType = username.substring(0, 1)
        username = username.substring(1)
        val isGuest = "0" == msg.nextArg
        var avatar = msg.nextArg
        avatar = "000$avatar".substring(avatar.length)
        isUserGuest = isGuest
        onUserChanged(username, isGuest, avatar)

        // Update server counts (active battle and active users)
        service?.let {
            requestServerCountsOnly = true
            it.sendGlobalCommand("cmd", "rooms")
        }

        // onSearchBattlesChanged(new String[0], new String[0], new String[0]); TODO Wtf was this call ?
    }

    private fun processQueryResponse(msg: ServerMessage) {
        val query = msg.nextArg
        val queryContent = msg.nextArg
        when (query) {
            "rooms" -> processRoomsQueryResponse(queryContent)
            "roomlist" -> processRoomListQueryResponse(queryContent)
            "savereplay" -> {
            }
            "userdetails" -> processUserDetailsQueryResponse(queryContent)
            else -> Log.w(TAG, "Command queryresponse not handled, type=$query")
        }
    }

    private fun processRoomsQueryResponse(queryContent: String) {
        if (queryContent == "null") return
        try {
            val jsonObject = JSONObject(queryContent)
            val userCount = jsonObject.getInt("userCount")
            val battleCount = jsonObject.getInt("battleCount")
            onUpdateCounts(userCount, battleCount)
            if (requestServerCountsOnly) {
                requestServerCountsOnly = false
                return
            }
            var jsonArray = jsonObject.getJSONArray("official")
            val officialRooms = mutableListOf<RoomInfo>()
            for (i in 0 until jsonArray.length()) {
                val roomJson = jsonArray.getJSONObject(i)
                officialRooms.add(
                        RoomInfo(roomJson.getString("title"),
                                roomJson.getString("desc"),
                                roomJson.getInt("userCount")))
            }
            jsonArray = jsonObject.getJSONArray("chat")
            val chatRooms = mutableListOf<RoomInfo>()
            for (i in 0 until jsonArray.length()) {
                val roomJson = jsonArray.getJSONObject(i)
                chatRooms.add(
                        RoomInfo(roomJson.getString("title"),
                                roomJson.getString("desc"),
                                roomJson.getInt("userCount")))
            }
            onAvailableRoomsChanged(officialRooms, chatRooms)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun processRoomListQueryResponse(queryContent: String) {
//        if (queryContent == "null") return
//        try {
//            JSONObject(queryContent)
//        } catch (e: JSONException) {
//            e.printStackTrace()
//        }
    }

    private fun processUserDetailsQueryResponse(queryContent: String) {
        try {
            val jsonObject = JSONObject(queryContent)
            val userId = jsonObject.optString("userid")
            if (userId.isBlank()) return
            val name = jsonObject.optString("name")
            val online = jsonObject.has("status")
            val group = jsonObject.optString("group")
            val chatRooms = mutableListOf<String>()
            val battles = mutableListOf<String>()
            (jsonObject.opt("rooms") as? JSONObject)?.keys()?.forEach {
                if (it.startsWith("battle-"))
                    battles.add(it)
                else
                    chatRooms.add(it)
            }
            onUserDetails(userId, name, online, group, chatRooms, battles)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun processAvailableFormats(msg: ServerMessage) {
        val rawText: String = msg.remainingArgsRaw
        val categories: MutableList<BattleFormat.Category> = LinkedList() // /!\ needs to impl Serializable

        rawText.split("|,").forEach { rawCategory ->
            val catName = rawCategory.substringAfter("|").substringBefore("|")
            val formats = rawCategory.substringAfter(catName).split("|")
                    .filter { s -> s.isNotBlank() }
                    .map { s ->
                        BattleFormat(s.substringBefore(","), s.substringAfter(",").toInt(16))
                    }
            BattleFormat.Category().apply {
                label = catName
                battleFormats = formats
            }.also {
                categories.add(it)
            }
        }
        service?.putSharedData("formats", categories)
        onBattleFormatsChanged(categories)
    }

    private fun handlePopup(msg: ServerMessage) = onShowPopup(msg.args.filter { it.isNotBlank() }.joinToString("\n"))

    private fun handleUpdateSearch(msg: ServerMessage) {
        val jsonObject = Utils.jsonObject(msg.remainingArgsRaw) ?: return

        val searching = mutableListOf<String>()
        val searchingJson = jsonObject.optJSONArray("searching")
        searchingJson?.let {
            for (i in 0 until searchingJson.length())
                searching.add(searchingJson.getString(i))
        }

        val games = mutableMapOf<String, String>()
        val gamesJson = jsonObject.optJSONObject("games")
        gamesJson?.let {
            gamesJson.keys().forEach {
                games.put(it, gamesJson.getString(it))
            }
        }
        onSearchBattlesChanged(searching, games)
    }

    private fun handlePm(msg: ServerMessage) {
        val from = msg.nextArg.substring(1)
        val to = msg.nextArg.substring(1)
        val myUsername = service?.getSharedData<String>("myusername")?.substring(1)
        val with = if (myUsername == from) to else from
        var content = msg.nextArgSafe
        if (content != null && (content.startsWith("/raw") || content.startsWith("/html") || content.startsWith("/uhtml")))
            content = "Html messages not supported in pm."
        val message = "$from: $content"
        val messages = privateMessages.getOrPut(with, { mutableListOf<String>() })
        messages.add(message)
        onNewPrivateMessage(with, message)
    }

    private fun handleChallenges(message: ServerMessage) {
        val rawJson: String = message.remainingArgsRaw
        var to: String? = null
        var format: String? = null
        val from = mutableMapOf<String, String>()

        val jsonObject = Utils.jsonObject(rawJson)
        val challengeTo = jsonObject.optJSONObject("challengeTo")
        challengeTo?.let {
            to = it.getString("to")
            format = it.getString("format")
        }
        val challengesFrom = jsonObject.optJSONObject("challengesFrom")
        challengesFrom?.let {
            it.keys().forEach { key ->
                from[key] = challengesFrom.getString(key)
            }
        }
        onChallengesChange(to, format, from)
    }

    fun getPrivateMessages(with: String): List<String>? {
        return privateMessages[with]
    }

    protected abstract fun onConnectedToServer()
    protected abstract fun onUserChanged(userName: String, isGuest: Boolean, avatarId: String)
    protected abstract fun onUpdateCounts(userCount: Int, battleCount: Int)
    protected abstract fun onBattleFormatsChanged(battleFormats: List<@JvmSuppressWildcards BattleFormat.Category>)
    protected abstract fun onSearchBattlesChanged(searching: List<String>, games: Map<String, String>)
    protected abstract fun onUserDetails(id: String, name: String, online: Boolean, group: String, rooms: List<String>, battles: List<String>)
    protected abstract fun onShowPopup(message: String)
    protected abstract fun onAvailableRoomsChanged(officialRooms: List<@JvmSuppressWildcards RoomInfo>, chatRooms: List<@JvmSuppressWildcards RoomInfo>)
    protected abstract fun onAvailableBattleRoomsChanged(availableRoomsInfo: AvailableBattleRoomsInfo)
    protected abstract fun onNewPrivateMessage(with: String, message: String)
    protected abstract fun onChallengesChange(to: String?, format: String?, from: Map<String, String>)
    protected abstract fun onRoomInit(roomId: String, type: String)
    protected abstract fun onRoomDeinit(roomId: String)
    protected abstract fun onNetworkError()
}
