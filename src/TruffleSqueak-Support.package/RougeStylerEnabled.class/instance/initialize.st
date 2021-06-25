private
initialize
	super initialize.
	[ {[ Polyglot eval: #ruby string: 'require "rouge"' ]} do: [ :block | block value ]
			displayingProgress: [ :block | 'Initializing RougeStyler in Ruby...' ]
			every: 100 ] on: Error do: [ :e |
				self error: 'Failed to load rouge gem. Please make sure it is installed (via `gem install rouge`)'.
				^ RougeStylerDisabled basicNew initialize ].
	formatter := Polyglot eval: 'ruby' string: 'Rouge::Formatters::HTMLInline.new(Rouge::Themes::IgorPro.new)'.
	rubyUnbox := Polyglot eval: #ruby string: 'require "rouge"; Truffle::Interop.method(:unbox)'