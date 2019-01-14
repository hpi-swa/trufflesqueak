suite = {
    "mxversion": "5.195.0",
    "name": "graalsqueak",
    "versionConflictResolution": "latest",

    "imports": {
        "suites": [{
            "name": "truffle",
            "subdir": True,
            "version": "7a44af07d4dc34684c72648859196ea6c5e8f027",
            "urls": [{
                "url": "https://github.com/oracle/graal",
                "kind": "git"
            }],
        }],
    },

    "libraries": {
        "BOUNCY_CASTLE_CRYPTO_LIB":  {
            "sha1": "bd47ad3bd14b8e82595c7adaa143501e60842a84",
            "maven": {
                "groupId": "org.bouncycastle",
                "artifactId": "bcprov-jdk15on",
                "version": "1.60"
            }
        },
    },

    "projects": {
        "de.hpi.swa.graal.squeak": {
            "subDir": "src",
            "sourceDirs": ["src"],
            "dependencies": [
                "BOUNCY_CASTLE_CRYPTO_LIB",
                "graalsqueak:GRAALSQUEAK-SHARED",
                "truffle:TRUFFLE_API",
                "truffle:TRUFFLE_DSL_PROCESSOR",
            ],
            "checkstyle": "de.hpi.swa.graal.squeak",
            "checkstyleVersion": "8.8",
            "jacoco": "include",
            "javaCompliance": "8+",
            "annotationProcessors": ["truffle:TRUFFLE_DSL_PROCESSOR"],
            "workingSets": "GraalSqueak",
        },
        "de.hpi.swa.graal.squeak.launcher": {
            "subDir": "src",
            "sourceDirs": ["src"],
            "dependencies": [
                "graalsqueak:GRAALSQUEAK-SHARED",
                "sdk:GRAAL_SDK",
                "sdk:LAUNCHER_COMMON",
                "truffle:TRUFFLE_API",
            ],
            "checkstyle": "de.hpi.swa.graal.squeak",
            "checkstyleVersion": "8.8",
            "jacoco": "include",
            "javaCompliance": "8+",
            "workingSets": "GraalSqueak",
        },
        "de.hpi.swa.graal.squeak.shared": {
            "subDir": "src",
            "sourceDirs": ["src"],
            "dependencies": [
                "truffle:TRUFFLE_API",
            ],
            "checkstyle": "de.hpi.swa.graal.squeak",
            "checkstyleVersion": "8.8",
            "jacoco": "include",
            "javaCompliance": "8+",
            "workingSets": "GraalSqueak",
        },
        "de.hpi.swa.graal.squeak.test": {
            "subDir": "src",
            "sourceDirs": ["src"],
            "dependencies": ["de.hpi.swa.graal.squeak", "mx:JUNIT"],
            "checkstyle": "de.hpi.swa.graal.squeak",
            "jacoco": "include",
            "javaCompliance": "8+",
            "workingSets": "GraalSqueak",
        },
    },

    "defaultLicense": "BSD-3-Clause",
    "licenses": {
        "BSD-3-Clause": {
            "name": "The 3-Clause BSD License",
            "url": "http://opensource.org/licenses/BSD-3-Clause",
        },
    },

    "distributions": {
        "GRAALSQUEAK": {
            "path": "graalsqueak.jar",
            "dependencies": [
                "de.hpi.swa.graal.squeak",
            ],
            "distDependencies": [
                "GRAALSQUEAK-SHARED",
                "truffle:TRUFFLE_API",
                "truffle:TRUFFLE_DSL_PROCESSOR",
            ],
            "exclude": ["mx:JUNIT"],
            "sourcesPath": "graalsqueak.src.zip",
        },

        "GRAALSQUEAK-SHARED": {
            "dependencies": [
                "de.hpi.swa.graal.squeak.shared",
            ],
            "distDependencies": [
                "truffle:TRUFFLE_API",
            ],
            "path": "graalsqueak-shared.jar",
            "sourcesPath": "graalsqueak-shared.src.zip",
        },

        "GRAALSQUEAK-LAUNCHER": {
            "path": "graalsqueak-launcher.jar",
            "dependencies": [
                "de.hpi.swa.graal.squeak.launcher",
            ],
            "distDependencies": [
                "GRAALSQUEAK-SHARED",
                "sdk:GRAAL_SDK",
                "sdk:LAUNCHER_COMMON",
            ],
            "sourcesPath": "graalsqueak-launcher.src.zip",
        },

        "GRAALSQUEAK_GRAALVM_SUPPORT": {
            "native": True,
            "platformDependent": True,
            "description": "GraalSqueak support distribution for the GraalVM",
            "layout": {
                "./": [
                    "file:mx.graalsqueak/native-image.properties",
                ],
            }
        },

        "GRAALSQUEAK_TEST": {
            "path": "graalsqueak_test.jar",
            "javaCompliance": "8+",
            "dependencies": [
                "de.hpi.swa.graal.squeak.test",
            ],
            "exclude": ["mx:JUNIT"],
            "distDependencies": ["GRAALSQUEAK"],
        },
    },
}
