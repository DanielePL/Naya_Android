package com.example.menotracker.data

import androidx.compose.runtime.mutableStateListOf
import com.example.menotracker.data.models.Program
import com.example.menotracker.data.models.ProgramTemplate
import com.example.menotracker.data.models.ProgramTemplateDay
import com.example.menotracker.data.models.ProgramTemplatePhase
import com.example.menotracker.data.models.ProgramTemplateWeek
import com.example.menotracker.data.models.UserProgram
import com.example.menotracker.data.models.UserProgramProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import io.github.jan.supabase.postgrest.query.Columns
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.jan.supabase.postgrest.from

object ProgramRepository {
    private val _createdPrograms = mutableStateListOf<Program>()
    val createdPrograms: List<Program> = _createdPrograms

    private val _programTemplates = mutableStateListOf<ProgramTemplate>()
    val programTemplates: List<ProgramTemplate> = _programTemplates

    private var _isInitialized = false
    private var _templatesInitialized = false

    suspend fun initialize() {
        if (_isInitialized) return

        try {
            withContext(Dispatchers.IO) {
                println("--- FETCHING PROGRAMS FROM SUPABASE ---")
                val programs = SupabaseClient.client
                    .from("programs")
                    .select()
                    .decodeList<Program>()

                withContext(Dispatchers.Main) {
                    _createdPrograms.clear()
                    _createdPrograms.addAll(programs)
                    _isInitialized = true
                    println("✅ Loaded ${programs.size} programs from Supabase")
                }
            }
        } catch (e: Exception) {
            println("❌ ERROR loading programs: ${e.message}")
            e.printStackTrace()
        }
    }

    suspend fun initializeTemplates() {
        if (_templatesInitialized) return

        try {
            withContext(Dispatchers.IO) {
                println("--- FETCHING PROGRAM TEMPLATES FROM SUPABASE ---")
                val templates = SupabaseClient.client
                    .from("program_templates")
                    .select()
                    .decodeList<ProgramTemplate>()

                withContext(Dispatchers.Main) {
                    _programTemplates.clear()
                    _programTemplates.addAll(templates)
                    _templatesInitialized = true
                    println("✅ Loaded ${templates.size} program templates from Supabase")
                }
            }
        } catch (e: Exception) {
            println("❌ ERROR loading program templates: ${e.message}")
            e.printStackTrace()
        }
    }

    fun addProgram(program: Program) {
        if (_createdPrograms.none { it.id == program.id }) {
            _createdPrograms.add(program)
        }
    }

    suspend fun deleteProgram(programId: String) {
        try {
            withContext(Dispatchers.IO) {
                println("--- DELETING PROGRAM $programId FROM SUPABASE ---")
                SupabaseClient.client
                    .from("programs")
                    .delete {
                        filter {
                            eq("id", programId)
                        }
                    }
            }
            withContext(Dispatchers.Main) {
                _createdPrograms.removeAll { it.id == programId }
                println("✅ Successfully removed program $programId")
            }
        } catch(e: Exception) {
            println("❌ ERROR deleting program $programId: ${e.message}")
            e.printStackTrace()
        }
    }

    suspend fun refresh() {
        _isInitialized = false
        initialize()
    }

