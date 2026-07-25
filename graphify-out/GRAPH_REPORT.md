# Graph Report - CDC  (2026-05-17)

## Corpus Check
- 95 files · ~29,981 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 823 nodes · 1133 edges · 103 communities (64 shown, 39 thin omitted)
- Extraction: 85% EXTRACTED · 15% INFERRED · 0% AMBIGUOUS · INFERRED: 168 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `e791df85`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- [[_COMMUNITY_Community 0|Community 0]]
- [[_COMMUNITY_Community 1|Community 1]]
- [[_COMMUNITY_Community 2|Community 2]]
- [[_COMMUNITY_Community 3|Community 3]]
- [[_COMMUNITY_Community 4|Community 4]]
- [[_COMMUNITY_Community 5|Community 5]]
- [[_COMMUNITY_Community 6|Community 6]]
- [[_COMMUNITY_Community 7|Community 7]]
- [[_COMMUNITY_Community 8|Community 8]]
- [[_COMMUNITY_Community 9|Community 9]]
- [[_COMMUNITY_Community 10|Community 10]]
- [[_COMMUNITY_Community 11|Community 11]]
- [[_COMMUNITY_Community 12|Community 12]]
- [[_COMMUNITY_Community 13|Community 13]]
- [[_COMMUNITY_Community 14|Community 14]]
- [[_COMMUNITY_Community 15|Community 15]]
- [[_COMMUNITY_Community 16|Community 16]]
- [[_COMMUNITY_Community 17|Community 17]]
- [[_COMMUNITY_Community 18|Community 18]]
- [[_COMMUNITY_Community 19|Community 19]]
- [[_COMMUNITY_Community 20|Community 20]]
- [[_COMMUNITY_Community 21|Community 21]]
- [[_COMMUNITY_Community 22|Community 22]]
- [[_COMMUNITY_Community 23|Community 23]]
- [[_COMMUNITY_Community 24|Community 24]]
- [[_COMMUNITY_Community 25|Community 25]]
- [[_COMMUNITY_Community 26|Community 26]]
- [[_COMMUNITY_Community 27|Community 27]]
- [[_COMMUNITY_Community 28|Community 28]]
- [[_COMMUNITY_Community 29|Community 29]]
- [[_COMMUNITY_Community 30|Community 30]]
- [[_COMMUNITY_Community 31|Community 31]]
- [[_COMMUNITY_Community 32|Community 32]]
- [[_COMMUNITY_Community 33|Community 33]]
- [[_COMMUNITY_Community 34|Community 34]]
- [[_COMMUNITY_Community 35|Community 35]]
- [[_COMMUNITY_Community 36|Community 36]]
- [[_COMMUNITY_Community 37|Community 37]]
- [[_COMMUNITY_Community 38|Community 38]]
- [[_COMMUNITY_Community 39|Community 39]]
- [[_COMMUNITY_Community 40|Community 40]]
- [[_COMMUNITY_Community 41|Community 41]]
- [[_COMMUNITY_Community 42|Community 42]]
- [[_COMMUNITY_Community 43|Community 43]]
- [[_COMMUNITY_Community 44|Community 44]]
- [[_COMMUNITY_Community 45|Community 45]]
- [[_COMMUNITY_Community 46|Community 46]]
- [[_COMMUNITY_Community 47|Community 47]]
- [[_COMMUNITY_Community 48|Community 48]]
- [[_COMMUNITY_Community 49|Community 49]]
- [[_COMMUNITY_Community 50|Community 50]]
- [[_COMMUNITY_Community 51|Community 51]]
- [[_COMMUNITY_Community 52|Community 52]]
- [[_COMMUNITY_Community 53|Community 53]]
- [[_COMMUNITY_Community 54|Community 54]]
- [[_COMMUNITY_Community 55|Community 55]]
- [[_COMMUNITY_Community 56|Community 56]]
- [[_COMMUNITY_Community 57|Community 57]]
- [[_COMMUNITY_Community 58|Community 58]]
- [[_COMMUNITY_Community 59|Community 59]]
- [[_COMMUNITY_Community 60|Community 60]]
- [[_COMMUNITY_Community 61|Community 61]]
- [[_COMMUNITY_Community 62|Community 62]]
- [[_COMMUNITY_Community 63|Community 63]]
- [[_COMMUNITY_Community 64|Community 64]]
- [[_COMMUNITY_Community 65|Community 65]]
- [[_COMMUNITY_Community 66|Community 66]]
- [[_COMMUNITY_Community 67|Community 67]]
- [[_COMMUNITY_Community 68|Community 68]]
- [[_COMMUNITY_Community 69|Community 69]]
- [[_COMMUNITY_Community 70|Community 70]]
- [[_COMMUNITY_Community 71|Community 71]]
- [[_COMMUNITY_Community 72|Community 72]]
- [[_COMMUNITY_Community 73|Community 73]]
- [[_COMMUNITY_Community 74|Community 74]]
- [[_COMMUNITY_Community 75|Community 75]]
- [[_COMMUNITY_Community 76|Community 76]]
- [[_COMMUNITY_Community 77|Community 77]]
- [[_COMMUNITY_Community 78|Community 78]]
- [[_COMMUNITY_Community 79|Community 79]]
- [[_COMMUNITY_Community 80|Community 80]]
- [[_COMMUNITY_Community 81|Community 81]]
- [[_COMMUNITY_Community 82|Community 82]]
- [[_COMMUNITY_Community 83|Community 83]]
- [[_COMMUNITY_Community 84|Community 84]]
- [[_COMMUNITY_Community 85|Community 85]]
- [[_COMMUNITY_Community 86|Community 86]]
- [[_COMMUNITY_Community 87|Community 87]]
- [[_COMMUNITY_Community 88|Community 88]]
- [[_COMMUNITY_Community 89|Community 89]]
- [[_COMMUNITY_Community 90|Community 90]]
- [[_COMMUNITY_Community 91|Community 91]]
- [[_COMMUNITY_Community 92|Community 92]]

