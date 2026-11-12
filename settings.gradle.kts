pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "RP-Ideas"
include(":cleaner")
include(":equalizer")
include(":radio")
include(":vocalremover")
include(":secretmessage")
project(":secretmessage").projectDir = file("secretmessage/product")
include(":oorarivippu")
include(":smsguard")
include(":voicenotes")
include(":expensetracker")
include(":vasanthi")
include(":podcast")
include(":launcher")

// Isolated, owner-approved store preview; never part of existing releases.
include(":tamilanstore")

// Private manual daily-spend and information vault; isolated preview.
include(":dailyvault")

// Android TV Guard: Hardware truth auditor, TV debloater, and clean home launcher
include(":tvguard")
project(":tvguard").projectDir = file("tvguard/app")

// System App Disabler - RP: pre-installed bloat disabler (disable-only via Shizuku)
include(":appdisabler")
project(":appdisabler").projectDir = file("appdisabler/app")

