suite = {
    "mxversion": "5.180.2",
    "name": "graalsqueak",
    "versionConflictResolution": "latest",

    "imports": {
        "suites": [{
            "name": "truffle",
            "subdir": True,
            "version": "8bd02196b0bad10d05f6eb371a27819b07435e1a",
            "urls": [{
                "url": "https://github.com/oracle/graal",
                "kind": "git"
            }],
        }],
    },

    "libraries": {},

    "projects": {
        "de.hpi.swa.graal.squeak": {
            "subDir": "src",
            "sourceDirs": ["src"],
            "dependencies": [
                "graalsqueak:GRAALSQUEAK-CONFIG",
                "truffle:TRUFFLE_API",
                "truffle:TRUFFLE_DSL_PROCESSOR",
            ],
            "checkstyle": "de.hpi.swa.graal.squeak",
            "checkstyleVersion": "8.8",
            "jacoco": "include",
            "javaCompliance": "1.8",
            "annotationProcessors": ["truffle:TRUFFLE_DSL_PROCESSOR"],
            "workingSets": "GraalSqueak",
        },
        "de.hpi.swa.graal.squeak.launcher": {
            "subDir": "src",
            "sourceDirs": ["src"],
            "dependencies": [
                "graalsqueak:GRAALSQUEAK-CONFIG",
                "sdk:GRAAL_SDK",
                "sdk:LAUNCHER_COMMON",
                "truffle:TRUFFLE_API",
            ],
            "checkstyle": "de.hpi.swa.graal.squeak",
            "checkstyleVersion": "8.8",
            "jacoco": "include",
            "javaCompliance": "1.8",
            "workingSets": "GraalSqueak",
        },
        "de.hpi.swa.graal.squeak.config": {
            "subDir": "src",
            "sourceDirs": ["src"],
            "dependencies": [
                "truffle:TRUFFLE_API",
            ],
            "checkstyle": "de.hpi.swa.graal.squeak",
            "checkstyleVersion": "8.8",
            "jacoco": "include",
            "javaCompliance": "1.8",
            "workingSets": "GraalSqueak",
        },
        "de.hpi.swa.graal.squeak.test": {
            "subDir": "src",
            "sourceDirs": ["src"],
            "dependencies": ["de.hpi.swa.graal.squeak", "mx:JUNIT"],
            "checkstyle": "de.hpi.swa.graal.squeak",
            "jacoco": "include",
            "javaCompliance": "1.8",
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
                "GRAALSQUEAK-CONFIG",
                "truffle:TRUFFLE_API",
                "truffle:TRUFFLE_DSL_PROCESSOR",
            ],
            "exclude": ["mx:JUNIT"],
            "sourcesPath": "graalsqueak.src.zip",
        },

        "GRAALSQUEAK-CONFIG": {
            "dependencies": [
                "de.hpi.swa.graal.squeak.config",
            ],
            "distDependencies": [
                "truffle:TRUFFLE_API",
            ],
            "path": "graalsqueak-config.jar",
            "sourcesPath": "graalsqueak-config.src.zip",
        },

        "GRAALSQUEAK-LAUNCHER": {
            "path": "graalsqueak-launcher.jar",
            "dependencies": [
                "de.hpi.swa.graal.squeak.launcher",
            ],
            "distDependencies": [
                "GRAALSQUEAK-CONFIG",
                "sdk:GRAAL_SDK",
                "sdk:LAUNCHER_COMMON",
            ],
            "sourcesPath": "graalsqueak-launcher.src.zip",
        },

        "GRAALSQUEAK_GRAALVM_SUPPORT": {
            "native": True,
            "platformDependent": True,
            "description" : "GraalSqueak support distribution for the GraalVM",
            "layout": {
                "./": [
                    "file:mx.graalsqueak/native-image.properties",
                ],
            }
        },

        "GRAALSQUEAK_TEST": {
            "path": "graalsqueak_test.jar",
            "javaCompliance": "1.8",
            "dependencies": [
                "de.hpi.swa.graal.squeak.test",
            ],
            "exclude": ["mx:JUNIT"],
            "distDependencies": ["GRAALSQUEAK"],
        },
    },
}
