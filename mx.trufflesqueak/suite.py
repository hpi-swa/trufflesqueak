#
# Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
# Copyright (c) 2021-2026 Oracle and/or its affiliates
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
    "version": "25.0.2",
    "trufflesqueak:dependencyMap": {
        "cuis_7_3_test_image": "CuisTestImage-7.3-7036.zip",
        "cuis_7_3_test_image_tag": "24.1.2",
        "cuis_7_5_test_image": "CuisTestImage-7.5-7708.zip",
        "cuis_7_5_test_image_tag": "25.0.1",
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
                "version": "graal-25.0.1",
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
            "digest": "sha512:dbd9395b23a08718ec1273aa4f50a8d6e432a2aa79e6839d0fd13c9e16084fbaef3e3f0984a1c6d5a9ffecb595519c742250694c90bceb81e1eb55bc30af745e",
            "maven": {
                "groupId": "org.graalvm.js",
                "artifactId": "js-language",
                "version": "25.0.1",
            },
            "dependencies": ["REGEX_LANGUAGE", "SHADOWED_ICU4J"],
            "useModulePath": True,
            "licence": "UPL",
        },
        "REGEX_LANGUAGE": {
            "digest": "sha512:a215b10c4fd73eff7522a78ab830134ad23176ec0d36a2e620bb2ff9a417994eed5c227c0bc12e22890904ba958b4da44f92f4f78e922fbcd2857abdc168f869",
            "maven": {
                "groupId": "org.graalvm.regex",
                "artifactId": "regex",
                "version": "25.0.1",
            },
            "dependencies": ["SHADOWED_ICU4J"],
            "useModulePath": True,
            "licence": "UPL",
        },
        "SHADOWED_ICU4J": {
            "digest": "sha512:e122e16fe4fc8fa65714fe526f4eb625605a375a11e00edb6bd39352afeeb690c749c92f427135c1f01dc552b9659a28fda7730f01e0306234d8439facc7215a",
            "maven": {
                "groupId": "org.graalvm.shadowed",
                "artifactId": "icu4j",
                "version": "25.0.1",
            },
            "dependencies": ["SHADOWED_XZ"],
            "useModulePath": True,
        },
        "SHADOWED_XZ": {
            "digest": "sha512:b12f38e92164e15afe257b1907ac113f6fd39e2413f208030eb376fb74e9dea5e083cabe2b5f3c3fbf4caeb63889b2b869160228d1a6c0def7f070fb60e5b2c3",
            "maven": {
                "groupId": "org.graalvm.shadowed",
                "artifactId": "xz",
                "version": "25.0.1",
            },
            "useModulePath": True,
        },
        # ==========================================================================
        #  JWM (Java Window Manager) - AWT-Free Windowing
        # ==========================================================================
        "JWM": {
            "moduleName": "jwm",
            "maven": {
                "groupId": "io.github.humbleui",
                "artifactId": "jwm",
                "version": "0.4.23",
            },
            "digest": "sha256:7115f89f52e2832501d8197cec285151a680777e52e85dfcea620a60396a3283",
            "licence": "Apache-2.0",
            "useModulePath": True,
        },
        "JETBRAINS_ANNOTATIONS": {
            "moduleName": "org.jetbrains.annotations",
            "maven": {
                "groupId": "org.jetbrains",
                "artifactId": "annotations",
                "version": "24.1.0",
            },
            "digest": "sha256:27a770dc7ce50500918bb8c3c0660c98290630ec796b5e3cf6b90f403b3033c6",
            "licence": "Apache-2.0",
            "useModulePath": True,
        },
        "TYPES": {
            "moduleName": "io.github.humbleui.types",
            "maven": {
                "groupId": "io.github.humbleui",
                "artifactId": "types",
                "version": "0.2.0",
            },
            "digest": "sha256:38d94d00770c4f261ffb50ee68d5da853c416c8fe7c57842f0e28049fc26cca8",
            "licence": "Apache-2.0",
            "useModulePath": True,
        },
        # ==========================================================================
        #  Skija (Skia Bindings - HumbleUI fork)
        # ==========================================================================
        "SKIJA_SHARED": {
            "moduleName": "io.github.humbleui.skija.shared",
            "maven": {
                "groupId": "io.github.humbleui",
                "artifactId": "skija-shared",
                "version": "0.143.9",
            },
            "digest": "sha256:012a91105a10f2974547679803b304ffc37353fd28dca408b4a9acfa68642c0f",
            "licence": "Apache-2.0",
            "useModulePath": True,
        },
        "SKIJA_PLATFORM": {
            "licence": "Apache-2.0",
            "dependencies": ["SKIJA_SHARED"],
            "os_arch": {
                "linux": {
                    "amd64": {
                        "maven": {
                            "groupId": "io.github.humbleui",
                            "artifactId": "skija-linux-x64",
                            "version": "0.143.9",
                        },
                        "digest": "sha256:2fb033c4ea1594bda5a182e28cd91e675ab93b2029c5930c5d4c29515be9b70a",
                    },
                    "aarch64": {
                        "maven": {
                            "groupId": "io.github.humbleui",
                            "artifactId": "skija-linux-arm64",
                            "version": "0.143.9",
                        },
                        "digest": "sha256:0b103f144a7a0ae28bcdd0b7c79cabcc8459aec33011bb868720cc9750ceb16c",
                    },
                },
                "darwin": {
                    "aarch64": {
                        "maven": {
                            "groupId": "io.github.humbleui",
                            "artifactId": "skija-macos-arm64",
                            "version": "0.143.9",
                        },
                        "digest": "sha256:8c525e882b8ad9f648e28a97ce38698f9636d3cdab4696667f9de393067db81a",
                    },
                },
                "windows": {
                    "amd64": {
                        "maven": {
                            "groupId": "io.github.humbleui",
                            "artifactId": "skija-windows-x64",
                            "version": "0.143.9",
                        },
                        "digest": "sha256:cff43eb34b38f572753f2971f2cc9f53395835b83017325a61519c5d4f312001",
                    },
                },
            },
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
                # The Clean Stack: JWM + Skija
                "JWM",
                "TYPES",
                "JETBRAINS_ANNOTATIONS",
                "SKIJA_SHARED",
            ],
            "requires": [
                "java.datatransfer",
                "java.xml",
                # "java.desktop",  <-- REMOVED (Ensures AWT-free build)
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
                "JWM",
            ],
            "requires": [
                "java.xml",
                # "java.desktop", <-- REMOVED
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
                # Force headless AWT just in case some transitive dep tries to init it
                "-Djava.awt.headless=true",
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
                "-H:-DetectUserDirectoriesInImageHeap",  # ToDo: scrub GitHub path from JWM/Skija packages
                # JNI/JWM Configuration
                "-H:+JNI",
                # Tells Native Image to defer Skija/JWM native library loading until the app actually runs
                "--initialize-at-run-time=io.github.humbleui",
                # Bundle native OS windowing libraries
                "-H:IncludeResources=.*(jwm|skija).*\\.(dylib|so|dll)|.*trufflesqueak-icon\\.(png|ico|icns)",
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
            "exclude": [
                "JWM",
                "SKIJA_SHARED",
                "TYPES",
                "JETBRAINS_ANNOTATIONS",
            ],
            "distDependencies": [
                "TRUFFLESQUEAK_SHARED",
                "truffle:TRUFFLE_API",
                "truffle:TRUFFLE_NFI",
                "truffle:TRUFFLE_NFI_LIBFFI",  # runtime dependency
                "truffle:TRUFFLE_NFI_PANAMA",  # runtime dependency
                # Ensure runtime deps are on classpath
                "JWM",
                "SKIJA_SHARED",
                "TYPES",
                "JETBRAINS_ANNOTATIONS",
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
                    # Add Native Libraries for JWM & Skija
                    # JWM extracts libraries automatically, but for Native Image we bundle them
                    "extracted-dependency:JWM/*",
                    "extracted-dependency:SKIJA_PLATFORM/*",
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
            "exclude": [
                "JWM",
            ],
            "distDependencies": [
                "TRUFFLESQUEAK_SHARED",
                "sdk:LAUNCHER_COMMON",
                "sdk:MAVEN_DOWNLOADER",
                "JWM",
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
                    # Add Native Libraries for JWM & Skija (Platform Specific)
                    "extracted-dependency:JWM/*",
                    "extracted-dependency:SKIJA_PLATFORM/*",
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
                "SKIJA_PLATFORM",
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
                        # comment out next line to capture reachability metadata
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
