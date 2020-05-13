code actions
save

	| entry files |
	entry := self fileEntry.
	entry ifNil: [^ self].
		
	entry supportsPolyglot
		ifTrue: [
			files := PECodeBoxReadWriter
				getFilesToSaveFrom: self code
				path: entry fullPath
				id: self languageInfo id
				lineEndings: self lineEndings.
	
			files keysAndValuesDo: [:path :content | | file |
				[file := FileDirectory default forceNewFileNamed: path] ifError: [ | snippetDirectoryPath snippetDirectoryFileEntryDirectory |
					"containing folder does not exist. This happens when the snippet directory is newly created."
					snippetDirectoryPath := FileDirectory dirPathFor: path.
					FileDirectory splitName: snippetDirectoryPath to: [:existingDirectoryPath :snippetFolderName |
						(FileDirectory on: existingDirectoryPath) createDirectory: snippetFolderName].
					file := FileDirectory default forceNewFileNamed: path.
					snippetDirectoryFileEntryDirectory := PEFileEntryFSDirectory forPath: snippetDirectoryPath.
					self triggerEvent: #newSnippetDirectory with: snippetDirectoryFileEntryDirectory.]. 
				file truncate;
					nextPutAll: content]]
		ifFalse: [entry write: self code].
	
	self triggerEvent: #save