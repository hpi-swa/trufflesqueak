# TruffleSqueak Documentation

- [Polyglot API Reference](polyglot-api.md)
- [Image Creation](images.md)

## Comparing SistaV1 interpreters

The JVM startup property `--vm.Dtrufflesqueak.UseBytecodeDSL=true` (the default)
selects the Bytecode DSL interpreter. Set it to `false` to use the manually
written interpreter for both method calls and context resumptions.
For interpreter performance comparisons, include
`--engine.Compilation=false --experimental-options` in both runs.

Please feel free to open a [pull request][pull_request] if you'd like to
contribute.

[pull_request]: https://help.github.com/en/github/collaborating-with-issues-and-pull-requests/creating-a-pull-request
