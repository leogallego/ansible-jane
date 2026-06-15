package io.github.leogallego.ansiblejane.assistant.tools.local

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import io.github.leogallego.ansiblejane.assistant.tools.AapLocalTool
import io.github.leogallego.ansiblejane.data.ProjectRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

class GetProjectLocalTool(
    private val repository: ProjectRepository
) : AapLocalTool<GetProjectLocalTool.Args>(
    typeToken<Args>(), Args.serializer(),
    "get_project", "Get details of a specific project by ID — SCM URL, branch, last sync status"
) {
    @Serializable
    data class Args(
        @SerialName("project_id")
        @property:LLMDescription("ID of the project")
        val projectId: Int
    )

    override suspend fun execute(args: Args): String {
        val project = repository.getProject(args.projectId).getOrThrow()
        return networkJson.encodeToString(project)
    }
}
