package com.forgetmenot.app.ai

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.models.messages.Base64ImageSource
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.ImageBlockParam
import com.anthropic.models.messages.JsonOutputFormat
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.StopReason
import com.anthropic.models.messages.TextBlockParam
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** A task Claude pulled out of a message or screenshot, before the user confirms it. */
data class DraftTask(
    val title: String,
    val requester: String?,
    val details: String?,
    /** Epoch millis, or null when no due time was found. */
    val dueAt: Long?,
    val highPriority: Boolean,
    val warnings: List<String>,
)

data class Extraction(val tasks: List<DraftTask>, val skipped: List<String>)

class ExtractionException(message: String) : Exception(message)

/** Turns a pasted message or a chat screenshot into draft tasks. */
class TaskExtractor(private val apiKey: String, private val model: String) {

    fun extract(text: String?, imageBase64: String?, imageMediaType: String?): Extraction {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val blocks = mutableListOf<ContentBlockParam>()
        if (imageBase64 != null) {
            blocks += ContentBlockParam.ofImage(
                ImageBlockParam.builder()
                    .source(
                        Base64ImageSource.builder()
                            .data(imageBase64)
                            .mediaType(Base64ImageSource.MediaType.of(imageMediaType ?: "image/jpeg"))
                            .build()
                    )
                    .build()
            )
        }
        val instruction = buildString {
            append("Current time: ")
            append(now.format(DateTimeFormatter.ofPattern("EEEE yyyy-MM-dd HH:mm")))
            append(" (").append(zone.id).append(").\n")
            if (imageBase64 != null) append("The image is a screenshot of a work chat taken just now.\n")
            if (!text.isNullOrBlank()) append("Message:\n\"\"\"\n").append(text.trim()).append("\n\"\"\"\n")
        }
        blocks += ContentBlockParam.ofText(TextBlockParam.builder().text(instruction).build())

        val client = AnthropicOkHttpClient.builder().apiKey(apiKey).build()
        try {
            val params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(16000L)
                .system(SYSTEM_PROMPT)
                .addUserMessageOfBlockParams(blocks)
                .outputConfig(
                    OutputConfig.builder()
                        .effort(OutputConfig.Effort.LOW)
                        .format(
                            JsonOutputFormat.builder()
                                .schema(
                                    JsonOutputFormat.Schema.builder()
                                        .putAllAdditionalProperties(schema())
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .apply {
                    // If Opus declines a request, let the API retry it on another model instead of failing.
                    if (model.startsWith("claude-opus")) {
                        putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
                        putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                    }
                }
                .build()
            val response = client.messages().create(params)
            if (response.stopReason().orElse(null) == StopReason.REFUSAL) {
                throw ExtractionException("Claude declined to read this one. Add the task by hand.")
            }
            val json = response.content().firstNotNullOfOrNull { block -> block.text().orElse(null)?.text() }
                ?: throw ExtractionException("Claude didn't return any tasks.")
            return parse(json, zone)
        } finally {
            client.close()
        }
    }

    private fun parse(json: String, zone: ZoneId): Extraction {
        val root = JSONObject(json)
        val tasks = root.getJSONArray("tasks")
        val drafts = (0 until tasks.length()).map { i ->
            val t = tasks.getJSONObject(i)
            val due = t.optString("due", "")
            val dueAt = when {
                due.equals("ASAP", ignoreCase = true) -> System.currentTimeMillis()
                due.isBlank() -> null
                else -> runCatching {
                    LocalDateTime.parse(due).atZone(zone).toInstant().toEpochMilli()
                }.getOrNull()
            }
            val warnings = t.optJSONArray("warnings")
            DraftTask(
                title = t.getString("title"),
                requester = t.optString("requester").takeIf { it.isNotBlank() && it != "null" },
                details = t.optString("details").takeIf { it.isNotBlank() && it != "null" },
                dueAt = dueAt,
                highPriority = t.optBoolean("high_priority", false),
                warnings = if (warnings == null) emptyList() else (0 until warnings.length()).map { warnings.getString(it) },
            )
        }
        val skippedArr = root.optJSONArray("skipped")
        val skipped = if (skippedArr == null) emptyList() else (0 until skippedArr.length()).map { skippedArr.getString(it) }
        return Extraction(drafts, skipped)
    }

    private fun schema(): Map<String, JsonValue> {
        val str = mapOf("type" to "string")
        val task = mapOf(
            "type" to "object",
            "additionalProperties" to false,
            "required" to listOf("title", "requester", "details", "due", "high_priority", "warnings"),
            "properties" to mapOf(
                "title" to mapOf("type" to "string", "description" to "Short imperative action, e.g. \"Fill Zents extract order\""),
                "requester" to mapOf("type" to "string", "description" to "Name of the person asking, or empty string"),
                "details" to mapOf("type" to "string", "description" to "Key specifics joined with \" · \", product codes copied exactly, or empty string"),
                "due" to mapOf("type" to "string", "description" to "Local ISO datetime like 2026-10-23T17:00, \"ASAP\", or empty string"),
                "high_priority" to mapOf("type" to "boolean"),
                "warnings" to mapOf("type" to "array", "items" to str),
            ),
        )
        return mapOf(
            "type" to JsonValue.from("object"),
            "additionalProperties" to JsonValue.from(false),
            "required" to JsonValue.from(listOf("tasks", "skipped")),
            "properties" to JsonValue.from(
                mapOf(
                    "tasks" to mapOf("type" to "array", "items" to task),
                    "skipped" to mapOf("type" to "array", "items" to str),
                )
            ),
        )
    }

    companion object {
        private val SYSTEM_PROMPT = """
            You turn work chat messages into to-do items for the user, who forgets requests made to them.

            Find every request someone made of the user that is still open. For each, write:
            - title: a short imperative action.
            - requester: who asked.
            - details: the specifics needed to do the work (customer, amounts, product codes, containers). Copy codes and numbers exactly as written; never "correct" them.
            - due: when it is needed. Resolve relative dates ("Thursday", "tomorrow", "EOD") against when the message was sent, not against now. Use "ASAP" if it is urgent or overdue with no time given. Leave empty if there is no deadline.
            - high_priority: true if the person followed up, said it is urgent, or a customer is waiting.
            - warnings: anything the user must check, especially text cut off at the edge of a screenshot that may hide a deadline or detail, or characters you could not read with certainty (for example O versus 0 in a code).

            Messages labelled "You" were sent by the user: they are not requests to the user. A quoted or repeated request plus a follow-up such as "how is this coming along?" is one task, not two; the follow-up makes it high priority. Requests the user already completed or that someone thanked them for are done.

            In "skipped", briefly list messages you decided were not open tasks and why.
        """.trimIndent()
    }
}
