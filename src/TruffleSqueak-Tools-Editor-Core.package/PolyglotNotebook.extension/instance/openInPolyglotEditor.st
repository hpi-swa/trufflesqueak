*TruffleSqueak-Tools-Editor-Core
openInPolyglotEditor

	| editor newDirectoryName newDirectory pythonInfo |

	newDirectoryName := UIManager default
		request: 'Enter name for new directory containing the files'
		initialAnswer: 'polyJuS', self identityHash asString.
	newDirectoryName ifEmpty: [^ self].

	editor := (PolyglotEditor open) model.
	FileDirectory default assureExistenceOfPath: newDirectoryName.
	newDirectory := FileDirectory on: FileDirectory default pathName, FileDirectory slash, newDirectoryName.
	pythonInfo := PELanguageInfo forId: Polyglot languageIdPython.
	newDirectory forceNewFileNamed: 'main.', pythonInfo defaultExtension do: [:mainFile |
		mainFile nextPutAll: pythonInfo codeForPolyglotHeader; lf; lf.

		self cellContainer submorphs doWithIndex: [:codeCell :index | | newFileName languageInfo content |
			languageInfo := PELanguageInfo forId: codeCell languageID.
			content := codeCell editor text asString.
			codeCell isMarkdown
				ifTrue: [mainFile nextPutAll: (pythonInfo codeForComment: (PELineEndingManager withLf: content)); lf; lf]
				ifFalse: [ | path |
					newFileName := 'cell{1}.{2}' format: {index asString. languageInfo defaultExtension}.
					newDirectory forceNewFileNamed: newFileName do: [:stream | stream nextPutAll: content].
					editor class useAbsolutePathsForSnippets
						ifTrue: [path := newDirectory pathName, FileDirectory slash, newFileName]
						ifFalse: [path := newFileName].
					mainFile nextPutAll: (pythonInfo codeForPolyglotEvaluateFile: path in: codeCell languageID); lf; lf]]].
	editor setNewRootDirectory: newDirectory