// ═══════════════════════════════════════════════════════════════
// PROMETHEUS: AI MEAL PHOTO ANALYSIS
// Edge Function - Powered by Claude 3.5 Sonnet Vision
// ═══════════════════════════════════════════════════════════════

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import Anthropic from "npm:@anthropic-ai/sdk@0.30.1";

const ANTHROPIC_API_KEY = Deno.env.get("ANTHROPIC_API_KEY");

interface AnalyzeMealRequest {
  image_base64: string;
  meal_type?: "breakfast" | "lunch" | "dinner" | "snack";
  additional_context?: string; // Optional: "This is a protein shake" or "Restaurant meal"
}

interface MealItem {
  name: string;
  quantity: string;
  quantity_value: number;
  quantity_unit: string;
  calories: number;
  protein: number;
  carbs: number;
  fat: number;
  confidence: number; // 0-1 (AI confidence in this estimation)
}

interface AnalyzeMealResponse {
  success: boolean;
  meal_name: string;
  items: MealItem[];
  total: {
    calories: number;
    protein: number;
    carbs: number;
    fat: number;
  };
  ai_confidence: number; // Overall confidence (0-1)
  suggestions?: string; // Optional AI suggestions
  error?: string;
}

serve(async (req) => {
  // CORS headers
  if (req.method === "OPTIONS") {
    return new Response(null, {
      headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "POST, OPTIONS",
        "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
      },
    });
  }

  try {
    // Parse request
    const { image_base64, meal_type, additional_context }: AnalyzeMealRequest =
      await req.json();

    if (!image_base64) {
      return new Response(
        JSON.stringify({ success: false, error: "No image provided" }),
        {
          status: 400,
          headers: { "Content-Type": "application/json" },
        }
      );
    }

    // Initialize Anthropic client
    const client = new Anthropic({ apiKey: ANTHROPIC_API_KEY });

    // Build prompt
    const contextInfo = additional_context ? `\nAdditional context: ${additional_context}` : "";
    const mealTypeInfo = meal_type ? `\nMeal type: ${meal_type}` : "";

    const prompt = `You are a nutrition expert analyzing a meal photo. Identify all visible food items and provide accurate nutritional estimates.

${mealTypeInfo}${contextInfo}

IMPORTANT INSTRUCTIONS:
1. Identify each distinct food item in the image
2. Estimate portion sizes as accurately as possible (in grams or standard serving sizes)
3. Provide nutritional information (calories, protein, carbs, fat) for EACH item
4. Include a confidence score (0-1) for each item based on visibility and portion size clarity
5. Sum up totals accurately
6. If you're unsure about something, give your best estimate and reflect that in the confidence score
7. Be realistic - a typical chicken breast is 150-200g, not 500g

Return ONLY valid JSON (no markdown, no code blocks):
{
  "meal_name": "descriptive name (e.g. 'Grilled Chicken with Rice and Vegetables')",
  "items": [
    {
      "name": "food item name",
      "quantity": "readable amount (e.g. '200g' or '1 cup')",
      "quantity_value": numeric_value,
      "quantity_unit": "g/ml/piece/cup",
      "calories": number,
      "protein": number,
      "carbs": number,
      "fat": number,
      "confidence": 0.0-1.0
    }
  ],
  "total": {
    "calories": sum_of_all_calories,
    "protein": sum_of_all_protein,
    "carbs": sum_of_all_carbs,
    "fat": sum_of_all_fat
  },
  "ai_confidence": average_confidence,
  "suggestions": "optional helpful notes (e.g. 'Consider adding more protein' or 'Great balanced meal!')"
}`;

    // Call Claude API
    console.log("📸 Analyzing meal photo with Claude Vision...");

    const message = await client.messages.create({
      model: "claude-3-5-sonnet-20241022", // Latest Sonnet with vision
      max_tokens: 2000,
      messages: [
        {
          role: "user",
          content: [
            {
              type: "image",
              source: {
                type: "base64",
                media_type: "image/jpeg",
                data: image_base64,
              },
            },
            {
              type: "text",
              text: prompt,
            },
          ],
        },
      ],
    });

    // Parse response
    const responseText = message.content[0].text;
    console.log("🤖 Claude response:", responseText);

    // Clean response (remove markdown if present)
    let cleanedResponse = responseText.trim();
    if (cleanedResponse.startsWith("```json")) {
      cleanedResponse = cleanedResponse
        .replace(/^```json\s*/, "")
        .replace(/\s*```$/, "");
    } else if (cleanedResponse.startsWith("```")) {
      cleanedResponse = cleanedResponse
        .replace(/^```\s*/, "")
        .replace(/\s*```$/, "");
    }

    const analysisResult: AnalyzeMealResponse = JSON.parse(cleanedResponse);
    analysisResult.success = true;

    console.log("✅ Analysis complete:", {
      meal: analysisResult.meal_name,
      items: analysisResult.items.length,
      total_calories: analysisResult.total.calories,
    });

    return new Response(JSON.stringify(analysisResult), {
      headers: {
        "Content-Type": "application/json",
        "Access-Control-Allow-Origin": "*",
      },
    });
  } catch (error) {
    console.error("❌ Error analyzing meal:", error);

    return new Response(
      JSON.stringify({
        success: false,
        error: error.message || "Failed to analyze meal",
      }),
      {
        status: 500,
        headers: {
          "Content-Type": "application/json",
          "Access-Control-Allow-Origin": "*",
        },
      }
    );
  }
});

/* ═══════════════════════════════════════════════════════════════
 * DEPLOYMENT INSTRUCTIONS:
 *
 * 1. Install Supabase CLI: https://supabase.com/docs/guides/cli
 *
 * 2. Link to your project:
 *    supabase link --project-ref your-project-ref
 *
 * 3. Set secret (Anthropic API Key):
 *    supabase secrets set ANTHROPIC_API_KEY=your-api-key-here
 *
 * 4. Deploy function:
 *    supabase functions deploy analyze-meal-photo
 *
 * 5. Test with curl:
 *    curl -i --location --request POST \
 *      'https://your-project-ref.supabase.co/functions/v1/analyze-meal-photo' \
 *      --header 'Authorization: Bearer YOUR_ANON_KEY' \
 *      --header 'Content-Type: application/json' \
 *      --data '{"image_base64":"base64_string_here","meal_type":"lunch"}'
 *
 * ═══════════════════════════════════════════════════════════════ */