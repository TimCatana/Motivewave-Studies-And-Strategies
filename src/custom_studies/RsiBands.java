package custom_studies;

import com.motivewave.platform.sdk.common.DataContext;
import com.motivewave.platform.sdk.common.DataSeries;
import com.motivewave.platform.sdk.common.Defaults;
import com.motivewave.platform.sdk.common.Enums;
import com.motivewave.platform.sdk.common.desc.IntegerDescriptor;
import com.motivewave.platform.sdk.common.desc.PathDescriptor;
import com.motivewave.platform.sdk.common.desc.ShadeDescriptor;
import com.motivewave.platform.sdk.common.desc.ValueDescriptor;
import com.motivewave.platform.sdk.study.Study;
import com.motivewave.platform.sdk.study.StudyHeader;

import java.awt.Color;

/**
 * RSI Bands — price-space overlay.
 *
 * Converts RSI overbought/oversold thresholds back to price coordinates using
 * Wilder's smoothing (RMA / SMMA), matching the TradingView rsi-bands.pine
 * indicator exactly.
 *
 * Four bands are plotted directly on the price chart:
 *   Upper Band  — RSI overbought inner level  (reversal threshold)
 *   Upper Tail  — RSI overbought outer level  (tail threshold)
 *   Lower Band  — RSI oversold  inner level
 *   Lower Tail  — RSI oversold  outer level
 *
 * The region between Upper Tail and Upper Band is shaded orange.
 * The region between Lower Band and Lower Tail is shaded cyan.
 */
@StudyHeader(
    namespace = "custom",
    id        = "RSI_BANDS",
    label     = "RSI Bands",
    name      = "RSI Bands",
    desc      = "Price-space RSI overbought/oversold bands with Wilder smoothing",
    menu      = "Custom",
    overlay   = true,
    studyOverlay = true
)
public class RsiBands extends Study
{
    // -------------------------------------------------------------------------
    // Series value keys
    // -------------------------------------------------------------------------
    enum Values
    {
        RSI_MIDDLE,       // RMA(close, rsiLength)
        RSI_CLOSE_VOL,    // RMA(|close - close[1]|, rsiLength)
        UPPER_BAND_RAW,
        LOWER_BAND_RAW,
        UPPER_TAIL_RAW,
        LOWER_TAIL_RAW,
        UPPER_BAND,       // RMA(UPPER_BAND_RAW, smoothLength)
        LOWER_BAND,
        UPPER_TAIL,
        LOWER_TAIL
    }

    // -------------------------------------------------------------------------
    // Settings keys
    // -------------------------------------------------------------------------
    private static final String RSI_LENGTH    = "rsiLength";
    private static final String SMOOTH_LENGTH = "smoothLength";

    // -------------------------------------------------------------------------
    // Path / shade keys
    // -------------------------------------------------------------------------
    private static final String PATH_UPPER_BAND = "pathUpperBand";
    private static final String PATH_UPPER_TAIL = "pathUpperTail";
    private static final String PATH_LOWER_BAND = "pathLowerBand";
    private static final String PATH_LOWER_TAIL = "pathLowerTail";
    private static final String PATH_MIDDLE     = "pathMiddle";
    private static final String SHADE_UPPER     = "shadeUpper";
    private static final String SHADE_LOWER     = "shadeLower";

    // Matching the Pine Script default colors
    private static final Color COLOR_UPPER_LINE = new Color(0xFF, 0x6D, 0x00); // #FF6D00
    private static final Color COLOR_UPPER_FILL = new Color(0xFF, 0xB7, 0x4D); // #FFB74D
    private static final Color COLOR_LOWER_LINE = new Color(0x00, 0xBC, 0xD4); // #00BCD4
    private static final Color COLOR_LOWER_FILL = new Color(0x7F, 0xDB, 0xFF); // #7FDBFF

    // -------------------------------------------------------------------------
    // Fixed math thresholds (same as Pine Script)
    //   reversalThreshold = sqrt(3)
    //   tailThreshold     = sqrt((5 + sqrt(17)) / 2)
    // -------------------------------------------------------------------------
    private static final double REVERSAL_THRESHOLD = Math.sqrt(3.0);
    private static final double TAIL_THRESHOLD     = Math.sqrt((5.0 + Math.sqrt(17.0)) / 2.0);
    private static final double EPSILON            = 1e-10;

