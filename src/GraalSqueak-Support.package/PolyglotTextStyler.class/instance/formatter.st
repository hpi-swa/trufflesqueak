as yet unclassified
formatter
	^ formatter ifNil: [ formatter := Polyglot eval: 'ruby' string: 'require ''rouge''; Rouge::Formatters::HTMLInline.new(Rouge::Themes::IgorPro.new)' ]