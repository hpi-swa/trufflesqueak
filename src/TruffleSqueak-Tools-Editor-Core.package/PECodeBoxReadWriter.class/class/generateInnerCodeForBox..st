writing
generateInnerCodeForBox: aPCCodeBox

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