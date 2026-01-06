"""
AI Coach Router - Intelligent training assistant endpoints
"""

from fastapi import APIRouter, HTTPException
from fastapi.responses import JSONResponse
import os
import re

router = APIRouter(prefix="/api/v1/ai-coach", tags=["AI Coach"])

# Also add legacy endpoint
legacy_router = APIRouter(tags=["AI Coach Legacy"])


@router.post("/generate-program")
async def generate_ai_program(request_data: dict):
    """
    Generate a complete training program using AI Coach
    """
    try:
        from ai_coach_service.program_generator import ProgramGenerator

        user_id = request_data.get('user_id')
        assessment_data = request_data.get('assessment_data')
        program_duration_weeks = request_data.get('program_duration_weeks', 8)
        num_workouts = request_data.get('num_workouts', 4)

        if not user_id or not assessment_data:
            raise HTTPException(
                status_code=400,
                detail="user_id and assessment_data are required"
            )

        generator = ProgramGenerator()
        result = generator.generate_program(
            user_id=user_id,
            assessment_data=assessment_data,
            program_duration_weeks=program_duration_weeks,
            num_workouts=num_workouts
        )

        if not result.get('success'):
            raise HTTPException(
                status_code=500,
                detail=result.get('error', 'Program generation failed')
            )

        return JSONResponse(content=result)

    except HTTPException:
        raise
    except Exception as e:
        print(f"AI Coach endpoint error: {str(e)}")
        import traceback
        traceback.print_exc()
        raise HTTPException(
            status_code=500,
            detail=f"Internal server error: {str(e)}"
        )


@router.get("/exercises/stats")
async def get_exercise_database_stats():
    """Get statistics about the exercise database"""
    try:
        from ai_coach_service.exercise_database import ExerciseDatabase

        exercise_db = ExerciseDatabase()
        stats = exercise_db.get_exercise_stats()
        return JSONResponse(content=stats)
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Failed to get exercise stats: {str(e)}"
        )


@legacy_router.post("/ai-coach")
async def get_coaching_cues(request_data: dict):
    """
    Legacy chat endpoint for AI Coach
    """
    try:
        from ai_coach_service.ai_coach_client import AICoachClient

        exercise = request_data.get('exercise', '')
        context = request_data.get('context', '')

        system_prompt = """Du bist ein erfahrener Menopause-Wellness-Coach, der mit seiner Klientin spricht.

Deine Expertise:
- Perimenopause und Menopause Symptom-Management
- Hormon-angepasste Trainingsempfehlungen
- Knochengesundheit und Osteoporose-Prävention
- Ernährung für hormonelle Balance (Phytoöstrogene, Calcium, Vitamin D)
- Schlafoptimierung in der Menopause
- Stress-Management und Stimmungsregulation

Dein Coaching-Stil:
- Sprich natürlich und einfühlsam, nicht wie ein Lehrbuch
- Sei motivierend und unterstützend, aber ehrlich
- Verwende Umgangssprache, vermeide formelle Listen
- KEINE Markdown-Formatierung (**, ###, -, etc.) - nur reiner Text
- Halte Antworten kurz und fokussiert (2-4 Sätze für Begrüßungen, 4-8 Sätze für Fragen)

Bei Fragen zu Symptomen oder Training:
- Berücksichtige hormonelle Veränderungen
- Betone Recovery und Stress-Management
- Gib sanfte, aber effektive Empfehlungen

Bei Begrüßungen oder Smalltalk:
- Antworte warmherzig und frage wie es ihr geht
- Baue Vertrauen auf wie ein echter Coach"""

        user_message = exercise if exercise else "Hello"

        if context:
            user_prompt = f"I'm working on {exercise}. {context}"
        elif exercise and len(exercise) > 50:
            user_prompt = exercise
        elif exercise and any(word in exercise.lower() for word in ['squat', 'bench', 'deadlift', 'press', 'pull', 'push', 'row', 'curl', 'lunge', 'raise', 'extension', 'flexion']):
            user_prompt = f"Can you give me some coaching tips for {exercise}?"
        else:
            user_prompt = exercise if exercise else "Hello coach!"

        ai_client = AICoachClient()
        result = ai_client.client.chat.completions.create(
            model=ai_client.model,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt}
            ],
            max_tokens=500,
            temperature=0.8
        )

        response_text = result.choices[0].message.content

        # Log usage for cost tracking (legacy endpoint - no user_id available)
        try:
            from supabase import create_client
            supabase_url = os.environ.get("SUPABASE_URL")
            supabase_key = os.environ.get("SUPABASE_KEY")
            if supabase_url and supabase_key:
                supabase = create_client(supabase_url, supabase_key)
                supabase.rpc('log_usage_event', {
                    'p_user_id': None,
                    'p_event_type': 'ai_coach_chat',
                    'p_input_tokens': result.usage.prompt_tokens,
                    'p_output_tokens': result.usage.completion_tokens,
                    'p_metadata': {
                        'model': ai_client.model,
                        'endpoint': 'legacy_coaching_cues'
                    },
                    'p_success': True
                }).execute()
        except Exception as log_error:
            print(f"Warning: Failed to log legacy coaching usage: {log_error}")

        return {
            "exercise": exercise,
            "cues": response_text
        }

    except Exception as e:
        print(f"Coaching cues error: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Error generating coaching cues: {str(e)}"
        )


