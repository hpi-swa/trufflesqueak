building
nestedCodeBox
	
	| codeBox |
	self language ifNil: [self error: 'You need to select a language first!'] ifNotNil: [
		codeBox := PECodeBox new.
		self insertCodeBox: codeBox]

	"| codeBox file |
	self editArea fileEntry parent children
		detect: [:each | each name = 'snippets']
		ifFound: [:snippets |
			file := UIManager default 
				chooseFrom: {'new'}, (snippets children collect: #name)
				values: {#new}, snippets children.
			file ifNil: [^ self].
			codeBox := file = #new
				ifTrue: [PCCodeBox new]
				ifFalse: [PCCodeBoxReadWriter codeBoxWithFileName: file name
					referencedIn: self activeEditArea fileEntry]]
		ifNone: [codeBox := PCCodeBox new].
	
	self insertCodeBox: codeBox"
	