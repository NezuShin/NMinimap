package su.nezushin.nminimap.util;

import org.bukkit.Color;
import org.bukkit.map.MapPalette;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class ColorUtil {

    private static final Map<Integer, Byte> colorMap;

    private static final Map<Color, Byte> colorDistanceCache = new ConcurrentHashMap<>();


    public static byte exactColor(@NotNull Color color) {
        return colorMap.get(color.asRGB());
    }

    /**
     * Clear getNearestColor's cache
     */
    public static void clearColorCache(){
        colorDistanceCache.clear();
    }

    /**
     * Similar to deprecated MapPalette.matchColor(). Also caches colors to increase performance. Cache can be cleared using ColorUtil.clearColorCache()
     */
    public static byte getNearestColor(Color color) {
        if (color.getAlpha() < 128) return 0;
        if (colorDistanceCache.containsKey(color))
            return colorDistanceCache.get(color);

        int index = 0;
        double best = -1;

        for (int i = 4; i < colors.length; i++) {
            double distance = getDistance(color, colors[i]);
            if (distance < best || best == -1) {
                best = distance;
                index = i;
            }
        }

        // Minecraft has 248 colors, some of which have negative byte representations
        var c = (byte) (index < 128 ? index : -129 + (index - 127));

        colorDistanceCache.put(color, c);
        return c;
    }

    private static double getDistance(@NotNull Color c1, @NotNull Color c2) {
        // Paper start - Optimize color distance calculation by removing floating point math
        int rsum = c1.getRed() + c2.getRed(); // Use sum instead of mean for no division
        int r = c1.getRed() - c2.getRed();
        int g = c1.getGreen() - c2.getGreen();
        int b = c1.getBlue() - c2.getBlue();
        // All weights are 512x their original to avoid floating point division
        int weightR = 1024 + rsum;
        int weightG = 2048;
        int weightB = 1024 + (255 * 2 - rsum);

        // Division by 256 here is unnecessary as this won't change the result of the sort
        return weightR * r * r + weightG * g * g + weightB * b * b;
        // Paper end
    }

    public static final Color[] colors = {
            // Start generate - MapPalette#colors
            Color.fromRGB(0x00000000),
            Color.fromRGB(0x00000000),
            Color.fromRGB(0x00000000),
            Color.fromRGB(0x00000000),
            Color.fromRGB(0x597D27),
            Color.fromRGB(0x6D9930),
            Color.fromRGB(0x7FB238),
            Color.fromRGB(0x435E1D),
            Color.fromRGB(0xAEA473),
            Color.fromRGB(0xD5C98C),
            Color.fromRGB(0xF7E9A3),
            Color.fromRGB(0x827B56),
            Color.fromRGB(0x8C8C8C),
            Color.fromRGB(0xABABAB),
            Color.fromRGB(0xC7C7C7),
            Color.fromRGB(0x696969),
            Color.fromRGB(0xB40000),
            Color.fromRGB(0xDC0000),
            Color.fromRGB(0xFF0000),
            Color.fromRGB(0x870000),
            Color.fromRGB(0x7070B4),
            Color.fromRGB(0x8A8ADC),
            Color.fromRGB(0xA0A0FF),
            Color.fromRGB(0x545487),
            Color.fromRGB(0x757575),
            Color.fromRGB(0x909090),
            Color.fromRGB(0xA7A7A7),
            Color.fromRGB(0x585858),
            Color.fromRGB(0x005700),
            Color.fromRGB(0x006A00),
            Color.fromRGB(0x007C00),
            Color.fromRGB(0x004100),
            Color.fromRGB(0xB4B4B4),
            Color.fromRGB(0xDCDCDC),
            Color.fromRGB(0xFFFFFF),
            Color.fromRGB(0x878787),
            Color.fromRGB(0x737681),
            Color.fromRGB(0x8D909E),
            Color.fromRGB(0xA4A8B8),
            Color.fromRGB(0x565861),
            Color.fromRGB(0x6A4C36),
            Color.fromRGB(0x825E42),
            Color.fromRGB(0x976D4D),
            Color.fromRGB(0x4F3928),
            Color.fromRGB(0x4F4F4F),
            Color.fromRGB(0x606060),
            Color.fromRGB(0x707070),
            Color.fromRGB(0x3B3B3B),
            Color.fromRGB(0x2D2DB4),
            Color.fromRGB(0x3737DC),
            Color.fromRGB(0x4040FF),
            Color.fromRGB(0x212187),
            Color.fromRGB(0x645432),
            Color.fromRGB(0x7B663E),
            Color.fromRGB(0x8F7748),
            Color.fromRGB(0x4B3F26),
            Color.fromRGB(0xB4B1AC),
            Color.fromRGB(0xDCD9D3),
            Color.fromRGB(0xFFFCF5),
            Color.fromRGB(0x878581),
            Color.fromRGB(0x985924),
            Color.fromRGB(0xBA6D2C),
            Color.fromRGB(0xD87F33),
            Color.fromRGB(0x72431B),
            Color.fromRGB(0x7D3598),
            Color.fromRGB(0x9941BA),
            Color.fromRGB(0xB24CD8),
            Color.fromRGB(0x5E2872),
            Color.fromRGB(0x486C98),
            Color.fromRGB(0x5884BA),
            Color.fromRGB(0x6699D8),
            Color.fromRGB(0x365172),
            Color.fromRGB(0xA1A124),
            Color.fromRGB(0xC5C52C),
            Color.fromRGB(0xE5E533),
            Color.fromRGB(0x79791B),
            Color.fromRGB(0x599011),
            Color.fromRGB(0x6DB015),
            Color.fromRGB(0x7FCC19),
            Color.fromRGB(0x436C0D),
            Color.fromRGB(0xAA5974),
            Color.fromRGB(0xD06D8E),
            Color.fromRGB(0xF27FA5),
            Color.fromRGB(0x804357),
            Color.fromRGB(0x353535),
            Color.fromRGB(0x414141),
            Color.fromRGB(0x4C4C4C),
            Color.fromRGB(0x282828),
            Color.fromRGB(0x6C6C6C),
            Color.fromRGB(0x848484),
            Color.fromRGB(0x999999),
            Color.fromRGB(0x515151),
            Color.fromRGB(0x35596C),
            Color.fromRGB(0x416D84),
            Color.fromRGB(0x4C7F99),
            Color.fromRGB(0x284351),
            Color.fromRGB(0x592C7D),
            Color.fromRGB(0x6D3699),
            Color.fromRGB(0x7F3FB2),
            Color.fromRGB(0x43215E),
            Color.fromRGB(0x24357D),
            Color.fromRGB(0x2C4199),
            Color.fromRGB(0x334CB2),
            Color.fromRGB(0x1B285E),
            Color.fromRGB(0x483524),
            Color.fromRGB(0x58412C),
            Color.fromRGB(0x664C33),
            Color.fromRGB(0x36281B),
            Color.fromRGB(0x485924),
            Color.fromRGB(0x586D2C),
            Color.fromRGB(0x667F33),
            Color.fromRGB(0x36431B),
            Color.fromRGB(0x6C2424),
            Color.fromRGB(0x842C2C),
            Color.fromRGB(0x993333),
            Color.fromRGB(0x511B1B),
            Color.fromRGB(0x111111),
            Color.fromRGB(0x151515),
            Color.fromRGB(0x191919),
            Color.fromRGB(0x0D0D0D),
            Color.fromRGB(0xB0A836),
            Color.fromRGB(0xD7CD42),
            Color.fromRGB(0xFAEE4D),
            Color.fromRGB(0x847E28),
            Color.fromRGB(0x409A96),
            Color.fromRGB(0x4FBCB7),
            Color.fromRGB(0x5CDBD5),
            Color.fromRGB(0x307370),
            Color.fromRGB(0x345AB4),
            Color.fromRGB(0x3F6EDC),
            Color.fromRGB(0x4A80FF),
            Color.fromRGB(0x274387),
            Color.fromRGB(0x009928),
            Color.fromRGB(0x00BB32),
            Color.fromRGB(0x00D93A),
            Color.fromRGB(0x00721E),
            Color.fromRGB(0x5B3C22),
            Color.fromRGB(0x6F4A2A),
            Color.fromRGB(0x815631),
            Color.fromRGB(0x442D19),
            Color.fromRGB(0x4F0100),
            Color.fromRGB(0x600100),
            Color.fromRGB(0x700200),
            Color.fromRGB(0x3B0100),
            Color.fromRGB(0x937C71),
            Color.fromRGB(0xB4988A),
            Color.fromRGB(0xD1B1A1),
            Color.fromRGB(0x6E5D55),
            Color.fromRGB(0x703919),
            Color.fromRGB(0x89461F),
            Color.fromRGB(0x9F5224),
            Color.fromRGB(0x542B13),
            Color.fromRGB(0x693D4C),
            Color.fromRGB(0x804B5D),
            Color.fromRGB(0x95576C),
            Color.fromRGB(0x4E2E39),
            Color.fromRGB(0x4F4C61),
            Color.fromRGB(0x605D77),
            Color.fromRGB(0x706C8A),
            Color.fromRGB(0x3B3949),
            Color.fromRGB(0x835D19),
            Color.fromRGB(0xA0721F),
            Color.fromRGB(0xBA8524),
            Color.fromRGB(0x624613),
            Color.fromRGB(0x485225),
            Color.fromRGB(0x58642D),
            Color.fromRGB(0x677535),
            Color.fromRGB(0x363D1C),
            Color.fromRGB(0x703637),
            Color.fromRGB(0x8A4243),
            Color.fromRGB(0xA04D4E),
            Color.fromRGB(0x542829),
            Color.fromRGB(0x281C18),
            Color.fromRGB(0x31231E),
            Color.fromRGB(0x392923),
            Color.fromRGB(0x1E1512),
            Color.fromRGB(0x5F4B45),
            Color.fromRGB(0x745C54),
            Color.fromRGB(0x876B62),
            Color.fromRGB(0x473833),
            Color.fromRGB(0x3D4040),
            Color.fromRGB(0x4B4F4F),
            Color.fromRGB(0x575C5C),
            Color.fromRGB(0x2E3030),
            Color.fromRGB(0x56333E),
            Color.fromRGB(0x693E4B),
            Color.fromRGB(0x7A4958),
            Color.fromRGB(0x40262E),
            Color.fromRGB(0x352B40),
            Color.fromRGB(0x41354F),
            Color.fromRGB(0x4C3E5C),
            Color.fromRGB(0x282030),
            Color.fromRGB(0x352318),
            Color.fromRGB(0x412B1E),
            Color.fromRGB(0x4C3223),
            Color.fromRGB(0x281A12),
            Color.fromRGB(0x35391D),
            Color.fromRGB(0x414624),
            Color.fromRGB(0x4C522A),
            Color.fromRGB(0x282B16),
            Color.fromRGB(0x642A20),
            Color.fromRGB(0x7A3327),
            Color.fromRGB(0x8E3C2E),
            Color.fromRGB(0x4B1F18),
            Color.fromRGB(0x1A0F0B),
            Color.fromRGB(0x1F120D),
            Color.fromRGB(0x251610),
            Color.fromRGB(0x130B08),
            Color.fromRGB(0x852122),
            Color.fromRGB(0xA3292A),
            Color.fromRGB(0xBD3031),
            Color.fromRGB(0x641919),
            Color.fromRGB(0x682C44),
            Color.fromRGB(0x7F3653),
            Color.fromRGB(0x943F61),
            Color.fromRGB(0x4E2133),
            Color.fromRGB(0x401114),
            Color.fromRGB(0x4F1519),
            Color.fromRGB(0x5C191D),
            Color.fromRGB(0x300D0F),
            Color.fromRGB(0x0F585E),
            Color.fromRGB(0x126C73),
            Color.fromRGB(0x167E86),
            Color.fromRGB(0x0B4246),
            Color.fromRGB(0x286462),
            Color.fromRGB(0x327A78),
            Color.fromRGB(0x3A8E8C),
            Color.fromRGB(0x1E4B4A),
            Color.fromRGB(0x3C1F2B),
            Color.fromRGB(0x4A2535),
            Color.fromRGB(0x562C3E),
            Color.fromRGB(0x2D1720),
            Color.fromRGB(0x0E7F5D),
            Color.fromRGB(0x119B72),
            Color.fromRGB(0x14B485),
            Color.fromRGB(0x0A5F46),
            Color.fromRGB(0x464646),
            Color.fromRGB(0x565656),
            Color.fromRGB(0x646464),
            Color.fromRGB(0x343434),
            Color.fromRGB(0x987B67),
            Color.fromRGB(0xBA967E),
            Color.fromRGB(0xD8AF93),
            Color.fromRGB(0x725C4D),
            Color.fromRGB(0x597569),
            Color.fromRGB(0x6D9081),
            Color.fromRGB(0x7FA796),
            Color.fromRGB(0x43584F),
    };

    static {
        colorMap = new ConcurrentHashMap<>();

        for (var i = 0; i < colors.length; i++) {
            colorMap.put(colors[i].asRGB(), (byte) (i < 128 ? i : -129 + (i - 127)));
        }
    }
}