@router.get("/test")
async def test_ai_coach():
    """Test AI Coach configuration and connection"""
    try:
        from ai_coach_service.program_generator import ProgramGenerator

        generator = ProgramGenerator()
        is_working = generator.test_ai_connection()

        return {
            "ai_coach_available": is_working,
            "openai_api_configured": os.environ.get("OPENAI_API_KEY") is not None,
            "supabase_configured": os.environ.get("SUPABASE_URL") is not None
        }
    except Exception as e:
        return {
            "ai_coach_available": False,
            "error": str(e)
        }


# ============================================================
# AI COACH - PERSISTENT CHAT ENDPOINTS
# ============================================================

@router.post("/chat")
async def chat_with_coach(request_data: dict):
    """
    Chat with AI Coach with persistent conversation history

    Supports image attachments for vision analysis:
    {
        "user_id": "uuid",
        "conversation_id": "uuid" (optional),
        "message": "What do you see in this image?",
        "context": {},
        "attachments": [
            {
                "type": "image",
                "data": "base64 encoded image data",
                "file_name": "photo.jpg",
                "mime_type": "image/jpeg"
            }
        ]
    }
    """
    try:
        from ai_coach_service.ai_coach_client import AICoachClient
        from ai_coach_service.conversation_manager import ConversationManager
        from ai_coach_service.user_context_builder import UserContextBuilder
        from ai_coach_service.workout_parser import WorkoutParser

        user_id = request_data.get('user_id')
        conversation_id = request_data.get('conversation_id')
        message = request_data.get('message', '')
        context = request_data.get('context', {})
        attachments = request_data.get('attachments', [])

        if not user_id:
            raise HTTPException(status_code=400, detail="user_id is required")
        if not message:
            raise HTTPException(status_code=400, detail="message is required")

        conv_manager = ConversationManager()

        # Create new conversation if needed
        if not conversation_id:
            conv_result = conv_manager.create_conversation(
                user_id=user_id,
                title=None
            )
            if not conv_result.get('success'):
                raise HTTPException(
                    status_code=500,
                    detail=f"Failed to create conversation: {conv_result.get('error')}"
                )
            conversation_id = conv_result['conversation_id']
            print(f"Created new conversation: {conversation_id}")

        # Get conversation history
        history = conv_manager.get_recent_messages(conversation_id, count=20)

        # Build system prompt with user context
        context_builder = UserContextBuilder()
        user_context = context_builder.build_user_context(user_id)
        system_prompt = context_builder.format_system_prompt(user_context)

        print(f"User context loaded: Profile={user_context.get('profile') is not None}, "
              f"PRs={len(user_context.get('prs', []))}, "
              f"Exercises={len(user_context.get('exercises', []))}, "
              f"UserWorkouts={len(user_context.get('workouts', []))}, "
              f"PublicTemplates={len(user_context.get('public_templates', []))}")

        # Build messages array
        messages = [{"role": "system", "content": system_prompt}]

        for msg in history:
            messages.append({
                "role": msg["role"],
                "content": msg["content"]
            })

        # Build user message content - with or without images
        has_images = attachments and any(att.get('type') == 'image' for att in attachments)

        if has_images:
            # Build multimodal message with images (OpenAI Vision API format)
            user_content = [{"type": "text", "text": message}]

            for attachment in attachments:
                if attachment.get('type') == 'image':
                    image_data = attachment.get('data', '')
                    mime_type = attachment.get('mime_type', 'image/jpeg')

                    # OpenAI expects base64 with data URL prefix
                    if not image_data.startswith('data:'):
                        image_data = f"data:{mime_type};base64,{image_data}"

                    user_content.append({
                        "type": "image_url",
                        "image_url": {
                            "url": image_data,
                            "detail": "high"  # Use high detail for better analysis
                        }
                    })

                    print(f"📸 Added image attachment: {attachment.get('file_name', 'unknown')}")

            messages.append({"role": "user", "content": user_content})
            print(f"🖼️ Chat with {len([a for a in attachments if a.get('type') == 'image'])} image(s)")
        else:
            messages.append({"role": "user", "content": message})

        # Save user message (store text only in conversation history)
        conv_manager.add_message(
            conversation_id=conversation_id,
            role="user",
            content=message,
            metadata={
                **context,
                "has_attachments": len(attachments) if attachments else 0,
                "attachment_types": [att.get('type') for att in attachments] if attachments else []
            }
        )

        # Call OpenAI API - use gpt-4o for vision capability
        ai_client = AICoachClient()
        model_to_use = "gpt-4o" if has_images else ai_client.model

        result = ai_client.client.chat.completions.create(
            model=model_to_use,
            messages=messages,
            max_tokens=1000 if has_images else 500,  # More tokens for image analysis
            temperature=0.8
        )

        print(f"🤖 Using model: {model_to_use}")

        response_text = result.choices[0].message.content

        # Save assistant response
        conv_manager.add_message(
            conversation_id=conversation_id,
            role="assistant",
            content=response_text,
            metadata={
                "model": ai_client.model,
                "tokens_input": result.usage.prompt_tokens,
                "tokens_output": result.usage.completion_tokens
            }
        )

        # Log usage for cost tracking
        try:
            from supabase import create_client
            import os
            supabase_url = os.environ.get("SUPABASE_URL")
            supabase_key = os.environ.get("SUPABASE_KEY")
            if supabase_url and supabase_key:
                supabase = create_client(supabase_url, supabase_key)
                supabase.rpc('log_usage_event', {
                    'p_user_id': user_id,
                    'p_event_type': 'ai_coach_chat',
                    'p_input_tokens': result.usage.prompt_tokens,
                    'p_output_tokens': result.usage.completion_tokens,
                    'p_metadata': {
                        'model': ai_client.model,
                        'conversation_id': conversation_id,
                        'message_length': len(message)
                    },
                    'p_success': True
                }).execute()
                print(f"Logged AI Coach usage: {result.usage.prompt_tokens}+{result.usage.completion_tokens} tokens")
        except Exception as log_error:
            print(f"Warning: Failed to log AI Coach usage: {log_error}")

        # Update conversation title if first exchange
        if len(history) == 0:
            title = message[:50] + "..." if len(message) > 50 else message
            conv_manager.update_conversation_title(conversation_id, title)

        print(f"Chat: {len(history)+1} messages in conversation {conversation_id}")

        # Detect workout recommendations
        workout_parser = WorkoutParser()
        actions = []

        if workout_parser.detect_workout(response_text):
            parsed_workout = workout_parser.parse_workout(response_text)
            if parsed_workout:
                print(f"Detected workout: {parsed_workout['name']} with {len(parsed_workout['exercises'])} exercises")
                actions.append({
                    "type": "workout_created",
                    "data": {"workout": parsed_workout}
                })

        # Detect template recommendations
        template_pattern = r'\[RECOMMEND_TEMPLATE:([^:]+):([^\]]+)\]'
        template_matches = re.findall(template_pattern, response_text)

        for template_id, template_name in template_matches:
            template_id = template_id.strip()
            template_name = template_name.strip()

            template_data = None
            for template in user_context.get('public_templates', []):
                if template.get('id') == template_id:
                    template_data = template
                    break

            if template_data:
                print(f"Detected template recommendation: {template_name} (ID: {template_id})")
                actions.append({
                    "type": "recommend_template",
                    "data": {
                        "template": {
                            "template_id": template_id,
                            "name": template_name,
                            "description": template_data.get('description', ''),
                            "sports": template_data.get('sports', []),
                            "exercise_count": len(template_data.get('exercises', [])),
                            "exercise_names": [ex.get('name', '') for ex in template_data.get('exercises', [])[:5]]
                        }
                    }
                })
            else:
                print(f"Template ID not found in context: {template_id}")

        # Clean response text
        clean_response = re.sub(template_pattern, '', response_text).strip()

        return {
            "conversation_id": conversation_id,
            "message": clean_response,
            "actions": actions
        }

    except HTTPException:
        raise
    except Exception as e:
        print(f"Chat error: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Error processing chat: {str(e)}"
        )


