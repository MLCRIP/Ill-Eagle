package helium314.keyboard.latin.emigrant

import android.content.Context
import helium314.keyboard.latin.RichInputConnection
import helium314.keyboard.latin.settings.SettingsValues
import helium314.keyboard.latin.utils.Log
import org.json.JSONObject
import java.util.Locale
import java.util.Random

/**
 * Data representation of a word rule loaded from JSON.
 * Supports multiple alternative replacements.
 */
data class WordRule(
    val id: String,
    val match: String,
    val replacements: List<String>,
    val autoSuffixes: Boolean = true,
    /**
     * Optional explicit per-rule suffix map (overrides default global suffix mappings).
     */
    val explicitSuffixes: Map<String, String> = emptyMap()
) {
    fun selectReplacement(random: Random): String {
        if (replacements.isEmpty()) return ""
        if (replacements.size == 1) return replacements[0]
        return replacements[random.nextInt(replacements.size)]
    }
}

/**
 * Data representation of a multiword phrase rule loaded from JSON.
 * Supports multiple alternative replacements.
 */
data class PhraseRule(
    val id: String,
    val match: String,
    val matchWords: List<String>,
    val replacements: List<String>
) {
    fun selectReplacement(random: Random): String {
        if (replacements.isEmpty()) return ""
        if (replacements.size == 1) return replacements[0]
        return replacements[random.nextInt(replacements.size)]
    }
}

/**
 * Data representation of a contextual sentence rule loaded from JSON.
 * Supports multiple alternative replacements.
 */
data class ContextRule(
    val id: String,
    val contains: List<String>,
    val target: String,
    val replacements: List<String>
) {
    fun selectReplacement(random: Random): String {
        if (replacements.isEmpty()) return ""
        if (replacements.size == 1) return replacements[0]
        return replacements[random.nextInt(replacements.size)]
    }
}

/**
 * Data representation of a grammar pattern rule loaded from JSON.
 */
data class GrammarRule(
    val id: String,
    val subjects: List<String>,
    val verbReplacements: Map<String, String>
)

/**
 * Indicates which rule category applied to a word entry.
 */
enum class RuleCategory {
    NONE,
    WORD,
    SUFFIX,
    PHRASE,
    GRAMMAR,
    CONTEXT
}

/**
 * RAM-only record of a recently committed word.
 */
data class EmigrantWord(
    val typedWord: String,
    val autocorrectedWord: String,
    val displayedWord: String,
    val separator: String,
    val isEmigrantGenerated: Boolean = false,
    val contextApplied: Boolean = false,
    val ruleCategory: RuleCategory = RuleCategory.NONE
)

/**
 * Result container returned when a word is processed during commit.
 */
data class EmigrantResult(
    val wordToCommit: String,
    val isEmigrantGenerated: Boolean
)

/**
 * Parsed and validated ruleset container before in-memory activation.
 */
data class ParsedRuleset(
    val exactRules: Map<String, String>,
    val ruleByMatch: Map<String, WordRule>,
    val phraseRules: List<PhraseRule>,
    val contextRules: List<ContextRule>,
    val grammarRules: List<GrammarRule>,
    val defaultSuffixMap: Map<String, String>,
    val allInputSuffixesSorted: List<String>
)

/**
 * Exception thrown when JSON ruleset validation fails.
 */
class EmigrantValidationException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Transformation engine for Emigrant English.
 * Responsibilities:
 * - Loads word, global default suffixes, phrase rule sets, context sentence rules, and grammar rules from JSON assets or imported files.
 * - Supports multiple alternative replacements ("replacement": "a" OR "replacements": ["a", "b"]) selected randomly at runtime.
 * - Grouping & Deduplication: Identical match conditions are combined into alternative replacements; conflicting configurations are rejected.
 * - Deterministic Rule Priority Hierarchy:
 *   1. Context Rules (Highest priority; sorted by condition specificity `contains.size` descending)
 *   2. Phrase Rules (Multiword exact phrase matching)
 *   3. Grammar Rules (Subject-verb pattern rules)
 *   4. Word Rules (Single-word exact replacements)
 *   5. Suffix Rules (Automatic suffix handling)
 * - Testable Random Selection: Injectable java.util.Random instance for deterministic unit testing.
 * - Bounded RAM-only sentence history for contextual word replacement and subject-verb grammar replacement.
 * - Robust transactional validation: validateJson() validates into local structures without mutating engine state.
 */
class EmigrantEngine private constructor() {

    private val exactRules = HashMap<String, String>()
    private val ruleByMatch = HashMap<String, WordRule>()

    /**
     * Default global suffix mapping loaded from JSON (e.g. "s" -> "s", "ing" -> "ing").
     */
    private val defaultSuffixMap = HashMap<String, String>()

    /**
     * Phrase rules sorted by word count descending (longest phrase match first).
     */
    private val phraseRules = ArrayList<PhraseRule>()

    /**
     * Contextual sentence rules sorted by specificity (most contains words first).
     */
    private val contextRules = ArrayList<ContextRule>()

    /**
     * Grammar pattern rules loaded from JSON.
     */
    private val grammarRules = ArrayList<GrammarRule>()

    /**
     * Dynamically collected input suffixes across global defaults and explicit rule overrides, sorted by length descending.
     */
    private val allInputSuffixesSorted = ArrayList<String>()

    /**
     * RAM-only rolling word history (across sentence boundaries). Max 10 entries.
     */
    private val wordHistory = ArrayList<EmigrantWord>()
    private val maxHistorySize = 10