## God Nodes (most connected - your core abstractions)
1. `FirebaseUtils` - 26 edges
2. `ActionUtils` - 18 edges
3. `LibrariesForLibs` - 16 edges
4. `LibrariesForLibsInPluginsBlock` - 16 edges
5. `CDC — Comprehensive Device Capture` - 16 edges
6. `CDCSensorService` - 15 edges
7. `VersionAccessors` - 14 edges
8. `VersionAccessors` - 14 edges
9. `Device-side data capture (triggered by `ClickActions` + system services)` - 14 edges
10. `AccessibilityNotificationFragment` - 13 edges

## Surprising Connections (you probably didn't know these)
- `LibrariesForLibs` --extends--> `AbstractExternalDependencyFactory`  [EXTRACTED]
  .gradle/8.6/dependencies-accessors/4c5b35e16e9bfbb088aed62217a0be3d2825e0c1/sources/org/gradle/accessors/dm/LibrariesForLibs.java →   _Bridges community 15 → community 13_
- `LibrariesForLibsInPluginsBlock` --extends--> `AbstractExternalDependencyFactory`  [EXTRACTED]
  .gradle/8.6/dependencies-accessors/4c5b35e16e9bfbb088aed62217a0be3d2825e0c1/sources/org/gradle/accessors/dm/LibrariesForLibsInPluginsBlock.java →   _Bridges community 13 → community 16_
- `ComposeLibraryAccessors` --extends--> `SubDependencyFactory`  [EXTRACTED]
  .gradle/8.6/dependencies-accessors/4c5b35e16e9bfbb088aed62217a0be3d2825e0c1/sources/org/gradle/accessors/dm/LibrariesForLibsInPluginsBlock.java →   _Bridges community 13 → community 12_
- `ExtLibraryAccessors` --extends--> `SubDependencyFactory`  [EXTRACTED]
  .gradle/8.6/dependencies-accessors/4c5b35e16e9bfbb088aed62217a0be3d2825e0c1/sources/org/gradle/accessors/dm/LibrariesForLibsInPluginsBlock.java →   _Bridges community 13 → community 81_
- `VersionAccessors` --extends--> `VersionFactory`  [EXTRACTED]
  .gradle/8.6/dependencies-accessors/4c5b35e16e9bfbb088aed62217a0be3d2825e0c1/sources/org/gradle/accessors/dm/LibrariesForLibs.java →   _Bridges community 19 → community 17_

## Communities (103 total, 39 thin omitted)

### Community 0 - "Community 0"
Cohesion: 0.06
Nodes (11): PasswordActivity, Activity, Fragment, ClickActionsFragment, DashboardFragment, MessageFragment, MonitorFragment, OfflineClickActionsFragment (+3 more)

