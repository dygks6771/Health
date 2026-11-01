package com.example.health.game

import org.json.JSONArray
import org.json.JSONObject

private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
    (0 until length()).map { transform(getJSONObject(it)) }

private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }

private fun JSONObject.intOrNull(key: String): Int? = if (isNull(key)) null else getInt(key)

private fun JSONObject.longOrNull(key: String): Long? = if (isNull(key)) null else getLong(key)

private fun JSONObject.stringOrNull(key: String): String? = if (isNull(key)) null else getString(key)

data class StatInfo(val code: String, val name: String, val base: Int, val bonus: Int, val total: Int) {
    companion object {
        fun from(j: JSONObject) = StatInfo(
            j.getString("stat"), j.getString("name"), j.getInt("base"), j.getInt("bonus"), j.getInt("total"),
        )
    }
}

data class ItemInfo(
    val id: Long,
    val slot: String,
    val slotName: String,
    val name: String,
    val gradeName: String,
    val tier: Int,
    val statName: String,
    val value: Int,
    val subStatName: String?,
    val subValue: Int,
    val enhance: Int,
    val equipped: Boolean,
    val listed: Boolean,
    val sellPrice: Long,
    val nextEnhanceRate: Int?,
    val nextEnhanceStones: Int?,
    val nextEnhanceGold: Long?,
) {
    /** 예: "+3 숲지기의 대검" */
    val displayName: String get() = (if (enhance > 0) "+$enhance " else "") + name

    val statText: String
        get() = "$statName +$value" + (if (subStatName != null && subValue > 0) " · $subStatName +$subValue" else "")

    companion object {
        fun from(j: JSONObject) = ItemInfo(
            id = j.getLong("id"),
            slot = j.getString("slot"),
            slotName = j.getString("slotName"),
            name = j.getString("name"),
            gradeName = j.getString("gradeName"),
            tier = j.getInt("tier"),
            statName = j.getString("statName"),
            value = j.getInt("value"),
            subStatName = j.stringOrNull("subStatName"),
            subValue = j.getInt("subValue"),
            enhance = j.getInt("enhance"),
            equipped = j.getBoolean("equipped"),
            listed = j.optBoolean("listed", false),
            sellPrice = j.getLong("sellPrice"),
            nextEnhanceRate = j.intOrNull("nextEnhanceRate"),
            nextEnhanceStones = j.intOrNull("nextEnhanceStones"),
            nextEnhanceGold = j.longOrNull("nextEnhanceGold"),
        )

        fun listFrom(a: JSONArray): List<ItemInfo> = a.mapObjects(::from)
    }
}

/** 내 영웅 (로비) */
data class HeroInfo(
    val id: Long,
    val name: String,
    val job: String,
    val jobName: String,
    val jobRole: String,
    val appearance: Int,
    val level: Int,
    val expInLevel: Long,
    val expToNextLevel: Long,
    val gold: Long,
    val stamina: Int,
    val maxStamina: Int,
    val statPoints: Int,
    val enhanceStones: Int,
    val power: Int,
    val stats: List<StatInfo>,
    val equipment: List<ItemInfo>,
    val todayKm: Double,
    /** 오늘 실제 보상받은 거리 (속도 초과·하루 상한 제외) */
    val todayRewardKm: Double,
    val weekKm: Double,
) {
    val expProgress: Float get() = if (expToNextLevel <= 0) 0f else expInLevel.toFloat() / expToNextLevel
    val staminaProgress: Float get() = if (maxStamina <= 0) 0f else stamina.toFloat() / maxStamina
    val totalStats: Int get() = stats.sumOf { it.total }

    companion object {
        fun from(j: JSONObject) = HeroInfo(
            id = j.getLong("id"),
            name = j.getString("name"),
            job = j.getString("job"),
            jobName = j.getString("jobName"),
            jobRole = j.getString("jobRole"),
            appearance = j.getInt("appearance"),
            level = j.getInt("level"),
            expInLevel = j.getLong("expInLevel"),
            expToNextLevel = j.getLong("expToNextLevel"),
            gold = j.getLong("gold"),
            stamina = j.getInt("stamina"),
            maxStamina = j.getInt("maxStamina"),
            statPoints = j.getInt("statPoints"),
            enhanceStones = j.getInt("enhanceStones"),
            power = j.getInt("power"),
            stats = j.getJSONArray("stats").mapObjects(StatInfo::from),
            equipment = ItemInfo.listFrom(j.getJSONArray("equipment")),
            todayKm = j.getDouble("todayKm"),
            todayRewardKm = j.optDouble("todayRewardKm", j.getDouble("todayKm")),
            weekKm = j.getDouble("weekKm"),
        )
    }
}