    /**
     * RAM-only rolling sentence history (cleared on sentence boundaries). Max 20 entries.
     */
    private val sentenceHistory = ArrayList<EmigrantWord>()
    private val maxSentenceHistorySize = 20

    @Volatile
    private var isInitialized = false

    @Volatile
    private var isEmigrantModeEnabled = true

    // Injectable Random instance (defaults to standard Random)
    @Volatile
    private var random: Random = Random()

    // Feature Category Toggles
    @Volatile private var isWordRulesEnabled = true
    @Volatile private var isAutoSuffixesEnabled = true
    @Volatile private var isPhraseRulesEnabled = true
    @Volatile private var isContextRulesEnabled = true
    @Volatile private var isGrammarRulesEnabled = true

    @Volatile
    private var isPendingSentenceReset = false

    /**
     * Sets custom Random instance for deterministic unit testing.
     */
    fun setRandomForTesting(testRandom: Random) {
        synchronized(this) {
            this.random = testRandom
        }
    }

    /**
     * Enables or disables Emigrant mode.
     */
    fun setEmigrantModeEnabled(enabled: Boolean) {
        synchronized(this) {
            isEmigrantModeEnabled = enabled
            if (!enabled) {
                wordHistory.clear()
                sentenceHistory.clear()
                isPendingSentenceReset = false
            }
        }
    }

    /**
     * Checks if Emigrant mode is enabled.
     */
    fun isEmigrantModeEnabled(): Boolean = isEmigrantModeEnabled

    fun setRuleCategoryToggles(
        wordRules: Boolean,
        autoSuffixes: Boolean,
        phraseRules: Boolean,
        contextRules: Boolean,
        grammarRules: Boolean
    ) {
        synchronized(this) {
            isWordRulesEnabled = wordRules
            isAutoSuffixesEnabled = autoSuffixes
            isPhraseRulesEnabled = phraseRules
            isContextRulesEnabled = contextRules
            isGrammarRulesEnabled = grammarRules
        }
    }

    fun setWordRulesEnabled(enabled: Boolean) { isWordRulesEnabled = enabled }
    fun isWordRulesEnabled(): Boolean = isWordRulesEnabled

    fun setAutoSuffixesEnabled(enabled: Boolean) { isAutoSuffixesEnabled = enabled }
    fun isAutoSuffixesEnabled(): Boolean = isAutoSuffixesEnabled

    fun setPhraseRulesEnabled(enabled: Boolean) { isPhraseRulesEnabled = enabled }
    fun isPhraseRulesEnabled(): Boolean = isPhraseRulesEnabled

    fun setContextRulesEnabled(enabled: Boolean) { isContextRulesEnabled = enabled }
    fun isContextRulesEnabled(): Boolean = isContextRulesEnabled

    fun setGrammarRulesEnabled(enabled: Boolean) { isGrammarRulesEnabled = enabled }
    fun isGrammarRulesEnabled(): Boolean = isGrammarRulesEnabled

    /**
     * Initializes the engine using EmigrantRulesManager.
     */
    fun init(context: Context) {
        if (isInitialized) return
        synchronized(this) {
            if (isInitialized) return
            try {
                EmigrantRulesManager.init(context)
                isInitialized = true
                Log.i(TAG, "EmigrantEngine initialized with ${ruleByMatch.size} word rule(s), ${phraseRules.size} phrase rule(s), ${contextRules.size} context rule(s), ${grammarRules.size} grammar rule(s), and ${allInputSuffixesSorted.size} unique suffix(es).")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load Emigrant rules", e)
            }
        }
    }

    /**
     * Clears temporary in-memory history (called on field switch, editor reset, password fields, ruleset reload, etc.).
     */
    fun clearHistory() {
        synchronized(this) {
            wordHistory.clear()
            sentenceHistory.clear()
            isPendingSentenceReset = false
        }
    }

    /**
     * Called when Backspace modifies already committed text in editor.
     * Clears sentence history so deleted/edited words cannot trigger stale context transformations.
     */
    fun onCommittedTextDeleted() {
        synchronized(this) {
            sentenceHistory.clear()
            isPendingSentenceReset = false
            Log.d(TAG, "Sentence history cleared because committed text was modified by Backspace")
        }
    }

    /**
     * Handles standalone separators/punctuation inserted without a composing word (e.g. double-space-period, Enter, standalone punctuation).
     */
    fun onSeparatorWithoutWord(separator: String) {
        if (isSentenceBoundary(separator)) {
            synchronized(this) {
                sentenceHistory.clear()
                isPendingSentenceReset = false
                Log.d(TAG, "Sentence history cleared by boundary separator without word")
            }
        }
    }

    /**
     * Returns a copy of current word history (useful for unit testing).
     */
    fun getHistoryForTesting(): List<EmigrantWord> {
        synchronized(this) {
            return ArrayList(wordHistory)
        }
    }

    /**
     * Returns a copy of current sentence history (useful for unit testing).
     */
    fun getSentenceHistoryForTesting(): List<EmigrantWord> {
        synchronized(this) {
            return ArrayList(sentenceHistory)
        }
    }

