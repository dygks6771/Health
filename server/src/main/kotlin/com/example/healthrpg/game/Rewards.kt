package com.example.healthrpg.game

/** 승리 시 열리는 보상 상자 */
data class ChestResponse(val tier: ChestTier, val name: String) {
    companion object {
        fun of(tier: ChestTier) = ChestResponse(tier, tier.displayName)
    }
}
