rootProject.name = "vyay-platform"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include("services:core")
include("libs:vyay-events")
include("libs:vyay-auth-lib")

project(":services:core").name = "core"
project(":libs:vyay-events").name = "vyay-events"
project(":libs:vyay-auth-lib").name = "vyay-auth-lib"