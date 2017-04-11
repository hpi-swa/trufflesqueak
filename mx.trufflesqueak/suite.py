suite = {
  "mxversion" : "5.70.0",
  "name" : "trufflesqueak",
  "versionConflictResolution" : "latest",
  "imports" : {
    "suites" : [
            {
               "name" : "truffle",
               "version" : "master",
               "urls" : [
                    {"url" : "https://github.com/graalvm/truffle", "kind" : "git"},
                ]
            },

        ],
   },

  "libraries" : {
  },

  "projects" : {
    "de.hpi.swa.trufflesqueak" : {
      "subDir" : "trufflesqueak",
      "sourceDirs" : ["src"],
      "dependencies" : [
                "truffle:TRUFFLE_API",
                "truffle:TRUFFLE_DSL_PROCESSOR",
      ],
      "checkstyle" : "de.hpi.swa.trufflesqueak",
      "javaCompliance" : "1.8",
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "workingSets" : "Truffle,Squeak",
    },
    "de.hpi.swa.trufflesqueak.test" : {
      "subDir" : "trufflesqueak",
      "sourceDirs" : ["src"],
      "dependencies" : ["de.hpi.swa.trufflesqueak","mx:JUNIT"],
      "checkstyle" : "de.hpi.swa.trufflesqueak.test",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle,Python",
    },

  },

  "defaultLicense" : "BSD-3-Clause",
  "licenses" : {
    "BSD-3-Clause" : {
      "name" : "The 3-Clause BSD License",
      "url" : "http://opensource.org/licenses/BSD-3-Clause",
    },
  },


  "distributions" : {
    "TruffleSqueak" : {
      "path" : "trufflesqueak.jar",
      "dependencies" : [
        "de.hpi.swa.trufflesqueak",
      ],

      "distDependencies" : [
        "truffle:TRUFFLE_API",
        "truffle:TRUFFLE_DSL_PROCESSOR",
        ],

      "exclude" : [
        "mx:JUNIT",
        ],

      "sourcesPath" : "trufflesqueak.src.zip",
    },
  },
}
