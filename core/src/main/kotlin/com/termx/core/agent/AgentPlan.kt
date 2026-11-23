package com.termx.core.agent

/** A single planned action inside an agent plan. */
data class PlanStep(
    val tool: String,
    val args: Map<String, String>,
    var status: StepStatus = StepStatus.PENDING,
    var result: String? = null
)

enum class StepStatus { PENDING, RUNNING, DONE, FAILED, SKIPPED }

/** The execution plan produced by the AI for a user request. */
data class AgentPlan(
    val thinking: String,
    val steps: List<PlanStep>
)