@router.get("/conversations")
async def get_user_conversations(user_id: str):
    """Get all conversations for a user"""
    try:
        from ai_coach_service.conversation_manager import ConversationManager

        if not user_id:
            raise HTTPException(status_code=400, detail="user_id is required")

        conv_manager = ConversationManager()
        conversations = conv_manager.get_user_conversations(
            user_id=user_id,
            include_archived=False,
            limit=50
        )

        return conversations

    except HTTPException:
        raise
    except Exception as e:
        print(f"Error getting conversations: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Error retrieving conversations: {str(e)}"
        )


@router.get("/conversations/{conversation_id}/messages")
async def get_conversation_messages(conversation_id: str):
    """Get all messages in a conversation"""
    try:
        from ai_coach_service.conversation_manager import ConversationManager

        conv_manager = ConversationManager()
        messages = conv_manager.get_conversation_messages(
            conversation_id=conversation_id,
            limit=200
        )

        return messages

    except Exception as e:
        print(f"Error getting messages: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Error retrieving messages: {str(e)}"
        )


@router.delete("/conversations/{conversation_id}")
async def delete_conversation(conversation_id: str):
    """Archive a conversation (soft delete)"""
    try:
        from ai_coach_service.conversation_manager import ConversationManager

        conv_manager = ConversationManager()
        result = conv_manager.archive_conversation(conversation_id)

        if not result.get('success'):
            raise HTTPException(
                status_code=500,
                detail=result.get('error', 'Failed to archive conversation')
            )

        return result

    except HTTPException:
        raise
    except Exception as e:
        print(f"Error archiving conversation: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Error archiving conversation: {str(e)}"
        )