    /**
     * Validates a JSON string containing Emigrant rules and returns a ParsedRuleset without mutating active engine state.
     * Grouping: Identical match conditions are combined into alternative replacements; conflicting configurations are rejected.
     */
    fun validateJson(jsonString: String): ParsedRuleset {
        val tempExactRules = HashMap<String, String>()
        val tempRuleByMatch = HashMap<String, WordRule>()
        val tempPhraseRuleByMatch = HashMap<String, PhraseRule>()
        val tempContextRuleByKey = HashMap<String, ContextRule>()
        val tempGrammarRules = ArrayList<GrammarRule>()
        val tempDefaultSuffixMap = HashMap<String, String>()
        val tempSuffixSet = HashSet<String>()

        try {
            val root = JSONObject(jsonString)

            // Optional schema version check
            val version = root.optInt("version", 1)
            if (version < 0) {
                throw EmigrantValidationException("Invalid ruleset schema version: $version")
            }

            // 1. Parse global default suffixes from JSON
            val defaultSuffixesObj = root.optJSONObject("default_suffixes")
            if (defaultSuffixesObj != null) {
                val keys = defaultSuffixesObj.keys()
                while (keys.hasNext()) {
                    val rawKey = keys.next()
                    val outSuffix = defaultSuffixesObj.optString(rawKey, "")
                    val inSuffix = rawKey.lowercase(Locale.ROOT).trim()
                    if (inSuffix.isNotEmpty()) {
                        if (tempDefaultSuffixMap.containsKey(inSuffix)) {
                            val existingOut = tempDefaultSuffixMap[inSuffix]
                            if (existingOut != outSuffix) {
                                throw EmigrantValidationException("Conflicting duplicate default suffix definition for '$inSuffix': '$existingOut' vs '$outSuffix'")
                            }
                        } else {
                            tempDefaultSuffixMap[inSuffix] = outSuffix
                            tempSuffixSet.add(inSuffix)
                        }
                    }
                }
            }

            // 2. Parse word rules (supporting single "replacement" and array "replacements", and grouping identical matches)
            val wordRulesArray = root.optJSONArray("word_rules")
            if (wordRulesArray != null) {
                for (i in 0 until wordRulesArray.length()) {
                    val ruleObj = wordRulesArray.optJSONObject(i)
                        ?: throw EmigrantValidationException("Invalid word_rule entry at index $i")
                    val id = ruleObj.optString("id", "word_$i")
                    val match = ruleObj.optString("match").trim()
                    val autoSuffixes = ruleObj.optBoolean("auto_suffixes", true)

                    if (match.isEmpty()) {
                        throw EmigrantValidationException("Word rule at index $i is missing required 'match' string.")
                    }

                    // Extract replacement alternatives
                    val replacementsList = ArrayList<String>()
                    val singleRep = ruleObj.optString("replacement", "").trim()
                    if (singleRep.isNotEmpty()) {
                        replacementsList.add(singleRep)
                    }
                    val multiReps = ruleObj.optJSONArray("replacements")
                    if (multiReps != null) {
                        for (r in 0 until multiReps.length()) {
                            val rep = multiReps.optString(r, "").trim()
                            if (rep.isNotEmpty()) {
                                replacementsList.add(rep)
                            }
                        }
                    }

                    val distinctReplacements = replacementsList.distinct()
                    if (distinctReplacements.isEmpty()) {
                        throw EmigrantValidationException("Word rule at index $i ('$match') is missing required replacement(s).")
                    }

                    val lowerMatch = match.lowercase(Locale.ROOT)

                    val explicitSuffixMap = HashMap<String, String>()
                    val suffixesObj = ruleObj.optJSONObject("suffixes")
                    if (suffixesObj != null) {
                        val keys = suffixesObj.keys()
                        while (keys.hasNext()) {
                            val rawKey = keys.next()
                            val outSuffix = suffixesObj.optString(rawKey, "")
                            val inSuffix = rawKey.lowercase(Locale.ROOT).trim()
                            if (inSuffix.isNotEmpty()) {
                                explicitSuffixMap[inSuffix] = outSuffix
                                tempSuffixSet.add(inSuffix)
                            }
                        }
                    }

                    if (tempRuleByMatch.containsKey(lowerMatch)) {
                        // Group duplicate match entries if configurations are compatible
                        val existing = tempRuleByMatch[lowerMatch]!!
                        if (existing.autoSuffixes != autoSuffixes || existing.explicitSuffixes != explicitSuffixMap) {
                            throw EmigrantValidationException("Conflicting configuration for duplicate word rule '$lowerMatch'")
                        }
                        val combined = (existing.replacements + distinctReplacements).distinct()
                        val updatedRule = existing.copy(replacements = combined)
                        tempRuleByMatch[lowerMatch] = updatedRule
                        tempExactRules[lowerMatch] = updatedRule.replacements.first()
                    } else {
                        val rule = WordRule(id, lowerMatch, distinctReplacements, autoSuffixes, explicitSuffixMap)
                        tempRuleByMatch[lowerMatch] = rule
                        tempExactRules[lowerMatch] = distinctReplacements.first()
                    }
                }
            }

            // 3. Parse phrase rules (supporting single "replacement" and array "replacements", and grouping identical phrase matches)
            val phraseRulesArray = root.optJSONArray("phrase_rules")
            if (phraseRulesArray != null) {
                for (i in 0 until phraseRulesArray.length()) {
                    val ruleObj = phraseRulesArray.optJSONObject(i)
                        ?: throw EmigrantValidationException("Invalid phrase_rule entry at index $i")
                    val id = ruleObj.optString("id", "phrase_$i")
                    val match = ruleObj.optString("match").trim()

                    if (match.isEmpty()) {
                        throw EmigrantValidationException("Phrase rule at index $i is missing required 'match' string.")
                    }

                    val replacementsList = ArrayList<String>()
                    val singleRep = ruleObj.optString("replacement", "").trim()
                    if (singleRep.isNotEmpty()) {
                        replacementsList.add(singleRep)
                    }
                    val multiReps = ruleObj.optJSONArray("replacements")
                    if (multiReps != null) {
                        for (r in 0 until multiReps.length()) {
                            val rep = multiReps.optString(r, "").trim()
                            if (rep.isNotEmpty()) {
                                replacementsList.add(rep)
                            }
                        }
                    }

                    val distinctReplacements = replacementsList.distinct()
                    if (distinctReplacements.isEmpty()) {
                        throw EmigrantValidationException("Phrase rule at index $i ('$match') is missing required replacement(s).")
                    }

                    val lowerPhraseMatch = match.lowercase(Locale.ROOT)
                    val matchWords = lowerPhraseMatch.split("\\s+".toRegex()).filter { it.isNotEmpty() }
                    if (matchWords.size < 2) {
                        throw EmigrantValidationException("Phrase rule '$match' must contain at least two words.")
                    }

                    if (tempPhraseRuleByMatch.containsKey(lowerPhraseMatch)) {
                        val existing = tempPhraseRuleByMatch[lowerPhraseMatch]!!
                        val combined = (existing.replacements + distinctReplacements).distinct()
                        tempPhraseRuleByMatch[lowerPhraseMatch] = existing.copy(replacements = combined)
                    } else {
                        tempPhraseRuleByMatch[lowerPhraseMatch] = PhraseRule(id, lowerPhraseMatch, matchWords, distinctReplacements)
                    }
                }
            }

            // 4. Parse context sentence rules (supporting single "replacement" and array "replacements", and grouping identical context condition keys)
            val contextRulesArray = root.optJSONArray("context_rules")
            if (contextRulesArray != null) {
                for (i in 0 until contextRulesArray.length()) {
                    val ruleObj = contextRulesArray.optJSONObject(i)
                        ?: throw EmigrantValidationException("Invalid context_rule entry at index $i")
                    val id = ruleObj.optString("id", "context_$i")
                    val target = ruleObj.optString("target").trim().lowercase(Locale.ROOT)

                    if (target.isEmpty()) {
                        throw EmigrantValidationException("Context rule at index $i is missing required 'target' string.")
                    }

                    val replacementsList = ArrayList<String>()
                    val singleRep = ruleObj.optString("replacement", "").trim()
                    if (singleRep.isNotEmpty()) {
                        replacementsList.add(singleRep)
                    }
                    val multiReps = ruleObj.optJSONArray("replacements")
                    if (multiReps != null) {
                        for (r in 0 until multiReps.length()) {
                            val rep = multiReps.optString(r, "").trim()
                            if (rep.isNotEmpty()) {
                                replacementsList.add(rep)
                            }
                        }
                    }

                    val distinctReplacements = replacementsList.distinct()
                    if (distinctReplacements.isEmpty()) {
                        throw EmigrantValidationException("Context rule at index $i ('$target') is missing required replacement(s).")
                    }

                    val containsArray = ruleObj.optJSONArray("contains")
                        ?: throw EmigrantValidationException("Context rule at index $i ('$target') is missing required 'contains' array.")

                    val containsList = ArrayList<String>()
                    for (j in 0 until containsArray.length()) {
                        val word = containsArray.optString(j, "").trim().lowercase(Locale.ROOT)
                        if (word.isNotEmpty()) {
                            containsList.add(word)
                        }
                    }

                    if (containsList.size < 2) {
                        throw EmigrantValidationException("Context rule '$id' must contain at least two words in 'contains' array.")
                    }

                    if (!containsList.contains(target)) {
                        throw EmigrantValidationException("Context rule '$id' target '$target' must be present in 'contains' array $containsList.")
                    }

                    val contextKey = containsList.sorted().joinToString("+") + "->" + target
                    if (tempContextRuleByKey.containsKey(contextKey)) {
                        val existing = tempContextRuleByKey[contextKey]!!
                        val combined = (existing.replacements + distinctReplacements).distinct()
                        tempContextRuleByKey[contextKey] = existing.copy(replacements = combined)
                    } else {
                        tempContextRuleByKey[contextKey] = ContextRule(id, containsList, target, distinctReplacements)
                    }
                }
            }

            // 5. Parse grammar pattern rules
            val grammarRulesArray = root.optJSONArray("grammar_rules")
            if (grammarRulesArray != null) {
                for (i in 0 until grammarRulesArray.length()) {
                    val ruleObj = grammarRulesArray.optJSONObject(i)
                        ?: throw EmigrantValidationException("Invalid grammar_rule entry at index $i")
                    val id = ruleObj.optString("id", "grammar_$i")

                    val subjectsArray = ruleObj.optJSONArray("subjects")
                        ?: throw EmigrantValidationException("Grammar rule at index $i is missing required 'subjects' array.")

                    val subjectsList = ArrayList<String>()
                    for (j in 0 until subjectsArray.length()) {
                        val subj = subjectsArray.optString(j, "").trim().lowercase(Locale.ROOT)
                        if (subj.isNotEmpty()) {
                            subjectsList.add(subj)
                        }
                    }

                    if (subjectsList.isEmpty()) {
                        throw EmigrantValidationException("Grammar rule '$id' must contain at least one subject word.")
                    }

                    val verbReplacementsObj = ruleObj.optJSONObject("verb_replacements")
                        ?: throw EmigrantValidationException("Grammar rule '$id' is missing required 'verb_replacements' object.")

                    val verbMap = HashMap<String, String>()
                    val verbKeys = verbReplacementsObj.keys()
                    while (verbKeys.hasNext()) {
                        val rawVerbKey = verbKeys.next()
                        val replacementVerb = verbReplacementsObj.optString(rawVerbKey, "").trim()
                        val lowerVerbKey = rawVerbKey.lowercase(Locale.ROOT).trim()
                        if (lowerVerbKey.isNotEmpty() && replacementVerb.isNotEmpty()) {
                            verbMap[lowerVerbKey] = replacementVerb
                        }
                    }

                    if (verbMap.isEmpty()) {
                        throw EmigrantValidationException("Grammar rule '$id' must contain at least one verb replacement mapping.")
                    }

                    tempGrammarRules.add(GrammarRule(id, subjectsList, verbMap))
                }
            }

            // Sort phrase rules by word count descending (longest phrase match first)
            val tempPhraseRules = ArrayList(tempPhraseRuleByMatch.values)
            tempPhraseRules.sortByDescending { it.matchWords.size }

            // Sort context rules by specific matching condition priority (most contains words first)
            val tempContextRules = ArrayList(tempContextRuleByKey.values)
            tempContextRules.sortWith(Comparator { a, b ->
                b.contains.size.compareTo(a.contains.size)
            })

            // Dynamic suffix indexing sorted by length descending
            val tempSuffixesSorted = ArrayList(tempSuffixSet)
            tempSuffixesSorted.sortWith(Comparator { a, b ->
                if (a.length != b.length) b.length.compareTo(a.length)
                else a.compareTo(b)
            })

            return ParsedRuleset(
                exactRules = tempExactRules,
                ruleByMatch = tempRuleByMatch,
                phraseRules = tempPhraseRules,
                contextRules = tempContextRules,
                grammarRules = tempGrammarRules,
                defaultSuffixMap = tempDefaultSuffixMap,
                allInputSuffixesSorted = tempSuffixesSorted
            )
        } catch (e: Exception) {
            if (e is EmigrantValidationException) throw e
            throw EmigrantValidationException("Failed to parse rules JSON: ${e.message}", e)
        }
    }

