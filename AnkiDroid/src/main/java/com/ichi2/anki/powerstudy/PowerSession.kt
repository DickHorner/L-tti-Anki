// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.powerstudy

const val POWER_STUDY_BOX_COUNT = 6
const val POWER_STUDY_INACTIVE_BOX = 0
const val DEFAULT_POWER_STUDY_ACTIVATION_LIMIT = 30

data class PowerCardState(
    val cardId: Long,
    val box: Int,
) {
    init {
        require(box in POWER_STUDY_INACTIVE_BOX..POWER_STUDY_BOX_COUNT) {
            "box must be between $POWER_STUDY_INACTIVE_BOX and $POWER_STUDY_BOX_COUNT"
        }
    }
}

data class PowerSession(
    val id: String,
    val name: String,
    val cards: List<PowerCardState>,
    val activationLimit: Int,
    val boxWeights: List<Int>,
) {
    init {
        require(id.isNotBlank()) { "id must not be blank" }
        require(name.isNotBlank()) { "name must not be blank" }
        require(cards.isNotEmpty()) { "cards must not be empty" }
        require(cards.map { it.cardId }.distinct().size == cards.size) { "card IDs must be unique within a session" }
        require(activationLimit > 0) { "activationLimit must be positive" }
        require(boxWeights.size == POWER_STUDY_BOX_COUNT) {
            "boxWeights must contain exactly $POWER_STUDY_BOX_COUNT entries"
        }
        require(boxWeights.all { it > 0 }) { "boxWeights must be positive" }
    }

    val isComplete: Boolean
        get() = cards.all { it.box == POWER_STUDY_BOX_COUNT }
}