@router.post("/save-workout")
async def save_ai_workout(request_data: dict):
    """Save AI-generated workout to user's workout templates"""
    try:
        from ai_coach_service.user_context_builder import UserContextBuilder

        user_id = request_data.get('user_id')
        workout = request_data.get('workout')

        if not user_id or not workout:
            raise HTTPException(status_code=400, detail="user_id and workout required")

        context_builder = UserContextBuilder()
        supabase = context_builder.client

        workout_data = {
            "user_id": user_id,
            "name": workout.get("name", "AI Coach Workout"),
            "description": workout.get("description", "Generated by AI Coach"),
            "created_by": "ai_coach",
            "is_public": False
        }

        result = supabase.table("workout_templates")\
            .insert(workout_data)\
            .execute()

        if not result.data or len(result.data) == 0:
            raise Exception("Failed to create workout template")

        workout_template_id = result.data[0]["id"]
        print(f"Created workout template: {workout_template_id}")

        exercises_data = []
        for exercise in workout.get("exercises", []):
            exercise_search = supabase.table("exercises")\
                .select("id")\
                .ilike("name", f"%{exercise['exercise_name']}%")\
                .limit(1)\
                .execute()

            if exercise_search.data and len(exercise_search.data) > 0:
                exercise_id = exercise_search.data[0]["id"]

                exercises_data.append({
                    "workout_template_id": workout_template_id,
                    "exercise_id": exercise_id,
                    "order_index": exercise.get("order_index", 0),
                    "target_sets": exercise.get("sets", 3),
                    "target_reps": exercise.get("reps", 10),
                    "notes": exercise.get("notes") or exercise.get("intensity")
                })

        if exercises_data:
            supabase.table("workout_template_exercises")\
                .insert(exercises_data)\
                .execute()

            print(f"Added {len(exercises_data)} exercises to workout")

        return {
            "success": True,
            "workout_template_id": workout_template_id,
            "exercises_added": len(exercises_data)
        }

    except HTTPException:
        raise
    except Exception as e:
        print(f"Error saving workout: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Error saving workout: {str(e)}"
        )


# ============================================================
# CROSSFIT WOD SCANNER ENDPOINTS
# ============================================================

