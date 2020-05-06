drawing
renderIntoForm
	Polyglot export: '_fastr_graphics' value: self graphics2D.
	Polyglot eval: 'R' string: ('
	library(ggplot2)
	library(lattice)
	data("mpg")
	bindings <- import("bindings")
	awt({1}, {2}, import("_fastr_graphics"))
	{3}
	' format: { form width. form height. plotCode})