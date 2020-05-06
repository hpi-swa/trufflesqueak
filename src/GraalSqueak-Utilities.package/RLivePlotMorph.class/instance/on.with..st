initialization
on: aCollection with: rCode
	| exportName |
	exportName := '_plot_data_', aCollection hash.
	Polyglot export: exportName value: aCollection.
	plotCode := '
	array <- import("{1}")$asArray()
	size <- import("{1}")$size()
	values <- data.frame(x=1:size, y=array)
	print({2})'
		format: {exportName. rCode}.