@router.post("/scan-wod")
async def scan_wod_from_image(request_data: dict):
    """
    Scan a CrossFit WOD from a whiteboard photo using Vision AI

    Request body:
    {
        "user_id": "uuid",
        "image_base64": "base64 encoded image data",
        "image_type": "image/jpeg" (optional),
        "box_name": "CrossFit Box Name" (optional),
        "save_to_database": true/false (default: true),
        "is_public": true/false (default: false)
    }

    Returns:
    {
        "success": true/false,
        "wod": { parsed WOD data },
        "wod_template_id": "uuid" (if saved),
        "confidence": 0.0-1.0
    }
    """
    try:
        from ai_coach_service.wod_vision_parser import WodVisionParser, WodDatabaseService

        user_id = request_data.get('user_id')
        image_base64 = request_data.get('image_base64')
        image_type = request_data.get('image_type', 'image/jpeg')
        box_name = request_data.get('box_name')
        save_to_db = request_data.get('save_to_database', True)
        is_public = request_data.get('is_public', False)

        if not image_base64:
            raise HTTPException(status_code=400, detail="image_base64 is required")

        print(f"Scanning WOD image for user {user_id}")

        # Parse the WOD from image
        parser = WodVisionParser()
        result = parser.parse_wod_image(
            image_base64=image_base64,
            image_type=image_type,
            additional_context=f"From CrossFit box: {box_name}" if box_name else None
        )

        if not result.get('success'):
            raise HTTPException(
                status_code=422,
                detail=result.get('error', 'Could not parse WOD from image')
            )

        wod_data = result.get('wod', {})

        # Log usage for cost tracking
        try:
            from supabase import create_client
            supabase_url = os.environ.get("SUPABASE_URL")
            supabase_key = os.environ.get("SUPABASE_KEY")
            if supabase_url and supabase_key:
                tokens = result.get('tokens_used', {})
                supabase = create_client(supabase_url, supabase_key)
                supabase.rpc('log_usage_event', {
                    'p_user_id': user_id,
                    'p_event_type': 'wod_scan',
                    'p_input_tokens': tokens.get('input', 0),
                    'p_output_tokens': tokens.get('output', 0),
                    'p_metadata': {
                        'model': 'gpt-4o',
                        'wod_name': wod_data.get('name'),
                        'wod_type': wod_data.get('wod_type'),
                        'movements_count': len(wod_data.get('movements', []))
                    },
                    'p_success': True
                }).execute()
                print(f"Logged WOD scan usage")
        except Exception as log_error:
            print(f"Warning: Failed to log WOD scan usage: {log_error}")

        response = {
            "success": True,
            "wod": wod_data,
            "confidence": wod_data.get('confidence', 0.8)
        }

        # Save to database if requested
        if save_to_db:
            try:
                db_service = WodDatabaseService()
                save_result = db_service.save_wod(
                    wod_data=wod_data,
                    user_id=user_id,
                    source_box_name=box_name,
                    is_public=is_public
                )

                if save_result.get('success'):
                    response['wod_template_id'] = save_result.get('wod_template_id')
                    response['movements_saved'] = save_result.get('movements_count', 0)
                    print(f"WOD saved: {save_result.get('wod_template_id')}")
                else:
                    response['save_error'] = save_result.get('error')
                    print(f"Warning: WOD parsed but not saved: {save_result.get('error')}")

            except Exception as save_error:
                response['save_error'] = str(save_error)
                print(f"Warning: Could not save WOD: {save_error}")

        return response

    except HTTPException:
        raise
    except Exception as e:
        print(f"WOD scan error: {str(e)}")
        import traceback
        traceback.print_exc()
        raise HTTPException(
            status_code=500,
            detail=f"Error scanning WOD: {str(e)}"
        )


@router.post("/scan-wod-url")
async def scan_wod_from_url(request_data: dict):
    """
    Scan a CrossFit WOD from an image URL (e.g., Supabase Storage)

    Request body:
    {
        "user_id": "uuid",
        "image_url": "https://...",
        "box_name": "CrossFit Box Name" (optional),
        "save_to_database": true/false (default: true),
        "is_public": true/false (default: false)
    }
    """
    try:
        from ai_coach_service.wod_vision_parser import WodVisionParser, WodDatabaseService

        user_id = request_data.get('user_id')
        image_url = request_data.get('image_url')
        box_name = request_data.get('box_name')
        save_to_db = request_data.get('save_to_database', True)
        is_public = request_data.get('is_public', False)

        if not image_url:
            raise HTTPException(status_code=400, detail="image_url is required")

        print(f"Scanning WOD from URL for user {user_id}")

        # Parse the WOD from URL
        parser = WodVisionParser()
        result = parser.parse_wod_from_url(
            image_url=image_url,
            additional_context=f"From CrossFit box: {box_name}" if box_name else None
        )

        if not result.get('success'):
            raise HTTPException(
                status_code=422,
                detail=result.get('error', 'Could not parse WOD from image')
            )

        wod_data = result.get('wod', {})

        response = {
            "success": True,
            "wod": wod_data,
            "confidence": wod_data.get('confidence', 0.8)
        }

        # Save to database if requested
        if save_to_db:
            try:
                db_service = WodDatabaseService()
                save_result = db_service.save_wod(
                    wod_data=wod_data,
                    user_id=user_id,
                    source_image_url=image_url,
                    source_box_name=box_name,
                    is_public=is_public
                )

                if save_result.get('success'):
                    response['wod_template_id'] = save_result.get('wod_template_id')
                    response['movements_saved'] = save_result.get('movements_count', 0)

            except Exception as save_error:
                response['save_error'] = str(save_error)

        return response

    except HTTPException:
        raise
    except Exception as e:
        print(f"WOD URL scan error: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Error scanning WOD: {str(e)}"
        )


