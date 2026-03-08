package custom_studies;

import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.common.desc.*;
import com.motivewave.platform.sdk.study.*;
import java.util.*;

@StudyHeader(
    namespace="custom", id="SEGMENTED_IB", label="Segmented IB", 
    name="Segmented Initial Balance",
    desc="Tracks daily initial balance high and low",
    menu="Custom", overlay=true
)
public class SegmentedInitialBalance extends Study 
{
    enum Values { IB_HIGH, IB_LOW };
    
    private int lastDayKey = -1;
    private double currentHigh = Double.NaN;
    private double currentLow = Double.NaN;
    private int sessionStartIndex = -1;
    private boolean sessionActive = false;

    @Override
    public void initialize(Defaults defaults) 
    {
        var sd = createSD();
        var tab = sd.addTab("General");

        var sessGroup = tab.addGroup("Session");
        sessGroup.addRow(new StringDescriptor("IB_START", "IB Start Time (HHMMSS)", "093000"));
        sessGroup.addRow(new StringDescriptor("IB_END", "IB End Time (HHMMSS)", "103000"));
        
        tab = sd.addTab("Display");
        var ibGroup = tab.addGroup("IB Lines");
        ibGroup.addRow(new PathDescriptor(Inputs.TOP_PATH, "IB High", defaults.getGreen(), 2.0f, null, true, false, true));
        ibGroup.addRow(new PathDescriptor(Inputs.BOTTOM_PATH, "IB Low", defaults.getRed(), 2.0f, null, true, false, true));

        var desc = createRD();
        desc.exportValue(new ValueDescriptor(Values.IB_HIGH, "IB High", new String[]{}));
        desc.exportValue(new ValueDescriptor(Values.IB_LOW, "IB Low", new String[]{}));
        desc.declarePath(Values.IB_HIGH, Inputs.TOP_PATH);
        desc.declarePath(Values.IB_LOW, Inputs.BOTTOM_PATH);
    }

    @Override
    protected void calculate(int index, DataContext ctx) 
    {
        if (index < 1) return;

        DataSeries series = ctx.getDataSeries();
        long barTime = series.getStartTime(index);

        // Get session times
        String ibStartStr = getSettings().getString("IB_START", "093000");
        String ibEndStr = getSettings().getString("IB_END", "103000");
        int ibStart = Integer.parseInt(ibStartStr);
        int ibEnd = Integer.parseInt(ibEndStr);

        // Get current bar time in HHMMSS format
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(barTime);
        int dayKey = cal.get(Calendar.YEAR) * 10000 + cal.get(Calendar.DAY_OF_YEAR);
        
        int barHour = cal.get(Calendar.HOUR_OF_DAY);
        int barMin = cal.get(Calendar.MINUTE);
        int barSec = cal.get(Calendar.SECOND);
        int barTimeHHMMSS = barHour * 10000 + barMin * 100 + barSec;

        boolean isInSession = barTimeHHMMSS >= ibStart && barTimeHHMMSS < ibEnd;

        // New day detected
        if (dayKey != lastDayKey) {
            lastDayKey = dayKey;
            currentHigh = Double.NaN;
            currentLow = Double.NaN;
            sessionActive = false;
            sessionStartIndex = -1;
        }

        // Track session high/low
        if (isInSession) {
            if (!sessionActive) {
                // Session just started
                sessionActive = true;
                sessionStartIndex = index;
                currentHigh = series.getHigh(index);
                currentLow = series.getLow(index);
            } else {
                // Update high/low
                currentHigh = Math.max(currentHigh, series.getHigh(index));
                currentLow = Math.min(currentLow, series.getLow(index));
            }
        } else if (sessionActive) {
            // Session ended
            sessionActive = false;
        }

        // Set values for all bars in the range
        if (sessionStartIndex >= 0 && !Double.isNaN(currentHigh) && !Double.isNaN(currentLow)) {
            series.setDouble(index, Values.IB_HIGH, currentHigh);
            series.setDouble(index, Values.IB_LOW, currentLow);
        }
    }
}
