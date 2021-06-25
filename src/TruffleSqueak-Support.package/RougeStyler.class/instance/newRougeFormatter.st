rouge
newRougeFormatter
	^ Polyglot eval: 'ruby' string: 'Rouge::Formatters::HTMLInline.new(Rouge::Themes::IgorPro.new)'