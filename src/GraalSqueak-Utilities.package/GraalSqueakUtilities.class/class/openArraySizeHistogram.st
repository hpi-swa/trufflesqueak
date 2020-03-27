statistics
openArraySizeHistogram
	HistogramMorph openOn: (Array allInstances collect: [ :ea | ea size ])