reading
codeBoxForSnippet: aPCFileEntry

	| end lineEndings begin content codeBox beginOfCode endOfConfig json beginOfConfig  code innerCodeBoxes tmp languageInfo |
	languageInfo := PELanguageInfo forExtension: aPCFileEntry extension.
	
	codeBox := PECodeBox new.
	codeBox fileEntry: aPCFileEntry.
	codeBox language: languageInfo polyglotId.
	
	content := aPCFileEntry read.
	lineEndings := codeBox innerLanguageInfo defaultLineEndings.
	begin := content findString: self snippetCodeBeginMarker, ':'.
	beginOfConfig := (content findString: ':' startingAt: begin) + 1.
	endOfConfig := (content findString: lineEndings startingAt: begin) - 1.
	json := languageInfo unescapedCommentContent: (content copyFrom: beginOfConfig to: endOfConfig).
	
	json := Json readFrom: json readStream.
	beginOfCode := endOfConfig + lineEndings size + 1.
	
	codeBox boxName: json boxName.
	codeBox exportVariables: json exportVariables.
	codeBox importVariables: json importVariables.
	
	end := content findString: self snippetCodeEndMarker.
	end := ((content copyFrom: 1 to: end) findLastOccurrenceOfString: lineEndings startingAt: 1) - lineEndings size.
	
	code := content
		copyFrom: beginOfCode + languageInfo customCodeBeforeCodeBoxContent size
		to: end.
	tmp := (self addCodeBoxesTo: code file: aPCFileEntry).
	innerCodeBoxes := tmp second.
	code := tmp first.
	codeBox code: code.
	
	^ {codeBox. tmp second}