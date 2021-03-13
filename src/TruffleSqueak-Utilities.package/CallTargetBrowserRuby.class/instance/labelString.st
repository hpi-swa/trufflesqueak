toolbuilder
labelString
	| label |
	label := 'CallTargetBrowser for TruffleRuby'.
	self selectedModuleName
		ifNotNil: [ :name | label := label, ': ', name ].
	^ label