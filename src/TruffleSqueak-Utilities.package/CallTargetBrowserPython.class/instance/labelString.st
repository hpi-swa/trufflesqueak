toolbuilder
labelString
	| label |
	label := 'CallTargetBrowser for GraalPython'.
	self selectedModuleName
		ifNotNil: [ :name | label := label, ': ', name ].
	^ label