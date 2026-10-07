// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.powerstudy

import org.junit.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PowerStudyEngineTest {
    private val boxWeights = listOf(50, 20, 12, 8, 8, 2)

    @Test
    fun `createSession activates only the configured buffer`() {
        val session =
            PowerStudyEngine(Random(1)).createSession(
                id = "english",
                name = "English vocabulary",
                cardIds = (1L..40L).toList(),
                boxWeights = boxWeights,
            )

        assertEquals(30, session.cards.count { it.box == 1 })
        assertEquals(10, session.cards.count { it.box == POWER_STUDY_INACTIVE_BOX })
    }

    @Test
    fun `correct answer moves one box up and refills box one`() {
        val engine = PowerStudyEngine(Random(1))
        var session =
            engine.createSession(
                id = "english",
                name = "English vocabulary",
                cardIds = listOf(1L, 2L, 3L),
                boxWeights = boxWeights,
                activationLimit = 2,
            )
        val cardId = session.cards.first { it.box == 1 }.cardId

        session = engine.answerCorrect(session, cardId)

        assertEquals(2, session.cards.first { it.cardId == cardId }.box)
        assertEquals(2, session.cards.count { it.box == 1 })
        assertEquals(0, session.cards.count { it.box == POWER_STUDY_INACTIVE_BOX })
    }

    @Test
    fun `wrong answer moves one box down without activating beyond the buffer`() {
        val engine = PowerStudyEngine(Random(1))
        val session =
            PowerSession(
                id = "history",
                name = "History page 24",
                cards =
                    listOf(
                        PowerCardState(1L, 1),
                        PowerCardState(2L, 1),
                        PowerCardState(3L, 2),
                        PowerCardState(4L, POWER_STUDY_INACTIVE_BOX),
                    ),
                activationLimit = 2,
                boxWeights = boxWeights,
            )

        val updated = engine.answerWrong(session, 3L)

        assertEquals(1, updated.cards.first { it.cardId == 3L }.box)
        assertEquals(3, updated.cards.count { it.box == 1 })
        assertEquals(POWER_STUDY_INACTIVE_BOX, updated.cards.first { it.cardId == 4L }.box)
    }

    @Test
    fun `answers are clamped to the first and sixth box`() {
        val engine = PowerStudyEngine(Random(1))
        val firstBoxSession = sessionWithCards(PowerCardState(1L, 1))
        val sixthBoxSession = sessionWithCards(PowerCardState(1L, 6), PowerCardState(2L, 5))

        assertEquals(1, engine.answerWrong(firstBoxSession, 1L).cards.single().box)
        assertEquals(6, engine.answerCorrect(sixthBoxSession, 1L).cards.first { it.cardId == 1L }.box)
    }

    @Test
    fun `the same Anki card can have independent state in multiple sessions`() {
        val engine = PowerStudyEngine(Random(1))
        val english = sessionWithCards(PowerCardState(42L, 2), id = "english")
        val french = sessionWithCards(PowerCardState(42L, 5), id = "french")

        val updatedEnglish = engine.answerCorrect(english, 42L)

        assertEquals(3, updatedEnglish.cards.single().box)
        assertEquals(5, french.cards.single().box)
    }

    @Test
    fun `nextCard never returns an inactive card`() {
        val engine = PowerStudyEngine(Random(1))
        val session =
            sessionWithCards(
                PowerCardState(1L, 3),
                PowerCardState(2L, POWER_STUDY_INACTIVE_BOX),
            )

        repeat(20) {
            assertEquals(1L, engine.nextCard(session))
        }
    }

    @Test
    fun `nextCard uses configured box weights`() {
        val engine = PowerStudyEngine(Random(1))
        val session =
            PowerSession(
                id = "weighted",
                name = "Weighted",
                cards = (1L..6L).mapIndexed { index, cardId -> PowerCardState(cardId, index + 1) },
                activationLimit = 30,
                boxWeights = listOf(1000, 1, 1, 1, 1, 1),
            )

        val firstBoxDraws = List(1_000) { engine.nextCard(session) }.count { it == 1L }

        assertTrue(firstBoxDraws > 950)
    }

    @Test
    fun `nextCard returns null when all cards reached box six`() {
        val session = sessionWithCards(PowerCardState(1L, 6), PowerCardState(2L, 6))

        assertTrue(session.isComplete)
        assertNull(PowerStudyEngine(Random(1)).nextCard(session))
    }

    @Test
    fun `inactive cards cannot be answered`() {
        val session = sessionWithCards(PowerCardState(1L, POWER_STUDY_INACTIVE_BOX))

        assertFailsWith<IllegalArgumentException> {
            PowerStudyEngine(Random(1)).answerCorrect(session, 1L)
        }
    }

    private fun sessionWithCards(
        vararg cards: PowerCardState,
        id: String = "session",
    ) =
        PowerSession(
            id = id,
            name = id,
            cards = cards.toList(),
            activationLimit = 30,
            boxWeights = boxWeights,
        )
}
