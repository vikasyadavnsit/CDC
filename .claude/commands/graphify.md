Run graphify commands on the CDC codebase knowledge graph.

Usage:
- `/graphify query "<question>"` — search the knowledge graph for an answer
- `/graphify path "<A>" "<B>"` — find relationships between two nodes
- `/graphify explain "<concept>"` — get a focused explanation of a concept
- `/graphify update` — re-index the graph after code changes

When invoked, run the appropriate `graphify` CLI command based on the argument provided by the user. If no argument is given, run `graphify query "$ARGUMENTS"` using the provided input.

Execute: `graphify $ARGUMENTS`