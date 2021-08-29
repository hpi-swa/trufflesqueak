Class {
	#name : #PECodeBoxReadWriter,
	#superclass : #Object,
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #reading }
PECodeBoxReadWriter class >> addCodeBoxesTo: aTextOrString file: aPCFileEntry [

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
]

{ #category : #'path utils' }
PECodeBoxReadWriter class >> baseNameOf: aString [

	^ aString copyFrom: (aString lastIndexOfAnyOf: '/\') + 1 to: aString size
]

{ #category : #writing }
PECodeBoxReadWriter class >> box: aPCCodeBox referencedFromDirectory: directoryPath lineEndings: aString languageId: languageID [

	| code languageInfo relativePath pathToEvaluate |
	aPCCodeBox language ifNil: [self error: 'You need to select the target language'].
	aPCCodeBox boxName ifEmpty: [self error: 'You need to give a name to this snippet.'].
	
	languageInfo := PELanguageInfo forId: languageID.
	
	aPCCodeBox fileEntry
		ifNil: [ | directoryName |
			"new code box; default to snippets directory relative to the surrounding file, if the surrounding file isn't already inside a snippets directory."
			directoryName := self baseNameOf: directoryPath.
			directoryName = 'snippets'
				ifTrue: [relativePath := './', aPCCodeBox fileName]
				ifFalse: [relativePath := './snippets/', aPCCodeBox fileName]]
		ifNotNil: ["existing code box; generate a relative path to the code box from the surrounding file"
			relativePath := self
				make: (aPCCodeBox fileEntry fullPath copyReplaceAll: '\' with: '/')
				relativeTo: (directoryPath copyReplaceAll: '\' with: '/')].
	
	code := WriteStream on: ''.
	code
		nextPutAll: (languageInfo codeForComment: self codeBoxBeginMarker, ':', relativePath); 
		nextPutAll: aString.
	
	aPCCodeBox importVariables do: [:each | code 
		nextPutAll: (languageInfo codeForPolyglotExportOf: each to: each);
		nextPutAll: languageInfo codeForStatementSeparator;
		nextPutAll: aString].
	
	
	PolyglotEditor useAbsolutePathsForSnippets
		ifTrue: [pathToEvaluate := directoryPath, '/', (relativePath copyFrom: 3 to: relativePath size)]
		ifFalse: [pathToEvaluate := relativePath].
	
	code
		nextPutAll: (languageInfo codeForPolyglotEvaluateFile: pathToEvaluate in: aPCCodeBox language);
		nextPutAll: languageInfo codeForStatementSeparator;
		nextPutAll: aString.
	
	aPCCodeBox exportVariables do: [:each | code 
		nextPutAll: (languageInfo codeForAssignmentOf: (languageInfo codeForPolyglotImport: each) to: each);
		nextPutAll: languageInfo codeForStatementSeparator;
		nextPutAll: aString].
	
	code
		nextPutAll: (languageInfo codeForComment: self codeBoxEndMarker).
	
	^ {code contents. relativePath. self generateInnerCodeForBox: aPCCodeBox}
]

{ #category : #markers }
PECodeBoxReadWriter class >> codeBoxBeginMarker [

	^ 'CODE BOX BEGIN'
]

{ #category : #markers }
PECodeBoxReadWriter class >> codeBoxEndMarker [

	^ 'CODE BOX END'
]

{ #category : #reading }
PECodeBoxReadWriter class >> codeBoxForSnippet: aPCFileEntry [

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
]

{ #category : #'path utils' }
PECodeBoxReadWriter class >> directorySeparatorsUsedIn: aString [

	^ (aString indexOf: $/) > 0 ifTrue: ['/'] ifFalse: ['\']
]

{ #category : #writing }
PECodeBoxReadWriter class >> generateInnerCodeForBox: aPCCodeBox [

	| code offset codeText header variableDeclaration |
	code := WriteStream on: ''.
	variableDeclaration := aPCCodeBox innerLanguageInfo codeForPredeclaringVariables: aPCCodeBox importVariables.
	variableDeclaration ifNotEmpty: [code
		nextPutAll: variableDeclaration;
		nextPutAll: aPCCodeBox innerLanguageInfo defaultLineEndings].
	header := aPCCodeBox innerLanguageInfo codeForPolyglotHeader.
	header ifNotEmpty: [code
		nextPutAll: header;
		nextPutAll: aPCCodeBox innerLanguageInfo defaultLineEndings].
	
	"imports"
	aPCCodeBox importVariables do: [:each | code 
		nextPutAll: (aPCCodeBox innerLanguageInfo codeForAssignmentOf: (aPCCodeBox innerLanguageInfo codeForPolyglotImport: each) to: each);
		nextPutAll: aPCCodeBox innerLanguageInfo codeForStatementSeparator;
		nextPutAll: aPCCodeBox innerLanguageInfo defaultLineEndings].
	
	"code"
	code
		nextPutAll: (aPCCodeBox innerLanguageInfo codeForComment: self snippetCodeBeginMarker, ':', (self getJsonForBox: aPCCodeBox));
		nextPutAll: aPCCodeBox innerLanguageInfo defaultLineEndings;
		nextPutAll: aPCCodeBox innerLanguageInfo customCodeBeforeCodeBoxContent.
	offset := code position.
	code
		nextPutAll: aPCCodeBox code;
		nextPutAll: aPCCodeBox innerLanguageInfo defaultLineEndings;
		nextPutAll: (aPCCodeBox innerLanguageInfo codeForComment: self snippetCodeEndMarker);
		nextPutAll: aPCCodeBox innerLanguageInfo defaultLineEndings.
	
	"exports"
	aPCCodeBox exportVariables do: [:each | code 
		nextPutAll: (aPCCodeBox innerLanguageInfo codeForPolyglotExportOf: each to: each); 
		nextPutAll: aPCCodeBox innerLanguageInfo codeForStatementSeparator;
		nextPutAll: aPCCodeBox innerLanguageInfo defaultLineEndings].
	code nextPutAll: aPCCodeBox innerLanguageInfo customCodeAfterCodeBoxExports.
	
	"Keep text attributes of the original code. This is necessary because it may contain nested code boxes."
	codeText := code contents asText.
	aPCCodeBox code asText runs withStartStopAndValueDo: [:start :stop :attributes | 
		attributes do: [:attribute | codeText addAttribute: attribute from: start + offset to: stop + offset]].

	^ codeText
]

{ #category : #writing }
PECodeBoxReadWriter class >> getFilesToSaveFrom: aText path: aString id: id lineEndings: lineEndings [

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
]

{ #category : #writing }
PECodeBoxReadWriter class >> getJsonForBox: aPCCodeBox [

	^ Json render: (JsonObject newFrom: {
		#boxName -> aPCCodeBox boxName.
		#exportVariables -> aPCCodeBox exportVariables.
		#importVariables -> aPCCodeBox importVariables.
		#language -> aPCCodeBox language})
]

{ #category : #'path utils' }
PECodeBoxReadWriter class >> make: dest relativeTo: root [

	"Given two ABSOLUTE paths using forward slashes, dest and root, return a relative path from root to dest"
	| destParts result rootParts |
	result := './'.
	destParts := dest splitBy: '/'.
	rootParts := root splitBy: '/'.
	
	1 to: rootParts size do: [:i | | destPart rootPart |
		rootPart := rootParts at: i.
		destPart := destParts at: i.
		
		destPart = rootPart ifFalse: [
			rootParts size - i + 1 timesRepeat: [result := result, '../'].
			result := result, ((destParts copyFrom: i to: destParts size) joinSeparatedBy: '/').
			^ result]].
	
	^ result, ((destParts copyFrom: rootParts size + 1 to: destParts size) joinSeparatedBy: '/').
]

{ #category : #'path utils' }
PECodeBoxReadWriter class >> parentFolderOf: aString [

	^ aString copyFrom: 1 to: (aString lastIndexOfAnyOf: '/\') - 1
]

{ #category : #markers }
PECodeBoxReadWriter class >> snippetCodeBeginMarker [

	^ 'CODE BEGIN'
]

{ #category : #markers }
PECodeBoxReadWriter class >> snippetCodeEndMarker [

	^ 'CODE END'
]