/** 러닝 기록 1건과 그 보상 */
data class RunHistory(
    val runDate: String,
    val startTime: String,
    val distanceKm: Double,
    val countedKm: Double,
    val expAwarded: Int,
    val goldAwarded: Int,
    val rejectedReason: String?,
) {
    companion object {
        fun listFrom(j: JSONObject): List<RunHistory> = j.getJSONArray("runs").mapObjects {
            RunHistory(
                it.getString("runDate"), it.getString("startTime"), it.getDouble("distanceKm"), it.getDouble("countedKm"),
                it.getInt("expAwarded"), it.getInt("goldAwarded"), it.stringOrNull("rejectedReason"),
            )
        }
    }
}

/** 친구 등 다른 영웅 요약 */
data class HeroSummary(
    val id: Long,
    val name: String,
    val job: String,
    val jobName: String,
    val appearance: Int,
    val level: Int,
    val power: Int,
) {
    companion object {
        fun from(j: JSONObject) = HeroSummary(
            j.getLong("id"), j.getString("name"), j.getString("job"), j.getString("jobName"),
            j.getInt("appearance"), j.getInt("level"), j.getInt("power"),
        )
    }
}

data class JobInfo(
    val job: String,
    val name: String,
    val role: String,
    val primaryName: String,
    val secondaryName: String,
    val description: String,
)

data class GameMeta(val jobs: List<JobInfo>, val appearances: List<String>) {
    companion object {
        fun from(j: JSONObject) = GameMeta(
            jobs = j.getJSONArray("jobs").mapObjects {
                JobInfo(
                    it.getString("job"), it.getString("name"), it.getString("role"),
                    it.getString("primaryName"), it.getString("secondaryName"), it.getString("description"),
                )
            },
            appearances = j.getJSONArray("appearances").mapObjects { it.getString("name") },
        )
    }
}

data class RunReward(
    val newRuns: Int,
    val countedKm: Double,
    val expGained: Long,
    val goldGained: Long,
    val staminaGained: Int,
    val levelBefore: Int,
    val levelAfter: Int,
    val statPointsGained: Int,
    val rejected: List<String>,
) {
    val leveledUp: Boolean get() = levelAfter > levelBefore

    companion object {
        fun from(j: JSONObject) = RunReward(
            newRuns = j.getInt("newRuns"),
            countedKm = j.getDouble("countedKm"),
            expGained = j.getLong("expGained"),
            goldGained = j.getLong("goldGained"),
            staminaGained = j.getInt("staminaGained"),
            levelBefore = j.getInt("levelBefore"),
            levelAfter = j.getInt("levelAfter"),
            statPointsGained = j.getInt("statPointsGained"),
            rejected = j.getJSONArray("rejected").strings(),
        )
    }
}

data class HuntInfo(
    val slot: Int,
    val difficulty: String,
    val difficultyName: String,
    val monster: String,
    val regionName: String,
    val targetLevel: Int,
    val requiredPower: Int,
    val rewardGold: Int,
    val staminaCost: Int,
    val cleared: Boolean,
    val available: Boolean,
    val reasons: List<String>,
) {
    companion object {
        fun from(j: JSONObject) = HuntInfo(
            j.getInt("slot"), j.getString("difficulty"), j.getString("difficultyName"), j.getString("monster"),
            j.getString("regionName"), j.getInt("targetLevel"), j.getInt("requiredPower"), j.getInt("rewardGold"),
            j.getInt("staminaCost"), j.getBoolean("cleared"), j.getBoolean("available"), j.getJSONArray("reasons").strings(),
        )
    }
}