    // Cached per-length threshold constants (recomputed only when rsiLength changes)
    private int    cachedRsiLength      = -1;
    private double cachedInvSqrtN;
    private double cachedOverboughtInner;
    private double cachedOverboughtOuter;
    private double cachedOversoldInner;
    private double cachedOversoldOuter;

    // -------------------------------------------------------------------------
    // initialize
    // -------------------------------------------------------------------------
    @Override
    public void initialize(Defaults defaults)
    {
        var sd = createSD();

        // --- Settings tab ---
        var tab = sd.addTab("General");
        var settingsGroup = tab.addGroup("RSI Waves Settings");
        settingsGroup.addRow(new IntegerDescriptor(RSI_LENGTH,    "RSI Length",     14, 2,  500, 1));
        settingsGroup.addRow(new IntegerDescriptor(SMOOTH_LENGTH, "Band Smoothing",  8, 1,   50, 1));

        // --- Display tab ---
        tab = sd.addTab("Display");
        var upperGroup = tab.addGroup("Upper Bands (Overbought)");
        upperGroup.addRow(new PathDescriptor(PATH_UPPER_BAND, "Upper Band", COLOR_UPPER_LINE, 2.0f, null, true, true, false));
        upperGroup.addRow(new PathDescriptor(PATH_UPPER_TAIL, "Upper Tail", COLOR_UPPER_FILL, 1.0f, null, true, true, false));
        upperGroup.addRow(new ShadeDescriptor(SHADE_UPPER, "Upper Fill", PATH_UPPER_TAIL, PATH_UPPER_BAND,
                Enums.ShadeType.BOTH, COLOR_UPPER_FILL, true, true));

        var lowerGroup = tab.addGroup("Lower Bands (Oversold)");
        lowerGroup.addRow(new PathDescriptor(PATH_LOWER_BAND, "Lower Band", COLOR_LOWER_LINE, 2.0f, null, true, true, false));
        lowerGroup.addRow(new PathDescriptor(PATH_LOWER_TAIL, "Lower Tail", COLOR_LOWER_FILL, 1.0f, null, true, true, false));
        lowerGroup.addRow(new ShadeDescriptor(SHADE_LOWER, "Lower Fill", PATH_LOWER_BAND, PATH_LOWER_TAIL,
                Enums.ShadeType.BOTH, COLOR_LOWER_FILL, true, true));

        var middleGroup = tab.addGroup("RSI 50 Level");
        middleGroup.addRow(new PathDescriptor(PATH_MIDDLE, "RSI Middle (50)", defaults.getGrey(), 1.0f,
                new float[]{4, 4}, true, false, false));

        // --- Result descriptor ---
        var desc = createRD();
        desc.exportValue(new ValueDescriptor(Values.UPPER_BAND,  "RSI Upper Band",   new String[]{}));
        desc.exportValue(new ValueDescriptor(Values.UPPER_TAIL,  "RSI Upper Tail",   new String[]{}));
        desc.exportValue(new ValueDescriptor(Values.LOWER_BAND,  "RSI Lower Band",   new String[]{}));
        desc.exportValue(new ValueDescriptor(Values.LOWER_TAIL,  "RSI Lower Tail",   new String[]{}));
        desc.exportValue(new ValueDescriptor(Values.RSI_MIDDLE,  "RSI Middle (50)",  new String[]{}));

        desc.declarePath(Values.UPPER_BAND,  PATH_UPPER_BAND);
        desc.declarePath(Values.UPPER_TAIL,  PATH_UPPER_TAIL);
        desc.declarePath(Values.LOWER_BAND,  PATH_LOWER_BAND);
        desc.declarePath(Values.LOWER_TAIL,  PATH_LOWER_TAIL);
        desc.declarePath(Values.RSI_MIDDLE,  PATH_MIDDLE);

        desc.setRangeKeys(Values.UPPER_BAND, Values.UPPER_TAIL, Values.LOWER_BAND, Values.LOWER_TAIL);
    }