### Community 1 - "Community 1"
Cohesion: 0.09
Nodes (6): AppContext, CDCFileReader, CDCOrganisedFileAppender, CDCUnorganisedFileAppender, CryptoUtils, MessageUtils

### Community 2 - "Community 2"
Cohesion: 0.12
Nodes (3): AccessibilityUtils, run(), FirebaseUtils

### Community 3 - "Community 3"
Cohesion: 0.08
Nodes (5): AppUsageReportData, SpinnerItem, SystemAppUsageStatisticsFragment, AppUsageStats, FileExplorer

### Community 4 - "Community 4"
Cohesion: 0.11
Nodes (3): KeyStrokesFragment, SettingsFragment, CommonUtil

### Community 5 - "Community 5"
Cohesion: 0.1
Nodes (5): SensorEventListener, Service, CDCSensorService, ResetService, ScreenshotService

### Community 6 - "Community 6"
Cohesion: 0.07
Nodes (29): All trigger keys, appUsageReport, callLogs, cdc/flatUserDetails/\<androidId\>, cdc/shayari, cdc/users/\<androidId\>/appSettings/appTriggerSettingsDataMap, cdc/users/\<androidId\>/userDeviceData, code:block1 (cdc/) (+21 more)

### Community 7 - "Community 7"
Cohesion: 0.11
Nodes (6): MainActivity, AppCompatActivity, MyModule, PermissionHandler, CDCVpnService, VpnService

### Community 8 - "Community 8"
Cohesion: 0.08
Nodes (25): App overview (what a user experiences), App usage (two paths), Business logic & data flow (high level), Cloud messaging (Firebase Cloud Messaging), Contacts / SMS / call logs, Core navigation UI, Device admin & battery settings, Device-side data capture (triggered by `ClickActions` + system services) (+17 more)

### Community 9 - "Community 9"
Cohesion: 0.1
Nodes (20): Admin viewer, Architecture overview, Build & run, Capture (device side), CDC — Comprehensive Device Capture, code:block1 (┌──────────────────────────────────────────────────────┐), code:block2 (cdc/), Contributing (+12 more)

### Community 10 - "Community 10"
Cohesion: 0.15
Nodes (4): MessageDialog, RegexInputFilter, AccessibilityNotificationFragment, InputFilter

### Community 12 - "Community 12"
Cohesion: 0.11
Nodes (7): BundleFactory, BundleAccessors, BundleAccessors, ComposeLibraryAccessors, ComposePreviewLibraryAccessors, EspressoLibraryAccessors, WorkLibraryAccessors

### Community 13 - "Community 13"
Cohesion: 0.14
Nodes (7): AbstractExternalDependencyFactory, ComposeLibraryAccessors, ComposePreviewLibraryAccessors, EspressoLibraryAccessors, ExtLibraryAccessors, WorkLibraryAccessors, SubDependencyFactory

### Community 14 - "Community 14"
Cohesion: 0.22
Nodes (3): getPermissionTypeByRequestCode(), PermissionManager, LoggerUtils

### Community 18 - "Community 18"
Cohesion: 0.15
Nodes (5): AndroidPluginAccessors, PluginAccessors, AndroidPluginAccessors, PluginAccessors, PluginFactory

### Community 20 - "Community 20"
Cohesion: 0.17
Nodes (3): CallStateListener, CallStateListenerImpl, CallUtils

### Community 25 - "Community 25"
Cohesion: 0.18
Nodes (10): applicationId, artifactType, kind, type, baselineProfiles, elements, elementType, minSdkVersionForDexing (+2 more)

### Community 26 - "Community 26"
Cohesion: 0.28
Nodes (3): Connect-Wifi(), Get-UsbDevice(), Get-WifiDevice()

### Community 27 - "Community 27"
Cohesion: 0.25
Nodes (7): client, configuration_version, project_info, firebase_url, project_id, project_number, storage_bucket

### Community 29 - "Community 29"
Cohesion: 0.32
Nodes (3): BroadcastReceiver, ResetBroadcastReceiver, StatisticsBroadcastReceiver

### Community 30 - "Community 30"
Cohesion: 0.29
Nodes (6): 10, android_version, version_code, 31, android_version, version_code

### Community 34 - "Community 34"
Cohesion: 0.4
Nodes (4): AppSettings, AppTriggerSettingsData, User, UserDeviceData

### Community 45 - "Community 45"
Cohesion: 0.67
Nodes (3): 11, android_version, version_code

