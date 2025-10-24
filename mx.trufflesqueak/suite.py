#
# Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
# Copyright (c) 2021-2025 Oracle and/or its affiliates
#
# Licensed under the MIT License.
#

suite = {
    # ==========================================================================
    #  METADATA
    # ==========================================================================
    "name": "trufflesqueak",
    "mxversion": "7.54.7",
    "versionConflictResolution": "latest",
    "version": "25.0.0",
    "trufflesqueak:dependencyMap": {
        "cuis_test_image": "CuisTestImage-7.3-7036.zip",
        "cuis_test_image_tag": "24.1.2",
        "test_image": "TruffleSqueakTestImage-6.0-22104-64bit.zip",
        "test_image_tag": "22.3.0",
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
    "licenses": {
        "GFTC": {
            "name": "GraalVM Free Terms and Conditions (GFTC) including License for Early Adopter Versions",
            "url": "https://www.oracle.com/downloads/licenses/graal-free-license.html",
        },
    },
    # ==========================================================================
    #  DEPENDENCIES
    # ==========================================================================
    "imports": {
        "suites": [
            {
                "name": "truffle",
                "subdir": True,
                "version": "graal-25.0.0",
                "urls": [{"url": "https://github.com/oracle/graal", "kind": "git"}],
            }
        ],
    },
    # ==========================================================================
    #  LIBRARIES
    # ==========================================================================
    "libraries": {
        "OSVM_PLUGINS": {
            "baseurl": "https://github.com/hpi-swa/trufflesqueak/releases/download/24.2.2/osvm-plugins-202509110624",
            "os_arch": {
                "linux": {
                    "aarch64": {
                        "urls": ["{baseurl}-linux-aarch64.zip"],
                        "digest": "sha512:cec920765eae6dca8b95e5a24b34333fdaf5b2b9d634ff4cb42fd1a73fec24ab6fafcde7a5c27bc06441fa1c381bd05c8d48f09e3b56d8e61c590fe37d17076f",
                    },
                    "amd64": {
                        "urls": ["{baseurl}-linux-amd64.zip"],
                        "digest": "sha512:ef7e6bcebb0b544908a68439685ac5ddfd60cfff13d0fcf77e0433e3a65d4185fc079907144a593e3667466fdafa2c38fbca94ba52adc7227c4a59829c5aa375",
                    },
                },
                "darwin": {
                    "aarch64": {
                        "urls": ["{baseurl}-darwin-aarch64.zip"],
                        "digest": "sha512:662239e86d9a50344d23b0ff20571059f0218569455855b80f29b1f356af9fa476dfc863d86dfddf896aa48732023bf9c1132c3c9cc365feed9340f1de55d8cb",
                    },
                    "amd64": {
                        "urls": ["{baseurl}-darwin-amd64.zip"],
                        "digest": "sha512:21e1ddf84e34b228af8a352f77d810591143010c1ea0fd95b02c96822cbc1c2bdbf754de08333259cd7101d14f0ce2d19b035c7366176e07634438b968091ed7",
                    },
                },
                "windows": {
                    "amd64": {
                        "urls": ["{baseurl}-windows-amd64.zip"],
                        "digest": "sha512:92dd38360192b6623dac6452791b6caa0c3108a9d31d3453895b5d63dff35e8ec0aa48660fdcf3a0d10c4782101447fb6afda420ff6dd056d506b02df0c0f9f5",
                    },
                },
                "<others>": {"<others>": {"optional": True}},
            },
        },
        "GRAALJS_LANGUAGE": {
            "digest": "sha512:0aa2eac9b820f3d95a0c3ed092be0a9a31c32f0cae8a5ba2539cfb84580dff56fae696d2c87e194bf81867bc72374e12392c49088e9290eb0088b30caf3469d5",
            "maven": {
                "groupId": "org.graalvm.js",
                "artifactId": "js-language",
                "version": "24.2.2",
            },
            "dependencies": ["REGEX_LANGUAGE", "SHADOWED_ICU4J"],
            "useModulePath": True,
            "licence": "UPL",
        },
        "REGEX_LANGUAGE": {
            "digest": "sha512:8a64e0ec61965e6c1a961a806d4242a69d739392a36d74653ac57f20f8b3a948910d8cb501765462449c3396fe10345c14f94e9c659d850c94f6f93507a0bbff",
            "maven": {
                "groupId": "org.graalvm.regex",
                "artifactId": "regex",
                "version": "24.2.2",
            },
            "dependencies": ["SHADOWED_ICU4J"],
            "useModulePath": True,
            "licence": "UPL",
        },
        "SHADOWED_ICU4J": {
            "digest": "sha512:739e16b2ad0ac0d3a5bbab956c22bc9b316478d3be601059bc233bb996eff4a967972646d9c5f66423a8fa8cd0cf0e5c90635bda6ead25c0213b5b60b83e4b53",
            "maven": {
                "groupId": "org.graalvm.shadowed",
                "artifactId": "icu4j",
                "version": "24.2.2",
            },
            "dependencies": ["SHADOWED_XZ"],
            "useModulePath": True,
        },
        "SHADOWED_XZ": {
            "digest": "sha512:c38d48ef37f3264b08a898f5f884704530019ce8b89a4cb25b0b7c6229f4ccf603107c4a44be9cc3311641f9d35c261f24c2fed7dba11fd6ea0dbb4517e44b66",
            "maven": {
                "groupId": "org.graalvm.shadowed",
                "artifactId": "xz",
                "version": "24.2.2",
            },
            "useModulePath": True,
        },
        "TRUFFLE-ENTERPRISE": {
            "digest": "sha512:8470d6f2d33f4f6b33263343125e03d34ee1716d1c17ca0e7cea868d7b008c31f93c323cb3b982bf437f48ea7cd054394a1ee95660dba3ae734e9989c92cf7ba",
            "maven": {
                "groupId": "org.graalvm.truffle",
                "artifactId": "truffle-enterprise",
                "version": "24.2.2",
            },
            "useModulePath": True,
            "licence": "GFTC",
        },
        "SDK-NATIVEBRIDGE": {
            "digest": "sha512:eb433421d0be6b62e54261ee9aecb3a11b5e4bfd3c3cfd2de41e2266f81788cfa33e0d72ae7c507701936a8b3ff259e6e295537581b543115ec0d437af3072f1",
            "maven": {
                "groupId": "org.graalvm.sdk",
                "artifactId": "nativebridge",
                "version": "24.2.2",
            },
            "useModulePath": True,
        },
    },
    # ==========================================================================
    #  PROJECTS
    # ==========================================================================
    "projects": {
        "de.hpi.swa.trufflesqueak": {
            "subDir": "src",
            "sourceDirs": [
                "src",
                "resources",
            ],
            "dependencies": [
                "TRUFFLESQUEAK_SHARED",
                "truffle:TRUFFLE_API",
                "truffle:TRUFFLE_NFI",
            ],
            "requires": [
                "java.datatransfer",
                "java.desktop",
                "java.logging",
                "java.management",
                "jdk.management",
                "jdk.unsupported",
            ],
            "requiresConcealed": {
                "java.base": ["jdk.internal.module"],
            },
            "checkstyleVersion": "10.7.0",
            "jacoco": "include",
            "javaCompliance": "21+",
            "annotationProcessors": ["truffle:TRUFFLE_DSL_PROCESSOR"],
            "workingSets": "TruffleSqueak",
        },
        "de.hpi.swa.trufflesqueak.launcher": {
            "subDir": "src",
            "sourceDirs": ["src"],
            "dependencies": [
                "TRUFFLESQUEAK_SHARED",
                "sdk:LAUNCHER_COMMON",
                "sdk:MAVEN_DOWNLOADER",
            ],
            "requires": [
                "java.desktop",
            ],
            "checkstyle": "de.hpi.swa.trufflesqueak",
            "jacoco": "include",
            "javaCompliance": "21+",
            "workingSets": "TruffleSqueak",
        },
        "de.hpi.swa.trufflesqueak.ffi.native": {
            "subDir": "src",
            "class": "CMakeNinjaProject",
            "vpath": True,
            "ninja_targets": ["all"],
            "os_arch": {
                "<others>": {
                    "<others>": {
                        "cmakeConfig": {},
                        "results": [
                            "<lib:SqueakFFIPrims>",
                            "<lib:InterpreterProxy>",
                        ],
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
            "javaCompliance": "21+",
            "workingSets": "TruffleSqueak",
        },
        "de.hpi.swa.trufflesqueak.tck": {
            "subDir": "src",
            "sourceDirs": ["src"],
            "dependencies": ["TRUFFLESQUEAK_SHARED", "sdk:POLYGLOT_TCK", "mx:JUNIT"],
            "checkstyle": "de.hpi.swa.trufflesqueak",
            "javaCompliance": "21+",
            "workingSets": "TruffleSqueak",
            "testProject": True,
        },
        "de.hpi.swa.trufflesqueak.test": {
            "subDir": "src",
            "sourceDirs": ["src"],
            "dependencies": [
                "de.hpi.swa.trufflesqueak",
                "mx:JUNIT",
                "sdk:MAVEN_DOWNLOADER",
                "GRAALJS_LANGUAGE",
                "REGEX_LANGUAGE",
            ],
            "checkstyle": "de.hpi.swa.trufflesqueak",
            "jacoco": "include",
            "javaCompliance": "21+",
            "workingSets": "TruffleSqueak",
            "testProject": True,
        },
        "trufflesqueak_thin_launcher": {
            "class": "ThinLauncherProject",
            "mainClass": "de.hpi.swa.trufflesqueak.launcher.TruffleSqueakLauncher",
            "jar_distributions": ["trufflesqueak:TRUFFLESQUEAK_LAUNCHER"],
            "option_vars": [
                "TRUFFLESQUEAK_OPTIONS",
            ],
            "relative_home_paths": {
                "smalltalk": "..",
            },
            "relative_jre_path": "../jvm",
            "relative_module_path": "../modules",
            "relative_extracted_lib_paths": {
                "truffle.attach.library": "../jvmlibs/<lib:truffleattach>",
                "truffle.nfi.library": "../jvmlibs/<lib:trufflenfi>",
            },
            "liblang_relpath": "../lib/<lib:smalltalkvm>",
            "default_vm_args": [
                "--vm.-add-exports=java.base/jdk.internal.module=de.hpi.swa.trufflesqueak",
            ],
        },
        "libsmalltalkvm": {
            "class": "LanguageLibraryProject",
            "dependencies": [
                "TRUFFLESQUEAK_STANDALONE_DEPENDENCIES",
            ],
            "buildDependencies": [
                "TRUFFLESQUEAK_STANDALONE_COMMON",
            ],
            "build_args": [
                # "-Dpolyglot.image-build-time.PreinitializeContexts=smalltalk",
                # Configure launcher
                "-Dorg.graalvm.launcher.class=de.hpi.swa.trufflesqueak.launcher.TruffleSqueakLauncher",
                "-H:+IncludeNodeSourcePositions",  # for improved stack traces on deopts
                "-H:+DetectUserDirectoriesInImageHeap",
            ],
            "dynamicBuildArgs": "libsmalltalkvm_build_args",
        },
    },
    # ==========================================================================
    #  DISTRIBUTIONS
    # ==========================================================================
    "distributions": {
        "TRUFFLESQUEAK": {
            "description": "TruffleSqueak virtual machine",
            "moduleInfo": {
                "name": "de.hpi.swa.trufflesqueak",
                "exports": [
                    "de.hpi.swa.trufflesqueak to org.graalvm.truffle",
                    "de.hpi.swa.trufflesqueak*",  # allow reflection
                ],
                "requires": [
                    "jdk.unsupported",  # sun.misc.Unsafe
                ],
            },
            "useModulePath": True,
            "dependencies": [
                "de.hpi.swa.trufflesqueak",
            ],
            "distDependencies": [
                "TRUFFLESQUEAK_SHARED",
                "truffle:TRUFFLE_API",
                "truffle:TRUFFLE_NFI",
                "truffle:TRUFFLE_NFI_LIBFFI",  # runtime dependency
                "truffle:TRUFFLE_NFI_PANAMA",  # runtime dependency
            ],
            "javaProperties": {
                "org.graalvm.language.smalltalk.home": "<path:TRUFFLESQUEAK_HOME>",
            },
            "maven": {
                "artifactId": "smalltalk-language",
                "groupId": "de.hpi.swa.trufflesqueak",
                "tag": ["default", "public"],
            },
            "noMavenJavadoc": True,
            "license": ["MIT"],
        },
        "TRUFFLE_ENTERPRISE_PLACEHOLDER": {
            "maven": {
                "groupId": "org.graalvm.truffle",
                "artifactId": "truffle-enterprise",
            },
            "testDistribution": True,  # ensure it does not get 'maven-deploy'ed
            "noMavenJavadoc": True,
            "license": ["GFTC"],
        },
        "SMALLTALK": {
            "type": "pom",
            "runtimeDependencies": [
                "TRUFFLESQUEAK",
                "TRUFFLE_ENTERPRISE_PLACEHOLDER",
            ],
            "description": "TruffleSqueak virtual machine for Oracle GraalVM",
            "maven": {
                "groupId": "de.hpi.swa.trufflesqueak",
                "artifactId": "smalltalk",
                "tag": ["default", "public"],
            },
            "license": ["MIT", "GFTC"],
        },
        "SMALLTALK_COMMUNITY": {
            "type": "pom",
            "runtimeDependencies": [
                "TRUFFLESQUEAK",
                "truffle:TRUFFLE_RUNTIME",
            ],
            "description": "TruffleSqueak virtual machine for GraalVM CE",
            "maven": {
                "groupId": "de.hpi.swa.trufflesqueak",
                "artifactId": "smalltalk-community",
                "tag": ["default", "public"],
            },
            "license": ["MIT"],
        },
        "TRUFFLESQUEAK_HOME": {
            "native": True,
            "platformDependent": True,
            "description": "TruffleSqueak home distribution",
            "layout": {
                "LICENSE_TRUFFLESQUEAK.txt": "file:LICENSE",
                "README_TRUFFLESQUEAK.md": "file:README.md",
                "lib/": [
                    "dependency:de.hpi.swa.trufflesqueak.ffi.native/*",
                    {
                        "source_type": "extracted-dependency",
                        "dependency": "OSVM_PLUGINS",
                        "path": "*",
                    },
                ],
            },
            "maven": False,
        },
        "TRUFFLESQUEAK_LAUNCHER": {
            "description": "TruffleSqueak launcher",
            "moduleInfo": {
                "name": "de.hpi.swa.trufflesqueak.launcher",
                "exports": [
                    "de.hpi.swa.trufflesqueak.launcher to org.graalvm.launcher",
                ],
            },
            "useModulePath": True,
            "dependencies": [
                "de.hpi.swa.trufflesqueak.launcher",
            ],
            "distDependencies": [
                "TRUFFLESQUEAK_SHARED",
                "sdk:LAUNCHER_COMMON",
                "sdk:MAVEN_DOWNLOADER",
            ],
            "maven": {
                "groupId": "de.hpi.swa.trufflesqueak",
                "artifactId": "smalltalk-launcher",
                "tag": ["default", "public"],
            },
            "noMavenJavadoc": True,
            "license": ["MIT"],
        },
        "TRUFFLESQUEAK_SHARED": {
            "description": "TruffleSqueak shared distribution",
            "moduleInfo": {
                "name": "de.hpi.swa.trufflesqueak.shared",
                "exports": [
                    "de.hpi.swa.trufflesqueak.shared",
                ],
            },
            "dependencies": [
                "de.hpi.swa.trufflesqueak.shared",
            ],
            "distDependencies": [
                "sdk:GRAAL_SDK",
            ],
            "maven": {
                "groupId": "de.hpi.swa.trufflesqueak",
                "artifactId": "trufflesqueak-shared",
                "tag": ["default", "public"],
            },
            "noMavenJavadoc": True,
            "license": ["MIT"],
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
            "maven": False,
        },
        "TRUFFLESQUEAK_TEST": {
            "description": "TruffleSqueak JUnit and SUnit tests",
            "moduleInfo": {
                "name": "de.hpi.swa.trufflesqueak.test",
                "exports": [
                    # Export everything to junit and dependent test distributions.
                    "de.hpi.swa.trufflesqueak.test*",
                ],
            },
            "useModulePath": True,
            "javaCompliance": "21+",
            "dependencies": [
                "de.hpi.swa.trufflesqueak.test",
            ],
            "exclude": ["mx:JUNIT", "mx:HAMCREST"],
            "distDependencies": [
                "TRUFFLESQUEAK",
                "TRUFFLESQUEAK_SHARED",
                "sdk:MAVEN_DOWNLOADER",
                "truffle:TRUFFLE_API",
            ],
            "testDistribution": True,
            "maven": False,
        },
        "TRUFFLESQUEAK_GRAALVM_SUPPORT_PLATFORM_SPECIFIC": {
            "description": "Platform-specific TruffleSqueak home files",
            "fileListPurpose": "native-image-resources",
            "native": True,
            "platformDependent": True,
            "layout": {
                "lib/": [
                    "dependency:de.hpi.swa.trufflesqueak.ffi.native/*",
                    {
                        "source_type": "extracted-dependency",
                        "dependency": "OSVM_PLUGINS",
                        "path": "*",
                    },
                ],
            },
            "license": ["MIT"],
            "maven": False,
        },
        "TRUFFLESQUEAK_GRAALVM_SUPPORT_NO_NI_RESOURCES": {
            "description": "TruffleSqueak support distribution for the GraalVM, the contents is not included as native image resources.",
            "native": True,
            "platformDependent": True,
            "layout": {
                "./": [
                    "file:LICENSE",
                    "file:README.md",
                ],
            },
            "maven": False,
        },
        "TRUFFLESQUEAK_STANDALONE_DEPENDENCIES": {
            "description": "TruffleSqueak standalone dependencies",
            "class": "DynamicPOMDistribution",
            "distDependencies": [
                "trufflesqueak:TRUFFLESQUEAK_LAUNCHER",
                "trufflesqueak:TRUFFLESQUEAK",
                "sdk:TOOLS_FOR_STANDALONE",
            ],
            "dynamicDistDependencies": "trufflesqueak_standalone_deps",
            "maven": False,
        },
        "TRUFFLESQUEAK_STANDALONE_COMMON": {
            "description": "Common layout for Native and JVM standalones",
            "type": "dir",
            "platformDependent": True,
            "platforms": "local",
            "layout": {
                "./": [
                    "extracted-dependency:TRUFFLESQUEAK_GRAALVM_SUPPORT_PLATFORM_SPECIFIC",
                    "extracted-dependency:TRUFFLESQUEAK_GRAALVM_SUPPORT_NO_NI_RESOURCES",
                ],
                "bin/<exe:trufflesqueak>": "dependency:trufflesqueak_thin_launcher",
                "bin/<exe:trufflesqueak-polyglot-get>": "dependency:trufflesqueak_thin_launcher",
                "release": "dependency:sdk:STANDALONE_JAVA_HOME/release",
            },
        },
        "TRUFFLSQUEAK_NATIVE_STANDALONE_JDK_LIBRARIES": {
            "description": "JDK libraries for TruffleSqueak Native standalone.",
            "maven": False,
            "native": True,
            "platformDependent": True,
            "platforms": "local",
            "type": "dir",
            "os_arch": {
                "linux": {
                    "<others>": {
                        "layout": {
                            "lib/": "dependency:libsmalltalkvm/jdk_libraries/*",
                        },
                    },
                },
                "windows": {
                    "<others>": {
                        "layout": {
                            # JDK libraries need to be in bin/ on Windows
                            "bin/": "dependency:libsmalltalkvm/jdk_libraries/*",
                        },
                    },
                },
                # AWT not supported on macOS yet, so no JDK libraries yet
                "<others>": {"<others>": {"optional": True}},
            },
        },
        "TRUFFLESQUEAK_NATIVE_STANDALONE": {
            "description": "TruffleSqueak Native standalone",
            "type": "dir",
            "platformDependent": True,
            "platforms": "local",
            "layout": {
                "./": [
                    "dependency:TRUFFLESQUEAK_STANDALONE_COMMON/*",
                    "dependency:TRUFFLSQUEAK_NATIVE_STANDALONE_JDK_LIBRARIES/*",
                ],
                "lib/": "dependency:libsmalltalkvm",
            },
        },
        "TRUFFLESQUEAK_JVM_STANDALONE": {
            "description": "TruffleSqueak JVM standalone",
            "type": "dir",
            "platformDependent": True,
            "platforms": "local",
            "layout": {
                "./": [
                    "dependency:TRUFFLESQUEAK_STANDALONE_COMMON/*",
                ],
                "jvm/": {
                    "source_type": "dependency",
                    "dependency": "sdk:STANDALONE_JAVA_HOME",
                    "path": "*",
                    "exclude": [
                        # related to Native Image
                        "bin/native-image*",
                        "lib/static",
                        "lib/svm",
                        "lib/<lib:native-image-agent>",
                        "lib/<lib:native-image-diagnostics-agent>",
                        # unnecessary and big
                        "lib/src.zip",
                        # "jmods",
                    ],
                },
                "jvmlibs/": [
                    "extracted-dependency:truffle:TRUFFLE_ATTACH_GRAALVM_SUPPORT",
                    "extracted-dependency:truffle:TRUFFLE_NFI_NATIVE_GRAALVM_SUPPORT",
                ],
                "modules/": [
                    "classpath-dependencies:TRUFFLESQUEAK_STANDALONE_DEPENDENCIES",
                ],
            },
        },
    },
}
