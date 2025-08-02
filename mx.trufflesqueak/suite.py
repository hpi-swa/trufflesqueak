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
                "version": "release/graal-vm/25.0",
                "urls": [{"url": "https://github.com/oracle/graal", "kind": "git"}],
            }
        ],
    },
    # ==========================================================================
    #  LIBRARIES
    # ==========================================================================
    "libraries": {
        "BOUNCYCASTLE-PROVIDER": {
            "digest": "sha512:dbc5b525d805823b53dbaded11547155a14f795212ce6fe1e93d6da431081ea9480718ea2fc17dc7906f8489aadb68e781afd1e771d26f9f8a09b21552bb165c",
            "sourceDigest": "sha512:4ce8b88e26af98c3cb8a3691ace366e960e36a8225d14685447b4aa9838b92334bdb63f8ba4baf651d28c8e063e21d0cbca8f2fcf8eecd003362ae62b6c87dbd",
            "maven": {
                "groupId": "org.bouncycastle",
                "artifactId": "bcprov-jdk18on",
                "version": "1.76",
            },
            "moduleName": "org.bouncycastle.provider",
        },
        "BOUNCYCASTLE-PKIX": {
            "digest": "sha512:b924374168e25f21ab7f6dd4f6755e55a401cbbbaa0d6f17a0c9bf59e61dc42750b200c494c413f6f8c27fc16d9312f51fc15c979e4298916f5bd0c329cbbffa",
            "sourceDigest": "sha512:6945aedc041f9282ee3569aef46c6df8940643e5a66236c5e95fafdc4dead4b94d3d64f32750ce2f131b4fdd398aacd200968103fc3e4d22eb2dc171aedb48dd",
            "maven": {
                "groupId": "org.bouncycastle",
                "artifactId": "bcpkix-jdk18on",
                "version": "1.76",
            },
            "moduleName": "org.bouncycastle.pkix",
        },
        "BOUNCYCASTLE-UTIL": {
            "digest": "sha512:385d95b4c32053bb3734c342d5f3255bcc1cee7e35649965bb5fbf8733ec37009fd5f5e06817a45e7857a2e62e923563ce1231ee3a1de411f788dfa93d39ce41",
            "sourceDigest": "sha512:8d2068b8a90381dde75f25059dfdf3073a2657ea8f7d65872f972aaae6b780a4156b39d922e10302f4c4ddaf22d5057c02e9a0cb2a228f0a43730dfba46b1b22",
            "maven": {
                "groupId": "org.bouncycastle",
                "artifactId": "bcutil-jdk18on",
                "version": "1.76",
            },
            "moduleName": "org.bouncycastle.util",
        },
        "OSVM_PLUGINS": {
            "baseurl": "https://github.com/hpi-swa/trufflesqueak/releases/download/23.1.0/osvm-plugins-202312181441",
            "os_arch": {
                "linux": {
                    "amd64": {
                        "urls": ["{baseurl}-linux-amd64.zip"],
                        "digest": "sha512:5e94f289e5e1c71772b3033fda31e637cdcbea17321f2a4448a6755dff6db2db210086cffc993320249bcb6a1df395c17a2a06aedc9636159623336ca92e8008",
                    },
                    "aarch64": {
                        "urls": ["{baseurl}-linux-aarch64.zip"],
                        "digest": "sha512:b4801b2a442ca383c6d5718c5a085b1446e66010e73587f166ff2726d393ecc47d7a195bba9d586e7f6c40d587e9a89c874a39adb3f65e9633a12703b40268e9",
                    },
                },
                "windows": {
                    "amd64": {
                        "urls": ["{baseurl}-windows-amd64.zip"],
                        "digest": "sha512:10ec2b4b783bb83a814866ea237a424138802a99ee63b3cfbe2d2b2c6607e94ea000922f58f8a159108f66c0509764bc48b62885337d2a198534337eb2ed6f8e",
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
                "BOUNCYCASTLE-PROVIDER",
                "BOUNCYCASTLE-PKIX",
                "BOUNCYCASTLE-UTIL",
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
                "--vm.Xms512M",
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
                # From mx.trufflesmalltalk/native-image.properties
                # "-Dpolyglot.image-build-time.PreinitializeContexts=smalltalk",
                "-Dorg.graalvm.language.smalltalk.home=<path:TRUFFLESQUEAK_STANDALONE_COMMON>",
                # Configure launcher
                "-Dorg.graalvm.launcher.class=de.hpi.swa.trufflesqueak.launcher.TruffleSqueakLauncher",
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
                "truffle:TRUFFLE_NFI_LIBFFI",
            ],
            "exclude": [
                "BOUNCYCASTLE-PROVIDER",
                "BOUNCYCASTLE-PKIX",
                "BOUNCYCASTLE-UTIL",
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
                "bin/trufflesqueak": "dependency:trufflesqueak_thin_launcher",
                "bin/trufflesqueak-polyglot-get": "dependency:trufflesqueak_thin_launcher",
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
                    "extracted-dependency:truffle:TRUFFLE_NFI_NATIVE_GRAALVM_SUPPORT",
                ],
                "modules/": [
                    "classpath-dependencies:TRUFFLESQUEAK_STANDALONE_DEPENDENCIES",
                ],
            },
        },
    },
}