    /**
     * Validates and parses the JSON string containing Emigrant rules.
     */
    fun validateAndParseJson(jsonString: String) {
        val parsed = validateJson(jsonString)
        activateParsedRuleset(parsed)
    }

    /**
     * Activates a validated ParsedRuleset into active memory.
     */
    fun activateParsedRuleset(parsed: ParsedRuleset) {
        synchronized(this) {
            exactRules.clear()
            exactRules.putAll(parsed.exactRules)

            ruleByMatch.clear()
            ruleByMatch.putAll(parsed.ruleByMatch)

            phraseRules.clear()
            phraseRules.addAll(parsed.phraseRules)

            contextRules.clear()
            contextRules.addAll(parsed.contextRules)

            grammarRules.clear()
            grammarRules.addAll(parsed.grammarRules)

            defaultSuffixMap.clear()
            defaultSuffixMap.putAll(parsed.defaultSuffixMap)

            allInputSuffixesSorted.clear()
            allInputSuffixesSorted.addAll(parsed.allInputSuffixesSorted)

            wordHistory.clear()
            sentenceHistory.clear()
            isPendingSentenceReset = false
        }
    }

    /**
     * Parses and activates JSON ruleset in memory.
     */
    fun parseRulesJson(jsonString: String) {
        validateAndParseJson(jsonString)
    }

