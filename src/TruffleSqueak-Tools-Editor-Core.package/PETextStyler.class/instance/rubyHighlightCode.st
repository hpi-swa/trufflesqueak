private
rubyHighlightCode

	^ '
require "rouge"
source = Polyglot.import("polyglot-editor:code")
lexerName = Polyglot.import("polyglot-editor:lexerName")

lexer = Rouge::Lexer.find(lexerName).new
theme = Rouge::Theme.find("base16.solarized")

pos = 1
lexer.lex(source.to_s).map { |token, text| 
	style = theme.get_style(token)
	[pos, pos += text.size, style.fg()]
}.reject { | triple | triple[2].nil? }'