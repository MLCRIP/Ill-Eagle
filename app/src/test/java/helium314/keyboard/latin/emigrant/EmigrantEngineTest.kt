package helium314.keyboard.latin.emigrant

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import helium314.keyboard.latin.LastComposedWord
import helium314.keyboard.latin.RichInputConnection
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import java.util.Random
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class EmigrantEngineTest {

    private val sampleJson = """
        {
          "version": 1,
          "default_suffixes": {
            "s": "s",
            "es": "es",
            "ed": "ed",
            "ing": "ing",
            "ly": "ly",
            "ity": "ity",
            "ities": "ities",
            "ness": "ness"
          },
          "word_rules": [
            {
              "id": "word_illegal",
              "match": "illegal",
              "replacement": "ill eagle"
            },
            {
              "id": "word_jam_1",
              "match": "jam",
              "replacement": "jelly"
            },
            {
              "id": "word_jam_2",
              "match": "jam",
              "replacements": ["marmalade", "jelly"]
            },
            {
              "id": "word_mistake",
              "match": "mistake",
              "replacement": "miss steak"
            },
            {
              "id": "word_message",
              "match": "message",
              "replacement": "massage"
            },
            {
              "id": "word_massage",
              "match": "massage",
              "replacement": "message"
            },
            {
              "id": "word_no_auto_suffixes",
              "match": "exactonly",
              "replacement": "exact result",
              "auto_suffixes": false
            },
            {
              "id": "word_explicit_suffix_override",
              "match": "customstem",
              "replacement": "custom replacement",
              "suffixes": {
                "ly": "-ly"
              }
            }
          ],
          "phrase_rules": [
            {
              "id": "phrase_ill_eagle",
              "match": "ill eagle",
              "replacement": "illegal"
            }
          ],
          "context_rules": [
            {
              "id": "context_jam_traffic_1",
              "contains": ["jam", "traffic"],
              "target": "jam",
              "replacement": "strawberry jam"
            },
            {
              "id": "context_jam_traffic_2",
              "contains": ["jam", "traffic"],
              "target": "jam",
              "replacements": ["raspberry jam", "fruit jam"]
            },
            {
              "id": "context_jam_traffic_heavy",
              "contains": ["jam", "traffic", "heavy"],
              "target": "jam",
              "replacement": "heavy traffic jam"
            },
            {
              "id": "context_cash_bank",
              "contains": ["cash", "bank"],
              "target": "bank",
              "replacement": "river bank"
            }
          ],
          "grammar_rules": [
            {
              "id": "grammar_bad_be",
              "subjects": ["i", "you", "we", "they"],
              "verb_replacements": {
                "am": "is",
                "are": "is"
              }
            }
          ]
        }
    """.trimIndent()

    @Before
    fun setUp() {
        val engine = EmigrantEngine.instance
        engine.resetForTesting()

        // Deterministic random for standard tests (picks index 0)
        val defaultRandom = Mockito.mock(Random::class.java)
        Mockito.`when`(defaultRandom.nextInt(Mockito.anyInt())).thenReturn(0)
        engine.setRandomForTesting(defaultRandom)

        engine.parseRulesJson(sampleJson)
    }

    @Test
    fun testMilestone7_MultipleReplacementsGroupingAndDeduplication() {
        val engine = EmigrantEngine.instance

        // "jam" rule combines "jelly" and "marmalade" (deduplicating "jelly")
        val parsed = engine.validateJson(sampleJson)
        val jamRule = parsed.ruleByMatch["jam"]!!
        assertEquals(listOf("jelly", "marmalade"), jamRule.replacements)

        // Context rule "jam"+"traffic" combines "strawberry jam", "raspberry jam", "fruit jam"
        val contextRule = parsed.contextRules.first { it.target == "jam" && it.contains.size == 2 }
        assertEquals(listOf("strawberry jam", "raspberry jam", "fruit jam"), contextRule.replacements)
    }

    @Test
    fun testMilestone7_DeterministicRandomSelection() {
        val engine = EmigrantEngine.instance

        // Fix random seed to pick second option ("marmalade")
        val testRandom = Mockito.mock(Random::class.java)
        Mockito.`when`(testRandom.nextInt(2)).thenReturn(1) // picks index 1 ("marmalade")
        engine.setRandomForTesting(testRandom)

        val res = engine.processWordCommit("jam", "jam", " ")
        assertEquals("marmalade", res.wordToCommit)
    }

    @Test
    fun testMilestone7_ContradictoryDuplicateWordRuleConfigurationRejected() {
        val engine = EmigrantEngine.instance
        val badJson = """
            {
              "word_rules": [
                { "id": "w1", "match": "test", "replacement": "r1", "auto_suffixes": true },
                { "id": "w2", "match": "test", "replacement": "r2", "auto_suffixes": false }
              ]
            }
        """.trimIndent()

        val ex = assertFailsWith<EmigrantValidationException> {
            engine.validateJson(badJson)
        }
        assertTrue(ex.message!!.contains("Conflicting configuration for duplicate word rule 'test'"))
    }

    @Test
    fun testMilestone6_ContextWinsOverOrdinaryWordReplacementForward() {
        val engine = EmigrantEngine.instance

        // "jam" alone -> "jelly"
        val res1 = engine.processWordCommit("jam", "jam", " ")
        assertEquals("jelly", res1.wordToCommit)

        engine.clearHistory()

        // "traffic" then "jam" -> Context rule "context_jam_traffic" wins over word rule!
        engine.processWordCommit("traffic", "traffic", " ")
        val res2 = engine.processWordCommit("jam", "jam", " ")
        assertEquals("strawberry jam", res2.wordToCommit)
    }

    @Test
    fun testMilestone6_ContextOverridesAlreadyTransformedTargetReverse() {
        val engine = EmigrantEngine.instance
        val connection = Mockito.mock(RichInputConnection::class.java)

        // 1. "jam" -> transformed by word rule to "jelly"
        val res1 = engine.processWordCommit("jam", "jam", " ")
        assertEquals("jelly", res1.wordToCommit)

        // 2. "in"
        engine.processWordCommit("in", "in", " ")

        // 3. "traffic"
        engine.processWordCommit("traffic", "traffic", " ")

        // Expected in editor before cursor: "jelly in traffic"
        val expectedInEditor = "jelly in traffic"
        Mockito.`when`(connection.getTextBeforeCursor(Mockito.eq(expectedInEditor.length), Mockito.anyInt()))
            .thenReturn(expectedInEditor)

        // Context rule overrides earlier "jelly" with "strawberry jam"!
        val applied = engine.evaluatePhraseRulesAfterCommit(connection)
        assertTrue(applied)

        Mockito.verify(connection).deleteTextBeforeCursor(expectedInEditor.length)
        Mockito.verify(connection).commitText("strawberry jam in traffic", 1)
    }

    @Test
    fun testMilestone6_NoContextMeansOrdinaryWordReplacementRemains() {
        val engine = EmigrantEngine.instance

        engine.processWordCommit("jam", "jam", " ")
        engine.processWordCommit("in", "in", " ")
        val res = engine.processWordCommit("jar", "jar", " ")

        // Word replacement "jelly" remains because no context rule matched
        assertEquals("jar", res.wordToCommit)
        val history = engine.getSentenceHistoryForTesting()
        assertEquals("jelly", history[0].displayedWord)
    }

    @Test
    fun testMilestone6_StrongerContextOverridesWeakerContext() {
        val engine = EmigrantEngine.instance

        engine.processWordCommit("heavy", "heavy", " ")
        engine.processWordCommit("traffic", "traffic", " ")

        // "context_jam_traffic_heavy" has contains.size = 3 (more specific than context_jam_traffic size = 2)
        val res = engine.processWordCommit("jam", "jam", " ")
        assertEquals("heavy traffic jam", res.wordToCommit)
    }

    @Test
    fun testMilestone6_SentenceBoundariesPreventInappropriateContextMatches() {
        val engine = EmigrantEngine.instance
        val connection = Mockito.mock(RichInputConnection::class.java)

        engine.processWordCommit("jam", "jam", ". ") // Period ends sentence 1
        engine.evaluatePhraseRulesAfterCommit(connection)

        engine.processWordCommit("traffic", "traffic", " ")

        val applied = engine.evaluatePhraseRulesAfterCommit(connection)
        assertFalse(applied)
    }

    @Test
    fun testMilestone6_NoRepeatedTransformationsOrLoops() {
        val engine = EmigrantEngine.instance
        val connection = Mockito.mock(RichInputConnection::class.java)

        engine.processWordCommit("jam", "jam", " ")
        engine.processWordCommit("traffic", "traffic", " ")

        Mockito.`when`(connection.getTextBeforeCursor(Mockito.anyInt(), Mockito.anyInt()))
            .thenReturn("jelly traffic")

        val applied1 = engine.evaluatePhraseRulesAfterCommit(connection)
        assertTrue(applied1)

        // Second call must NOT re-apply transformation
        val applied2 = engine.evaluatePhraseRulesAfterCommit(connection)
        assertFalse(applied2)
    }

    @Test
    fun testMilestone6_ContextRulesOffPreservesOrdinaryWordReplacements() {
        val engine = EmigrantEngine.instance
        engine.setContextRulesEnabled(false)

        engine.processWordCommit("traffic", "traffic", " ")
        val res = engine.processWordCommit("jam", "jam", " ")

        // Word rule "jelly" applies because Context Rules is OFF
        assertEquals("jelly", res.wordToCommit)
    }

    @Test
    fun testMilestone6_WordReplacementsOffDoesNotDisableEligibleContextRules() {
        val engine = EmigrantEngine.instance
        engine.setWordRulesEnabled(false)

        // "jam" alone remains "jam" when Word Replacements is OFF
        val res1 = engine.processWordCommit("jam", "jam", " ")
        assertEquals("jam", res1.wordToCommit)

        engine.clearHistory()

        // Eligible context rule STILL runs when Word Replacements is OFF
        engine.processWordCommit("traffic", "traffic", " ")
        val res2 = engine.processWordCommit("jam", "jam", " ")
        assertEquals("strawberry jam", res2.wordToCommit)
    }

    @Test
    fun testMilestone6_EditorMismatchSkipsReplacementSafely() {
        val engine = EmigrantEngine.instance
        val connection = Mockito.mock(RichInputConnection::class.java)

        engine.processWordCommit("jam", "jam", " ")
        engine.processWordCommit("in", "in", " ")
        engine.processWordCommit("traffic", "traffic", " ")

        // Editor contains mismatching text
        Mockito.`when`(connection.getTextBeforeCursor(Mockito.anyInt(), Mockito.anyInt()))
            .thenReturn("mismatched editor text")

        val applied = engine.evaluatePhraseRulesAfterCommit(connection)
        assertFalse(applied)

        Mockito.verify(connection, Mockito.never()).deleteTextBeforeCursor(Mockito.anyInt())
    }

    @Test
    fun testGrammarIAmReadyToIIsReady() {
        val engine = EmigrantEngine.instance

        engine.processWordCommit("I", "I", " ")
        val res = engine.processWordCommit("am", "am", " ")

        assertEquals("is", res.wordToCommit)
        assertTrue(res.isEmigrantGenerated)
    }

    @Test
    fun testGrammarYouAreCorrectToYouIsCorrect() {
        val engine = EmigrantEngine.instance

        engine.processWordCommit("You", "You", " ")
        val res = engine.processWordCommit("are", "are", " ")

        assertEquals("is", res.wordToCommit)
        assertTrue(res.isEmigrantGenerated)
    }

    @Test
    fun testGrammarCapitalizationIAMREADYToIISREADY() {
        val engine = EmigrantEngine.instance

        engine.processWordCommit("I", "I", " ")
        val res = engine.processWordCommit("AM", "AM", " ")

        assertEquals("IS", res.wordToCommit)
        assertTrue(res.isEmigrantGenerated)
    }

    @Test
    fun test1_MultipleTargetsCashBankCashBank() {
        val engine = EmigrantEngine.instance

        engine.processWordCommit("cash", "cash", " ")
        val res1 = engine.processWordCommit("bank", "bank", " ")
        assertEquals("river bank", res1.wordToCommit)

        engine.processWordCommit("cash", "cash", " ")
        val res2 = engine.processWordCommit("bank", "bank", " ")
        assertEquals("river bank", res2.wordToCommit)
    }

    @Test
    fun test2_MultipleTargetsBankCashCashBankIllegal() {
        val engine = EmigrantEngine.instance
        val connection = Mockito.mock(RichInputConnection::class.java)

        engine.processWordCommit("bank", "bank", " ")
        engine.processWordCommit("cash", "cash", " ")

        Mockito.`when`(connection.getTextBeforeCursor(Mockito.eq("bank cash".length), Mockito.anyInt()))
            .thenReturn("bank cash")
        val applied1 = engine.evaluatePhraseRulesAfterCommit(connection)
        assertTrue(applied1)

        engine.processWordCommit("cash", "cash", " ")
        val res1 = engine.processWordCommit("bank", "bank", " ")
        assertEquals("river bank", res1.wordToCommit)

        val res2 = engine.processWordCommit("illegal", "illegal", " ")
        assertEquals("ill eagle", res2.wordToCommit)
    }

    @Test
    fun test3_IllegalCashBank() {
        val engine = EmigrantEngine.instance

        val res1 = engine.processWordCommit("illegal", "illegal", " ")
        assertEquals("ill eagle", res1.wordToCommit)

        engine.processWordCommit("cash", "cash", " ")
        val res2 = engine.processWordCommit("bank", "bank", " ")
        assertEquals("river bank", res2.wordToCommit)
    }

    @Test
    fun test5_BackspaceOnCommittedTextClearsSentenceHistory() {
        val engine = EmigrantEngine.instance

        engine.processWordCommit("bank", "bank", " ")
        assertEquals(1, engine.getSentenceHistoryForTesting().size)

        engine.onCommittedTextDeleted()
        assertEquals(0, engine.getSentenceHistoryForTesting().size)
    }

    @Test
    fun test8_ForwardAndReverseContextFromPart1Work() {
        val engine = EmigrantEngine.instance
        val connection = Mockito.mock(RichInputConnection::class.java)

        engine.processWordCommit("I", "I", " ")
        engine.processWordCommit("need", "need", " ")
        engine.processWordCommit("cash", "cash", " ")
        engine.processWordCommit("from", "from", " ")
        engine.processWordCommit("the", "the", " ")
        val res = engine.processWordCommit("bank", "bank", " ")
        assertEquals("river bank", res.wordToCommit)

        engine.clearHistory()

        engine.processWordCommit("the", "the", " ")
        engine.processWordCommit("bank", "bank", " ")
        engine.processWordCommit("has", "has", " ")
        engine.processWordCommit("cash", "cash", " ")

        Mockito.`when`(connection.getTextBeforeCursor(Mockito.eq("bank has cash".length), Mockito.anyInt()))
            .thenReturn("bank has cash")

        val applied = engine.evaluatePhraseRulesAfterCommit(connection)
        assertTrue(applied)

        Mockito.verify(connection).deleteTextBeforeCursor("bank has cash".length)
        Mockito.verify(connection).commitText("river bank has cash", 1)
    }

    @Test
    fun test10_WordRulesSuffixesPhraseRulesAndImportWork() {
        val engine = EmigrantEngine.instance
        val connection = Mockito.mock(RichInputConnection::class.java)

        assertEquals("ill eagle", engine.processWord("illegal"))
        assertEquals("ill eaglely", engine.processWord("illegally"))

        engine.processWordCommit("ill", "I'll", " ")
        engine.processWordCommit("eagle", "eagle", "")

        Mockito.`when`(connection.getTextBeforeCursor(Mockito.eq("I'll eagle".length), Mockito.anyInt()))
            .thenReturn("I'll eagle")

        val applied = engine.evaluatePhraseRulesAfterCommit(connection)
        assertTrue(applied)

        Mockito.verify(connection).deleteTextBeforeCursor("I'll eagle".length)
        Mockito.verify(connection).commitText("illegal", 1)
    }

    @Test
    fun testEmigrantModeToggle() {
        val engine = EmigrantEngine.instance
        assertTrue(engine.isEmigrantModeEnabled())
        assertEquals("ill eagle", engine.processWord("illegal"))

        engine.setEmigrantModeEnabled(false)
        assertFalse(engine.isEmigrantModeEnabled())
        assertEquals("illegal", engine.processWord("illegal"))

        val res = engine.processWordCommit("illegal", "illegal", " ")
        assertEquals("illegal", res.wordToCommit)
        assertFalse(res.isEmigrantGenerated)

        engine.setEmigrantModeEnabled(true)
        assertTrue(engine.isEmigrantModeEnabled())
        assertEquals("ill eagle", engine.processWord("illegal"))
    }

    @Test
    fun testDuplicateWordRuleMatchRejectedWhenConfigDiffers() {
        val engine = EmigrantEngine.instance
        val badJson = """
            {
              "word_rules": [
                { "id": "r1", "match": "illegal", "replacement": "ill eagle", "auto_suffixes": true },
                { "id": "r2", "match": "ILLEGAL", "replacement": "conflicting duplicate", "auto_suffixes": false }
              ]
            }
        """.trimIndent()

        val ex = assertFailsWith<EmigrantValidationException> {
            engine.validateJson(badJson)
        }
        assertTrue(ex.message!!.contains("Conflicting configuration for duplicate word rule 'illegal'"))
    }

    @Test
    fun testConflictingDefaultSuffixesRejected() {
        val engine = EmigrantEngine.instance
        val conflictingSuffixJson = """
            {
              "default_suffixes": {
                "ing": "ing",
                " Ing ": "-ing"
              }
            }
        """.trimIndent()

        val ex = assertFailsWith<EmigrantValidationException> {
            engine.validateJson(conflictingSuffixJson)
        }
        assertTrue(ex.message!!.contains("Conflicting duplicate default suffix definition for 'ing'"))
    }

    @Test
    fun testReversibleWordRulesAllowed() {
        val engine = EmigrantEngine.instance
        assertEquals("massage", engine.processWord("message"))
        assertEquals("message", engine.processWord("massage"))
    }
}