    /**
     * Called during word commit in InputLogic.
     * Evaluates single-word rules, grammar rules, and forward-order context rules, records history, and returns EmigrantResult.
     */
    fun processWordCommit(
        typedWord: String?,
        autocorrectedWord: String?,
        separator: String,
        settingsValues: SettingsValues? = null
    ): EmigrantResult {
        val typed = typedWord ?: ""
        val autocorrected = autocorrectedWord ?: ""

        // If Emigrant mode is disabled or password field, bypass transformations
        if (!isEmigrantModeEnabled || (settingsValues != null && settingsValues.mInputAttributes.mIsPasswordField)) {
            clearHistory()
            return EmigrantResult(autocorrected, false)
        }

        val candidateTypedWord = if (typed.isNotEmpty()) typed else autocorrected
        val wordToTest = if (autocorrected.isNotEmpty()) autocorrected else typed
        val wordProcessed = processWordResult(wordToTest)

        var transformed = wordProcessed.word
        var isEmigrantGenerated = wordProcessed.isGenerated
        var appliedCategory = wordProcessed.category

        var contextApplied = false

        synchronized(this) {
            // Apply deferred pending sentence reset if previous word/punctuation ended a sentence
            if (isPendingSentenceReset) {
                sentenceHistory.clear()
                isPendingSentenceReset = false
            }

            // Check Grammar Rules (if no single-word rule matched, and sentence history has preceding subject)
            if (isGrammarRulesEnabled && appliedCategory == RuleCategory.NONE && grammarRules.isNotEmpty() && sentenceHistory.isNotEmpty()) {
                val previousEntry = sentenceHistory.last()
                val prevTyped = previousEntry.typedWord.lowercase(Locale.ROOT)
                val prevDisplayed = previousEntry.displayedWord.lowercase(Locale.ROOT)
                val lowerVerbKey = wordToTest.lowercase(Locale.ROOT)

                for (rule in grammarRules) {
                    if (rule.subjects.contains(prevTyped) || rule.subjects.contains(prevDisplayed)) {
                        if (rule.verbReplacements.containsKey(lowerVerbKey)) {
                            val replacementVerb = rule.verbReplacements[lowerVerbKey]!!
                            transformed = preserveCase(candidateTypedWord, replacementVerb)
                            isEmigrantGenerated = true
                            appliedCategory = RuleCategory.GRAMMAR
                            Log.i(TAG, "Applied grammar rule '${rule.id}'")
                            break
                        }
                    }
                }
            }

            // Check Forward-Order Context Rules (highest priority, can override word rules)
            if (isContextRulesEnabled && contextRules.isNotEmpty()) {
                val candidateSentence = ArrayList(sentenceHistory)
                val testEntry = EmigrantWord(
                    typedWord = candidateTypedWord,
                    autocorrectedWord = autocorrected,
                    displayedWord = transformed,
                    separator = separator,
                    isEmigrantGenerated = isEmigrantGenerated,
                    contextApplied = false,
                    ruleCategory = appliedCategory
                )
                candidateSentence.add(testEntry)

                val sentenceWords = candidateSentence.map { it.displayedWord.lowercase(Locale.ROOT) }
                val typedSentenceWords = candidateSentence.map { it.typedWord.lowercase(Locale.ROOT) }

                for (rule in contextRules) {
                    val lowerTarget = rule.target.lowercase(Locale.ROOT)
                    val lowerCurrent = candidateTypedWord.lowercase(Locale.ROOT)

                    if (lowerCurrent == lowerTarget || transformed.lowercase(Locale.ROOT) == lowerTarget) {
                        // Check if all required words in rule.contains are present in candidate sentence
                        var allPresent = true
                        for (required in rule.contains) {
                            if (!sentenceWords.contains(required) && !typedSentenceWords.contains(required)) {
                                allPresent = false
                                break
                            }
                        }
                        if (allPresent) {
                            if (!testEntry.contextApplied) {
                                val selectedReplacement = rule.selectReplacement(random)
                                transformed = preserveCase(candidateTypedWord, selectedReplacement)
                                isEmigrantGenerated = true
                                contextApplied = true
                                appliedCategory = RuleCategory.CONTEXT
                                Log.i(TAG, "Applied forward context rule '${rule.id}'")
                                break
                            }
                        }
                    }
                }
            }

            val entry = EmigrantWord(
                typedWord = candidateTypedWord,
                autocorrectedWord = autocorrected,
                displayedWord = transformed,
                separator = if (separator.isNotEmpty()) separator else " ",
                isEmigrantGenerated = isEmigrantGenerated,
                contextApplied = contextApplied,
                ruleCategory = appliedCategory
            )

            wordHistory.add(entry)
            if (wordHistory.size > maxHistorySize) {
                wordHistory.removeAt(0)
            }

            sentenceHistory.add(entry)
            if (sentenceHistory.size > maxSentenceHistorySize) {
                sentenceHistory.removeAt(0)
            }

            // Defer sentence history reset until after evaluation if separator contains sentence boundary
            if (isSentenceBoundary(separator)) {
                isPendingSentenceReset = true
            }
        }

        return EmigrantResult(transformed, isEmigrantGenerated)
    }

