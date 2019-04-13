actions
save
	| stream fName |
	fName := UIManager default saveFilenameRequest:'Please enter the name' translated initialAnswer: 'polyglot-notebook.ipynb'.
	stream := FileDirectory default forceNewFileNamed: fName.
	self asJson jsonWriteOn: stream