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

class EchoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EchoRepository
    val allNotes: StateFlow<List<JournalNote>>
    val allDecisions: StateFlow<List<Decision>>
    val cognitiveProfiles: StateFlow<List<CognitiveProfile>>

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
}

data class ChatMessage(
    val sender: String, // "User" or "EchoSystem"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