    /**
     * Checks if separator string contains sentence-ending boundary punctuation (. ! ? newline).
     */
    private fun isSentenceBoundary(separator: String): Boolean {
        if (separator.isEmpty()) return false
        return separator.any { it == '.' || it == '!' || it == '?' || it == '\n' || it == '\r' }
    }

    /**
     * Evaluates phrase rules and reverse-order context rules after a word commit.
     * Ensures deferred sentence boundary reset is executed safely upon completion.
     */
    fun evaluatePhraseRulesAfterCommit(connection: RichInputConnection?): Boolean {
        if (!isEmigrantModeEnabled || connection == null) {
            checkAndApplyPendingSentenceReset()
            return false
        }

        try {
            // 1. Evaluate Phrase Rules
            val phraseApplied = evaluatePhraseRulesInternal(connection)
            if (phraseApplied) return true

            // 2. Evaluate Reverse-Order Context Rules
            return evaluateContextRulesAfterCommitInternal(connection)
        } finally {
            checkAndApplyPendingSentenceReset()
        }
    }

    /**
     * Checks and applies deferred pending sentence reset.
     */
    private fun checkAndApplyPendingSentenceReset() {
        synchronized(this) {
            if (isPendingSentenceReset) {
                sentenceHistory.clear()
                isPendingSentenceReset = false
            }
        }
    }

