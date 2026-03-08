# MotiveWave Custom Studies & Strategies

Flexible project structure for building custom MotiveWave studies and strategies.

## Sample Project:

https://www.motivewave.com/sdk.htm

## Project Structure

```
motivewave-indicators/
├── src/
│   ├── custom_studies/                      ← Chart analysis indicators
│   │   └── SegmentedInitialBalance.java
│   └── custom_strategies/                   ← Automated trading strategies
│       └── BracketAverager.java
├── bin/
│   ├── SegmentedInitialBalance.jar          ← Deploy individually
│   └── BracketAverager.jar                  ← Deploy individually
├── lib/
│   └── mwave_sdk.jar                        ← MotiveWave SDK
├── build.sh                                 ← Bash build script (recommended)
├── build.xml                                ← Ant build configuration
└── README.md
```

## Studies vs Strategies

| Aspect | Studies | Strategies |
|--------|---------|-----------|
| **Purpose** | Display values/analysis on charts | Automate trading decisions |
| **Base Class** | Study | Study (with `strategy=true`) |
| **Context** | `DataContext` | `OrderContext` |
| **Can Trade** | ❌ No | ✅ Yes |
| **Example** | Initial Balance lines | Bracket Averager |

### Custom Studies
- Display technical analysis indicators on charts
- Calculate values from price data
- No order management capability
- Located in: `src/custom_studies/`

**Example: SegmentedInitialBalance**
- Displays historical Initial Balance ranges
- Shows 1.5x and 2.0x extension levels
- Customizable time periods

### Custom Strategies
- Execute trades automatically based on logic
- Manage orders, positions, and risk
- Located in: `src/custom_strategies/`

**Example: BracketAverager**
- Automatically manages bracket orders (SL + TP)
- Updates quantity on position changes
- Maintains fixed SL/TP prices while averaging

## Building

### Quick Build (Recommended)
```bash
./build.sh
```

This compiles each study and strategy into its **own JAR file** in `bin/`:
- `SegmentedInitialBalance.jar`
- `BracketAverager.jar`

### Manual Build with javac
```bash
for file in src/custom_studies/*.java src/custom_strategies/*.java; do
  class_name=$(basename $file .java)
  mkdir -p bin/.temp/$class_name
  javac -d bin/.temp/$class_name -cp "lib/mwave_sdk.jar" -encoding UTF-8 $file
  jar cvf bin/$class_name.jar -C bin/.temp/$class_name .
done
rm -rf bin/.temp
```

### Build with Ant (if installed)
```bash
ant build      # Compile studies and strategies into separate JARs
ant clean      # Remove all JAR files
```

## Deployment to MotiveWave

Each study/strategy is its **own JAR file** — deploy only what you need:

1. Build the project: `./build.sh`
2. Copy **individual JAR files** to MotiveWave's indicators directory:
   - Copy `SegmentedInitialBalance.jar` → MotiveWave indicators
   - Copy `BracketAverager.jar` → MotiveWave indicators
   - Or copy both!
3. Restart MotiveWave
4. Enable indicators/strategies in MotiveWave's UI

> **Flexibility**: You can load any combination of indicators and strategies independently.

## Adding New Studies or Strategies

### Create a New Study
1. Create a new file in `src/custom_studies/MyStudy.java`
2. Extend `Study` class
3. Implement required methods: `initialize()`, `calculate()`
4. Use `@StudyHeader` annotation (set `strategy=false` or omit)
5. Run: `./build.sh`
6. A new `MyStudy.jar` will be created in `bin/`

### Create a New Strategy
1. Create a new file in `src/custom_strategies/MyStrategy.java`
2. Extend `Study` class
3. Implement required methods + order management hooks
4. Use `@StudyHeader` annotation with `strategy=true`
5. Implement: `onActivate()`, `onDeactivate()`, `onOrderFilled()`, etc.
6. Run: `./build.sh`
7. A new `MyStrategy.jar` will be created in `bin/`

**Each new file automatically gets its own JAR file** — Deploy only the ones you need!

## SDK Documentation

- **Study API**: https://www.motivewave.com/sdk/javadoc/com/motivewave/platform/sdk/study/Study.html
- **StudyHeader**: https://www.motivewave.com/sdk/javadoc/com/motivewave/platform/sdk/study/StudyHeader.html
- **Order Management**: https://www.motivewave.com/sdk/javadoc/com/motivewave/platform/sdk/order_mgmt/OrderContext.html

## Included Indicators

### SegmentedInitialBalance (Study)
Displays Initial Balance with historical tracking and extension levels.
- **Namespace**: `custom`
- **ID**: `SEGMENTED_IB`
- **Settings**:
  - IB Start/End times
  - Timezone
  - Max historical prints
  - Show 1.5x and 2.0x extensions

### BracketAverager (Strategy)
Automatically manages bracket orders when averaging into positions.
- **Namespace**: `custom_strategies`
- **ID**: `BRACKET_AVERAGER`
- **Features**:
  - Auto-attaches bracket when strategy activated
  - Updates bracket on partial fills
  - Maintains fixed SL/TP prices
  - Supports long and short positions
- **Settings**:
  - Stop Loss offset (in ticks)
  - Take Profit offset (in ticks)
