suite = {
    "mxversion": "5.128.1",
    "name": "trufflesqueak",
    "versionConflictResolution": "latest",

    "imports": {
        "suites": [{
            "name": "truffle",
            "subdir": True,
            "version": "c17a85c54a07eb6d921a036f325d4616ea5f252a",
            "urls": [{
                "url": "https://github.com/graalvm/graal",
                "kind": "git"
            }],
        }],
    },

    "libraries": {},

    "projects": {
        "de.hpi.swa.trufflesqueak": {
            "subDir": "trufflesqueak",
            "sourceDirs": ["src"],
            "dependencies": [
                "truffle:TRUFFLE_API",
                "truffle:TRUFFLE_DSL_PROCESSOR",
                "sdk:GRAAL_SDK",
                "sdk:LAUNCHER_COMMON",
                # "tools:CHROMEINSPECTOR",
            ],
            "checkstyle": "de.hpi.swa.trufflesqueak",
            "javaCompliance": "1.8",
            "annotationProcessors": ["truffle:TRUFFLE_DSL_PROCESSOR"],
            "workingSets": "TruffleSqueak",
        },
        "de.hpi.swa.trufflesqueak.test": {
            "subDir": "trufflesqueak",
            "sourceDirs": ["src"],
            "dependencies": ["de.hpi.swa.trufflesqueak", "mx:JUNIT"],
            "checkstyle": "de.hpi.swa.trufflesqueak.test",
            "javaCompliance": "1.8",
            "workingSets": "TruffleSqueak",
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
        "TRUFFLESQUEAK": {
            "path": "trufflesqueak.jar",
            "dependencies": [
                "de.hpi.swa.trufflesqueak",
            ],
            "distDependencies": [
                "truffle:TRUFFLE_API",
                "truffle:TRUFFLE_DSL_PROCESSOR",
                "sdk:LAUNCHER_COMMON",
            ],
            "exclude": ["mx:JUNIT"],
            "sourcesPath": "trufflesqueak.src.zip",
        },

        "TRUFFLESQUEAK_TEST": {
            "path": "trufflesqueak_test.jar",
            "javaCompliance": "1.8",
            "dependencies": [
                "de.hpi.swa.trufflesqueak.test",
            ],
            "exclude": ["mx:JUNIT"],
            "distDependencies": ["TRUFFLESQUEAK"],
        },
    },
}
