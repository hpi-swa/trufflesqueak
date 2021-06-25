private
initialize
	super initialize.
	[ {[ Polyglot eval: #ruby string: 'require "rouge"' ]} do: [ :block | block value ]
			displayingProgress: [ :block | 'Initializing RougeStyler in Ruby...' ]
			every: 100 ] on: Error do: [ :e | ^ RougeStylerDisabled basicNew initialize ].
	formatter := Polyglot eval: 'ruby' string: 'Rouge::Formatters::HTMLInline.new(Rouge::Themes::IgorPro.new)'.
	rubyUnbox := Polyglot eval: #ruby string: 'require "rouge"; Truffle::Interop.method(:unbox)'