@router.get("/wods")
async def get_wods(
    user_id: str = None,
    wod_type: str = None,
    difficulty: str = None,
    max_duration: int = None,
    search: str = None,
    limit: int = 20,
    offset: int = 0
):
    """
    Search and list WODs from the database

    Query parameters:
    - user_id: Filter by user (optional)
    - wod_type: Filter by type (amrap, emom, for_time, etc.)
    - difficulty: Filter by difficulty (beginner, intermediate, advanced, elite)
    - max_duration: Max duration in minutes
    - search: Search query for name
    - limit: Max results (default 20)
    - offset: Pagination offset
    """
    try:
        from ai_coach_service.wod_vision_parser import WodDatabaseService

        db_service = WodDatabaseService()
        wods = db_service.search_wods(
            query=search,
            wod_type=wod_type,
            difficulty=difficulty,
            max_duration=max_duration,
            limit=limit,
            offset=offset
        )

        return {
            "wods": wods,
            "count": len(wods),
            "limit": limit,
            "offset": offset
        }

    except Exception as e:
        print(f"Error getting WODs: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Error retrieving WODs: {str(e)}"
        )


@router.get("/wods/{wod_id}")
async def get_wod_detail(wod_id: str):
    """
    Get a single WOD with all movements and scaling options
    """
    try:
        from ai_coach_service.wod_vision_parser import WodDatabaseService

        db_service = WodDatabaseService()
        wod = db_service.get_wod_with_movements(wod_id)

        if not wod:
            raise HTTPException(status_code=404, detail="WOD not found")

        return wod

    except HTTPException:
        raise
    except Exception as e:
        print(f"Error getting WOD detail: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Error retrieving WOD: {str(e)}"
        )


@router.post("/wods/{wod_id}/log-result")
async def log_wod_result(wod_id: str, request_data: dict):
    """
    Log a user's result/score for a WOD

    Request body:
    {
        "user_id": "uuid",
        "score_type": "rounds_reps|time|weight|reps",
        "rounds_completed": number (optional),
        "reps_completed": number (optional),
        "time_seconds": number (optional),
        "weight_kg": number (optional),
        "total_reps": number (optional),
        "scaling_level": "rx|scaled|foundations",
        "completed_within_cap": true/false,
        "notes": "string" (optional),
        "video_url": "string" (optional)
    }
    """
    try:
        from supabase import create_client

        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")

        if not supabase_url or not supabase_key:
            raise HTTPException(status_code=500, detail="Database not configured")

        supabase = create_client(supabase_url, supabase_key)

        user_id = request_data.get('user_id')
        if not user_id:
            raise HTTPException(status_code=400, detail="user_id is required")

        result_data = {
            "user_id": user_id,
            "wod_template_id": wod_id,
            "score_type": request_data.get('score_type', 'rounds_reps'),
            "rounds_completed": request_data.get('rounds_completed'),
            "reps_completed": request_data.get('reps_completed'),
            "time_seconds": request_data.get('time_seconds'),
            "weight_kg": request_data.get('weight_kg'),
            "total_reps": request_data.get('total_reps'),
            "scaling_level": request_data.get('scaling_level', 'rx'),
            "completed_within_cap": request_data.get('completed_within_cap', True),
            "notes": request_data.get('notes'),
            "video_url": request_data.get('video_url')
        }

        result = supabase.table("wod_results").insert(result_data).execute()

        if not result.data:
            raise Exception("Failed to log WOD result")

        return {
            "success": True,
            "result_id": result.data[0]["id"]
        }

    except HTTPException:
        raise
    except Exception as e:
        print(f"Error logging WOD result: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Error logging result: {str(e)}"
        )


# ============================================================
# AI EXERCISE CREATOR - Generate exercises from unknown movements
# ============================================================

