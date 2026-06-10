package com.example.data

import kotlinx.coroutines.flow.Flow

class EchoRepository(private val dao: EchoDao) {

    val allNotes: Flow<List<JournalNote>> = dao.getAllNotes()
    val allDecisions: Flow<List<Decision>> = dao.getAllDecisions()
    val cognitiveProfiles: Flow<List<CognitiveProfile>> = dao.getCognitiveProfiles()

    suspend fun insertNote(note: JournalNote) {
        dao.insertNote(note)
    }

    suspend fun deleteNoteById(id: Int) {
        dao.deleteNoteById(id)
    }

    suspend fun insertDecision(decision: Decision) {
        dao.insertDecision(decision)
    }

    suspend fun updateDecision(decision: Decision) {
        dao.updateDecision(decision)
    }

    suspend fun deleteDecisionById(id: Int) {
        dao.deleteDecisionById(id)
    }

    suspend fun insertProfile(profile: CognitiveProfile) {
        dao.insertProfile(profile)
    }

    suspend fun preseedProfilesIfEmpty() {
        val defaultDimensions = listOf(
            "Curiosity" to "Tracks search topics, exploratory reading, and open-ended journaling interests.",
            "Decision" to "Measures planning clarity, trade-off depth, confidence calibration, and outcome analysis.",
            "Learning" to "Measures abstract mental schemas, concept mastery speeds, and retention depth across topics.",
            "Creativity" to "Represents architectural asymmetry, divergent note-taking hooks, and abstract synthesis output.",
            "Focus" to "Calculates visual rest metrics, continuous session lengths, and mental work focus ratios.",
            "Communication" to "Monitors phrasing complexity, feedback directness, vocabulary variance, and logical brevity.",
            "Risk" to "Analyzes risk appetite when executing ambitious goals and making heavy, uncertain choices.",
            "Productivity" to "Maps daily habits, prompt-action correlation, and high-velocity timeline completions.",
            "Memory" to "Correlates recall strengths, historical note queries, and semantic knowledge retrieval.",
            "Preference" to "Calculates stylistic leanings, design sensibilities, and favorite work environments."
        )
        val now = System.currentTimeMillis()
        for ((dimension, description) in defaultDimensions) {
            dao.insertProfileIfNotExist(dimension, 0.5f, description, now)
        }
    }
}
