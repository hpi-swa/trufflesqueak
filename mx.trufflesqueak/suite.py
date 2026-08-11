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
    "mxversion": "7.83.0",
    "versionConflictResolution": "latest",
    "version": "25.1.3",
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
                "version": "vm-25.1.3",
                "urls": [{"url": "https://github.com/oracle/graal", "kind": "git"}],
            }
        ],
    },
    # ==========================================================================
    #  LIBRARIES
    # ==========================================================================
    "libraries": {
        "OSVM_PLUGINS": {
            "baseurl": "https://github.com/fniephaus/opensmalltalk-vm/releases/download/202607030734",
            "os_arch": {
                "linux": {
                    "aarch64": {
                        "urls": ["{baseurl}/squeak.cog.spur_linux64ARMv8.tar.gz"],
                        "digest": "sha512:94a548dafa36843518759f37c8c118b4bb770f8707a9722cc40dbde661699ea755fbc6248a0b380a75a2d272f6b19512484c27b22abac0124b220f048dcf4690",
                    },
                    "amd64": {
                        "urls": ["{baseurl}/squeak.cog.spur_linux64x64.tar.gz"],
                        "digest": "sha512:669100d86a56ce64bde5b9dda981b8f4e1f675cc9b72ee4cb728f1156602fe474ea5f2c75cfa0fcae600f7dfe4c8a72248e373e3fb7d194aa865d3c895c390f0",
                    },
                },
                "darwin": {
                    "aarch64": {
                        "urls": ["{baseurl}/squeak.cog.spur_macos64ARMv8.tar.gz"],
                        "digest": "sha512:8fde0da8c4c42900d943867b3531b546f21462d32ae57965f6a67ef8dae4041b924209e0edcff4c3f69b4b3789d401d578a7a4a3d330c54574173f53c346a7bc",
                    },
                },
                "windows": {
                    "amd64": {
                        "urls": ["{baseurl}/squeak.cog.spur_win64x64.zip"],
                        "digest": "sha512:a96afc9f28384f07539b85bf400cfdab28b6c05c08510ae5153040ec92d8365d01f1a7158f9e2cca4080c7451f8c870e518f04670f38c567b793e8793a9d0806",
                    },
                },
                "<others>": {"<others>": {"optional": True}},
            },
        },
        "LWJGL_SDL_PLATFORM": {
            "os_arch": {
                "linux": {
                    "amd64": {
                        "maven": {
                            "groupId": "org.lwjgl",
                            "artifactId": "lwjgl-sdl",
                            "classifier": "natives-linux",
                            "version": "3.4.2",
                        },
                        "digest": "sha256:027e3d8a25fc26c9fe14ed805b75a1cf8263350eb1e6db70ca98827e0ba16b05",
                    },
                    "aarch64": {
                        "maven": {
                            "groupId": "org.lwjgl",
                            "artifactId": "lwjgl-sdl",
                            "classifier": "natives-linux-arm64",
                            "version": "3.4.2",
                        },
                        "digest": "sha256:df567065f76f73061303d99b9cd9091991223aa92519fbd429cd1032cef9f28c",
                    },
                },
                "darwin": {
                    "aarch64": {
                        "maven": {
                            "groupId": "org.lwjgl",
                            "artifactId": "lwjgl-sdl",
                            "classifier": "natives-macos-arm64",
                            "version": "3.4.2",
                        },
                        "digest": "sha256:bae5634007b3e84e80549a2105f36e072efca18080f47fb7a7d146df4110e3a7",
                    },
                },
                "windows": {
                    "amd64": {
                        "maven": {
                            "groupId": "org.lwjgl",
                            "artifactId": "lwjgl-sdl",
                            "classifier": "natives-windows",
                            "version": "3.4.2",
                        },
                        "digest": "sha256:2a93346f7b3bfac86cf036e8db0c829042fb88f79fb0ce47dc243ea3b0e17494",
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
                "de.hpi.swa.trufflesqueak.interpreterproxy.bindings",
                "TRUFFLESQUEAK_SHARED",
                "truffle:TRUFFLE_API",
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
            "javaCompliance": "24+",
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
                "java.management",
            ],
            "checkstyle": "de.hpi.swa.trufflesqueak",
            "jacoco": "include",
            "javaCompliance": "24+",
            "workingSets": "TruffleSqueak",
        },
        "de.hpi.swa.trufflesqueak.interpreterproxy.bindings": {
            "subDir": "src",
            "sourceDirs": ["src"],
            "eclipseformat": False,
            "forceJavac": True,
            "javac.lint.overrides": "-restricted",
            "jacoco": "exclude",
            "javaCompliance": "24+",
            "workingSets": "TruffleSqueak",
        },
        "de.hpi.swa.trufflesqueak.sdl3": {
            "subDir": "src",
            "sourceDirs": ["src"],
            "eclipseformat": False,
            "forceJavac": True,
            "javac.lint.overrides": "none",
            "jacoco": "exclude",
            "javaCompliance": "24+",
            "workingSets": "TruffleSqueak",
        },
        "de.hpi.swa.trufflesqueak.shared": {
            "subDir": "src",
            "sourceDirs": ["src"],
            "dependencies": [
                "de.hpi.swa.trufflesqueak.sdl3",
                "sdk:GRAAL_SDK",
            ],
            "requires": [
                "java.net.http",
            ],
            "checkstyle": "de.hpi.swa.trufflesqueak",
            "jacoco": "include",
            "javaCompliance": "24+",
            "workingSets": "TruffleSqueak",
        },
        "de.hpi.swa.trufflesqueak.tck": {
            "subDir": "src",
            "sourceDirs": ["src"],
            "dependencies": ["TRUFFLESQUEAK_SHARED", "sdk:POLYGLOT_TCK", "mx:JUNIT"],
            "checkstyle": "de.hpi.swa.trufflesqueak",
            "javaCompliance": "24+",
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
            ],
            "checkstyle": "de.hpi.swa.trufflesqueak",
            "jacoco": "include",
            "javaCompliance": "24+",
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
                "java.library.path": "../lib",
            },
            "liblang_relpath": "../lib/<lib:smalltalkvm>",
            "default_vm_args": [
                "--vm.-add-exports=java.base/jdk.internal.module=de.hpi.swa.trufflesqueak",
                "--vm.-enable-native-access=de.hpi.swa.trufflesqueak.sdl3",
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
                "TRUFFLESQUEAK_SDL3",
                "TRUFFLESQUEAK_SHARED",
                "truffle:TRUFFLE_API",
            ],
            "javaProperties": {
                "org.graalvm.language.smalltalk.home": "<path:TRUFFLESQUEAK_GRAALVM_SUPPORT_PLATFORM_SPECIFIC>",
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
        "TRUFFLESQUEAK_SDL3": {
            "description": "TruffleSqueak SDL3 bindings distribution",
            "moduleInfo": {
                "name": "de.hpi.swa.trufflesqueak.sdl3",
                "exports": [
                    "de.hpi.swa.trufflesqueak.sdl3",
                    "de.hpi.swa.trufflesqueak.sdl3.bindings",
                ],
            },
            "useModulePath": True,
            "dependencies": [
                "de.hpi.swa.trufflesqueak.sdl3",
            ],
            "distDependencies": [
                "sdk:GRAAL_SDK",
            ],
            "maven": {
                "groupId": "de.hpi.swa.trufflesqueak",
                "artifactId": "trufflesqueak-sdl3",
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
            "useModulePath": True,
            "dependencies": [
                "de.hpi.swa.trufflesqueak.shared",
            ],
            "distDependencies": [
                "TRUFFLESQUEAK_SDL3",
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
            "javaCompliance": "24+",
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
            "os_arch": {
                "linux": {
                    "amd64": {
                        "layout": {
                            "lib/": [
                                "extracted-dependency:OSVM_PLUGINS/sqcogspur64linuxht/lib/squeak/*/JPEGReadWriter2Plugin.so",
                                "extracted-dependency:OSVM_PLUGINS/sqcogspur64linuxht/lib/squeak/*/LocalePlugin.so",
                                "extracted-dependency:OSVM_PLUGINS/sqcogspur64linuxht/lib/squeak/*/SqueakSSL.so",
                                "extracted-dependency:LWJGL_SDL_PLATFORM/linux/x64/org/lwjgl/sdl/libSDL3.so",
                            ],
                        },
                    },
                    "aarch64": {
                        "layout": {
                            "lib/": [
                                "extracted-dependency:OSVM_PLUGINS/sqcogspur64ARMv8linuxht/lib/squeak/*/JPEGReadWriter2Plugin.so",
                                "extracted-dependency:OSVM_PLUGINS/sqcogspur64ARMv8linuxht/lib/squeak/*/LocalePlugin.so",
                                "extracted-dependency:OSVM_PLUGINS/sqcogspur64ARMv8linuxht/lib/squeak/*/SqueakSSL.so",
                                "extracted-dependency:LWJGL_SDL_PLATFORM/linux/arm64/org/lwjgl/sdl/libSDL3.so",
                            ],
                        },
                    },
                },
                "darwin": {
                    "aarch64": {
                        "layout": {
                            "lib/": [
                                "extracted-dependency:OSVM_PLUGINS/Squeak.app/Contents/Resources/JPEGReadWriter2Plugin.bundle",
                                "extracted-dependency:OSVM_PLUGINS/Squeak.app/Contents/Resources/LocalePlugin.bundle",
                                "extracted-dependency:OSVM_PLUGINS/Squeak.app/Contents/Resources/SqueakSSL.bundle",
                                "extracted-dependency:LWJGL_SDL_PLATFORM/macos/arm64/org/lwjgl/sdl/libSDL3.dylib",
                            ],
                        },
                    },
                },
                "windows": {
                    "amd64": {
                        "layout": {
                            "lib/": [
                                "extracted-dependency:OSVM_PLUGINS/JPEGReadWriter2Plugin.dll",
                                "extracted-dependency:OSVM_PLUGINS/LocalePlugin.dll",
                                "extracted-dependency:OSVM_PLUGINS/SqueakSSL.dll",
                                "extracted-dependency:LWJGL_SDL_PLATFORM/windows/x64/org/lwjgl/sdl/SDL3.dll",
                            ],
                        },
                    },
                },
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
                "./resources/README.md": "string:Directory for Smalltalk image, changes, and sources files.\n",
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
        "TRUFFLESQUEAK_NATIVE_STANDALONE": {
            "description": "TruffleSqueak Native standalone",
            "type": "dir",
            "platformDependent": True,
            "platforms": "local",
            "layout": {
                "./": [
                    "dependency:TRUFFLESQUEAK_STANDALONE_COMMON/*",
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
                ],
                "modules/": [
                    "classpath-dependencies:TRUFFLESQUEAK_STANDALONE_DEPENDENCIES",
                ],
            },
        },
    },
}
