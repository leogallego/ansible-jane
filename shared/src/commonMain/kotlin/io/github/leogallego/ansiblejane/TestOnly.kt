package io.github.leogallego.ansiblejane

@RequiresOptIn(
    message = "This API is internal and intended for testing only.",
    level = RequiresOptIn.Level.ERROR
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class TestOnly
