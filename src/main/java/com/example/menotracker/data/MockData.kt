// app/src/main/java/com/example/myapplicationtest/data/MockData.kt

package com.example.menotracker.data

import com.example.menotracker.data.models.Exercise

object MockData {
    val exercises = listOf(
        // ═══════════════════════════════════════════════════════════════
        // SQUAT VARIATIONS - Both Power & Technique Score
        // ═══════════════════════════════════════════════════════════════
        Exercise(
            id = "1",
            name = "Front Squat",
            mainMuscle = "Legs",
            secondaryMuscles = listOf("Glutes", "Core"),
            equipment = listOf("Barbell"),
            tempo = "3110",
            restTimeInSeconds = 120,
            tutorial = "1. Set the barbell up in a squat rack just below shoulder height.\n2. Approach the bar and position your shoulders underneath it, crossing your arms to grip the bar.\n3. Stand up to un-rack the barbell, take a step back, and assume a shoulder-width stance with your toes pointed slightly out.\n4. Keeping your chest up and elbows high, descend into a squat until your hips are below your knees.\n5. Drive through your heels to return to the starting position.",
            notes = "Focus on maintaining an upright torso throughout the lift.",
            supportsPowerScore = true,
            supportsTechniqueScore = true,
            vbtMeasurementType = "average",
            vbtCategory = "Squat"
        ),

        Exercise(
            id = "11",
            name = "Barbell Back Squat",
            mainMuscle = "Legs",
            secondaryMuscles = listOf("Glutes", "Core"),
            equipment = listOf("Barbell"),
            tempo = "3010",
            restTimeInSeconds = 120,
            tutorial = "1. Position the barbell on your upper back.\n2. Stand with feet shoulder-width apart.\n3. Descend by pushing hips back and bending knees.\n4. Lower until thighs are parallel to ground.\n5. Drive through heels to return to starting position.",
            notes = "Keep chest up and core tight throughout the movement.",
            supportsPowerScore = true,
            supportsTechniqueScore = true,
            vbtMeasurementType = "average",
            vbtCategory = "Squat"
        ),

        // ═══════════════════════════════════════════════════════════════
        // DEADLIFT VARIATIONS - Both Power & Technique Score
        // ═══════════════════════════════════════════════════════════════
        Exercise(
            id = "2",
            name = "Romanian Deadlift",
            mainMuscle = "Legs",
            secondaryMuscles = listOf("Hamstrings", "Glutes", "Lower Back"),
            equipment = listOf("Barbell", "Dumbbell"),
            tempo = "3010",
            restTimeInSeconds = 90,
            tutorial = "1. Stand with your feet hip-width apart, holding a barbell or dumbbells in front of your thighs.\n2. Hinge at your hips, keeping your back straight and legs relatively straight (with a slight bend in the knees).\n3. Lower the weight until you feel a deep stretch in your hamstrings.\n4. Return to the starting position by driving your hips forward.",
            notes = "Avoid rounding your lower back.",
            supportsPowerScore = true,
            supportsTechniqueScore = true,
            vbtMeasurementType = "average",
            vbtCategory = "Deadlift"
        ),

        Exercise(
            id = "6",
            name = "Conventional Deadlift",
            mainMuscle = "Back",
            secondaryMuscles = listOf("Glutes", "Hamstrings", "Legs"),
            equipment = listOf("Barbell"),
            tempo = "2010",
            restTimeInSeconds = 120,
            tutorial = "1. Stand with your mid-foot under the barbell.\n2. Hinge at your hips and bend your knees to grip the bar with a shoulder-width grip.\n3. Keeping your back straight, chest up, and hips down, lift the bar by driving through your heels and extending your hips and knees.\n4. Lower the bar with control back to the ground.",
            notes = "This is a full-body compound movement. Form is critical.",
            supportsPowerScore = true,
            supportsTechniqueScore = true,
            vbtMeasurementType = "average",
            vbtCategory = "Deadlift"
        ),

        // ═══════════════════════════════════════════════════════════════
        // PRESSING VARIATIONS - Both Power & Technique Score
        // ═══════════════════════════════════════════════════════════════
        Exercise(
            id = "19",
            name = "Barbell Bench Press",
            mainMuscle = "Chest",
            secondaryMuscles = listOf("Shoulders", "Triceps"),
            equipment = listOf("Barbell"),
            tempo = "2010",
            restTimeInSeconds = 90,
            tutorial = "1. Lie on a flat bench, positioning yourself so your eyes are under the bar.\n2. Grip the bar slightly wider than shoulder-width apart.\n3. Unrack the bar and lower it to your mid-chest.\n4. Press the bar back up to the starting position until your arms are fully extended.",
            notes = "Keep your feet flat on the floor and your shoulder blades retracted.",
            supportsPowerScore = true,
            supportsTechniqueScore = true,
            vbtMeasurementType = "average",
            vbtCategory = "Press"
        ),

        Exercise(
            id = "8",
            name = "Incline Bench Press",
            mainMuscle = "Chest",
            secondaryMuscles = listOf("Shoulders", "Triceps"),
            equipment = listOf("Barbell"),
            tempo = "2010",
            restTimeInSeconds = 90,
            tutorial = "1. Set bench to 30-45 degree incline.\n2. Lie back and grip bar wider than shoulder-width.\n3. Lower bar to upper chest.\n4. Press back up to starting position.",
            notes = "Targets upper chest more than flat bench press.",
            supportsPowerScore = true,
            supportsTechniqueScore = true,
            vbtMeasurementType = "average",
            vbtCategory = "Press"
        ),

        Exercise(
            id = "21",
            name = "Close-Grip Bench Press",
            mainMuscle = "Arms",
            secondaryMuscles = listOf("Chest", "Triceps"),
            equipment = listOf("Barbell"),
            tempo = "2010",
            restTimeInSeconds = 75,
            tutorial = "1. Lie on bench and grip bar with hands shoulder-width apart.\n2. Lower bar to lower chest.\n3. Press back up, focusing on tricep engagement.",
            notes = "Excellent for tricep development.",
            supportsPowerScore = true,
            supportsTechniqueScore = true,
            vbtMeasurementType = "average",
            vbtCategory = "Press"
        ),

        Exercise(
            id = "13",
            name = "Overhead Press",
            mainMuscle = "Shoulders",
            secondaryMuscles = listOf("Triceps", "Core"),
            equipment = listOf("Barbell"),
            tempo = "2010",
            restTimeInSeconds = 90,
            tutorial = "1. Start with barbell at shoulder height.\n2. Press bar overhead until arms are fully extended.\n3. Lower with control back to shoulders.",
            notes = "Keep core tight to protect lower back.",
            supportsPowerScore = true,
            supportsTechniqueScore = true,
            vbtMeasurementType = "average",
            vbtCategory = "Press"
        ),

        // ═══════════════════════════════════════════════════════════════
        // OLYMPIC MOVEMENTS - Power Score Only
        // ═══════════════════════════════════════════════════════════════
        Exercise(
            id = "16",
            name = "Power Clean",
            mainMuscle = "Olympic",
            secondaryMuscles = listOf("Legs", "Back", "Shoulders"),
            equipment = listOf("Barbell"),
            tempo = "EXPLOSIVE",
            restTimeInSeconds = 120,
            tutorial = "1. Start with bar on floor, grip slightly wider than shoulder-width.\n2. Explosively pull bar from floor.\n3. Drop under bar and catch at shoulder height.\n4. Stand up to complete the lift.",
            notes = "Focus on explosive power and speed.",
            supportsPowerScore = true,
            supportsTechniqueScore = false,
            vbtMeasurementType = "peak",
            vbtCategory = "Olympic"
        ),

        // ═══════════════════════════════════════════════════════════════
        // ROWING VARIATIONS - Power Score Only
        // ═══════════════════════════════════════════════════════════════
        Exercise(
            id = "20",
            name = "Barbell Row",
            mainMuscle = "Back",
            secondaryMuscles = listOf("Biceps", "Rear Delts"),
            equipment = listOf("Barbell"),
            tempo = "2011",
            restTimeInSeconds = 75,
            tutorial = "1. Hinge at hips with slight bend in knees.\n2. Pull bar to lower chest.\n3. Lower with control.",
            notes = "Keep back straight throughout movement.",
            supportsPowerScore = true,
            supportsTechniqueScore = false,
            vbtMeasurementType = "average",
            vbtCategory = "Row"
        ),

        // ═══════════════════════════════════════════════════════════════
        // NON-VBT EXERCISES
        // ═══════════════════════════════════════════════════════════════
        Exercise(
            id = "3",
            name = "Dumbbell Shoulder Press",
            mainMuscle = "Shoulders",
            secondaryMuscles = listOf("Triceps"),
            equipment = listOf("Dumbbell"),
            tempo = "2010",
            restTimeInSeconds = 60,
            tutorial = "1. Sit on a bench with back support, holding a dumbbell in each hand at shoulder height, palms facing forward.\n2. Press the dumbbells overhead until your arms are fully extended.\n3. Slowly lower the dumbbells back to the starting position.",
            notes = "Keep your core engaged to protect your spine."
        ),

        Exercise(
            id = "4",
            name = "Calf Raises",
            mainMuscle = "Legs",
            secondaryMuscles = listOf("Calves"),
            equipment = listOf("Bodyweight", "Dumbbell", "Machine"),
            trackWeight = true,
            tutorial = "1. Stand on a flat surface or the edge of a step.\n2. Push through the balls of your feet to raise your heels as high as you can.\n3. Hold the peak contraction for a moment before slowly lowering your heels.",
            notes = "Can be performed with added weight for increased difficulty."
        ),

        Exercise(
            id = "5",
            name = "Lunges",
            mainMuscle = "Legs",
            secondaryMuscles = listOf("Glutes", "Quads"),
            equipment = listOf("Bodyweight", "Dumbbell"),
            tutorial = "1. Step forward with one leg, lowering your hips until both knees are bent at a 90-degree angle.\n2. Ensure your front knee is directly above your ankle and your back knee is hovering just off the ground.\n3. Push off your front foot to return to the starting position.",
            notes = "Keep your torso upright and core engaged."
        ),

        Exercise(
            id = "7",
            name = "Plank",
            mainMuscle = "Core",
            secondaryMuscles = listOf("Abs"),
            equipment = listOf("Bodyweight"),
            trackDuration = true,
            trackReps = false,
            trackSets = true,
            trackWeight = false,
            tutorial = "1. Assume a push-up position, but with your weight resting on your forearms instead of your hands.\n2. Your body should form a straight line from your shoulders to your ankles.\n3. Engage your core and glutes to prevent your hips from sagging.\n4. Hold this position for the desired duration.",
            notes = "Focus on breathing steadily."
        ),

        Exercise(
            id = "9",
            name = "Tricep Pushdowns",
            mainMuscle = "Arms",
            secondaryMuscles = listOf("Triceps"),
            equipment = listOf("Cable"),
            tempo = "2011",
            restTimeInSeconds = 60,
            tutorial = "1. Attach a straight or rope attachment to a high pulley.\n2. Grip the attachment and push down until arms are fully extended.\n3. Return to starting position with control.",
            notes = "Keep elbows tucked to sides."
        ),

        Exercise(
            id = "10",
            name = "Cable Row",
            mainMuscle = "Back",
            secondaryMuscles = listOf("Biceps"),
            equipment = listOf("Cable"),
            tempo = "2011",
            restTimeInSeconds = 60,
            tutorial = "1. Sit at cable row machine.\n2. Pull handle to torso.\n3. Return with control.",
            notes = "Squeeze shoulder blades together at peak contraction."
        ),

        Exercise(
            id = "12",
            name = "Bulgarian Split Squat",
            mainMuscle = "Legs",
            secondaryMuscles = listOf("Glutes", "Quads"),
            equipment = listOf("Dumbbell"),
            tempo = "3010",
            restTimeInSeconds = 75,
            tutorial = "1. Place rear foot on elevated surface.\n2. Lower front leg until thigh is parallel to ground.\n3. Drive through front heel to return to start.",
            notes = "Excellent for single-leg strength and balance."
        ),

        Exercise(
            id = "14",
            name = "Dips",
            mainMuscle = "Chest",
            secondaryMuscles = listOf("Triceps", "Shoulders"),
            equipment = listOf("Bodyweight"),
            tutorial = "1. Grip parallel bars and lift yourself up.\n2. Lower body by bending elbows.\n3. Push back up to starting position.",
            notes = "Lean forward for chest emphasis, stay upright for triceps."
        ),

        Exercise(
            id = "15",
            name = "Lat Pulldown",
            mainMuscle = "Back",
            secondaryMuscles = listOf("Biceps"),
            equipment = listOf("Machine"),
            tempo = "2011",
            restTimeInSeconds = 60,
            tutorial = "1. Sit at lat pulldown machine.\n2. Pull bar down to upper chest.\n3. Return with control.",
            notes = "Pull with your back, not your arms."
        ),

        Exercise(
            id = "17",
            name = "Chest Flyes",
            mainMuscle = "Chest",
            secondaryMuscles = listOf("Shoulders"),
            equipment = listOf("Dumbbell"),
            tempo = "2011",
            restTimeInSeconds = 60,
            tutorial = "1. Lie on bench with dumbbells above chest.\n2. Lower dumbbells out to sides in arc motion.\n3. Bring back together above chest.",
            notes = "Keep slight bend in elbows throughout."
        ),

        Exercise(
            id = "18",
            name = "Leg Extension",
            mainMuscle = "Legs",
            secondaryMuscles = listOf("Quads"),
            equipment = listOf("Machine"),
            tempo = "2011",
            restTimeInSeconds = 60,
            tutorial = "1. Sit in leg extension machine.\n2. Extend legs until fully straight.\n3. Lower with control.",
            notes = "Isolation exercise for quadriceps."
        ),

        Exercise(
            id = "22",
            name = "Russian Twists",
            mainMuscle = "Core",
            secondaryMuscles = listOf("Abs", "Obliques"),
            equipment = listOf("Bodyweight"),
            trackReps = true,
            tutorial = "1. Sit on floor with knees bent.\n2. Lean back slightly and lift feet off ground.\n3. Rotate torso side to side.",
            notes = "Can add weight for increased difficulty."
        ),

        Exercise(
            id = "23",
            name = "Hyperextensions",
            mainMuscle = "Back",
            secondaryMuscles = listOf("Glutes", "Hamstrings"),
            equipment = listOf("Bodyweight"),
            tutorial = "1. Position yourself on hyperextension bench.\n2. Lower upper body toward floor.\n3. Raise back up to starting position.",
            notes = "Great for lower back strength."
        ),

        Exercise(
            id = "24",
            name = "Lateral Raises",
            mainMuscle = "Shoulders",
            secondaryMuscles = listOf("Traps"),
            equipment = listOf("Dumbbell"),
            tempo = "2011",
            restTimeInSeconds = 60,
            tutorial = "1. Stand with dumbbells at sides.\n2. Raise dumbbells out to sides until parallel to ground.\n3. Lower with control.",
            notes = "Focus on lateral deltoid activation."
        )
    )
}