    // -------------------------------------------------------------------------
    // calculate
    // -------------------------------------------------------------------------
    @Override
    protected void calculate(int index, DataContext ctx)
    {
        if (index < 1) return;

        DataSeries series = ctx.getDataSeries();

        int    rsiLength    = getSettings().getInteger(RSI_LENGTH,    14);
        int    smoothLength = getSettings().getInteger(SMOOTH_LENGTH,  8);
        double rmaAlpha     = 1.0 / rsiLength;

        double close     = series.getClose(index);
        double prevClose = series.getClose(index - 1);

        // --- RMA(close, rsiLength) → rsiMiddle ---
        double rsiMiddle = rma(series, index, Values.RSI_MIDDLE, close, rmaAlpha);
        series.setDouble(index, Values.RSI_MIDDLE, rsiMiddle);

        // --- RMA(|close - close[1]|, rsiLength) → rsiCloseVol ---
        double absDiff = Math.abs(close - prevClose);
        double rsiCloseVol = rma(series, index, Values.RSI_CLOSE_VOL, absDiff, rmaAlpha);
        series.setDouble(index, Values.RSI_CLOSE_VOL, rsiCloseVol);

        // --- Denominator: max(rsiCloseVol, ε) * (rsiLength - 1) ---
        double rsiCloseDenominator = Math.max(rsiCloseVol, EPSILON) * (rsiLength - 1.0);

        // --- Threshold constants: cached per rsiLength (only depend on length, not price) ---
        if (rsiLength != cachedRsiLength) {
            cachedRsiLength      = rsiLength;
            cachedInvSqrtN       = 1.0 / Math.sqrt(Math.max(rsiLength - 1.0, 1.0));
            cachedOverboughtInner = 50.0 + 50.0 * Math.tanh(REVERSAL_THRESHOLD * cachedInvSqrtN);
            cachedOverboughtOuter = 50.0 + 50.0 * Math.tanh(TAIL_THRESHOLD     * cachedInvSqrtN);
            cachedOversoldInner   = 50.0 - 50.0 * Math.tanh(REVERSAL_THRESHOLD * cachedInvSqrtN);
            cachedOversoldOuter   = 50.0 - 50.0 * Math.tanh(TAIL_THRESHOLD     * cachedInvSqrtN);
        }

        // --- price_at_rsi: rsiMiddle + ((rsiValue - 50) / 50) * rsiCloseDenominator ---
        double upperBandRaw = priceAtRsi(cachedOverboughtInner, rsiMiddle, rsiCloseDenominator);
        double upperTailRaw = priceAtRsi(cachedOverboughtOuter, rsiMiddle, rsiCloseDenominator);
        double lowerBandRaw = priceAtRsi(cachedOversoldInner,   rsiMiddle, rsiCloseDenominator);
        double lowerTailRaw = priceAtRsi(cachedOversoldOuter,   rsiMiddle, rsiCloseDenominator);

        series.setDouble(index, Values.UPPER_BAND_RAW, upperBandRaw);
        series.setDouble(index, Values.UPPER_TAIL_RAW, upperTailRaw);
        series.setDouble(index, Values.LOWER_BAND_RAW, lowerBandRaw);
        series.setDouble(index, Values.LOWER_TAIL_RAW, lowerTailRaw);

        // --- RMA smoothing of final output bands ---
        double smoothAlpha = 1.0 / smoothLength;
        series.setDouble(index, Values.UPPER_BAND,  rma(series, index, Values.UPPER_BAND,  upperBandRaw, smoothAlpha));
        series.setDouble(index, Values.UPPER_TAIL,  rma(series, index, Values.UPPER_TAIL,  upperTailRaw, smoothAlpha));
        series.setDouble(index, Values.LOWER_BAND,  rma(series, index, Values.LOWER_BAND,  lowerBandRaw, smoothAlpha));
        series.setDouble(index, Values.LOWER_TAIL,  rma(series, index, Values.LOWER_TAIL,  lowerTailRaw, smoothAlpha));
        // RSI 50 price level = rsiMiddle (already computed above, just needs its path written)
        series.setDouble(index, Values.RSI_MIDDLE,  rsiMiddle);

        series.setComplete(index);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Wilder's RMA (SMMA): rma[i] = rma[i-1] + alpha * (src - rma[i-1])
     * Seeds with the raw value on the first available bar.
     */
    private double rma(DataSeries series, int index, Values key, double rawValue, double alpha)
    {
        Double prev = series.getDouble(index - 1, key);
        if (prev == null || Double.isNaN(prev)) return rawValue;
        return prev + alpha * (rawValue - prev);
    }

    /** Converts an RSI level back to its price equivalent. */
    private double priceAtRsi(double rsiValue, double rsiMiddle, double rsiCloseDenominator)
    {
        return rsiMiddle + ((rsiValue - 50.0) / 50.0) * rsiCloseDenominator;
    }
}
