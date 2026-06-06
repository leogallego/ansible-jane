package io.github.leogallego.ansiblejane.model

interface LaunchableTemplate {
    val id: Int
    val name: String
    val description: String
    val labels: List<Label>
    val canStart: Boolean
}
