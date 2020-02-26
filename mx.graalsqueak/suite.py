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
    "name": "graalsqueak",
    "versionConflictResolution": "latest",

    "version": "1.0.0-rc7",
    "graalsqueak:dependencyMap": {
        "graalvm": "19.3.0",
        "image": "GraalSqueakImage-1.0.0-rc7.zip",
        "image_tag": "1.0.0-rc7",
        "jdk8_update": "232",
        "jvmci": "jvmci-19.3-b05",
        "test_image": "GraalSqueakTestImage-19329-64bit.zip",
        "test_image_tag": "1.0.0-rc6",
    },

    "release": False,
    "groupId": "de.hpi.swa.graal.squeak",
    "url": "https://github.com/hpi-swa/graalsqueak",

    "developer": {
        "name": "Fabio Niephaus and contributors",
        "email": "code+graalsqueak@fniephaus.com",
        "organization": "Software Architecture Group, HPI, Potsdam, Germany",
        "organizationUrl": "https://www.hpi.uni-potsdam.de/swa/",
    },

    "scm": {
        "url": "https://github.com/hpi-swa/graalsqueak/",
        "read": "https://github.com/hpi-swa/graalsqueak.git",
        "write": "git@github.com:hpi-swa/graalsqueak.git",
    },

    # ==========================================================================
    #  DEPENDENCIES
    # ==========================================================================
    "imports": {
        "suites": [{
            "name": "truffle",
            "subdir": True,
            "version": "72d10ce1cd95b094d371e308e922a5960d8c35a8",
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
        "de.hpi.swa.graal.squeak": {
            "subDir": "src",
            "sourceDirs": ["src"],
            "dependencies": [
                "GRAALSQUEAK_SHARED",
                "BOUNCY_CASTLE_CRYPTO_LIB",
                "truffle:TRUFFLE_API",
            ],
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
                "GRAALSQUEAK_SHARED",
                "sdk:GRAAL_SDK",
                "sdk:LAUNCHER_COMMON",
            ],
            "checkstyle": "de.hpi.swa.graal.squeak",
            "jacoco": "include",
            "javaCompliance": "8+",
            "workingSets": "GraalSqueak",
        },
        "de.hpi.swa.graal.squeak.shared": {
            "subDir": "src",
            "sourceDirs": ["src"],
            "dependencies": [
                "sdk:GRAAL_SDK",
            ],
            "checkstyle": "de.hpi.swa.graal.squeak",
            "jacoco": "include",
            "javaCompliance": "8+",
            "workingSets": "GraalSqueak",
        },
        "de.hpi.swa.graal.squeak.tck": {
            "subDir": "src",
            "sourceDirs": ["src"],
            "dependencies": [
                "GRAALSQUEAK_SHARED",
                "sdk:POLYGLOT_TCK",
                "mx:JUNIT"
            ],
            "checkstyle": "de.hpi.swa.graal.squeak",
            "javaCompliance": "8+",
            "workingSets": "GraalSqueak",
            "testProject": True,
        },
        "de.hpi.swa.graal.squeak.test": {
            "subDir": "src",
            "sourceDirs": ["src"],
            "dependencies": [
                "de.hpi.swa.graal.squeak",
                "mx:JUNIT"
            ],
            "checkstyle": "de.hpi.swa.graal.squeak",
            "jacoco": "include",
            "javaCompliance": "8+",
            "workingSets": "GraalSqueak",
            "testProject": True,
        },
    },

    # ==========================================================================
    #  DISTRIBUTIONS
    # ==========================================================================
    "distributions": {
        "GRAALSQUEAK": {
            "description": "GraalSqueak engine",
            "dependencies": [
                "de.hpi.swa.graal.squeak",
            ],
            "distDependencies": [
                "GRAALSQUEAK_SHARED",
                "truffle:TRUFFLE_API",
            ],
            "exclude": ["mx:JUNIT"],
        },

        "GRAALSQUEAK_SHARED": {
            "dependencies": [
                "de.hpi.swa.graal.squeak.shared",
            ],
            "distDependencies": [
                "sdk:GRAAL_SDK",
            ],
        },

        "GRAALSQUEAK_LAUNCHER": {
            "dependencies": [
                "de.hpi.swa.graal.squeak.launcher",
            ],
            "distDependencies": [
                "GRAALSQUEAK_SHARED",
                "sdk:GRAAL_SDK",
                "sdk:LAUNCHER_COMMON",
            ],
        },

        "GRAALSQUEAK_TCK": {
            "description": "TCK-based interoperability tests",
            "dependencies": [
                "de.hpi.swa.graal.squeak.tck",
            ],
            "exclude": ["mx:JUNIT"],
            "distDependencies": [
                # <workaround>TCK does not load languages correctly in 19.3
                # https://github.com/oracle/graal/commit/d5de10b9cc889104ac4c381fc17e8e92ff9cd186
                "GRAALSQUEAK",
                # </workaround>
                "GRAALSQUEAK_SHARED",
                "sdk:POLYGLOT_TCK",
            ],
            "testDistribution": True,
        },

        "GRAALSQUEAK_GRAALVM_SUPPORT": {
            "native": True,
            "platformDependent": True,
            "description": "GraalSqueak support distribution for the GraalVM",
            "layout": {
                "LICENSE_GRAALSQUEAK.txt": "file:LICENSE",
                "README_GRAALSQUEAK.md": "file:README.md",
                "resources": {
                    "source_type": "file",
                    "path": "src/resources",
                    "exclude": ["src/resources/.gitignore"],
                },
                "native-image.properties": "file:mx.graalsqueak/native-image.properties",
            },
            "maven": False,
        },

        "GRAALSQUEAK_TEST": {
            "description": "JUnit and SUnit tests",
            "javaCompliance": "8+",
            "dependencies": [
                "de.hpi.swa.graal.squeak.test",
            ],
            "exclude": ["mx:JUNIT"],
            "distDependencies": ["GRAALSQUEAK"],
            "testDistribution": True,
        },
    },
}
