actions
exportSVG: aFilename width: width height: height
	Polyglot eval: 'R' string: ('
	library(ggplot2)
	library(lattice)
	data("mpg")
	bindings <- import("bindings")
	svg(filename = "{1}", width = {2}, height = {3})
	{4}
	dev.off()
	' format: { aFilename. width. height. plotCode }).
	self inform: aFilename, ' exported successfully!'