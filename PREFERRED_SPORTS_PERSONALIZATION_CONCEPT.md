# 🔥 PREFERRED SPORTS PERSONALIZATION CONCEPT

## 🎯 Ziel
Die gesamte App basierend auf den ausgewählten "Preferred Sports" des Users personalisieren, damit jeder User nur relevante Inhalte für seine Sportarten sieht.

---

## 📊 Übersicht der Personalisierung

### **Betroffene Screens:**
1. ✅ **Account Screen** - PRs nur für ausgewählte Sports
2. 🔥 **Library** - Exercises filtern nach Sport
3. 🤖 **AI Coach** - Sport-spezifische Workout-Vorschläge
4. 📚 **Campus (Workout Templates)** - Templates nach Sport filtern
5. 🏠 **Home Screen** - Sport-spezifische Quick-Actions & Stats
6. 🏋️ **Workout Builder** - Exercises nach Sport vorfiltern

---

## 🏋️ Die 6 Kraftsportarten

### **1. Olympic Weightlifting**
- **Focus:** Technik, Explosivität, Mobility
- **Haupt-Lifts:** Snatch, Clean & Jerk, Power Variations
- **Key Metrics:** Power Score, Bar Velocity, Technique Score

### **2. Powerlifting**
- **Focus:** Maximalkraft, 1RM
- **Haupt-Lifts:** Squat, Bench Press, Deadlift
- **Key Metrics:** Wilks Score, Total, 1RM

### **3. Strongman**
- **Focus:** Funktionelle Kraft, Carries, Event-Training
- **Haupt-Exercises:** Atlas Stones, Farmer's Walk, Yoke, Log Press
- **Key Metrics:** Weight/Distance, Time

### **4. CrossFit**
- **Focus:** Allround-Fitness, WODs, Conditioning
- **Haupt-WODs:** Fran, Grace, Murph, AMRAP, EMOM
- **Key Metrics:** Time, Reps, Power Output

### **5. General Strength**
- **Focus:** Hypertrophy, allgemeine Fitness
- **Haupt-Exercises:** Compound + Isolation Movements
- **Key Metrics:** Volume, Reps, Progressive Overload

### **6. Hyrox**
- **Focus:** Hybrid-Training (Kraft + Cardio)
- **Haupt-Events:** SkiErg, Sled, Burpees, Running
- **Key Metrics:** Race Time, Split Times

---

## 🎨 Personalisierung pro Screen

### **1. ACCOUNT SCREEN** ✅ (Already implemented)
**Status:** Bereits implementiert!
- PRs werden nach Sport kategorisiert (Olympic Weightlifting, Powerlifting, etc.)
- Nur Sports mit ausgewählten Preferred Sports werden prominent angezeigt

**Weitere Verbesserungen:**
- ⭐ Top 3 PRs für jeden ausgewählten Sport im Header anzeigen
- 📈 Fortschritts-Graph für Haupt-Lifts

---

### **2. LIBRARY SCREEN** 🔥 **HIGH PRIORITY**
**Problem:** Aktuell zeigt die Library ALLE 810+ Exercises an
**Lösung:** Smart Filtering basierend auf Preferred Sports

#### **Implementierung:**

```kotlin
// Exercise Model erweitern
data class Exercise(
    // ... existing fields
    val sports: List<String> = emptyList()  // ["Weightlifting", "Powerlifting", "General Strength"]
)
```

#### **Filtering Logic:**
1. **Primäre Ansicht:** Nur Exercises für ausgewählte Sports
2. **Tab-Navigation:**
   - "My Sports" (gefiltert nach preferred sports)
   - "All Exercises" (alle 810+ exercises)
3. **Quick Filters:** Buttons für jede ausgewählte Sportart

#### **Beispiel für User mit Weightlifting + Powerlifting:**
```
Library Screen:
[My Sports] [All Exercises]

Quick Filters:
[💪 Weightlifting] [🏋️ Powerlifting]

Exercises angezeigt:
- Snatch (Weightlifting)
- Clean & Jerk (Weightlifting)
- Squat (Both)
- Bench Press (Powerlifting)
- Deadlift (Both)
```

