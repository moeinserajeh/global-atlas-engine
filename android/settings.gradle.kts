pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "GlobalAtlas"
include(
    ":app",
    ":core:common", ":core:model", ":core:designsystem", ":core:network",
    ":core:database", ":core:datastore", ":core:map", ":core:testing",
    ":feature:atlas", ":feature:country", ":feature:region", ":feature:search",
    ":feature:downloads"
)
