Extension { #name : #PolyglotWorkspace }

{ #category : #'*TruffleSqueak-Tools-Editor-Core' }
PolyglotWorkspace >> openInPolyglotEditor [

	| editor languageExtension newFileName fileEntryForNewFile |

	newFileName := UIManager default
		request: 'Enter name for new file, extension will be added automatically'
		initialAnswer: 'workspace', self identityHash asString.
	newFileName ifEmpty: [^ self].

	editor := (PolyglotEditor open) model.
	languageExtension := (PELanguageInfo forId: self languageInfo getId asString) defaultExtension asString.
	FileDirectory default forceNewFileNamed: newFileName, '.', languageExtension do: [:stream | 
		stream nextPutAll: self contents asString.
		fileEntryForNewFile := PEFileEntryFSFile on: stream name asString asDirectoryEntry].
	editor openFile: fileEntryForNewFile
]