    /**
     * Evaluates reverse-order context rules (e.g. "bank" typed earlier, "cash" typed now -> "bank" becomes "river bank").
     * Excludes pending final separator from expected editor string because pending separator is inserted after commit.
     * Finds unprocessed target entries (contextApplied == false).
     */
    private fun evaluateContextRulesAfterCommitInternal(connection: RichInputConnection): Boolean {
        if (!isContextRulesEnabled) return false
        synchronized(this) {
            if (sentenceHistory.size < 2 || contextRules.isEmpty()) return false

            val sentenceWords = sentenceHistory.map { it.displayedWord.lowercase(Locale.ROOT) }
            val typedSentenceWords = sentenceHistory.map { it.typedWord.lowercase(Locale.ROOT) }

            for (rule in contextRules) {
                // Check if all required words in rule.contains exist in current sentence
                var allPresent = true
                for (required in rule.contains) {
                    if (!sentenceWords.contains(required) && !typedSentenceWords.contains(required)) {
                        allPresent = false
                        break
                    }
                }
                if (!allPresent) continue

                // Check if target word is an UNPROCESSED earlier entry in sentenceHistory (not the last word)
                val targetIndex = (0 until sentenceHistory.size - 1).firstOrNull { i ->
                    val entry = sentenceHistory[i]
                    !entry.contextApplied && (
                        entry.displayedWord.lowercase(Locale.ROOT) == rule.target ||
                        entry.typedWord.lowercase(Locale.ROOT) == rule.target
                    )
                } ?: -1

                if (targetIndex < 0) continue

                // Construct expected string in text field from target word to cursor EXCLUDING pending final separator
                val expectedBuilder = StringBuilder()
                val lastWordIndex = sentenceHistory.size - 1
                for (i in targetIndex..lastWordIndex) {
                    expectedBuilder.append(sentenceHistory[i].displayedWord)
                    if (i < lastWordIndex) {
                        expectedBuilder.append(sentenceHistory[i].separator)
                    }
                }
                val expectedDisplayed = expectedBuilder.toString()

                // Verify actual text in Android InputConnection
                val textBeforeCursorCs = connection.getTextBeforeCursor(expectedDisplayed.length, 0) ?: continue
                val textBeforeCursor = textBeforeCursorCs.toString()

                if (textBeforeCursor.equals(expectedDisplayed, ignoreCase = true)) {
                    // Verification succeeded!
                    val originalTargetWord = sentenceHistory[targetIndex].typedWord
                    val selectedReplacement = rule.selectReplacement(random)
                    val replacementWord = preserveCase(originalTargetWord, selectedReplacement)

                    // Construct replacementSegment EXCLUDING pending final separator
                    val replacementBuilder = StringBuilder()
                    for (i in targetIndex..lastWordIndex) {
                        if (i == targetIndex) {
                            replacementBuilder.append(replacementWord)
                        } else {
                            replacementBuilder.append(sentenceHistory[i].displayedWord)
                        }
                        if (i < lastWordIndex) {
                            replacementBuilder.append(sentenceHistory[i].separator)
                        }
                    }
                    val replacementSegment = replacementBuilder.toString()

                    // Replace text in editor safely
                    connection.deleteTextBeforeCursor(expectedDisplayed.length)
                    connection.commitText(replacementSegment, 1)

                    // Update sentenceHistory & wordHistory for history consistency
                    val oldEntry = sentenceHistory[targetIndex]
                    val updatedEntry = oldEntry.copy(
                        displayedWord = replacementWord,
                        isEmigrantGenerated = true,
                        contextApplied = true,
                        ruleCategory = RuleCategory.CONTEXT
                    )
                    sentenceHistory[targetIndex] = updatedEntry

                    val wordHistIdx = wordHistory.indexOfLast { it.typedWord == oldEntry.typedWord && it.displayedWord == oldEntry.displayedWord }
                    if (wordHistIdx >= 0) {
                        wordHistory[wordHistIdx] = wordHistory[wordHistIdx].copy(
                            displayedWord = replacementWord,
                            isEmigrantGenerated = true,
                            contextApplied = true,
                            ruleCategory = RuleCategory.CONTEXT
                        )
                    }

                    Log.i(TAG, "Applied reverse context rule '${rule.id}'")
                    return true
                } else {
                    Log.w(TAG, "Reverse context rule '${rule.id}' skipped due to editor text mismatch")
                }
            }
        }

        return false
    }

    /**
     * Evaluates phrase rules internal helper.
     */
    private fun evaluatePhraseRulesInternal(connection: RichInputConnection): Boolean {
        if (!isPhraseRulesEnabled) return false
        synchronized(this) {
            if (wordHistory.isEmpty() || phraseRules.isEmpty()) return false

            for (rule in phraseRules) {
                val n = rule.matchWords.size
                if (wordHistory.size < n) continue

                val recentN = wordHistory.takeLast(n)

                // 1. Check if typed words match the phrase rule
                var matchesTyped = true
                for (i in 0 until n) {
                    val historyWord = recentN[i].typedWord.lowercase(Locale.ROOT)
                    val expectedWord = rule.matchWords[i]
                    if (historyWord != expectedWord) {
                        matchesTyped = false
                        break
                    }
                }
                if (!matchesTyped) continue

                // 2. Prevent recursive loops: skip if any word in candidate sequence was generated by EmigrantEngine
                if (recentN.any { it.isEmigrantGenerated }) {
                    continue
                }

                // 3. Construct expected displayed string in text field before cursor
                val expectedBuilder = StringBuilder()
                for (i in 0 until n) {
                    expectedBuilder.append(recentN[i].displayedWord)
                    if (i < n - 1) {
                        expectedBuilder.append(recentN[i].separator)
                    }
                }
                val expectedDisplayed = expectedBuilder.toString()

                // 4. Verify actual text in Android InputConnection
                val textBeforeCursorCs = connection.getTextBeforeCursor(expectedDisplayed.length, 0) ?: continue
                val textBeforeCursor = textBeforeCursorCs.toString()

                if (textBeforeCursor.equals(expectedDisplayed, ignoreCase = true)) {
                    // Verification succeeded!
                    val firstTypedWord = recentN[0].typedWord
                    val selectedReplacement = rule.selectReplacement(random)
                    val finalReplacement = preserveCase(firstTypedWord, selectedReplacement)

                    // Safe replacement in text field
                    connection.deleteTextBeforeCursor(expectedDisplayed.length)
                    connection.commitText(finalReplacement, 1)

                    // Update history: replace last n entries with combined entry
                    repeat(n) {
                        wordHistory.removeAt(wordHistory.size - 1)
                    }
                    val combinedEntry = EmigrantWord(
                        typedWord = rule.match,
                        autocorrectedWord = finalReplacement,
                        displayedWord = finalReplacement,
                        separator = "",
                        isEmigrantGenerated = true,
                        contextApplied = false,
                        ruleCategory = RuleCategory.PHRASE
                    )
                    wordHistory.add(combinedEntry)

                    // Synchronize sentenceHistory if matching entries exist
                    if (sentenceHistory.size >= n) {
                        repeat(n) {
                            sentenceHistory.removeAt(sentenceHistory.size - 1)
                        }
                        sentenceHistory.add(combinedEntry)
                    }

                    Log.i(TAG, "Applied phrase rule '${rule.match}'")
                    return true
                } else {
                    Log.w(TAG, "Phrase rule '${rule.match}' skipped due to editor text mismatch")
                }
            }
        }

        return false
    }