@router.post("/create-exercise")
async def create_exercise_from_ai(request_data: dict):
    """
    Use AI to create a new exercise from an unknown movement name.
    This is used when scanning CrossFit WODs that contain exercises
    not in the database.

    Request body:
    {
        "user_id": "uuid",
        "movement_name": "Turkish Get-Up",
        "context": "CrossFit WOD, barbell workout, etc." (optional)
    }

    Returns:
    {
        "success": true/false,
        "exercise": { complete exercise data },
        "exercise_id": "uuid"
    }
    """
    try:
        from ai_coach_service.ai_coach_client import AICoachClient
        from supabase import create_client
        import uuid

        user_id = request_data.get('user_id')
        movement_name = request_data.get('movement_name', '').strip()
        context = request_data.get('context', '')

        if not movement_name:
            raise HTTPException(status_code=400, detail="movement_name is required")

        print(f"🤖 AI Exercise Creator: Generating exercise for '{movement_name}'")

        # Build prompt for AI
        system_prompt = """You are an expert strength & conditioning coach with encyclopedic knowledge of exercises.

Given an exercise or movement name, you must provide complete exercise metadata in JSON format.

IMPORTANT RULES:
1. Return ONLY valid JSON, no markdown, no explanations
2. Be accurate about muscle groups and equipment
3. Use standardized names for muscle groups: Chest, Back, Shoulders, Biceps, Triceps, Quadriceps, Hamstrings, Glutes, Calves, Core, Full Body, Cardio
4. Use standardized equipment: Barbell, Dumbbell, Kettlebell, Bodyweight, Cable Machine, Machine, Resistance Band, Medicine Ball, Pull-up Bar, Rings, Box, Rope, Rower, Bike, Ski Erg, None
5. For CrossFit movements, always include "CrossFit" in sports
6. Determine tracking type based on movement:
   - Most strength exercises: track_reps=true, track_weight=true
   - Cardio/running: track_distance=true, track_duration=true
   - Holds/planks: track_duration=true
   - Calisthenics: track_reps=true, track_weight=false

Return this exact JSON structure:
{
    "name": "Proper Exercise Name (properly capitalized)",
    "category": "Primary muscle group",
    "secondary_muscle_groups": ["list", "of", "secondary", "muscles"],
    "equipment": ["equipment", "needed"],
    "level": "beginner|intermediate|advanced",
    "sports": ["CrossFit", "General Strength", etc.],
    "track_reps": true/false,
    "track_sets": true/false,
    "track_weight": true/false,
    "track_duration": true/false,
    "track_distance": true/false,
    "tutorial": "Brief 1-2 sentence description of how to perform the exercise",
    "notes": "Key coaching cues or safety tips"
}"""

        user_prompt = f"""Create exercise metadata for: {movement_name}

Context: {context if context else 'CrossFit/functional fitness movement'}

Return ONLY the JSON object."""

        # Call OpenAI API
        ai_client = AICoachClient()
        result = ai_client.client.chat.completions.create(
            model="gpt-4-turbo-preview",
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt}
            ],
            max_tokens=500,
            temperature=0.3  # Lower temperature for more consistent output
        )

        response_text = result.choices[0].message.content

        # Parse JSON response
        import json
        try:
            # Try direct JSON parse
            exercise_data = json.loads(response_text)
        except json.JSONDecodeError:
            # Try to extract JSON from markdown
            if "```json" in response_text:
                json_start = response_text.find("```json") + 7
                json_end = response_text.find("```", json_start)
                json_text = response_text[json_start:json_end].strip()
                exercise_data = json.loads(json_text)
            elif "```" in response_text:
                json_start = response_text.find("```") + 3
                json_end = response_text.find("```", json_start)
                json_text = response_text[json_start:json_end].strip()
                exercise_data = json.loads(json_text)
            else:
                start = response_text.find("{")
                end = response_text.rfind("}") + 1
                if start != -1 and end > start:
                    exercise_data = json.loads(response_text[start:end])
                else:
                    raise ValueError("No valid JSON in response")

        print(f"✅ AI generated exercise: {exercise_data.get('name')}")

        # Generate unique ID
        exercise_id = str(uuid.uuid4())

        # Prepare data for Supabase insert
        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")

        if not supabase_url or not supabase_key:
            raise HTTPException(status_code=500, detail="Database not configured")

        supabase = create_client(supabase_url, supabase_key)

        # Build exercise record
        exercise_record = {
            "id": exercise_id,
            "name": exercise_data.get("name", movement_name),
            "category": exercise_data.get("category", "Full Body"),
            "secondary_muscle_groups": exercise_data.get("secondary_muscle_groups", []),
            "equipment": exercise_data.get("equipment", ["Bodyweight"]),
            "level": exercise_data.get("level", "intermediate"),
            "visibility": "user",  # User-created exercises
            "owner_id": user_id,
            "sports": exercise_data.get("sports", ["CrossFit"]),
            "track_reps": exercise_data.get("track_reps", True),
            "track_sets": exercise_data.get("track_sets", True),
            "track_weight": exercise_data.get("track_weight", True),
            "track_duration": exercise_data.get("track_duration", False),
            "track_distance": exercise_data.get("track_distance", False),
            "track_rpe": False,
            "tutorial": exercise_data.get("tutorial", ""),
            "notes": exercise_data.get("notes", ""),
            "vbt_enabled": False,
            "bartracker_enabled": False
        }

        # Insert into exercises_new table
        insert_result = supabase.table("exercises_new").insert(exercise_record).execute()

        if not insert_result.data:
            raise Exception("Failed to insert exercise into database")

        print(f"✅ Exercise saved to database: {exercise_id}")

        # Log usage for cost tracking
        try:
            supabase.rpc('log_usage_event', {
                'p_user_id': user_id,
                'p_event_type': 'ai_exercise_create',
                'p_input_tokens': result.usage.prompt_tokens,
                'p_output_tokens': result.usage.completion_tokens,
                'p_metadata': {
                    'model': 'gpt-4-turbo-preview',
                    'movement_name': movement_name,
                    'created_exercise': exercise_data.get('name')
                },
                'p_success': True
            }).execute()
        except Exception as log_error:
            print(f"Warning: Failed to log AI exercise create usage: {log_error}")

        return {
            "success": True,
            "exercise": exercise_record,
            "exercise_id": exercise_id
        }

    except HTTPException:
        raise
    except Exception as e:
        print(f"❌ AI Exercise Creator error: {str(e)}")
        import traceback
        traceback.print_exc()
        raise HTTPException(
            status_code=500,
            detail=f"Error creating exercise: {str(e)}"
        )


