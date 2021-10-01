toolbuilder
moduleAndClassList
	^ ((Polyglot eval: #python string: 'import sys; list(sys.modules.keys())')
			asCollection collect: [ :ea | ea asString ]) sorted