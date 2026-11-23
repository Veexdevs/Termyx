package com.termx.core.agent

import com.termx.core.ai.AiClient
import com.termx.core.ai.ChatMessage
import com.termx.core.util.TermyxJson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The TERMYX autonomous agent.
 *
 * It interprets a request, asks the AI for a structured plan using the registered
 * tool catalog, executes each step through the real [Tool] implementations, and —
 * when a step fails — asks the AI to replan with the observed error. Every action
 * goes through the tool system and never bypasses the permission/confirmation layer.
 */
class Agent(
    private val ctx: ToolContext,
    private val maxReplans: Int = 3,
    private val maxSteps: Int = 16
) {
    private val systemPrompt: String
        get() = """
You are TERMYX, an autonomous operations agent running inside a Linux environment on Android/Windows.
You plan and execute real tasks using tools. You NEVER invent results.

Available tools (name: description):
${ToolRegistry.catalog()}

Respond with a single JSON object and nothing else:
{
  "thinking": "short reasoning",
  "steps": [
    { "tool": "terminal.execute", "args": { "command": "uname -a" } }
  ]
}

Rules:
- Only use the tools listed above.
- Keep the plan under $maxSteps steps.
- For destructive operations (delete, execute_authorized) the tool requires user confirmation; still include them but the host may reject.
- Prefer small, verifiable steps. Read output before deciding next steps.
""".trimIndent()

    suspend fun run(userRequest: String, onEvent: (AgentEvent) -> Unit) {
        val provider = ctx.defaultProvider
        if (provider == null) {
            onEvent(AgentEvent.Error("No AI provider configured. Add one in AI Providers."))
            return
        }

        val messages = mutableListOf(
            ChatMessage("system", systemPrompt),
            ChatMessage("user", userRequest)
        )

        repeat(maxReplans + 1) { replanIndex ->
            val text = try {
                AiClient.chat(provider, messages)
            } catch (e: Exception) {
                onEvent(AgentEvent.Error("AI request failed: ${e.message}"))
                return
            }

            val plan = try {
                parsePlan(text).also { onEvent(AgentEvent.Planning(it.thinking)) }
            } catch (e: Exception) {
                onEvent(AgentEvent.Error("Could not parse plan: ${e.message}"))
                return
            }

            var allOk = true
            val results = mutableListOf<String>()

            plan.steps.forEachIndexed { index, step ->
                if (index >= maxSteps) return@forEachIndexed
                onEvent(AgentEvent.StepStart(index, step))
                step.status = StepStatus.RUNNING
                ctx.onLog("▶ ${step.tool} ${step.args}")

                val tool = ToolRegistry.byName(step.tool)
                if (tool == null) {
                    step.status = StepStatus.FAILED
                    step.result = "Unknown tool: ${step.tool}"
                    onEvent(AgentEvent.StepFailed(index, step, ToolResult.fail(step.result!!)))
                    allOk = false
                    results.add("Step ${index + 1} [${step.tool}]: UNKNOWN TOOL")
                    return@forEachIndexed
                }

                if (tool.dangerous && !ctx.confirm("Confirm dangerous operation '${step.tool}' with args $step.args?")) {
                    step.status = StepStatus.SKIPPED
                    step.result = "Rejected by user"
                    onEvent(AgentEvent.StepFailed(index, step, ToolResult.fail("Rejected by user")))
                    allOk = false
                    results.add("Step ${index + 1} [${step.tool}]: REJECTED")
                    return@forEachIndexed
                }

                val result = runCatching { tool.execute(step.args, ctx) }.getOrElse {
                    ToolResult.fail(it.message ?: "exception")
                }
                step.status = if (result.success) StepStatus.DONE else StepStatus.FAILED
                step.result = result.output.take(4000)
                results.add("Step ${index + 1} [${step.tool}]: ${if (result.success) "OK" else "FAILED"} — ${result.output.take(300)}")
                if (result.success) onEvent(AgentEvent.StepDone(index, step, result))
                else {
                    onEvent(AgentEvent.StepFailed(index, step, result))
                    allOk = false
                }
            }

            if (allOk) {
                val summary = summarize(provider, messages + ChatMessage("assistant", text) + ChatMessage("user", "Summarize the result for the user."), onEvent)
                onEvent(AgentEvent.Done(summary))
                return
            }

            if (replanIndex == maxReplans) {
                onEvent(AgentEvent.Error("Plan did not complete after $maxReplans replans.\n" + results.joinToString("\n")))
                return
            }

            onEvent(AgentEvent.Replanning("Some steps failed. Observed results:\n" + results.joinToString("\n")))
            messages.add(ChatMessage("assistant", text))
            messages.add(
                ChatMessage(
                    "user",
                    "Some steps failed or were rejected. Based on these results, produce a corrected plan " +
                        "to finish the original request:\n" + results.joinToString("\n")
                )
            )
        }
    }

    private suspend fun summarize(provider: com.termx.core.model.AiProvider, messages: List<ChatMessage>, onEvent: (AgentEvent) -> Unit): String =
        try {
            AiClient.chat(provider, messages).take(2000)
        } catch (e: Exception) {
            onEvent(AgentEvent.Log("Summary skipped: ${e.message}"))
            "Task completed (summary unavailable)."
        }

    companion object {
        /** Extract and parse a plan JSON object from a model response (strips code fences). */
        fun parsePlan(text: String): AgentPlan {
            val jsonText = extractJson(text)
            val root = TermyxJson.parseToJsonElement(jsonText).jsonObject
            val thinking = root["thinking"]?.jsonPrimitive?.content ?: ""
            val steps = (root["steps"] as? JsonArray)?.mapNotNull { el ->
                val obj = el as? JsonObject ?: return@mapNotNull null
                val tool = obj["tool"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val args = (obj["args"] as? JsonObject)?.mapValues { it.value.jsonPrimitive.content } ?: emptyMap()
                PlanStep(tool, args)
            } ?: emptyList()
            require(steps.isNotEmpty()) { "plan contained no steps" }
            return AgentPlan(thinking, steps)
        }

        private fun extractJson(text: String): String {
            val fence = "```"
            var s = text.trim()
            if (s.startsWith(fence)) {
                s = s.dropWhile { it == '`' }.dropWhile { it.isLetter() }.trim()
                val end = s.indexOf(fence)
                if (end >= 0) s = s.substring(0, end)
            }
            val start = s.indexOf('{')
            val end = s.lastIndexOf('}')
            return if (start >= 0 && end > start) s.substring(start, end + 1) else s
        }
    }
}
