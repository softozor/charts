package integration

import common.templates.NexusDockerLogin
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.DslContext
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs

class Integration(
    dockerToolsTag: String
) : BuildType({
    templates(
        NexusDockerLogin
    )

    id("Integration")
    name = "Integration"

    vcs {
        root(DslContext.settingsRoot)
        cleanCheckout = true
        branchFilter = """
            +:*
        """.trimIndent()
    }

    triggers {
        vcs {
            branchFilter = """
                +:*
                -:v*
            """.trimIndent()
        }
    }

    steps {
        script {
            name = "Push Chart 'hasura'"
            scriptContent = """
                #! /bin/sh
                set -e
                helm dependency update hasura
                CHART_PACKAGE=${'$'}(helm package --app-version %hasura.version% --version %hasura.chart.version% hasura | cut -d":" -f2 | tr -d '[:space:]')
                curl -is -u "%system.package-manager.deployer.username%:%system.package-manager.deployer.password%" "https://%system.package-manager.hostname%/repository/helm-hosted/" --upload-file "${'$'}CHART_PACKAGE"
            """.trimIndent()
            workingDir = "./softozor"
            dockerPull = true
            dockerImage = "%system.docker-registry.group%/docker-tools/devspace:$dockerToolsTag"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
        }
    }

    features {
        perfmon {
        }
    }

    params {
        param("teamcity.vcsTrigger.runBuildInNewEmptyBranch", "true")
    }
})