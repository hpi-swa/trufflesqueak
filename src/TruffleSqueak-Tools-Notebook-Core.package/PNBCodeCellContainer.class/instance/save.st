actions
save
	| stream fName json |
	json := self asJson.
	fName := UIManager default saveFilenameRequest:'Please enter the name' translated initialAnswer: 'polyglot-notebook.ipynb'.
	(Polyglot isPermitted: #python) ifTrue: [ self class offerToValidateOutput: json asJsonString ].
	stream := FileDirectory default forceNewFileNamed: fName.
	json jsonWriteOn: stream