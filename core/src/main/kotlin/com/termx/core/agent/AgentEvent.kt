package com.termx.core.agent

/** Real-time progress events emitted by [Agent.run] for the UI to render. */
sealed interface AgentEvent {
    data class Log(val message: String) : AgentEvent
    data class Planning(val thinking: String) : AgentEvent
    data class StepStart(val index: Int, val step: PlanStep) : AgentEvent
    data class StepDone(val index: Int, val step: PlanStep, val result: ToolResult) : AgentEvent
    data class StepFailed(val index: Int, val step: PlanStep, val result: ToolResult) : AgentEvent
    data class Replanning(val reason: String) : AgentEvent
    data class Done(val summary: String) : AgentEvent
    data class Error(val message: String) : AgentEvent
}