### Community 46 - "Community 46"
Cohesion: 0.67
Nodes (3): 12, android_version, version_code

### Community 47 - "Community 47"
Cohesion: 0.67
Nodes (3): 13, android_version, version_code

### Community 48 - "Community 48"
Cohesion: 0.67
Nodes (3): 14, android_version, version_code

### Community 49 - "Community 49"
Cohesion: 0.67
Nodes (3): 15, android_version, version_code

### Community 50 - "Community 50"
Cohesion: 0.67
Nodes (3): 16, android_version, version_code

### Community 51 - "Community 51"
Cohesion: 0.67
Nodes (3): 17, android_version, version_code

### Community 52 - "Community 52"
Cohesion: 0.67
Nodes (3): 18, android_version, version_code

### Community 53 - "Community 53"
Cohesion: 0.67
Nodes (3): 19, android_version, version_code

### Community 54 - "Community 54"
Cohesion: 0.67
Nodes (3): 1, android_version, version_code

### Community 55 - "Community 55"
Cohesion: 0.67
Nodes (3): 2, android_version, version_code

### Community 56 - "Community 56"
Cohesion: 0.67
Nodes (3): 20, android_version, version_code

### Community 57 - "Community 57"
Cohesion: 0.67
Nodes (3): 21, android_version, version_code

### Community 58 - "Community 58"
Cohesion: 0.67
Nodes (3): 22, android_version, version_code

### Community 59 - "Community 59"
Cohesion: 0.67
Nodes (3): 23, android_version, version_code

### Community 60 - "Community 60"
Cohesion: 0.67
Nodes (3): 24, android_version, version_code

### Community 61 - "Community 61"
Cohesion: 0.67
Nodes (3): 25, android_version, version_code

### Community 62 - "Community 62"
Cohesion: 0.67
Nodes (3): 26, android_version, version_code

### Community 63 - "Community 63"
Cohesion: 0.67
Nodes (3): 27, android_version, version_code

### Community 64 - "Community 64"
Cohesion: 0.67
Nodes (3): 28, android_version, version_code

### Community 65 - "Community 65"
Cohesion: 0.67
Nodes (3): 29, android_version, version_code

### Community 66 - "Community 66"
Cohesion: 0.67
Nodes (3): 30, android_version, version_code

### Community 67 - "Community 67"
Cohesion: 0.67
Nodes (3): 32, android_version, version_code

### Community 68 - "Community 68"
Cohesion: 0.67
Nodes (3): 33, android_version, version_code

### Community 69 - "Community 69"
Cohesion: 0.67
Nodes (3): 34, android_version, version_code

### Community 70 - "Community 70"
Cohesion: 0.67
Nodes (3): 3, android_version, version_code

### Community 71 - "Community 71"
Cohesion: 0.67
Nodes (3): 4, android_version, version_code

### Community 72 - "Community 72"
Cohesion: 0.67
Nodes (3): 5, android_version, version_code

### Community 73 - "Community 73"
Cohesion: 0.67
Nodes (3): 6, android_version, version_code

### Community 74 - "Community 74"
Cohesion: 0.67
Nodes (3): 7, android_version, version_code

### Community 75 - "Community 75"
Cohesion: 0.67
Nodes (3): 8, android_version, version_code

### Community 76 - "Community 76"
Cohesion: 0.67
Nodes (3): 9, android_version, version_code

## Knowledge Gaps
- **150 isolated node(s):** `PreToolUse`, `allow`, `project_number`, `firebase_url`, `project_id` (+145 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **39 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `FirebaseUtils` connect `Community 2` to `Community 0`?**
  _High betweenness centrality (0.019) - this node is a cross-community bridge._
- **Why does `ActionUtils` connect `Community 24` to `Community 3`, `Community 4`, `Community 7`, `Community 10`, `Community 11`, `Community 21`, `Community 29`?**
  _High betweenness centrality (0.018) - this node is a cross-community bridge._
- **What connects `PreToolUse`, `allow`, `project_number` to the rest of the system?**
  _150 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.06 - nodes in this community are weakly interconnected._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.09 - nodes in this community are weakly interconnected._
- **Should `Community 2` be split into smaller, more focused modules?**
  _Cohesion score 0.12 - nodes in this community are weakly interconnected._
- **Should `Community 3` be split into smaller, more focused modules?**
  _Cohesion score 0.08 - nodes in this community are weakly interconnected._