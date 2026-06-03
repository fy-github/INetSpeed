pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "INetSpeed"

include(":app")
include(":core:designsystem")
include(":core:data")
include(":core:iperf3")
include(":core:network-discovery")
include(":core:privacy")
include(":core:sync")
include(":core:ads")
include(":feature:speedtest")
include(":feature:servers")
include(":feature:tools")
include(":feature:history")
include(":feature:report")
include(":feature:settings")