data class GuildBoard(val power: Int, val hunts: List<HuntInfo>) {
    companion object {
        fun from(j: JSONObject) = GuildBoard(j.getInt("power"), j.getJSONArray("hunts").mapObjects(HuntInfo::from))
    }
}

data class RegionInfo(
    val tier: Int,
    val name: String,
    val description: String,
    val dungeonName: String,
    val boss: String,
    val minLevel: Int,
    val minTotalStats: Int,
    val staminaCost: Int,
    val rewardStones: Int,
    val rewardGold: Long,
    val dropRate: Int,
    val dropGrades: String,
    val clears: Long,
    val available: Boolean,
    val reasons: List<String>,
) {
    companion object {
        fun listFrom(j: JSONObject): List<RegionInfo> = j.getJSONArray("regions").mapObjects {
            val d = it.getJSONObject("dungeon")
            RegionInfo(
                tier = it.getInt("tier"),
                name = it.getString("name"),
                description = it.getString("description"),
                dungeonName = d.getString("name"),
                boss = d.optString("boss", d.getString("name")),
                minLevel = d.getInt("minLevel"),
                minTotalStats = d.getInt("minTotalStats"),
                staminaCost = d.getInt("staminaCost"),
                rewardStones = d.getInt("rewardStones"),
                rewardGold = d.getLong("rewardGold"),
                dropRate = d.getInt("dropRate"),
                dropGrades = d.getString("dropGrades"),
                clears = d.getLong("clears"),
                available = d.getBoolean("available"),
                reasons = d.getJSONArray("reasons").strings(),
            )
        }
    }
}

data class FriendEntry(val friendshipId: Long, val hero: HeroSummary)

data class FriendsInfo(val friends: List<FriendEntry>, val incoming: List<FriendEntry>, val outgoing: List<FriendEntry>) {
    companion object {
        private fun entries(a: JSONArray) =
            a.mapObjects { FriendEntry(it.getLong("friendshipId"), HeroSummary.from(it.getJSONObject("hero"))) }

        fun from(j: JSONObject) = FriendsInfo(
            entries(j.getJSONArray("friends")), entries(j.getJSONArray("incoming")), entries(j.getJSONArray("outgoing")),
        )
    }
}

data class RaidStageInfo(val stage: Int, val name: String, val threshold: Long, val damageRate: Double)

data class RaidGradeInfo(val grade: String, val minDamage: Long, val gold: Long, val stones: Int, val itemGrade: String?)

data class RaidInfo(
    val bossName: String,
    val partySize: Int,
    val turns: Int,
    val staminaCost: Int,
    val stages: List<RaidStageInfo>,
    val grades: List<RaidGradeInfo>,
    val friends: List<HeroSummary>,
    val bestGrade: String?,
    val bestDamage: Long?,
    val remainingThisWeek: Int,
) {
    companion object {
        fun from(j: JSONObject) = RaidInfo(
            bossName = j.getString("bossName"),
            partySize = j.getInt("partySize"),
            turns = j.getInt("turns"),
            staminaCost = j.getInt("staminaCost"),
            stages = j.getJSONArray("stages").mapObjects {
                RaidStageInfo(it.getInt("stage"), it.getString("name"), it.getLong("threshold"), it.getDouble("damageRate"))
            },
            grades = j.getJSONArray("grades").mapObjects {
                RaidGradeInfo(
                    it.getString("grade"), it.getLong("minDamage"), it.getLong("gold"), it.getInt("stones"),
                    it.stringOrNull("itemGrade"),
                )
            },
            friends = j.getJSONArray("friends").mapObjects(HeroSummary::from),
            bestGrade = j.stringOrNull("bestGrade"),
            bestDamage = j.longOrNull("bestDamage"),
            remainingThisWeek = j.optInt("remainingThisWeek", 1),
        )
    }
}

data class RaidTurnInfo(val turn: Int, val damage: Long, val totalDamage: Long, val stage: Int, val stageName: String, val evolved: Boolean)

