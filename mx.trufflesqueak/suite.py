#
# Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
# Copyright (c) 2021-2022 Oracle and/or its affiliates
#
# Licensed under the MIT License.
#

suite = {

    # ==========================================================================
    #  METADATA
    # ==========================================================================
    "name": "trufflesqueak",
    "mxversion": "5.320.4",
    "versionConflictResolution": "latest",

    "version": "22.1.0",
    "trufflesqueak:dependencyMap": {
        "cuis_test_image": "CuisTestImage-6.0-5053.zip",
        "cuis_test_image_tag": "21.3.0",
        "icon": "trufflesqueak-icon.png",
        "icon_tag": "21.0.0.1",
        "test_image": "TruffleSqueakTestImage-6.0alpha-20228b-64bit.zip",
        "test_image_tag": "21.1.0",
    },

    "release": False,
    "groupId": "de.hpi.swa.trufflesqueak",
    "url": "https://github.com/hpi-swa/trufflesqueak",

    "developer": {
        "name": "Fabio Niephaus and contributors",
        "email": "code+trufflesqueak@fniephaus.com",
        "organization": "Software Architecture Group, HPI, Potsdam, Germany",
        "organizationUrl": "https://www.hpi.uni-potsdam.de/swa/",
    },

    "scm": {
        "url": "https://github.com/hpi-swa/trufflesqueak/",
        "read": "https://github.com/hpi-swa/trufflesqueak.git",
        "write": "git@github.com:hpi-swa/trufflesqueak.git",
    },

    # ==========================================================================
    #  DEPENDENCIES
    # ==========================================================================
    "imports": {
        "suites": [{
            "name": "truffle",
            "subdir": True,
            "version": "5878642f461c6bee933fee68624bfc31dee79f49",
            "urls": [{
                "url": "https://github.com/oracle/graal",
                "kind": "git"
            }],
        }],
    },

    # ==========================================================================
    #  LIBRARIES
    # ==========================================================================
    "libraries": {
        "BOUNCY_CASTLE_CRYPTO_LIB":  {
            "moduleName": "org.bouncycastle",
            "sha1": "bd47ad3bd14b8e82595c7adaa143501e60842a84",
            "maven": {
                "groupId": "org.bouncycastle",
                "artifactId": "bcprov-jdk15on",
                "version": "1.60"
            }
        },
    },

    # ==========================================================================
    #  PROJECTS
    # ==========================================================================
    "projects": {
        "de.hpi.swa.trufflesqueak": {
            "subDir": "src",
            "sourceDirs": ["src"],
            "dependencies": [
                "TRUFFLESQUEAK_SHARED",
                "BOUNCY_CASTLE_CRYPTO_LIB",
                "truffle:TRUFFLE_API",
            ],
            "requires": [
                "java.datatransfer",
                "java.desktop",
                "java.logging",
                "java.management",
                "jdk.unsupported",
            ],
            "requiresConcealed" : {
                "java.base" : ["jdk.internal.module"],
            },
            "checkstyleVersion": "8.36.1",
            "jacoco": "include",
            "javaCompliance": "11+",
            "annotationProcessors": ["truffle:TRUFFLE_DSL_PROCESSOR"],
            "workingSets": "TruffleSqueak",
        },
        "de.hpi.swa.trufflesqueak.launcher": {
            "subDir": "src",
            "sourceDirs": ["src"],
            "dependencies": [
                "TRUFFLESQUEAK_SHARED",
                "sdk:LAUNCHER_COMMON",
            ],
            "requires": [
                "java.desktop",
            ],
            "checkstyle": "de.hpi.swa.trufflesqueak",
            "jacoco": "include",
            "javaCompliance": "11+",
            "workingSets": "TruffleSqueak",
        },
        "de.hpi.swa.trufflesqueak.ffi.native": {
            "subDir": "src",
            "native": "shared_lib",
            "deliverable": "SqueakFFIPrims",
            "os_arch": {
                "windows": {
                    "<others>": {
                        "cflags": []
                    }
                },
                "linux": {
                    "<others>": {
                        "cflags": ["-g", "-Wall", "-Werror", "-D_GNU_SOURCE"],
                        "ldlibs": ["-ldl"],
                    },
                },
                "<others>": {
                    "<others>": {
                        "cflags": ["-g", "-Wall", "-Werror"],
                        "ldlibs": ["-ldl"],
                    },
                },
            },
        },
        "de.hpi.swa.trufflesqueak.shared": {
            "subDir": "src",
            "sourceDirs": ["src"],
            "dependencies": [
                "sdk:GRAAL_SDK",
            ],
            "checkstyle": "de.hpi.swa.trufflesqueak",
            "jacoco": "include",
            "javaCompliance": "11+",
            "workingSets": "TruffleSqueak",
        },
        "de.hpi.swa.trufflesqueak.tck": {
            "subDir": "src",
            "sourceDirs": ["src"],
            "dependencies": [
                "TRUFFLESQUEAK_SHARED",
                "sdk:POLYGLOT_TCK",
                "mx:JUNIT"
            ],
            "checkstyle": "de.hpi.swa.trufflesqueak",
            "javaCompliance": "11+",
            "workingSets": "TruffleSqueak",
            "testProject": True,
        },
        "de.hpi.swa.trufflesqueak.test": {
            "subDir": "src",
            "sourceDirs": ["src"],
            "dependencies": [
                "de.hpi.swa.trufflesqueak",
                "mx:JUNIT"
            ],
            "checkstyle": "de.hpi.swa.trufflesqueak",
            "jacoco": "include",
            "javaCompliance": "11+",
            "workingSets": "TruffleSqueak",
            "testProject": True,
        },
    },

    # ==========================================================================
    #  DISTRIBUTIONS
    # ==========================================================================
    "distributions": {
        "TRUFFLESQUEAK": {
            "description": "TruffleSqueak engine",
            "moduleInfo": {
                "name": "de.hpi.swa.trufflesqueak",
                "exports": [
                    "de.hpi.swa.trufflesqueak to org.graalvm.truffle",
                ],
                "requires": [
                    "jdk.unsupported" # sun.misc.Unsafe
                ],
                "requiresConcealed": {
                    "org.graalvm.truffle": [
                        "com.oracle.truffle.api",
                        "com.oracle.truffle.api.instrumentation",
                    ],
                },
            },
            "dependencies": [
                "de.hpi.swa.trufflesqueak",
            ],
            "distDependencies": [
                "TRUFFLESQUEAK_SHARED",
                "truffle:TRUFFLE_API",
            ],
            "javaProperties": {
                "org.graalvm.language.smalltalk.home": "<path:TRUFFLESQUEAK_HOME>",
            },
        },

        "TRUFFLESQUEAK_HOME": {
            "native": True,
            "platformDependent": True,
            "description": "TruffleSqueak home distribution",
            "layout": {
                "LICENSE_TRUFFLESQUEAK.txt": "file:LICENSE",
                "README_TRUFFLESQUEAK.md": "file:README.md",
                "lib/": "dependency:de.hpi.swa.trufflesqueak.ffi.native",
                "native-image.properties": "file:mx.trufflesqueak/native-image.properties",
            },
            "maven": False,
        },

        "TRUFFLESQUEAK_LAUNCHER": {
            "description": "TruffleSqueak launcher",
            "moduleInfo": {
                "name": "de.hpi.swa.trufflesqueak.launcher",
            },
            "dependencies": [
                "de.hpi.swa.trufflesqueak.launcher",
            ],
            "distDependencies": [
                "TRUFFLESQUEAK_SHARED",
                "sdk:LAUNCHER_COMMON",
            ],
        },

        "TRUFFLESQUEAK_SHARED": {
            "description": "TruffleSqueak shared distribution",
            "moduleInfo": {
                "name": "de.hpi.swa.trufflesqueak.shared",
            },
            "dependencies": [
                "de.hpi.swa.trufflesqueak.shared",
            ],
            "distDependencies": [
                "sdk:GRAAL_SDK",
            ],
        },

        "TRUFFLESQUEAK_TCK": {
            "description": "TruffleSqueak TCK-based interoperability tests",
            "dependencies": [
                "de.hpi.swa.trufflesqueak.tck",
            ],
            "exclude": ["mx:JUNIT", "mx:HAMCREST"],
            "distDependencies": [
                # <workaround>TCK does not load languages correctly in 19.3
                # https://github.com/oracle/graal/commit/d5de10b9cc889104ac4c381fc17e8e92ff9cd186
                "TRUFFLESQUEAK",
                # </workaround>
                "TRUFFLESQUEAK_SHARED",
                "sdk:POLYGLOT_TCK",
            ],
            "testDistribution": True,
        },

        "TRUFFLESQUEAK_TEST": {
            "description": "TruffleSqueak JUnit and SUnit tests",
            "javaCompliance": "11+",
            "dependencies": [
                "de.hpi.swa.trufflesqueak.test",
            ],
            "exclude": ["mx:JUNIT", "mx:HAMCREST"],
            "distDependencies": ["TRUFFLESQUEAK"],
            "testDistribution": True,
        },
    },
}
