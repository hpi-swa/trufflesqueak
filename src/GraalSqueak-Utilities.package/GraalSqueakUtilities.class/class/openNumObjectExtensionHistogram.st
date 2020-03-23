statistics
openNumObjectExtensionHistogram
	HistogramMorph openOn: (self getAllLayouts collect: [ :ea | ea getNumObjectExtension wrappedJavaObject ])