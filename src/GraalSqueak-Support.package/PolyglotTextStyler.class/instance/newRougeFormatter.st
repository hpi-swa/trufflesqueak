rouge
newRougeFormatter
	<foreignSend>
	^ Polyglot eval: 'ruby' string: '	require "rouge";
	Rouge::Formatters::HTMLInline.new(Rouge::Themes::IgorPro.new)'