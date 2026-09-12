---
name: compose-screen
description: Recipe for building a new screen in EasyTrain the standard way — typed route, Hilt ViewModel with a single StateFlow<UiState>, stateless Composable with previews, navigation wiring, string resources, and the mandatory ViewModel (Turbine) and Robolectric UI tests. Use for every new screen, tab, or bottom sheet.
argument-hint: "[ScreenName] [feature-module]"
---

# Build a Compose screen

Screen: `$0` · Module: `feature/$1`

Read `.claude/standards.md` §2 (Compose) and §6 (Testing) first. A screen is five files and two
tests. Don't merge them into one file and don't add extra layers.

## Files

```
feature/$1/src/main/kotlin/com/easytrain/feature/$1/
  $0Navigation.kt      # route object + NavGraphBuilder/NavController extensions
  $0ViewModel.kt       # @HiltViewModel, one StateFlow<$0UiState>, event functions
  $0UiState.kt         # sealed interface or data class (see below)
  $0Screen.kt          # $0Route (stateful wrapper) + $0Screen (stateless) + previews
  res/values/strings.xml  # this module's strings only (prefix keys with $1_)
feature/$1/src/test/kotlin/.../$0ViewModelTest.kt
feature/$1/src/test/kotlin/.../$0ScreenTest.kt      # Robolectric + compose test rule
```

## Navigation

```kotlin
@Serializable data class $0Route(val id: String)          // or data object when no args

fun NavController.navigateTo$0(id: String, navOptions: NavOptions? = null) =
    navigate($0Route(id), navOptions)

fun NavGraphBuilder.$0Screen(onBack: () -> Unit, onOpenX: (String) -> Unit) {
    composable<$0Route> { $0Route(onBack = onBack, onOpenX = onOpenX) }
}
```
The ViewModel reads args with `savedStateHandle.toRoute<$0Route>()`. Screens never receive a
`NavController`; `app` passes lambdas.

## UiState

Pick one:
- **Sealed** (`Loading` / `Success(data)` / `Error(AppError)`) when the screen is useless without data.
- **Data class** with nullable sections when partial rendering matters (Today, WorkoutLogger, TraineeDetail tabs).

Everything the Composable needs is precomputed here (formatted weights via `UnitsFormatter`,
relative dates, adherence fractions). Composables format nothing.

## ViewModel

```kotlin
@HiltViewModel
class $0ViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: XRepository,
) : ViewModel() {
    private val args = savedStateHandle.toRoute<$0Route>()

    val state: StateFlow<$0UiState> = repository.observeX(args.id)
        .map { $0UiState.Success(it.toUi()) as $0UiState }
        .catch { emit($0UiState.Error(it.toAppError())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), $0UiState.Loading)

    fun onSomething(id: String) { viewModelScope.launch { repository.save(...) } }
}
```
Reads come from Room flows; writes call a repository `suspend` function and return immediately
(offline-first — no loading spinner for local writes). Transient one-shot effects (snackbar,
navigate-after-save) use a `Channel<Effect>` exposed as `receiveAsFlow()`.

## Composable

- `$0Route(viewModel = hiltViewModel(), …)` collects with `collectAsStateWithLifecycle()` and passes state + lambdas down.
- `$0Screen(state, on…)` is `@Composable internal fun`, stateless, uses `EasyTrainScaffold` from `core/ui`, theme tokens only, `LazyColumn` keys, `contentDescription` on icon buttons.
- Previews: one per meaningful state, using builders from `core/testing` (`previewWorkout()`); wrap in `EasyTrainTheme`.
- Empty and error states are real UI (`EmptyState` / `ErrorBanner` from `core/ui`) with an action, not a bare `Text`.

## Tests (both mandatory)

```kotlin
class $0ViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val repository = FakeXRepository()

    @Test fun `emits success once repository has data`() = runTest {
        val vm = $0ViewModel(SavedStateHandle(mapOf("id" to "w1")), repository)
        vm.state.test {
            assertEquals($0UiState.Loading, awaitItem())
            repository.emit(testX(id = "w1"))
            assertTrue(awaitItem() is $0UiState.Success)
        }
    }
}
```
`$0ScreenTest` (Robolectric, `@Config(sdk = [34])`, `createComposeRule()`): asserts loading, empty, content and error states render the expected texts/`testTag`s, and that tapping the primary action invokes its lambda.

## Wire-up checklist

- [ ] `app/…/EasyTrainNavHost.kt`: call `$0Screen(...)` in the correct role graph
- [ ] module `build.gradle.kts` only adds `implementation(projects.core.…)` — never another feature
- [ ] strings added to this module's `strings.xml`
- [ ] `ktlintFormat` run; `testDebugUnitTest` green for the module
- [ ] `PROJECT_MANIFEST.md` → feature section updated