data class RaidMember(val name: String, val job: String, val jobName: String, val appearance: Int, val damage: Long)

data class RaidResult(
    val totalDamage: Long,
    val finalStage: Int,
    val finalStageName: String,
    val grade: String,
    val turns: List<RaidTurnInfo>,
    val members: List<RaidMember>,
    val rewardGold: Long,
    val rewardStones: Int,
    val item: ItemInfo?,
    val hero: HeroInfo,
) {
    companion object {
        fun from(j: JSONObject) = RaidResult(
            totalDamage = j.getLong("totalDamage"),
            finalStage = j.getInt("finalStage"),
            finalStageName = j.getString("finalStageName"),
            grade = j.getString("grade"),
            turns = j.getJSONArray("turns").mapObjects {
                RaidTurnInfo(
                    it.getInt("turn"), it.getLong("damage"), it.getLong("totalDamage"),
                    it.getInt("stage"), it.getString("stageName"), it.getBoolean("evolved"),
                )
            },
            members = j.getJSONArray("members").mapObjects {
                RaidMember(
                    it.getString("name"), it.optString("job", "WARRIOR"), it.getString("jobName"),
                    it.optInt("appearance", 0), it.getLong("damage"),
                )
            },
            rewardGold = j.getLong("rewardGold"),
            rewardStones = j.getInt("rewardStones"),
            item = if (j.isNull("item")) null else ItemInfo.from(j.getJSONObject("item")),
            hero = HeroInfo.from(j.getJSONObject("hero")),
        )
    }
}

data class ShopOffer(val category: String, val name: String, val slots: List<String>, val price: Long)

data class ShopInfo(val tier: Int, val regionName: String, val offers: List<ShopOffer>, val chances: List<Pair<String, Double>>) {
    companion object {
        fun from(j: JSONObject) = ShopInfo(
            tier = j.getInt("tier"),
            regionName = j.getString("regionName"),
            offers = j.getJSONArray("offers").mapObjects {
                ShopOffer(it.getString("category"), it.getString("name"), it.getJSONArray("slots").strings(), it.getLong("price"))
            },
            chances = j.getJSONArray("chances").mapObjects { it.getString("gradeName") to it.getDouble("percent") },
        )
    }
}

data class RankingEntry(
    val rank: Int,
    val heroId: Long,
    val name: String,
    val job: String,
    val jobName: String,
    val appearance: Int,
    val level: Int,
    val power: Int,
    val score: Long,
) {
    companion object {
        fun from(j: JSONObject) = RankingEntry(
            j.getInt("rank"), j.getLong("heroId"), j.getString("name"), j.getString("job"), j.getString("jobName"),
            j.getInt("appearance"), j.getInt("level"), j.getInt("power"), j.getLong("score"),
        )
    }
}

data class RankingInfo(val formula: String, val totalHeroes: Int, val entries: List<RankingEntry>, val me: RankingEntry?) {
    companion object {
        fun from(j: JSONObject) = RankingInfo(
            formula = j.getString("formula"),
            totalHeroes = j.getInt("totalHeroes"),
            entries = j.getJSONArray("entries").mapObjects(RankingEntry::from),
            me = if (j.isNull("me")) null else RankingEntry.from(j.getJSONObject("me")),
        )
    }
}

data class Listing(
    val id: Long,
    val item: ItemInfo,
    val price: Long,
    val sellerName: String,
    val mine: Boolean,
    val status: String,
    val buyerName: String?,
    val sellerReceives: Long,
) {
    companion object {
        fun from(j: JSONObject) = Listing(
            id = j.getLong("id"),
            item = ItemInfo.from(j.getJSONObject("item")),
            price = j.getLong("price"),
            sellerName = j.getString("sellerName"),
            mine = j.getBoolean("mine"),
            status = j.getString("status"),
            buyerName = j.stringOrNull("buyerName"),
            sellerReceives = j.getLong("sellerReceives"),
        )

        fun listFrom(a: JSONArray): List<Listing> = a.mapObjects(::from)
    }
}
