suite = {
    "mxversion": "5.154.0",
    "name": "graalsqueak",
    "versionConflictResolution": "latest",

    "imports": {
        "suites": [{
            "name": "truffle",
            "subdir": True,
            "version": "b76cd1450aa1085c6b82d3d7f1563fa0c26d6b03",
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
                "truffle:TRUFFLE_API",
                "truffle:TRUFFLE_DSL_PROCESSOR",
                "sdk:GRAAL_SDK",
                "sdk:LAUNCHER_COMMON",
                # "tools:CHROMEINSPECTOR",
            ],
            "checkstyle": "de.hpi.swa.graal.squeak",
            "checkstyleVersion": "8.8",
            "jacoco": "include",
            "javaCompliance": "1.8",
            "annotationProcessors": ["truffle:TRUFFLE_DSL_PROCESSOR"],
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
                "truffle:TRUFFLE_API",
                "truffle:TRUFFLE_DSL_PROCESSOR",
                "sdk:LAUNCHER_COMMON",
            ],
            "exclude": ["mx:JUNIT"],
            "sourcesPath": "graalsqueak.src.zip",
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