    private data class ProcessedWordResult(
        val word: String,
        val isGenerated: Boolean,
        val category: RuleCategory
    )

    /**
     * Processes a single completed word and returns word, whether generated, and rule category.
     */
    private fun processWordResult(word: String?): ProcessedWordResult {
        if (!isEmigrantModeEnabled || word.isNullOrEmpty()) {
            return ProcessedWordResult(word ?: "", false, RuleCategory.NONE)
        }
        val lowercaseKey = word.lowercase(Locale.ROOT)

        // 1. Exact match priority
        if (isWordRulesEnabled) {
            val rule = ruleByMatch[lowercaseKey]
            if (rule != null) {
                val selectedReplacement = rule.selectReplacement(random)
                return ProcessedWordResult(preserveCase(word, selectedReplacement), true, RuleCategory.WORD)
            }
        }

        // 2. Dynamic suffix match
        if (isAutoSuffixesEnabled) {
            for (inSuffix in allInputSuffixesSorted) {
                if (lowercaseKey.length > inSuffix.length && lowercaseKey.endsWith(inSuffix)) {
                    val directStem = lowercaseKey.dropLast(inSuffix.length)

                    val candidateStems = ArrayList<String>(4)
                    candidateStems.add(directStem)

                    if (inSuffix == "ing" || inSuffix == "ed" || inSuffix == "able") {
                        candidateStems.add("${directStem}e")
                    }

                    if (directStem.endsWith("i")) {
                        candidateStems.add("${directStem.dropLast(1)}y")
                    }

                    val firstInChar = inSuffix[0]
                    val isVowelSuffix = firstInChar == 'a' || firstInChar == 'e' || firstInChar == 'i' || firstInChar == 'o' || firstInChar == 'u'
                    if (isVowelSuffix && directStem.length >= 2 && directStem[directStem.length - 1] == directStem[directStem.length - 2]) {
                        candidateStems.add(directStem.dropLast(1))
                    }

                    for (stem in candidateStems) {
                        val rule = ruleByMatch[stem]
                        if (rule != null) {
                            val baseReplacement = rule.selectReplacement(random)
                            if (rule.explicitSuffixes.containsKey(inSuffix)) {
                                val outSuffix = rule.explicitSuffixes[inSuffix]!!
                                val result = formatSuffixJoin(baseReplacement, outSuffix)
                                return ProcessedWordResult(preserveCase(word, result), true, RuleCategory.SUFFIX)
                            }
                            if (rule.autoSuffixes && defaultSuffixMap.containsKey(inSuffix)) {
                                val outSuffix = defaultSuffixMap[inSuffix]!!
                                val result = formatSuffixJoin(baseReplacement, outSuffix)
                                return ProcessedWordResult(preserveCase(word, result), true, RuleCategory.SUFFIX)
                            }
                        }
                    }
                }
            }
        }

        return ProcessedWordResult(word, false, RuleCategory.NONE)
    }

    /**
     * Processes a single completed word (after normal autocorrect has finished).
     */
    fun processWord(word: String?): String {
        return processWordResult(word).word
    }

    /**
     * Joins base replacement with output suffix according to formatting rules.
     */
    private fun formatSuffixJoin(baseReplacement: String, outSuffix: String): String {
        if (outSuffix.isEmpty()) return baseReplacement
        var base = baseReplacement
        val lowerSuffix = outSuffix.lowercase(Locale.ROOT)
        if (base.endsWith("e") && (lowerSuffix == "ing" || lowerSuffix == "ed" || lowerSuffix == "es")) {
            base = base.dropLast(1)
        } else if (base.endsWith("y") && (lowerSuffix.startsWith("e") || lowerSuffix.startsWith("i"))) {
            base = base.dropLast(1) + "i"
        }
        return base + outSuffix
    }

    /**
     * Preserves capitalization style of the original word when applying the replacement.
     */
    private fun preserveCase(original: String, replacement: String): String {
        if (original.isEmpty() || replacement.isEmpty()) return replacement
        if (original.all { it.isUpperCase() }) {
            return replacement.uppercase(Locale.ROOT)
        }
        if (original[0].isUpperCase()) {
            return replacement.substring(0, 1).uppercase(Locale.ROOT) + replacement.substring(1)
        }
        return replacement
    }

    /**
     * Resets internal state (useful for unit testing).
     */
    fun resetForTesting() {
        synchronized(this) {
            exactRules.clear()
            ruleByMatch.clear()
            phraseRules.clear()
            contextRules.clear()
            grammarRules.clear()
            allInputSuffixesSorted.clear()
            defaultSuffixMap.clear()
            wordHistory.clear()
            sentenceHistory.clear()
            isPendingSentenceReset = false
            isInitialized = false
            isEmigrantModeEnabled = true
            isWordRulesEnabled = true
            isAutoSuffixesEnabled = true
            isPhraseRulesEnabled = true
            isContextRulesEnabled = true
            isGrammarRulesEnabled = true
            random = Random()
        }
    }

    companion object {
        private const val TAG = "EmigrantEngine"
        const val RULES_FILE_NAME = "emigrant_rules.json"

        @JvmStatic
        val instance: EmigrantEngine by lazy { EmigrantEngine() }
    }
}