    /**
     * Fetch phases for a specific program template
     */
    suspend fun fetchProgramPhases(programTemplateId: String): List<ProgramTemplatePhase> {
        return try {
            withContext(Dispatchers.IO) {
                println("--- FETCHING PHASES FOR PROGRAM $programTemplateId ---")
                val phases = SupabaseClient.client
                    .from("program_template_phases")
                    .select {
                        filter {
                            eq("program_template_id", programTemplateId)
                        }
                    }
                    .decodeList<ProgramTemplatePhase>()
                    .sortedBy { it.sortOrder ?: it.startWeek }

                println("✅ Loaded ${phases.size} phases for program $programTemplateId")
                phases
            }
        } catch (e: Exception) {
            println("❌ ERROR loading phases: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // USER PROGRAMS
    // ═══════════════════════════════════════════════════════════════

    private val _userPrograms = mutableStateListOf<UserProgram>()
    val userPrograms: List<UserProgram> = _userPrograms

    private var _userProgramsInitialized = false

    /**
     * Initialize user programs for the current user
     */
    suspend fun initializeUserPrograms(userId: String) {
        if (_userProgramsInitialized) return

        try {
            withContext(Dispatchers.IO) {
                println("--- FETCHING USER PROGRAMS FOR $userId ---")
                val programs = SupabaseClient.client
                    .from("user_programs")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<UserProgram>()

                withContext(Dispatchers.Main) {
                    _userPrograms.clear()
                    _userPrograms.addAll(programs)
                    _userProgramsInitialized = true
                    println("✅ Loaded ${programs.size} user programs")
                }
            }
        } catch (e: Exception) {
            println("❌ ERROR loading user programs: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Start a program from a template - copies to user's programs
     * @return The new UserProgram ID, or null if failed
     */
    suspend fun startProgramFromTemplate(template: ProgramTemplate, userId: String): String? {
        return try {
            val newId = UUID.randomUUID().toString()
            val userProgram = UserProgram.fromTemplate(template, userId, newId)

            withContext(Dispatchers.IO) {
                println("--- STARTING PROGRAM FROM TEMPLATE: ${template.name} ---")
                SupabaseClient.client
                    .from("user_programs")
                    .insert(userProgram)

                withContext(Dispatchers.Main) {
                    _userPrograms.add(userProgram)
                    println("✅ Successfully created user program: ${userProgram.name}")
                }
            }
            newId
        } catch (e: Exception) {
            println("❌ ERROR starting program: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Delete a user program
     */
    suspend fun deleteUserProgram(programId: String): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                println("--- DELETING USER PROGRAM $programId ---")
                SupabaseClient.client
                    .from("user_programs")
                    .delete {
                        filter {
                            eq("id", programId)
                        }
                    }
            }
            withContext(Dispatchers.Main) {
                _userPrograms.removeAll { it.id == programId }
                // Clear active program if it's the one being deleted
                if (_activeProgram.value?.id == programId) {
                    clearActiveProgram()
                }
                println("✅ Successfully removed user program $programId")
            }
            true
        } catch (e: Exception) {
            println("❌ ERROR deleting user program: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Update user program details (name, status)
     */
    suspend fun updateUserProgram(
        userProgramId: String,
        name: String? = null,
        status: String? = null
    ): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                println("--- UPDATING USER PROGRAM $userProgramId ---")
                SupabaseClient.client
                    .from("user_programs")
                    .update({
                        name?.let { set("name", it) }
                        status?.let { set("status", it) }
                    }) {
                        filter {
                            eq("id", userProgramId)
                        }
                    }
            }

            // Update local state
            val index = _userPrograms.indexOfFirst { it.id == userProgramId }
            if (index >= 0) {
                val current = _userPrograms[index]
                val updated = current.copy(
                    name = name ?: current.name,
                    status = status ?: current.status
                )
                _userPrograms[index] = updated

                // Update active program if it's the same one
                if (_activeProgram.value?.id == userProgramId) {
                    _activeProgram.value = updated
                }
            }

            println("✅ User program updated successfully")
            true
        } catch (e: Exception) {
            println("❌ ERROR updating user program: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Pause a user program
     */
    suspend fun pauseUserProgram(userProgramId: String): Boolean {
        return updateUserProgram(userProgramId, status = "paused")
    }

    /**
     * Resume a paused program
     */
    suspend fun resumeUserProgram(userProgramId: String): Boolean {
        return updateUserProgram(userProgramId, status = "active")
    }

    /**
     * Complete a program
     */
    suspend fun completeUserProgram(userProgramId: String): Boolean {
        return updateUserProgram(userProgramId, status = "completed")
    }

    /**
     * Refresh user programs
     */
    suspend fun refreshUserPrograms(userId: String) {
        _userProgramsInitialized = false
        initializeUserPrograms(userId)
    }

    // ═══════════════════════════════════════════════════════════════
    // ACTIVE PROGRAM (currently selected for training)
    // ═══════════════════════════════════════════════════════════════

    private val _activeProgram = MutableStateFlow<UserProgram?>(null)
    val activeProgram: StateFlow<UserProgram?> = _activeProgram.asStateFlow()

    private val _activeProgramWeeks = MutableStateFlow<List<ProgramTemplateWeek>>(emptyList())
    val activeProgramWeeks: StateFlow<List<ProgramTemplateWeek>> = _activeProgramWeeks.asStateFlow()

    private val _currentWeekDays = MutableStateFlow<List<ProgramTemplateDay>>(emptyList())
    val currentWeekDays: StateFlow<List<ProgramTemplateDay>> = _currentWeekDays.asStateFlow()

    private val _activeProgramProgress = MutableStateFlow<List<UserProgramProgress>>(emptyList())
    val activeProgramProgress: StateFlow<List<UserProgramProgress>> = _activeProgramProgress.asStateFlow()

    /**
     * Set the active program and load its weeks
     */
    suspend fun setActiveProgram(userProgram: UserProgram) {
        _activeProgram.value = userProgram

        // Load weeks for this program's template
        userProgram.programTemplateId?.let { templateId ->
            loadProgramWeeks(templateId)
            loadCurrentWeekDays(userProgram.currentWeek)
            loadProgramProgress(userProgram.id)
        }
    }

    /**
     * Clear active program
     */
    fun clearActiveProgram() {
        _activeProgram.value = null
        _activeProgramWeeks.value = emptyList()
        _currentWeekDays.value = emptyList()
        _activeProgramProgress.value = emptyList()
    }

    /**
     * Load all weeks for a program template
     */
    private suspend fun loadProgramWeeks(programTemplateId: String) {
        try {
            withContext(Dispatchers.IO) {
                println("--- FETCHING WEEKS FOR PROGRAM $programTemplateId ---")
                val weeks = SupabaseClient.client
                    .from("program_template_weeks")
                    .select {
                        filter {
                            eq("program_template_id", programTemplateId)
                        }
                    }
                    .decodeList<ProgramTemplateWeek>()
                    .sortedBy { it.weekNumber }

                _activeProgramWeeks.value = weeks
                println("✅ Loaded ${weeks.size} weeks for program")
            }
        } catch (e: Exception) {
            println("❌ ERROR loading weeks: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Load training days for a specific week
     */
    suspend fun loadCurrentWeekDays(weekNumber: Int) {
        val weeks = _activeProgramWeeks.value
        val currentWeek = weeks.find { it.weekNumber == weekNumber }

        if (currentWeek == null) {
            println("⚠️ Week $weekNumber not found in program")
            _currentWeekDays.value = emptyList()
            return
        }

        try {
            withContext(Dispatchers.IO) {
                println("--- FETCHING DAYS FOR WEEK ${currentWeek.id} ---")
                val days = SupabaseClient.client
                    .from("program_template_days")
                    .select {
                        filter {
                            eq("program_template_week_id", currentWeek.id)
                        }
                    }
                    .decodeList<ProgramTemplateDay>()
                    .sortedBy { it.sortOrder ?: it.dayOfWeek ?: 0 }

                _currentWeekDays.value = days
                println("✅ Loaded ${days.size} training days for week $weekNumber")
            }
        } catch (e: Exception) {
            println("❌ ERROR loading days: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Load progress for a user program
     */
    private suspend fun loadProgramProgress(userProgramId: String) {
        try {
            withContext(Dispatchers.IO) {
                println("--- FETCHING PROGRESS FOR USER PROGRAM $userProgramId ---")
                val progress = SupabaseClient.client
                    .from("user_program_progress")
                    .select {
                        filter {
                            eq("user_program_id", userProgramId)
                        }
                    }
                    .decodeList<UserProgramProgress>()

                _activeProgramProgress.value = progress
                println("✅ Loaded ${progress.size} progress entries")
            }
        } catch (e: Exception) {
            println("❌ ERROR loading progress: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Update user program start date
     */
    suspend fun updateProgramStartDate(userProgramId: String, startDate: String): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                println("--- UPDATING START DATE FOR $userProgramId to $startDate ---")
                SupabaseClient.client
                    .from("user_programs")
                    .update({
                        set("start_date", startDate)
                    }) {
                        filter {
                            eq("id", userProgramId)
                        }
                    }
            }

            // Update local state
            val index = _userPrograms.indexOfFirst { it.id == userProgramId }
            if (index >= 0) {
                val updated = _userPrograms[index].copy(startDate = startDate)
                _userPrograms[index] = updated
                _activeProgram.value = updated
            }

            println("✅ Start date updated successfully")
            true
        } catch (e: Exception) {
            println("❌ ERROR updating start date: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Update current week for a user program
     */
    suspend fun updateCurrentWeek(userProgramId: String, weekNumber: Int): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                println("--- UPDATING CURRENT WEEK FOR $userProgramId to $weekNumber ---")
                SupabaseClient.client
                    .from("user_programs")
                    .update({
                        set("current_week", weekNumber)
                    }) {
                        filter {
                            eq("id", userProgramId)
                        }
                    }
            }

            // Update local state
            val index = _userPrograms.indexOfFirst { it.id == userProgramId }
            if (index >= 0) {
                val updated = _userPrograms[index].copy(currentWeek = weekNumber)
                _userPrograms[index] = updated
                _activeProgram.value = updated
            }

            // Load new week's days
            loadCurrentWeekDays(weekNumber)

            println("✅ Current week updated to $weekNumber")
            true
        } catch (e: Exception) {
            println("❌ ERROR updating current week: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Mark a workout day as completed
     */
    suspend fun markDayCompleted(
        userProgramId: String,
        programTemplateDayId: String,
        weekNumber: Int,
        workoutSessionId: String? = null
    ): Boolean {
        return try {
            val progressId = UUID.randomUUID().toString()
            val progress = UserProgramProgress(
                id = progressId,
                userProgramId = userProgramId,
                programTemplateDayId = programTemplateDayId,
                weekNumber = weekNumber,
                completedAt = java.time.Instant.now().toString(),
                skipped = false,
                workoutSessionId = workoutSessionId
            )

            withContext(Dispatchers.IO) {
                println("--- MARKING DAY $programTemplateDayId AS COMPLETED ---")
                SupabaseClient.client
                    .from("user_program_progress")
                    .insert(progress)
            }

            // Update local state
            _activeProgramProgress.value = _activeProgramProgress.value + progress
            println("✅ Day marked as completed")
            true
        } catch (e: Exception) {
            println("❌ ERROR marking day completed: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Skip a workout day
     */
    suspend fun skipDay(
        userProgramId: String,
        programTemplateDayId: String,
        weekNumber: Int,
        notes: String? = null
    ): Boolean {
        return try {
            val progressId = UUID.randomUUID().toString()
            val progress = UserProgramProgress(
                id = progressId,
                userProgramId = userProgramId,
                programTemplateDayId = programTemplateDayId,
                weekNumber = weekNumber,
                completedAt = java.time.Instant.now().toString(),
                skipped = true,
                notes = notes
            )

            withContext(Dispatchers.IO) {
                println("--- SKIPPING DAY $programTemplateDayId ---")
                SupabaseClient.client
                    .from("user_program_progress")
                    .insert(progress)
            }

            _activeProgramProgress.value = _activeProgramProgress.value + progress
            println("✅ Day skipped")
            true
        } catch (e: Exception) {
            println("❌ ERROR skipping day: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Get active user program (first active one)
     */
    fun getActiveUserProgram(): UserProgram? {
        return _userPrograms.find { it.status == "active" }
    }
}