toolbuilder
moduleAndClassList
	^ ((Polyglot eval: #ruby string: 'Object.constants.select{|x|
										begin; Object.const_get(x).is_a? Module; rescue; false; end;}')
			asCollection collect: [ :ea | ea asString ]) sorted