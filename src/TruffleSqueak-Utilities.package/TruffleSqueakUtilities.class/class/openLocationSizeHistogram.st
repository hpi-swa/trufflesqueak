statistics
openLocationSizeHistogram
	HistogramMorph openOn: (self getAllLayouts collect: [ :ea | ea getLocations size ])