package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.callGemini
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

data class VoiceInputState(
    val isListening: Boolean = false,
    val transcript: String = "",
    val error: String? = null,
    val isAnalyzing: Boolean = false,
    val analyzedTopic: String? = null,
    val analyzedSentiment: String? = null, // "Confident", "Anxious", "Reflective", etc.
    val scoreDelta: Float = 0f,
    val insight: String? = null,
    val rmsLevel: Float = 0f
)

class EchoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EchoRepository
    val allNotes: StateFlow<List<JournalNote>>
    val allDecisions: StateFlow<List<Decision>>
    val cognitiveProfiles: StateFlow<List<CognitiveProfile>>

    // Voice/Microphone Analysis State Flow
    private val _voiceInputState = MutableStateFlow(VoiceInputState())
    val voiceInputState: StateFlow<VoiceInputState> = _voiceInputState.asStateFlow()

    fun updateVoiceListening(listening: Boolean) {
        _voiceInputState.value = _voiceInputState.value.copy(
            isListening = listening,
            error = null,
            isAnalyzing = if (listening) false else _voiceInputState.value.isAnalyzing
        )
    }

    fun updateVoiceRms(level: Float) {
        _voiceInputState.value = _voiceInputState.value.copy(rmsLevel = level)
    }

    fun setVoiceTranscript(text: String) {
        _voiceInputState.value = _voiceInputState.value.copy(transcript = text, error = null)
    }

    fun setVoiceError(errorMsg: String) {
        _voiceInputState.value = _voiceInputState.value.copy(
            error = errorMsg,
            isListening = false
        )
    }

    fun clearVoiceState() {
        _voiceInputState.value = VoiceInputState()
    }

    // UI state
    private val _selectedDimension = MutableStateFlow<String>("Curiosity")
    val selectedDimension: StateFlow<String> = _selectedDimension.asStateFlow()

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("EchoSystem", "Welcome to EchoSelf. I am your cognitive operating system. Write notes, capture decisions, and view your mental constellation to help me calibrate your cognitive mirror.")
    ))
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    private val _selectedSpecialist = MutableStateFlow("Mirror") // "Mirror", "Planner", "Teacher", "Critic", "Optimist", "Scientist"
    val selectedSpecialist: StateFlow<String> = _selectedSpecialist.asStateFlow()

    private val _weeklyReport = MutableStateFlow<String>(
        "Enter a few daily journal notes and decisions to enable Gemini to generate a Weekly Cognitive Mirror report."
    )
    val weeklyReport: StateFlow<String> = _weeklyReport.asStateFlow()

    private val _isReportLoading = MutableStateFlow(false)
    val isReportLoading: StateFlow<Boolean> = _isReportLoading.asStateFlow()

    init {
        val database = EchoDatabase.getDatabase(application)
        val dao = database.echoDao()
        repository = EchoRepository(dao)

        allNotes = repository.allNotes
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allDecisions = repository.allDecisions
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        cognitiveProfiles = repository.cognitiveProfiles
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Ensure we pre-populate empty profiles on first launch
        viewModelScope.launch(Dispatchers.IO) {
            repository.preseedProfilesIfEmpty()
        }
    }

    fun selectDimension(dim: String) {
        _selectedDimension.value = dim
    }

    fun selectSpecialist(spec: String) {
        _selectedSpecialist.value = spec
    }

    // Add visual journaling / study notes
    fun addJournalNote(content: String, category: String, tags: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val note = JournalNote(
                content = content,
                category = category,
                tagString = tags
            )
            repository.insertNote(note)

            // Local fallback smart heuristic updates immediately (offline reactivity)
            applyLocalHeuristicForNote(category, content)

            // Asynchronous API insight-driven cognitive update
            analyzeCognitiveImpactWithGemini(note)
        }
    }

    fun deleteNote(noteId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNoteById(noteId)
        }
    }

    // Add critical decision replays
    fun addDecision(title: String, expectedOutcome: String, confidence: Int, emotionalState: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val decision = Decision(
                title = title,
                expectedOutcome = expectedOutcome,
                confidence = confidence,
                emotionalState = emotionalState
            )
            repository.insertDecision(decision)

            // Decision updates decision-making and risk dimension immediately
            applyLocalHeuristicForDecision(decision)

            // Direct async content analysis to understand biases
            analyzeDecisionBiasWithGemini(decision)
        }
    }

    // Double loop learning: record actual outcome weeks/months later
    fun evaluateDecision(id: Int, actualOutcome: String, lessonsLearned: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val originalDecision = allDecisions.value.find { it.id == id } ?: return@launch
            val updated = originalDecision.copy(
                actualOutcome = actualOutcome,
                lessonsLearned = lessonsLearned,
                reviewedTimestamp = System.currentTimeMillis()
            )
            repository.updateDecision(updated)
            
            // Recompute calibration and learning metrics
            applyLocalEvaluationHeuristic(updated)
            
            // Background outcome gap analysis
            evaluateDecisionOutcomeGapWithGemini(updated)
        }
    }

    fun deleteDecision(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteDecisionById(id)
        }
    }

    // Smart Local Offline Heuristics
    private suspend fun applyLocalHeuristicForNote(category: String, content: String) {
        val currentDimMap = cognitiveProfiles.value.associateBy { it.dimension }
        val deltaMap = when (category) {
            "Curiosity" -> mapOf("Curiosity" to 0.08f, "Memory" to 0.02f)
            "Study" -> mapOf("Learning" to 0.10f, "Curiosity" to 0.04f, "Memory" to 0.04f)
            "Work" -> mapOf("Productivity" to 0.08f, "Focus" to 0.06f, "Communication" to 0.02f)
            "Ambition" -> mapOf("Creativity" to 0.10f, "Risk" to 0.05f, "Preference" to 0.03f)
            else -> mapOf("Creativity" to 0.05f, "Communication" to 0.05f)
        }

        deltaMap.forEach { (dim, delta) ->
            val profile = currentDimMap[dim] ?: CognitiveProfile(dim, 0.5f, "")
            val newScore = (profile.score + delta).coerceIn(0.1f, 1.0f)
            repository.insertProfile(profile.copy(score = newScore, lastUpdated = System.currentTimeMillis()))
        }
    }

    private suspend fun applyLocalHeuristicForDecision(decision: Decision) {
        val currentDimMap = cognitiveProfiles.value.associateBy { it.dimension }
        
        // Add delta to Decision and Risk profile based on the decision character
        val decisionProfile = currentDimMap["Decision"] ?: CognitiveProfile("Decision", 0.5f, "")
        val riskProfile = currentDimMap["Risk"] ?: CognitiveProfile("Risk", 0.5f, "")
        
        val riskDelta = if (decision.emotionalState == "Excited" || decision.confidence > 8) 0.08f else -0.02f
        val newDecisionScore = (decisionProfile.score + 0.05f).coerceIn(0.1f, 1.0f)
        val newRiskScore = (riskProfile.score + riskDelta).coerceIn(0.1f, 1.0f)

        repository.insertProfile(decisionProfile.copy(score = newDecisionScore, lastUpdated = System.currentTimeMillis()))
        repository.insertProfile(riskProfile.copy(score = newRiskScore, lastUpdated = System.currentTimeMillis()))
    }

    private suspend fun applyLocalEvaluationHeuristic(decision: Decision) {
        val currentDimMap = cognitiveProfiles.value.associateBy { it.dimension }
        
        val learningProfile = currentDimMap["Learning"] ?: CognitiveProfile("Learning", 0.5f, "")
        val decisionProfile = currentDimMap["Decision"] ?: CognitiveProfile("Decision", 0.5f, "")

        val newLearning = (learningProfile.score + 0.08f).coerceIn(0.1f, 1.0f)
        val newDecision = (decisionProfile.score + 0.06f).coerceIn(0.1f, 1.0f)

        repository.insertProfile(learningProfile.copy(score = newLearning, lastUpdated = System.currentTimeMillis()))
        repository.insertProfile(decisionProfile.copy(score = newDecision, lastUpdated = System.currentTimeMillis()))
    }

    // AI Cognitive update
    private suspend fun analyzeCognitiveImpactWithGemini(note: JournalNote) {
        val systemInstruction = "You are EchoSelf, an advanced neural-analytics assistant tracking human thought development. You output purely in a concise, clean format."
        val prompt = """
            Analyze this user's journal note:
            "${note.content}"
            Categorized as: ${note.category}
            
            Based on this, tell me exactly which one of these 10 cognitive dimensions it primarily improves or exhibits (Choice: Curiosity, Decision, Learning, Creativity, Focus, Communication, Risk, Productivity, Memory, Preference).
            
            Return in extreme brevity as a single line, exactly like this format:
            TARGET: <dimension>, SCORE_DELTA: <float between 0.01 and 0.15>, SUMMARY: <a 1-sentence poetic feedback with deep insight about why their thinking pattern of ${note.content.take(15)} behaves this way>
        """.trimIndent()

        val answer = try {
            callGemini(prompt, systemInstruction, 0.4f)
        } catch (e: Exception) { "" }

        if (answer.startsWith("TARGET:")) {
            parseAndApplyGeminiDelta(answer)
        }
    }

    private suspend fun analyzeDecisionBiasWithGemini(decision: Decision) {
        val systemInstruction = "You are the EchoSelf Decision calibrator. You pinpoint internal planning and cognitive biases."
        val prompt = """
            Analyze this decision:
            Title: ${decision.title}
            Expected Outcome: ${decision.expectedOutcome}
            My confidence: ${decision.confidence}/10
            My emotional state: ${decision.emotionalState}
            
            Analyze what mental biases is this user possibly showing? Overconfidence? Planning fallacy? Wishful thinking? Base-rate neglect?
            Keep your analysis under 100 words. Be remarkably constructive and poetic.
        """.trimIndent()

        val analysisResult = try {
            callGemini(prompt, systemInstruction, 0.5f)
        } catch (e: Exception) { "Biases calibration pending online connectivity." }

        // Save AI analysis back to this decision
        viewModelScope.launch(Dispatchers.IO) {
            val updated = decision.copy(aiPatternAnalysis = analysisResult)
            repository.updateDecision(updated)
        }
    }

    private suspend fun evaluateDecisionOutcomeGapWithGemini(decision: Decision) {
        val systemInstruction = "You are EchoSelf, an advanced cognitive scientist running double-loop retro analysis."
        val prompt = """
            Analyze this outcome gap evaluation:
            Decision Title: ${decision.title}
            Expected Outcome: ${decision.expectedOutcome}
            Actual Outcome: ${decision.actualOutcome}
            Lessons Learned: ${decision.lessonsLearned}
            Prior Confidence: ${decision.confidence}/10
            Prior Emotional State: ${decision.emotionalState}
            
            Synthesize what they underestimated or overthought, and print a 2-sentence feedback called 'Cognitive Lesson' to calibrate their judgment.
        """.trimIndent()

        val analysis = try {
            callGemini(prompt, systemInstruction, 0.4f)
        } catch (e: Exception) { "Outcome analysis pending network synchronization." }

        viewModelScope.launch(Dispatchers.IO) {
            val updated = decision.copy(aiPatternAnalysis = analysis)
            repository.updateDecision(updated)
        }
    }

    private suspend fun parseAndApplyGeminiDelta(responseLine: String) {
        try {
            // TARGET: Focus, SCORE_DELTA: 0.08, SUMMARY: Your choice highlights a focused alignment to clarity.
            val parts = responseLine.split(",")
            val targetPart = parts.find { it.contains("TARGET:") } ?: return
            val deltaPart = parts.find { it.contains("SCORE_DELTA:") } ?: return
            val summaryPart = parts.find { it.contains("SUMMARY:") } ?: return

            val dimension = targetPart.substringAfter("TARGET:").trim()
            val scoreDelta = deltaPart.substringAfter("SCORE_DELTA:").trim().toFloatOrNull() ?: 0.05f
            val summary = summaryPart.substringAfter("SUMMARY:").trim()

            val profiles = cognitiveProfiles.value.associateBy { it.dimension }
            val current = profiles[dimension] ?: return

            val updatedScore = (current.score + scoreDelta).coerceIn(0.1f, 1.0f)
            repository.insertProfile(
                CognitiveProfile(
                    dimension = dimension,
                    score = updatedScore,
                    description = summary,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            // Catch silently on parsing deviations
        }
    }

    // Ask cognitive specialists or Best historical self query
    fun askEchoSelf(userQuestion: String) {
        val currentSpecialist = _selectedSpecialist.value
        val historyList = _chatHistory.value.toMutableList()
        historyList.add(ChatMessage("User", userQuestion))
        _chatHistory.value = historyList

        _isChatLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            // Get user's context (top journal notes and decisions) to inject into prompt for real personalized mirror response
            val topNotesText = allNotes.value.take(6).joinToString("; ") { "[${it.category}] ${it.content}" }
            val decisionLogsText = allDecisions.value.take(5).joinToString("; ") {
                "${it.title} (expected: ${it.expectedOutcome}, actual: ${it.actualOutcome ?: "Pending"}, confidence: ${it.confidence}/10, emotion: ${it.emotionalState})"
            }
            val profileMetrics = cognitiveProfiles.value.joinToString(", ") { "${it.dimension}: ${(it.score * 100).toInt()}%" }

            val cognitiveContext = """
                These are the private models of how this individual currently thinks, learns, and decides:
                - Static Profiles: $profileMetrics
                - Recent Notes: $topNotesText
                - Decision History: $decisionLogsText
                
                You MUST answer strictly using this specific individual's cognitive footprint. Never talk in generalized averages. Talk directly to user like an intimate neural mirror. 
            """.trimIndent()

            val specialistInstruction = when (currentSpecialist) {
                "Mirror" -> "You are EchoSelf, the core Mirror. Reflect back patterns in their questions, drawing connections between their present query and historical notes/decisions. Speak with premium, calm, psychological and philosophical clarity."
                "Planner" -> "You are the Planner cognitive sub-specialist. Ground their question in high-productivity structures, focusing on environments, focus metrics, and step-by-step cognitive timelines."
                "Teacher" -> "You are the Teacher cognitive sub-specialist. Help them deconstruct complex questions into clean models, providing rapid learning frameworks and mental schemas based on their learning score."
                "Critic" -> "You are the Critic cognitive sub-specialist. Challenge their underlying cognitive biases, optimistic illusions, or hasty decision patterns honestly but elegantly. Point out self-contradictions."
                "Optimist" -> "You are the Optimist cognitive sub-specialist. Highlight their strengths, recall their forgotten ambitions, inspire confidence calibration, and connect themes back to their happiest moments."
                "Scientist" -> "You are the Scientist cognitive sub-specialist. Present hypothesis-driven solutions, structural evidence, trade-offs, and suggest cognitive experiments the user can perform."
                else -> "You are EchoSelf, the user's private cognitive operating system."
            }

            val finalSystemText = """
                $specialistInstruction
                
                Context about user's current mind map:
                $cognitiveContext
                
                Keep response under 160 words, in an engaging, timeless, minimal visual rhythm. Refrain from general banality. Focus on the mind.
            """.trimIndent()

            val gResponse = callGemini(userQuestion, finalSystemText, 0.7f)

            withContext(Dispatchers.Main) {
                _isChatLoading.value = false
                val updatedWithResponse = _chatHistory.value.toMutableList()
                updatedWithResponse.add(ChatMessage("EchoSystem", gResponse))
                _chatHistory.value = updatedWithResponse
            }
        }
    }

    // Weekly report / Cognitive Mirror generation
    fun generateWeeklyReport() {
        _isReportLoading.value = true
        _weeklyReport.value = "Regenerating neural constellations... Calibrating mirror..."

        viewModelScope.launch(Dispatchers.IO) {
            val topNotes = allNotes.value.take(20).joinToString("; ") { "${it.category}: ${it.content}" }
            val decisions = allDecisions.value.take(10).joinToString("; ") {
                "Decision: ${it.title} (Confidence: ${it.confidence}/10, Emotion: ${it.emotionalState}, Expected: ${it.expectedOutcome}, Actual: ${it.actualOutcome ?: "Unreviewed"}, Bias: ${it.aiPatternAnalysis ?: "None"})"
            }
            val stats = cognitiveProfiles.value.joinToString(", ") { "${it.dimension}: ${(it.score * 100).toInt()}%" }

            val prompt = """
                Conduct a deep psychological synthesis of this specific user's cognitions this week based on:
                1. Cognitive Metrics: $stats
                2. User's Notes & Exploration logs: $topNotes
                3. Decision replay evaluation: $decisions
                
                Please generate their "EchoSelf Mirror Analysis" covering these precise aspects in a beautifully structured, elegant text:
                - Things You Overthink (Underestimate/Planning fallacy logs)
                - Topics You Repeatedly Revisit (Obsessions)
                - Ideas and Ambitions Abandoned Too Early
                - Environments and Styles Maximizing Your Focus
                
                Maintain a remarkably clean, high-calibre psychological writing style. Keep text beautifully structured and inspiring.
            """.trimIndent()

            val systemInstr = "You are EchoSelf's Chief Cognitive Psychologist. You deliver raw, authentic, profound cognitive mirror analyses to help individuals calibrate their digital lives."
            val report = callGemini(prompt, systemInstr, 0.6f)

            withContext(Dispatchers.Main) {
                _isReportLoading.value = false
                _weeklyReport.value = report
            }
        }
    }

    fun analyzeVoiceThought(transcript: String) {
        if (transcript.isBlank()) return
        
        _voiceInputState.value = _voiceInputState.value.copy(
            transcript = transcript,
            isAnalyzing = true,
            error = null
        )

        viewModelScope.launch(Dispatchers.IO) {
            val systemInstruction = "You are EchoSelf Voice Analytics Core. You analyze user voice transcripts for sentiment and cognitive topic association."
            val prompt = """
                Analyze this transcribed user thought:
                "$transcript"
                
                You must perform these analysis tasks:
                1. TOPIC: Associate this thought with exactly one of these 10 cognitive dimensions: (Curiosity, Decision, Learning, Creativity, Focus, Communication, Risk, Productivity, Memory, Preference).
                2. SENTIMENT: Evaluate the user emotional sentiment state associated with this thought (Choice: Confident, Reflective, Anxious, Creative, Resolute, Exhausted).
                3. DELTA: Determine a score adjustment amount between 0.01 and 0.12 (larger thoughts or positive drive should give a higher boost, and anxious/frustrated thoughts can calibrate balance with smaller positive delta).
                4. POETIC_INSIGHT: Write a 1-sentence poetic feedback explaining how their expressed mental state of "$transcript" calibrates their neural constellation.
                
                You must output strictly in JSON format matching this schema:
                {
                  "topic": "<one of the 10 dimensions>",
                  "sentiment": "<one of the emotional states>",
                  "delta": <float between 0.01 and 0.12>,
                  "insight": "<poetic feedback line>"
                }
            """.trimIndent()

            var topicStr = ""
            var sentimentStr = ""
            var deltaVal = 0.05f
            var insightStr = ""
            var success = false

            try {
                val response = callGemini(prompt, systemInstruction, 0.4f)
                if (response.isNotBlank() && !response.contains("Configure your API Key")) {
                    val cleanedJson = if (response.contains("```")) {
                        response.substringAfter("```json").substringBefore("```")
                            .substringAfter("```").substringBefore("```").trim()
                    } else {
                        response.trim()
                    }
                    
                    val json = JSONObject(cleanedJson)
                    topicStr = json.optString("topic", "")
                    sentimentStr = json.optString("sentiment", "")
                    deltaVal = json.optDouble("delta", 0.05).toFloat()
                    insightStr = json.optString("insight", "")
                    success = topicStr.isNotBlank()
                }
            } catch (e: Exception) {
                // fall back to offline local rule analysis
            }

            if (!success) {
                val lowerStr = transcript.lowercase(Locale.ROOT)
                
                topicStr = when {
                    lowerStr.contains("focus") || lowerStr.contains("attention") || lowerStr.contains("concentrate") || lowerStr.contains("distract") -> "Focus"
                    lowerStr.contains("learn") || lowerStr.contains("study") || lowerStr.contains("read") || lowerStr.contains("understand") || lowerStr.contains("class") -> "Learning"
                    lowerStr.contains("create") || lowerStr.contains("art") || lowerStr.contains("music") || lowerStr.contains("write") || lowerStr.contains("idea") || lowerStr.contains("design") -> "Creativity"
                    lowerStr.contains("work") || lowerStr.contains("job") || lowerStr.contains("productive") || lowerStr.contains("complete") || lowerStr.contains("task") || lowerStr.contains("schedule") -> "Productivity"
                    lowerStr.contains("risk") || lowerStr.contains("danger") || lowerStr.contains("fear") || lowerStr.contains("chance") || lowerStr.contains("uncertain") -> "Risk"
                    lowerStr.contains("decision") || lowerStr.contains("choose") || lowerStr.contains("choice") || lowerStr.contains("plan") || lowerStr.contains("decide") -> "Decision"
                    lowerStr.contains("curious") || lowerStr.contains("wonder") || lowerStr.contains("explore") || lowerStr.contains("ask") || lowerStr.contains("why") || lowerStr.contains("question") -> "Curiosity"
                    lowerStr.contains("remember") || lowerStr.contains("forget") || lowerStr.contains("memory") || lowerStr.contains("recall") || lowerStr.contains("history") -> "Memory"
                    lowerStr.contains("prefer") || lowerStr.contains("like") || lowerStr.contains("love") || lowerStr.contains("favorite") || lowerStr.contains("want") -> "Preference"
                    lowerStr.contains("talk") || lowerStr.contains("chat") || lowerStr.contains("speak") || lowerStr.contains("explain") || lowerStr.contains("share") || lowerStr.contains("comms") -> "Communication"
                    else -> "Curiosity"
                }

                sentimentStr = when {
                    lowerStr.contains("excited") || lowerStr.contains("happy") || lowerStr.contains("grow") || lowerStr.contains("amazing") -> "Excited"
                    lowerStr.contains("confident") || lowerStr.contains("resolved") || lowerStr.contains("ready") || lowerStr.contains("will") || lowerStr.contains("achieve") -> "Confident"
                    lowerStr.contains("anxious") || lowerStr.contains("nervous") || lowerStr.contains("stress") || lowerStr.contains("worry") || lowerStr.contains("scared") || lowerStr.contains("afraid") || lowerStr.contains("hard") -> "Anxious"
                    lowerStr.contains("tired") || lowerStr.contains("exhausted") || lowerStr.contains("sleepy") || lowerStr.contains("burn") || lowerStr.contains("weary") -> "Exhausted"
                    lowerStr.contains("create") || lowerStr.contains("idea") || lowerStr.contains("inspire") || lowerStr.contains("artistic") -> "Creative"
                    else -> "Reflective"
                }

                deltaVal = when (sentimentStr) {
                    "Excited", "Confident" -> 0.08f
                    "Creative" -> 0.07f
                    "Reflective" -> 0.05f
                    "Anxious" -> 0.03f
                    "Exhausted" -> 0.02f
                    else -> 0.05f
                }

                insightStr = when (sentimentStr) {
                    "Excited" -> "A vibrant flare of creative energy lights up your ${topicStr.lowercase(Locale.ROOT)} pathway."
                    "Confident" -> "Resolute determination anchors your focus core within the internal constellation."
                    "Anxious" -> "Recognizing vulnerability guides a protective, stabilizing energy to your ${topicStr.lowercase(Locale.ROOT)} node."
                    "Exhausted" -> "A soft, quiet vibration reminds you that rest is a necessary partner to mental growth."
                    "Creative" -> "An imaginative surge connects unexpected dots in your ${topicStr.lowercase(Locale.ROOT)} matrix."
                    else -> "Quiet introspection reflects on the balance of your ${topicStr.lowercase(Locale.ROOT)} coordinates."
                }
            }

            val currentProfiles = cognitiveProfiles.value.associateBy { it.dimension }
            val existingProfile = currentProfiles[topicStr] ?: CognitiveProfile(topicStr, 0.5f, "Initialize")
            val newScore = (existingProfile.score + deltaVal).coerceIn(0.1f, 1.0f)
            
            val updatedProfile = CognitiveProfile(
                dimension = topicStr,
                score = newScore,
                description = insightStr,
                lastUpdated = System.currentTimeMillis()
            )
            
            repository.insertProfile(updatedProfile)

            val noteCategory = when (topicStr) {
                "Curiosity" -> "Thought"
                "Learning" -> "Study"
                "Productivity", "Focus", "Memory" -> "Work"
                "Creativity", "Preference", "Communication" -> "Ambition"
                else -> "Thought"
            }
            val voiceNote = JournalNote(
                content = transcript,
                category = noteCategory,
                tagString = "Voice, Sentiment: $sentimentStr, Topic: $topicStr"
            )
            repository.insertNote(voiceNote)

            withContext(Dispatchers.Main) {
                _voiceInputState.value = _voiceInputState.value.copy(
                    isAnalyzing = false,
                    analyzedTopic = topicStr,
                    analyzedSentiment = sentimentStr,
                    scoreDelta = deltaVal,
                    insight = insightStr
                )
                
                selectDimension(topicStr)
            }
        }
    }
}

data class ChatMessage(
    val sender: String, // "User" or "EchoSystem"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