---

### **3. AI COACH** 🤖 **HIGH PRIORITY**
**Problem:** AI generiert generische Workouts
**Lösung:** Sport-spezifische Workout-Generierung

#### **Implementierung:**

```kotlin
// In AICoachViewModel
fun generateWorkout(userGoal: String, preferredSports: List<String>) {
    val sportContext = buildSportContext(preferredSports)

    val prompt = """
    Generate a workout for:
    - Sports: ${preferredSports.joinToString(", ")}
    - Goal: $userGoal
    - Sport Context: $sportContext

    Focus on exercises and programming specific to these sports.
    """
}

private fun buildSportContext(sports: List<String>): String {
    return sports.map { sport ->
        when (sport) {
            "Weightlifting" -> """
                Olympic Weightlifting: Focus on snatch, clean & jerk, power variations.
                Include mobility work, technique drills, and explosive movements.
            """
            "Powerlifting" -> """
                Powerlifting: Focus on squat, bench press, deadlift variations.
                Include accessory work for the big 3 lifts.
            """
            // ... other sports
            else -> ""
        }
    }.joinToString("\n")
}
```

#### **Beispiel-Output für Weightlifting + Powerlifting:**
```
Day 1: Snatch Focus + Squat Strength
- Snatch (technique work): 6x2 @ 70%
- Front Squat: 4x5 @ 75%
- Snatch Pulls: 3x4 @ 90%
- Core work

Day 2: Clean & Jerk + Bench Strength
- Clean & Jerk: 5x2 @ 75%
- Bench Press: 5x5 @ 80%
- Overhead Squat: 3x5
- Accessories
```

---

### **4. CAMPUS (Workout Templates)** 📚 **MEDIUM PRIORITY**
**Problem:** Templates zeigen alle Sports
**Lösung:** Templates nach Preferred Sports filtern

#### **Implementierung:**

```kotlin
// In WorkoutTemplateRepository
suspend fun getTemplatesForSports(sports: List<String>): List<WorkoutTemplate> {
    return templates.filter { template ->
        // Template hat mindestens einen Sport in common mit User
        template.sports.any { it in sports }
    }
}
```

#### **UI Changes:**
- Default: Nur Templates für ausgewählte Sports
- "Browse All" Button um alle Templates zu sehen
- Sport-Tags auf jedem Template (z.B. [💪 Weightlifting] [🏋️ Powerlifting])

---

### **5. HOME SCREEN** 🏠 **MEDIUM PRIORITY**
**Problem:** Generische Home Screen ohne Sport-Bezug
**Lösung:** Sport-spezifische Quick Actions & Stats

#### **Implementierung:**

```kotlin
// Sport-spezifische Kacheln
@Composable
fun SportSpecificQuickActions(preferredSports: List<String>) {
    LazyRow {
        items(preferredSports) { sport ->
            SportQuickActionCard(sport = sport)
        }
    }
}

@Composable
fun SportQuickActionCard(sport: String) {
    when (sport) {
        "Weightlifting" -> {
            // Quick Action: "Today's Technique Work"
            // Recent PRs: Snatch, Clean & Jerk
        }
        "Powerlifting" -> {
            // Quick Action: "Next Max Out Day"
            // Recent PRs: Squat, Bench, Deadlift Total
        }
        // ... other sports
    }
}
```

#### **Beispiel Home Screen:**
```
Home Screen:

🏋️ Your Sports
[💪 Weightlifting] [🏋️ Powerlifting]

📊 Recent PRs
Snatch: 100kg (+5kg) ⬆️
Squat: 180kg (+2.5kg) ⬆️

🎯 Quick Start
[Weightlifting Technique] [Powerlifting Strength]

📅 Upcoming
- Max Out Week (Powerlifting)
- Snatch Complex (Weightlifting)
```

---

