modules
moduleList
	^ ((Polyglot eval: 'ruby' string: 'Object.constants')
			asCollection collect: [ :ea | ea asString ]) sorted