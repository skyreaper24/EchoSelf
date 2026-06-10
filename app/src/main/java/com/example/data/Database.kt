package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "journal_notes")
data class JournalNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String, // "Journal", "Study", "Work", "Ambition"
    val tagString: String = "" // Comma-separated
)

@Entity(tableName = "decisions")
data class Decision(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val expectedOutcome: String,
    val actualOutcome: String? = null,
    val lessonsLearned: String? = null,
    val confidence: Int, // 1 to 10
    val emotionalState: String, // "Calm", "Anxious", "Excited", "Determined"
    val timestamp: Long = System.currentTimeMillis(),
    val reviewedTimestamp: Long? = null,
    val aiPatternAnalysis: String? = null
)

@Entity(tableName = "cognitive_profiles")
data class CognitiveProfile(
    @PrimaryKey val dimension: String, // "Curiosity", "Decision", "Learning", "Creativity", "Focus", "Communication", "Risk", "Productivity", "Memory", "Preference"
    val score: Float, // 0.0 to 1.0 (from 0% to 100%)
    val description: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Dao
interface EchoDao {
    // Journal Notes
    @Query("SELECT * FROM journal_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<JournalNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: JournalNote)

    @Query("DELETE FROM journal_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int)

    // Decisions
    @Query("SELECT * FROM decisions ORDER BY timestamp DESC")
    fun getAllDecisions(): Flow<List<Decision>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecision(decision: Decision)

    @Update
    suspend fun updateDecision(decision: Decision)

    @Query("DELETE FROM decisions WHERE id = :id")
    suspend fun deleteDecisionById(id: Int)

    // Cognitive Profiles
    @Query("SELECT * FROM cognitive_profiles")
    fun getCognitiveProfiles(): Flow<List<CognitiveProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: CognitiveProfile)

    @Query("INSERT OR IGNORE INTO cognitive_profiles (dimension, score, description, lastUpdated) VALUES (:dimension, :score, :description, :lastUpdated)")
    suspend fun insertProfileIfNotExist(dimension: String, score: Float, description: String, lastUpdated: Long)
}

@Database(entities = [JournalNote::class, Decision::class, CognitiveProfile::class], version = 1, exportSchema = false)
abstract class EchoDatabase : RoomDatabase() {
    abstract fun echoDao(): EchoDao

    companion object {
        @Volatile
        private var INSTANCE: EchoDatabase? = null

        fun getDatabase(context: Context): EchoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EchoDatabase::class.java,
                    "echoself_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
