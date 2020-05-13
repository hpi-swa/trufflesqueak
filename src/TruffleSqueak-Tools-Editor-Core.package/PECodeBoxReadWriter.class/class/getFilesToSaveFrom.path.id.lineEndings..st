writing
getFilesToSaveFrom: aText path: aString id: id lineEndings: lineEndings

	| content offset files |
	files := Dictionary new.
	
	content := aText asString.
	
	offset := 0.
	aText runs withStartStopAndValueDo: [:start :stop :attributes | (attributes
		select: [:each | (each isKindOf: TextAnchor) and: [each anchoredMorph model isKindOf: PECodeBox]]
		thenCollect: [:each | each anchoredMorph model])
		do: [:each | | replacement inner outer relativePath snippetPath directory |
			directory := (self parentFolderOf: aString).
			replacement := self
				box: each
				referencedFromDirectory: directory
				lineEndings: lineEndings
				languageId: id.
			outer := replacement first.
			relativePath := replacement second.
			inner := replacement third.
			
			content := content copyReplaceFrom: start + offset to: stop + offset with: outer.
			offset := offset + outer size - 1.
			
			"remove ./"
			relativePath := relativePath copyFrom: 2 to: relativePath size.
			"relativePath always uses /. Adjust to the directory separator used in the path of directory. We can't used FileDirectory pathNameDelimiter since our tests would then return different results depending on the OS"
			relativePath := relativePath copyReplaceAll: '/' with: (self directorySeparatorsUsedIn: directory).
			
			snippetPath := directory, relativePath.
			files addAll: (self 
				getFilesToSaveFrom: inner
				path: snippetPath
				id: each language  
				lineEndings: each innerLanguageInfo defaultLineEndings)]].

	files at: aString put: content.
	^ files