@router.post("/create-exercises-batch")
async def create_exercises_batch(request_data: dict):
    """
    Create multiple exercises from a list of unknown movement names.

    Request body:
    {
        "user_id": "uuid",
        "movements": ["Turkish Get-Up", "Devil Press", "Sandbag Carry"],
        "context": "CrossFit WOD"
    }

    Returns:
    {
        "success": true/false,
        "created": [{ exercise data }],
        "failed": [{ "name": "...", "error": "..." }]
    }
    """
    try:
        user_id = request_data.get('user_id')
        movements = request_data.get('movements', [])
        context = request_data.get('context', '')

        if not movements:
            raise HTTPException(status_code=400, detail="movements list is required")

        created = []
        failed = []

        for movement_name in movements:
            try:
                # Call the single exercise creator
                result = await create_exercise_from_ai({
                    "user_id": user_id,
                    "movement_name": movement_name,
                    "context": context
                })

                if result.get("success"):
                    created.append(result.get("exercise"))
                else:
                    failed.append({"name": movement_name, "error": "Unknown error"})

            except Exception as e:
                failed.append({"name": movement_name, "error": str(e)})

        return {
            "success": len(failed) == 0,
            "created": created,
            "failed": failed,
            "created_count": len(created),
            "failed_count": len(failed)
        }

    except HTTPException:
        raise
    except Exception as e:
        print(f"Batch exercise creation error: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Error in batch creation: {str(e)}"
        )


@router.get("/wods/{wod_id}/results")
async def get_wod_results(wod_id: str, user_id: str = None, limit: int = 50):
    """
    Get results/scores for a WOD

    Query parameters:
    - user_id: Filter by user (optional, defaults to all users)
    - limit: Max results (default 50)
    """
    try:
        from supabase import create_client

        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")

        if not supabase_url or not supabase_key:
            raise HTTPException(status_code=500, detail="Database not configured")

        supabase = create_client(supabase_url, supabase_key)

        query = supabase.table("wod_results")\
            .select("*")\
            .eq("wod_template_id", wod_id)\
            .order("completed_at", desc=True)\
            .limit(limit)

        if user_id:
            query = query.eq("user_id", user_id)

        result = query.execute()

        return {
            "results": result.data or [],
            "count": len(result.data or [])
        }

    except Exception as e:
        print(f"Error getting WOD results: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Error retrieving results: {str(e)}"
        )