// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.powerstudy

import kotlin.math.min
import kotlin.random.Random

class PowerStudyEngine(
    private val random: Random = Random.Default,
) {
    fun createSession(
        id: String,
        name: String,
        cardIds: List<Long>,
        boxWeights: List<Int>,
        activationLimit: Int = DEFAULT_POWER_STUDY_ACTIVATION_LIMIT,
    ): PowerSession {
        require(cardIds.isNotEmpty()) { "cardIds must not be empty" }
        require(cardIds.distinct().size == cardIds.size) { "cardIds must be unique" }

        val shuffledCardIds = cardIds.shuffled(random)
        val activeCount = min(activationLimit, shuffledCardIds.size)
        val cards =
            shuffledCardIds.mapIndexed { index, cardId ->
                PowerCardState(
                    cardId = cardId,
                    box = if (index < activeCount) 1 else POWER_STUDY_INACTIVE_BOX,
                )
            }

        return PowerSession(
            id = id,
            name = name,
            cards = cards,
            activationLimit = activationLimit,
            boxWeights = boxWeights.toList(),
        )
    }

    fun nextCard(session: PowerSession): Long? {
        if (session.isComplete) return null

        val cardsByBox =
            (1..POWER_STUDY_BOX_COUNT)
                .map { box -> box to session.cards.filter { it.box == box } }
                .filter { (_, cards) -> cards.isNotEmpty() }

        if (cardsByBox.isEmpty()) return null

        val totalWeight = cardsByBox.sumOf { (box) -> session.boxWeights[box - 1] }
        var draw = random.nextInt(totalWeight)
        val selectedBoxCards =
            cardsByBox.first { (box) ->
                draw -= session.boxWeights[box - 1]
                draw < 0
            }.second

        return selectedBoxCards.random(random).cardId
    }

    fun answerCorrect(
        session: PowerSession,
        cardId: Long,
    ): PowerSession = answer(session, cardId, delta = 1)

    fun answerWrong(
        session: PowerSession,
        cardId: Long,
    ): PowerSession = answer(session, cardId, delta = -1)

    private fun answer(
        session: PowerSession,
        cardId: Long,
        delta: Int,
    ): PowerSession {
        val cardIndex = session.cards.indexOfFirst { it.cardId == cardId }
        require(cardIndex >= 0) { "card $cardId does not belong to session " + session.id }

        val currentCard = session.cards[cardIndex]
        require(currentCard.box != POWER_STUDY_INACTIVE_BOX) { "inactive card $cardId cannot be answered" }

        val updatedCards = session.cards.toMutableList()
        updatedCards[cardIndex] =
            currentCard.copy(
                box = (currentCard.box + delta).coerceIn(1, POWER_STUDY_BOX_COUNT),
            )

        return refillActivationBuffer(session.copy(cards = updatedCards))
    }

    private fun refillActivationBuffer(session: PowerSession): PowerSession {
        val cardsInFirstBox = session.cards.count { it.box == 1 }
        var slotsToFill = (session.activationLimit - cardsInFirstBox).coerceAtLeast(0)
        if (slotsToFill == 0) return session

        val updatedCards = session.cards.toMutableList()
        for (index in updatedCards.indices) {
            if (slotsToFill == 0) break
            if (updatedCards[index].box != POWER_STUDY_INACTIVE_BOX) continue

            updatedCards[index] = updatedCards[index].copy(box = 1)
            slotsToFill--
        }

        return session.copy(cards = updatedCards)
    }
}
