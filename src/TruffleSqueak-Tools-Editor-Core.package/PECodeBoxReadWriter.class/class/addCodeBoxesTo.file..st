reading
addCodeBoxesTo: aTextOrString file: aPCFileEntry

	| beginMatches endMatches beginMatcher endMatcher input output offset lineEndingsSymbol codeBoxes languageInfo |
	output := Text new.
	codeBoxes := OrderedCollection new.
	input := aTextOrString asText.
	beginMatcher := RxMatcher forString: self codeBoxBeginMarker, '\:'.
	endMatcher := RxMatcher forString: self codeBoxEndMarker.
	
	beginMatches := beginMatcher matchingRangesIn: input.
	endMatches := endMatcher matchingRangesIn: input.
	
	beginMatches size = endMatches size ifFalse: [
		UIManager default inform: 'Could not parse code boxes: The amount of code box begin and end markers needs to match.'.
		^ {input. codeBoxes}].
	
	beginMatches ifEmpty: [^ {input. codeBoxes}].
	
	lineEndingsSymbol := PELineEndingManager detectLineEndings: input.
	lineEndingsSymbol = #multiple ifTrue: [
		UIManager default inform: 'Could not parse code boxes: The file has multiple kinds of line endings.'.
		^ {input. codeBoxes}].
	
	languageInfo := PELanguageInfo forExtension: aPCFileEntry extension.
	
	offset := 1.
	beginMatches with: endMatches do: [:begin :end | | lineStart lineEndings codeBox codeBoxPath snippetFile |
		lineEndings := PELineEndingManager idToString: lineEndingsSymbol.
		lineStart := ((input copyFrom: offset to: begin start - 1) findLastOccurrenceOfString: lineEndings startingAt: 1) + offset - 1.
		output := output, ((input copyFrom: offset to: lineStart) asText).
		codeBoxPath := languageInfo unescapedCommentContent: (input copyFrom: begin stop + 1 to: input size) asString.
		
		(codeBoxPath beginsWith: './') ifTrue: [
			"the code box uses a relative path. prepend the path of the file embedding the code box."
			codeBoxPath := aPCFileEntry parent fullPath, '/', (codeBoxPath copyAfter: $/)].
		
		snippetFile := aPCFileEntry class forPath: (codeBoxPath copyReplaceAll: '/' with: FileDirectory pathNameDelimiter asString).
		
		snippetFile
			ifNotNil: [ | innerCodeBoxes tmp |
				tmp := self codeBoxForSnippet: snippetFile.
				codeBox := tmp first.
				innerCodeBoxes := tmp second.
				codeBoxes addAll: innerCodeBoxes]
			ifNil: [
				UIManager default inform: 'Could not find code box at ', codeBoxPath.
				codeBox := PECodeBox new 
					code: 'Could not find code box.';
					yourself].
		
		codeBoxes add: codeBox.
		output := output, (	Text 
			string: (String value: 1) 
			attribute: (TextAnchor new anchoredMorph: (ToolBuilder build: codeBox))).
		offset := input findString: lineEndings startingAt: end stop + 1.
		offset = 0 ifTrue: [offset := end stop + 1]].
	
	output := output, ((input copyFrom: offset to: input size) asText).
	^ {output. codeBoxes}