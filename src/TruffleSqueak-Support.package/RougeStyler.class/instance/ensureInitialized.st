private
ensureInitialized
	rubyUnbox ifNil: [
		{[ rubyUnbox := Polyglot eval: #ruby string: 'require "rouge"; Truffle::Interop.method(:unbox)' ]} do: [ :block | block value ]
			displayingProgress: [ :block | 'Initializing RougeStyler in Ruby...' ]
			every: 100 ]