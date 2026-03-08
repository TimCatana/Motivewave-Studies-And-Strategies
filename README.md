# MotiveWave Custom Studies & Strategies

Flexible project structure for building custom MotiveWave studies and strategies.

## Project Structure

```
motivewave-indicators/
├── src/
│   ├── custom_studies/                      ← Chart analysis indicators
│   │   └── SegmentedInitialBalance.java
│   └── custom_strategies/                   ← Automated trading strategies
│       └── BracketAverager.java
├── bin/
│   └── motivewave-indicators.jar            ← Ready-to-use JAR
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

This will compile all studies and strategies into a single JAR file.

### Manual Build with javac
```bash
mkdir -p bin/classes
javac -d bin/classes -cp "lib/mwave_sdk.jar" -encoding UTF-8 -source 11 -target 11 \
  src/custom_studies/*.java src/custom_strategies/*.java
jar cvf bin/motivewave-indicators.jar -C bin/classes .
```

### Build with Ant (if installed)
```bash
ant build    # Compile studies and strategies
ant jar      # Compile and package into JAR
ant clean    # Remove build artifacts
```

## Deployment to MotiveWave

1. Build the project: `./build.sh`
2. Copy `bin/motivewave-indicators.jar` to MotiveWave's user indicators directory
3. Restart MotiveWave
4. Access studies in: **Indicators** menu
5. Access strategies in: **Strategies** panel

> **Each study/strategy is loaded individually** — You can enable/disable them independently in MotiveWave's UI.

## Adding New Studies or Strategies

### Create a New Study
1. Create a new file in `src/custom_studies/MyStudy.java`
2. Extend `Study` class
3. Implement required methods: `initialize()`, `calculate()`
4. Use `@StudyHeader` annotation (set `strategy=false` or omit)
5. Run: `./build.sh`

### Create a New Strategy
1. Create a new file in `src/custom_strategies/MyStrategy.java`
2. Extend `Study` class
3. Implement required methods + order management hooks
4. Use `@StudyHeader` annotation with `strategy=true`
5. Implement: `onActivate()`, `onDeactivate()`, `onOrderFilled()`, etc.
6. Run: `./build.sh`

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
