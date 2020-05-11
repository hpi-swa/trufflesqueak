#
# Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
#
# Licensed under the MIT License.
#

suite = {

    # ==========================================================================
    #  METADATA
    # ==========================================================================
    "mxversion": "5.253.0",
    "name": "trufflesqueak",
    "versionConflictResolution": "latest",

    "version": "1.0.0-rc9",
    "trufflesqueak:dependencyMap": {
        "graalvm": "20.0.0",
        "image": "TruffleSqueakImage-1.0.0-rc9.zip",
        "image_tag": "1.0.0-rc9",
        "jdk8_update": "242",
        "jdk11": "11.0.6",
        "jdk11_update": "9",
        "jvmci": "jvmci-20.0-b02",
        "test_image": "GraalSqueakTestImage-19329-64bit.zip",
        "test_image_tag": "1.0.0-rc6",
    },

    "release": True,
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
            "version": "022fcbd2479944aeb411f851dc2cb062b1bbb441",
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
            "checkstyleVersion": "8.8",
            "jacoco": "include",
            "javaCompliance": "8+",
            "annotationProcessors": ["truffle:TRUFFLE_DSL_PROCESSOR"],
            "workingSets": "TruffleSqueak",
        },
        "de.hpi.swa.trufflesqueak.launcher": {
            "subDir": "src",
            "sourceDirs": ["src"],
            "dependencies": [
                "TRUFFLESQUEAK_SHARED",
                "sdk:GRAAL_SDK",
                "sdk:LAUNCHER_COMMON",
            ],
            "checkstyle": "de.hpi.swa.trufflesqueak",
            "jacoco": "include",
            "javaCompliance": "8+",
            "workingSets": "TruffleSqueak",
        },
        "de.hpi.swa.trufflesqueak.shared": {
            "subDir": "src",
            "sourceDirs": ["src"],
            "dependencies": [
                "sdk:GRAAL_SDK",
            ],
            "checkstyle": "de.hpi.swa.trufflesqueak",
            "jacoco": "include",
            "javaCompliance": "8+",
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
            "javaCompliance": "8+",
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
            "javaCompliance": "8+",
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
            "dependencies": [
                "de.hpi.swa.trufflesqueak",
            ],
            "distDependencies": [
                "TRUFFLESQUEAK_SHARED",
                "truffle:TRUFFLE_API",
            ],
            "exclude": ["mx:JUNIT"],
        },

        "TRUFFLESQUEAK_SHARED": {
            "dependencies": [
                "de.hpi.swa.trufflesqueak.shared",
            ],
            "distDependencies": [
                "sdk:GRAAL_SDK",
            ],
        },

        "TRUFFLESQUEAK_LAUNCHER": {
            "dependencies": [
                "de.hpi.swa.trufflesqueak.launcher",
            ],
            "distDependencies": [
                "TRUFFLESQUEAK_SHARED",
                "sdk:GRAAL_SDK",
                "sdk:LAUNCHER_COMMON",
            ],
        },

        "TRUFFLESQUEAK_TCK": {
            "description": "TCK-based interoperability tests",
            "dependencies": [
                "de.hpi.swa.trufflesqueak.tck",
            ],
            "exclude": ["mx:JUNIT"],
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

        "TRUFFLESQUEAK_GRAALVM_SUPPORT": {
            "native": True,
            "platformDependent": True,
            "description": "TruffleSqueak support distribution for the GraalVM",
            "layout": {
                "LICENSE_TRUFFLESQUEAK.txt": "file:LICENSE",
                "README_TRUFFLESQUEAK.md": "file:README.md",
                "resources": {
                    "source_type": "file",
                    "path": "src/resources",
                    "exclude": ["src/resources/.gitignore"],
                },
                "native-image.properties": "file:mx.trufflesqueak/native-image.properties",
            },
            "maven": False,
        },

        "TRUFFLESQUEAK_TEST": {
            "description": "JUnit and SUnit tests",
            "javaCompliance": "8+",
            "dependencies": [
                "de.hpi.swa.trufflesqueak.test",
            ],
            "exclude": ["mx:JUNIT"],
            "distDependencies": ["TRUFFLESQUEAK"],
            "testDistribution": True,
        },
    },
}
