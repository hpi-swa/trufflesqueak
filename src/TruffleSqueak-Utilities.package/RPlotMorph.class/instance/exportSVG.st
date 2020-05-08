actions
exportSVG
	| fileName dimensions |
	fileName := UIManager default saveFilenameRequest: 'Please select file name' translated initialAnswer: 'plot.svg'.
	dimensions := UIManager default request: 'Dimensions (in inches)' initialAnswer: '7x7'.
	(dimensions includes: $x) ifTrue: [ | split |
		split := dimensions splitBy: 'x'.
		self exportSVG: fileName width: split first asInteger height: split second asInteger ]