# Workout Templates: Live Library Reference Implementation

## Was wurde geändert?

### 1. Database Schema Migration
Die Workout Templates verwenden jetzt **Live-Referenzen** zur Exercise Library statt Snapshot-Daten.

**Vorher:**
```
workout_template_exercises:
  - exercise_id (redundant)
  - exercise_name ❌ (Snapshot)
  - muscle_group ❌ (Snapshot)  
  - equipment ❌ (Snapshot)
```

**Nachher:**
```
workout_template_exercises:
  - exercise_id → exercises_new.id (Foreign Key)
  - order_index
```

Übungsdaten werden jetzt **live** aus `exercises_new` geladen via JOIN.

### 2. Code Änderungen

#### DTOs (WorkoutTemplate.kt)
- ✅ Neue `WorkoutTemplateExerciseWithDetails` DTO für JOINs
- ✅ Vereinfachte `CreateWorkoutExerciseRequest` (nur exercise_id)

#### Repository (WorkoutTemplateRepository.kt)
- ✅ `loadCompleteWorkoutTemplate()` macht jetzt JOIN mit `exercises_new`
- ✅ `saveWorkoutTemplate()` speichert nur noch exercise_id
- ✅ Neue Funktion `updateExerciseSets()` zum Bearbeiten von Sets

#### ViewModel (WorkoutBuilderViewModel.kt)
- ✅ Neue Funktion `updateTemplateSets()` zum direkten Editieren

## Nächste Schritte

### SCHRITT 1: Database Migration ausführen

**WICHTIG:** Backup zuerst!

1. Öffne Supabase SQL Editor
2. Führe **zuerst** dieses Script aus um die Daten zu prüfen:
   ```
   scripts/check_workout_templates_data.sql
   ```

3. Dann führe die Migration aus:
   ```
   scripts/migrate_workout_templates_to_live_reference.sql
   ```

Das Migration Script:
- ✅ Erstellt Backup Tabelle
- ✅ Konvertiert exercise_id zu UUID (falls noch nicht)
- ✅ Mapped exercise_id zu exercises_new.id basierend auf Namen
- ✅ Löscht redundante Spalten (exercise_name, muscle_group, equipment)
- ✅ Fügt Foreign Key Constraint hinzu
- ✅ Erstellt Index für Performance

### SCHRITT 2: App testen

Nach der Migration:

1. **Starte die App neu**
2. **Gehe zu Library → Workouts → Templates Tab**
3. **Prüfe:**
   - ✅ Werden alle Templates geladen?
   - ✅ Werden Übungen mit korrekten Namen angezeigt?
   - ✅ Werden Sets angezeigt (Reps, Weight, Rest)?

### SCHRITT 3: Edit-Funktionalität hinzufügen (Optional)

Die Backend-Logik für Editing ist fertig. Um Edit-Buttons in der UI zu zeigen:

**Beispiel Integration:**
```kotlin
// In deinem WorkoutTemplateCard Composable
IconButton(onClick = {
    // Zeige Edit-Dialog
    showEditDialog = true
}) {
    Icon(Icons.Default.Edit, "Edit Sets")
}

// Edit Dialog
if (showEditDialog) {
    EditSetsDialog(
        exercise = exercise,
        onSave = { updatedSets ->
            viewModel.updateTemplateSets(
                templateId = template.id,
                exerciseId = exercise.exerciseId,
                newSets = updatedSets,
                onSuccess = { /* Toast: Saved! */ },
                onFailure = { error -> /* Toast: Error */ }
            )
            showEditDialog = false
        },
        onDismiss = { showEditDialog = false }
    )
}
```

## Vorteile der neuen Struktur

✅ **Live Updates:** Änderungen an Übungen in der Library werden automatisch in Templates übernommen
✅ **Weniger Redundanz:** Keine duplizierten Daten mehr
✅ **Einfacheres Editieren:** Sets können direkt bearbeitet werden
✅ **Datenintegrität:** Foreign Key garantiert valide Exercise-Referenzen

## Rollback (Falls etwas schiefgeht)

```sql
-- In Supabase SQL Editor:
DROP TABLE workout_template_exercises;
ALTER TABLE workout_template_exercises_backup 
RENAME TO workout_template_exercises;
```

## Nächste Features (Ideen)

1. **Quick Edit UI:** Edit-Button bei jeder Übung im Template
2. **Bulk Edit:** Mehrere Sets gleichzeitig anpassen
3. **Template Variations:** Leicht/Medium/Schwer Varianten eines Templates
4. **Progressive Overload:** Automatisch Weight/Reps erhöhen basierend auf History

## Fragen?

Wenn du Fragen hast oder Probleme auftreten, check:
1. Supabase Logs für Errors
2. Android Logcat für "WorkoutTemplateRepo" Tags
3. Backup Tabelle ist da falls Rollback nötig
