## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

### Wiki pages (graphify-out/wiki/)
- **index.md** — full module map, god-node table, key data-flow summary — start here
- **firebase.md** — FirebaseUtils API, RTDB path tree, upload/read-back flows
- **ui.md** — MainActivity, bottom-nav, SettingsFragment, fragment-Firebase mapping
- **capture.md** — CDCSensorService, ScreenshotService, AccessibilityUtils, capture pipelines
- **data-models.md** — User, AppSettings, AppTriggerSettingsData, ClickActions enum, FileMap enum
- **services.md** — all background services, BroadcastReceivers, VPN service, interaction map
- **file-io.md** — CDCFileReader, CDCOrganisedFileAppender, CDCUnorganisedFileAppender, CryptoUtils

### God nodes (most connected — touch these carefully)
| Class | Edges | Role |
|-------|-------|------|
| `FirebaseUtils` | 26 | Central RTDB gateway — all uploads and reads |
| `ActionUtils` | 18 | Action dispatcher — bridges Firebase callbacks to services/UI |
| `CDCSensorService` | 15 | Foreground sensor capture service |
| `AccessibilityNotificationFragment` | 13 | Admin viewer for captured notifications |

### Rules
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- For broad navigation, read graphify-out/wiki/index.md before browsing raw source files.
- For module-specific questions, go directly to the relevant wiki page above.
- Read graphify-out/GRAPH_REPORT.md only for architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).