### **6. WORKOUT BUILDER** 🔨 **LOW PRIORITY**
**Problem:** Exercise-Auswahl zeigt alle 810+ Exercises
**Lösung:** Vorfiltern nach Preferred Sports

#### **Implementierung:**
- Default Filter auf "My Sports" setzen
- "Show All Exercises" Toggle
- Sport-Tabs für schnelle Navigation

---

## 🗄️ Datenbank Schema Updates

### **1. Exercises Tabelle erweitern:**
```sql
ALTER TABLE exercises ADD COLUMN sports TEXT[] DEFAULT '{}';

-- Beispiel Updates
UPDATE exercises SET sports = ARRAY['Weightlifting', 'General Strength'] WHERE name = 'Snatch';
UPDATE exercises SET sports = ARRAY['Powerlifting', 'General Strength'] WHERE name = 'Squat';
UPDATE exercises SET sports = ARRAY['CrossFit', 'General Strength'] WHERE name = 'Wall Balls';
```

### **2. Workout Templates erweitern:**
```sql
ALTER TABLE workout_templates ADD COLUMN sports TEXT[] DEFAULT '{}';
```

---

## 📈 Implementation Roadmap

### **Phase 1: Foundation** (Heute) ⚡
1. ✅ Exercises Tabelle erweitern (sports field)
2. ✅ Exercise Model erweitern
3. ✅ Sport-Mapping für alle 810 Exercises erstellen

### **Phase 2: Library Personalization** (Next) 🔥
1. Library Filtering implementieren
2. "My Sports" / "All Exercises" Tabs
3. Quick Filter Buttons

### **Phase 3: AI Coach Integration** 🤖
1. Sport Context in Prompt Builder
2. Sport-spezifische Workout Templates
3. Testing mit verschiedenen Sport-Kombinationen

### **Phase 4: Campus & Templates** 📚
1. Template Filtering
2. Sport Tags anzeigen
3. Template Recommendations

### **Phase 5: Home Screen** 🏠
1. Sport-spezifische Quick Actions
2. Sport-spezifische Stats
3. Personalisierte Widgets

---

## 🎯 Beispiel User Journey

**User: Daniel (Weightlifting + Powerlifting)**

1. **Login** → Profil lädt automatisch
2. **Home Screen:**
   - Sieht nur Weightlifting + Powerlifting Content
   - PRs: Snatch 100kg, Clean 120kg, Squat 180kg
   - Quick Action: "Snatch Technique Session" oder "Squat Max Out"

3. **Library:**
   - Default: Nur ~150 relevante Exercises (statt 810)
   - Quick Filters: [Weightlifting] [Powerlifting]
   - Kann "All Exercises" aktivieren wenn benötigt

4. **AI Coach:**
   - "Generate me a 4-day program"
   - AI generiert Mix aus Weightlifting & Powerlifting
   - Day 1: Snatch + Squat
   - Day 2: Bench + Clean & Jerk
   - etc.

5. **Campus:**
   - Templates gefiltert nach seinen Sports
   - "5x5 Powerlifting Program"
   - "Bulgarian Method Weightlifting"
   - Kann andere Sports Templates entdecken

---

## 🚀 Quick Wins (Was wir HEUTE machen können)

1. **Exercise Sports Mapping** - Alle 810 Exercises kategorisieren
2. **Library Filtering** - "My Sports" Tab implementieren
3. **Account Screen Enhancement** - Top 3 PRs anzeigen

Dann sehen wir wie komplex das wird und entscheiden die nächsten Schritte!

---

## 💡 Future Ideas (Nice-to-Have)

- **Sport Badges:** Achievements für jede Sportart
- **Sport-spezifische Challenges:** "30-Day Snatch Challenge"
- **Sport Communities:** Connect mit anderen Weightlifters
- **Competitive Features:** Leaderboards pro Sport
- **Sport-spezifische Form Analysis:** Snatch-spezifische Fehlerkennung

---

**Ready to start? Let's begin with Phase 1! 🔥**