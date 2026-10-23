# Module audit and native smoke

`distribution/module-audit.json` contains one row per module in the 79-module registry inventory.
Regenerate it with `python3 tools/module_audit.py`. References identify source candidates in Fabric
and Forge 1.8.9, including inactive Stonecutter branches; they do not assert that those hooks run.
The audit lists settings, availability, dependencies, persistence paths, activation/deactivation,
target references, known limits and separate source/unit/native evidence. `fullyCertified` remains
false: neither source presence nor a successful state toggle proves complete gameplay behavior.

Run the opt-in Fabric development harness using the existing native runner infrastructure:

```sh
vendor/trs-client/gradlew -p vendor/trs-client :fabric:26.3:runClient -PtrsVersions=26.3 -PtrsAutotest -PtrsAutotestOnly=certification
```

It creates/reopens `tfl-module-certification-26.3`, waits for an integrated world/player, and
sequentially enables/disables available offline modules with eight client ticks in each state.
It restores every tested module's complete config. Services `trsOnline`, `social` and `clips`
are skipped. The actual runtime availability gate controls optional bundled optimizations.
Fixed infrastructure (`userToggleable=false`, currently title/menu style) is recorded as settings
only; its always-active contract is not treated as a failed deactivation.
This is an adapter smoke, not input-driven feature acceptance or complete module certification.
Exceptions escaping renderer/tick hooks may crash the process before the runner can catch them;
an unfinished JSON report is a failure/incomplete run, never success.

Additional checks cover ConfigStore writing to a separate scratch file, Gson decode and registry
apply; temporary waypoint creation/removal verified through separate disk loads; and survival of
Core, HUD editor, crosshair editor and waypoint list screens. Screenshots are requested using
AutoTest's normal screenshot helper. Screen identity/survival is asserted; visual quality, pointer
input, editor dragging, actual HUD content, camera restoration and screenshot write completion
remain separate checks. No client restart, multiplayer, hardware GPU performance or Forge native
runtime is certified by this Fabric run.

The report is `<gameDirectory>/module-certification-<minecraft>.json`. Require terminal status
`smoke-complete`, all checks passing, all applicable module rows complete, plus normal process exit;
do not treat a process exit alone as a pass. The 240-second wall-clock watchdog requests normal
shutdown, marks the run timed out and gives the game 30 seconds to respond. Only an unresponsive
development process is hard stopped; that report explicitly says restoration is not guaranteed.
Normal completion restores the registry/config parts, removes its temporary waypoint, restores
pause-on-lost-focus, saves configuration and uses the existing disconnect plus `Minecraft.stop()`.
Use a clean isolated development game directory. User configs are never replaced by the scratch
config; shutdown applies the pre-run snapshot. Scratch config and test world are retained as evidence.

Attach a real run to the audit (this never upgrades gameplay certification automatically):

```sh
python3 tools/module_audit.py --runtime /path/to/module-certification-26.3.json
```

Common regression tests in `ModuleCertificationRegressionTest` separately verify every registered
module's full config after toggle/restore and save/load, including non-default zoom/HUD settings,
and the independence of unavailable online features from saved enabled state. The audit generator
does not execute these tests or claim their result. Native build/run and common tests must be
recorded by the release validation process for the exact tested revision.

The saved dev.3 state run preceded the final recording-availability guard. Its recorded availability is historical runtime evidence; the final policy is verified by HudAvailabilityTest and a separate production capture. State-smoke results are not promoted to complete gameplay certification.
